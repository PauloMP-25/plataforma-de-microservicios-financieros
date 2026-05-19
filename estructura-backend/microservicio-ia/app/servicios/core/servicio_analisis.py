"""
servicios/servicio_analisis.py  ·  v7.0 — FASE 5 (LUKA-COACH V4)
══════════════════════════════════════════════════════════════════════════════
Orquestador Principal — Conecta el Motor Analítico con el Coach IA.

Novedades FASE 5:
  - Latencia reducida mediante `asyncio.gather` para I/O concurrente.
  - Caché de respuesta completa en Redis antes de invocar el motor analítico
    (basada en el hash de datos financieros para invalidación natural).
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
import asyncio
import hashlib
from typing import Optional, Any, Dict, Type

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import (
    PeticionBase,
    RespuestaModulo,
    NombreModulo,
    EstadoCoach,
    PerfilUsuario
)
from app.servicios.ia.coach_ia import CoachIA
from app.utilidades.preparador_datos import json_a_dataframe
from app.utilidades.excepciones import HistorialInsuficienteError
from app.clientes.luka_clients import obtener_cliente_financiero, obtener_cliente_perfil
from app.persistencia.cache_redis import CacheRedis

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
        self._cache_redis = CacheRedis()
        
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
        Método ÚNICO y GENÉRICO para procesar cualquier módulo (FASE 5).
        """
        try:
            # 1. Auditoría Inmediata
            await self._auditar(peticion.usuario_id, f"SOLICITUD_{modulo_enum.value}", "INICIADO", ip)

            servicio = self._modulos.get(modulo_enum)
            if not servicio:
                raise ValueError(f"Módulo {modulo_enum} no registrado en el sistema.")

            # 2. Concurrencia HTTP (asyncio.gather) — FASE 5
            mes = getattr(peticion, 'mes', None)
            anio = getattr(peticion, 'anio', None)
            
            tarea_financiera = self._cliente_financiero.obtener_historial_transacciones_async(
                peticion.usuario_id, peticion.token, peticion.tamanio_pagina, mes, anio
            )
            tarea_perfil = self._cliente_perfil.obtener_perfil_usuario_async(
                peticion.usuario_id, peticion.token
            )
            
            # Esperamos ambas llamadas HTTP a la vez (reduce latencia un 50%)
            resp_financiero, dict_perfil = await asyncio.gather(tarea_financiera, tarea_perfil)
            
            # Convertimos respuestas a formatos de trabajo
            df = json_a_dataframe(resp_financiero.get("datos", []))
            perfil_full = PerfilUsuario.model_validate(dict_perfil)
            contexto = self._mapear_a_contexto(perfil_full)

            # 3. Caché de Resultado Completo — FASE 5
            # Construimos hash único: usuario + modulo + (mes/anio) + ultima transaccion
            if not df.empty:
                ultima_fecha = str(df['fecha'].max())
            else:
                ultima_fecha = "SIN_DATOS"
                
            hash_str = f"{peticion.usuario_id}_{modulo_enum.value}_{mes}_{anio}_{ultima_fecha}"
            hash_unico = hashlib.sha256(hash_str.encode()).hexdigest()
            # La clave incluye el usuario_id para permitir invalidación proactiva fácil con patrón
            clave_cache = f"ia:resultado_completo:{peticion.usuario_id}:{hash_unico}"

            resultado_cacheado = self._cache_redis.obtener(clave_cache)
            if resultado_cacheado:
                logger.info("[CACHE-HIT] Retornando análisis desde caché para usuario=%s modulo=%s", peticion.usuario_id, modulo_enum.value)
                return RespuestaModulo.model_validate_json(resultado_cacheado)

            # 4. Lógica Analítica + Prompting (Solo si no hay caché)
            logger.info("[CACHE-MISS] Ejecutando motor analítico y Gemini para usuario=%s", peticion.usuario_id)
            metricas = servicio.ejecutar_calculos(df, contexto, **kwargs)
            prompt = servicio.orquestar_prompt(metricas, contexto)

            # 5. Ejecución en Coach IA (Circuit Breaker incluido en FASE 3)
            consejo, estado, fallback = await self._coach.obtener_consejo_ia(
                peticion.usuario_id, modulo_enum, prompt, metricas, contexto.rol
            )

            await self._auditar(peticion.usuario_id, f"SOLICITUD_{modulo_enum.value}", "EXITOSO", ip)

            resultado_final = RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=consejo,
                estado_coach=estado,
                hallazgos=metricas,
                usando_fallback=fallback
            )
            
            # Guardamos en caché por 24 horas (invalidación proactiva ocurrirá si cambian datos)
            self._cache_redis.guardar(clave_cache, resultado_final.model_dump_json(), ex=86400)
            
            return resultado_final

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