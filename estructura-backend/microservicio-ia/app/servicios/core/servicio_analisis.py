"""
servicios/servicio_analisis.py  ·  v6.0 — ARQUITECTURA SOLID
══════════════════════════════════════════════════════════════════════════════
Orquestador Principal — Conecta el Motor Analítico con el Coach IA.

Flujo:
  1. Extrae datos del núcleo financiero (DataFrame).
  2. Obtiene el contexto estratégico del usuario (Perfil).
  3. Ejecuta el módulo correspondiente (Cálculos + Prompt).
  4. Ejecuta el consejo en CoachIA (Caché + Gemini).
  5. Retorna RespuestaModulo.
══════════════════════════════════════════════════════════════════════════════
"""

import logging
from typing import Optional, Any, Dict, Type

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import (
    PeticionBase,
    PeticionConFiltroFecha,
    PeticionSimularMeta,
    RespuestaModulo,
    NombreModulo,
    EstadoCoach,
    PerfilUsuario
)
from app.servicios.ia.coach_ia import CoachIA
from app.utilidades.preparador_datos import json_a_dataframe
from app.utilidades.excepciones import HistorialInsuficienteError
from app.clientes.luka_clients import obtener_cliente_financiero, obtener_cliente_perfil

# Motores Modulares
from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
from app.servicios.modulos.predecir_gastos import PredecirGastosService
from app.servicios.modulos.reporte_completo import ReporteCompletoService
from app.servicios.modulos.habitos_financieros import HabitosFinancierosService
from app.servicios.modulos.simular_meta import SimularMetaService
from app.servicios.modulos.reto_ahorro_dinamico import RetoAhorroDinamicoService
from app.servicios.modulos.analisis_estilo_de_vida import AnalisisEstiloVidaService

logger = logging.getLogger(__name__)

class ServicioAnalisis:
    def __init__(self) -> None:
        self._cliente_financiero = obtener_cliente_financiero()
        self._cliente_perfil = obtener_cliente_perfil()
        self._coach = CoachIA()
        self._publicador = publicador_auditoria
        
        # REGISTRY DE MÓDULOS (Abierto a extensión, cerrado a modificación)
        self._modulos = {
            NombreModulo.GASTO_HORMIGA: DeteccionGastosHormigaService(),
            NombreModulo.PREDECIR_GASTOS: PredecirGastosService(),
            NombreModulo.REPORTE_COMPLETO: ReporteCompletoService(),
            NombreModulo.HABITOS_FINANCIEROS: HabitosFinancierosService(),
            NombreModulo.SIMULAR_META: SimularMetaService(),
            NombreModulo.RETO_AHORRO_DINAMICO: RetoAhorroDinamicoService(),
            NombreModulo.ANALISIS_ESTILO_VIDA: AnalisisEstiloVidaService(),
        }

    async def procesar_modulo(
        self, 
        modulo_enum: NombreModulo, 
        peticion: PeticionBase, 
        ip: str, 
        **kwargs
    ) -> RespuestaModulo:
        """
        Método ÚNICO y GENÉRICO para procesar cualquier módulo.
        Cumple con SRP al delegar la lógica y con OCP al usar el registry.
        """
        try:
            # 1. Auditoría Inmediata (Detección de intención del usuario)
            await self._auditar(peticion.usuario_id, f"SOLICITUD_{modulo_enum.value}", "INICIADO", ip)

            # 2. Obtener la estrategia
            servicio = self._modulos.get(modulo_enum)
            if not servicio:
                raise ValueError(f"Módulo {modulo_enum} no registrado en el sistema.")

            # 3. Preparar contexto y datos
            df = self._obtener_dataframe(peticion)
            perfil_full = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
            contexto = self._mapear_a_contexto(perfil_full)

            # 4. Lógica Analítica + Prompting
            metricas = servicio.ejecutar_calculos(df, contexto, **kwargs)
            prompt = servicio.orquestar_prompt(metricas, contexto)

            # 5. Ejecución en Coach IA
            consejo, estado, fallback = await self._coach.obtener_consejo_ia(
                peticion.usuario_id, modulo_enum, prompt, metricas, contexto.rol
            )

            # 6. Auditoría de éxito
            await self._auditar(peticion.usuario_id, f"SOLICITUD_{modulo_enum.value}", "EXITOSO", ip)

            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=consejo,
                estado_coach=estado,
                hallazgos=metricas,
                usando_fallback=fallback
            )

        except HistorialInsuficienteError as e:
            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=str(e),
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                hallazgos={"error": "historial_insuficiente"}
            )
        except Exception as e:
            logger.error(f"Fallo crítico en {modulo_enum.value}: {e}", exc_info=True)
            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo="Lo siento, estamos optimizando tu experiencia. Intenta de nuevo en un momento.",
                estado_coach=EstadoCoach.ERROR,
                hallazgos={"error": str(e)}
            )

    # ── Mapeos y Soporte ──────────────────────────────────────────────────────

    def _obtener_dataframe(self, peticion: PeticionBase):
        mes = getattr(peticion, 'mes', None)
        anio = getattr(peticion, 'anio', None)
        resp = self._cliente_financiero.obtener_historial_transacciones(
            peticion.usuario_id, peticion.token, peticion.tamanio_pagina, mes, anio
        )
        return json_a_dataframe(resp)

    def _mapear_a_contexto(self, p: PerfilUsuario) -> ContextoEstrategicoIADTO:
        return ContextoEstrategicoIADTO(
            nombres=p.nombre,
            ocupacion=p.ocupacion or "Estudiante",
            ingreso_mensual=p.ingreso_mensual or 0.0,
            nombre_meta_principal=p.meta_ahorro_activa.nombre if p.meta_ahorro_activa else "Ninguna",
            porcentaje_meta_principal=p.meta_ahorro_activa.porcentaje_completado if p.meta_ahorro_activa else 0.0,
            tono_ia=p.configuracion_ia.tono_ia if p.configuracion_ia else "AMIGABLE",
            porcentaje_alerta_gasto=p.configuracion_ia.porcentaje_alerta_gasto if p.configuracion_ia else 80,
            rol=p.rol
        )

    async def _auditar(self, usuario_id: str, accion: str, detalles: str, ip: str):
        try:
            evento = EventoAuditoriaDTO(
                usuario_id=usuario_id, accion=accion, modulo="MS-IA", ip_origen=ip, detalles=detalles
            )
            await self._publicador.publicar_evento(evento)
        except: pass

def obtener_servicio_analisis() -> ServicioAnalisis:
    return ServicioAnalisis()