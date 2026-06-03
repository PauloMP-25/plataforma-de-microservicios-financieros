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

from datetime import datetime
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
    PerfilUsuario,
    InsightAnalitico
)
from app.servicios.ia.coach_ia import CoachIA
from app.utilidades.preparador_datos import json_a_dataframe
from app.utilidades.excepciones import HistorialInsuficienteError, LimiteDiarioExcedidoError
from app.clientes.luka_clients import obtener_cliente_financiero, obtener_cliente_perfil
from app.persistencia.cache_redis import CacheRedis

from app.servicios.fabrica_modulos import FabricaModulosAnalisis
from app.servicios.mappers import MapperContextoIA
from app.utilidades.auditoria_decorator import auditar_operacion

logger = logging.getLogger(__name__)

class ServicioAnalisis:
    def __init__(self) -> None:
        self._cliente_financiero = obtener_cliente_financiero()
        self._cliente_perfil = obtener_cliente_perfil()
        self._coach = CoachIA()
        self._cache_redis = CacheRedis()

    @auditar_operacion("MODULO_ANALISIS")

    async def procesar_modulo(
        self, 
        modulo_enum: NombreModulo, 
        peticion: PeticionBase, 
        ip: str, 
        **kwargs
    ) -> RespuestaModulo:
        """
        Método ÚNICO y GENÉRICO para procesar cualquier módulo (FASE 5 y 7).
        """
        try:
            # 1. Verificar si la cuota ya está agotada (sin incrementarla todavía)
            # Esto evita llamadas HTTP si el usuario ya no tiene cuota
            tipo_pool = "clasificacion" if modulo_enum == NombreModulo.AUTO_CLASIFICACION else "analitica"
            rol_upper = kwargs.get("rol", "FREE").upper()
            if tipo_pool == "analitica":
                limite = 20 if rol_upper == "PRO" else (50 if rol_upper == "PREMIUM" else 5)
            else:
                limite = 10 if rol_upper == "PRO" else (20 if rol_upper == "PREMIUM" else 3)
            
            fecha_actual = datetime.now()
            anio_act, semana_act, _ = fecha_actual.isocalendar()
            clave_cuota = f"ia:cuota:{tipo_pool}:{peticion.usuario_id}:{anio_act}:W{semana_act}"
            
            cuota_actual = self._cache_redis.obtener_cuota_actual(clave_cuota)
            if cuota_actual >= limite:
                raise LimiteDiarioExcedidoError(
                    mensaje=f"Has agotado tus {limite} consultas semanales de {tipo_pool}. Tu plan {rol_upper} se renueva el próximo lunes."
                )

            # 2. Obtener módulo de la Fábrica (Lazy Loading FASE 7)
            servicio = FabricaModulosAnalisis.obtener_modulo(modulo_enum)

            # 3. Concurrencia HTTP (asyncio.gather) — FASE 5
            mes = getattr(peticion, 'mes', None)
            anio = getattr(peticion, 'anio', None)
            dia_inicio = getattr(peticion, 'dia_inicio', None)
            mes_inicio = getattr(peticion, 'mes_inicio', None)
            anio_inicio = getattr(peticion, 'anio_inicio', None)
            dia_fin = getattr(peticion, 'dia_fin', None)
            mes_fin = getattr(peticion, 'mes_fin', None)
            anio_fin = getattr(peticion, 'anio_fin', None)
            
            tarea_financiera = self._cliente_financiero.obtener_historial_transacciones_async(
                peticion.usuario_id, 
                peticion.token, 
                peticion.tamanio_pagina, 
                mes, 
                anio,
                dia_inicio,
                mes_inicio,
                anio_inicio,
                dia_fin,
                mes_fin,
                anio_fin
            )
            tarea_perfil = self._cliente_perfil.obtener_perfil_usuario_async(
                peticion.usuario_id, peticion.token
            )
            
            # Esperamos ambas llamadas HTTP a la vez (reduce latencia un 50%)
            resp_financiero, dict_perfil = await asyncio.gather(tarea_financiera, tarea_perfil)
            
            # Convertimos respuestas a formatos de trabajo
            datos_raw = resp_financiero.get("datos", [])
            df = json_a_dataframe(datos_raw)
            contexto = ContextoEstrategicoIADTO.model_validate(dict_perfil)
            contexto.rol = kwargs.get("rol", "FREE")

            # 4. Hash estable de transacciones reales + descripción de rango
            datos_normalizados = sorted([json.dumps(t, sort_keys=True) for t in datos_raw])
            hash_txs = hashlib.sha256(
                json.dumps(datos_normalizados).encode()
            ).hexdigest()[:16]

            descripcion_rango = _construir_descripcion_rango(mes, anio, mes_inicio, anio_inicio, mes_fin, anio_fin)
            clave_firma = f"ia:firma:{peticion.usuario_id}:{modulo_enum.value}:{descripcion_rango}:{hash_txs}"

            # 5. Detección de consulta duplicada
            descripcion_previa = self._cache_redis.obtener_firma(clave_firma)
            if descripcion_previa:
                logger.info("[CACHE-HIT-FIRMA] Consulta duplicada detectada para usuario=%s modulo=%s rango=%s", peticion.usuario_id, modulo_enum.value, descripcion_rango)
                total_txs = len(df) if not df.empty else 0
                total_ing = float(df[df['tipo'] == 'INGRESO']['monto'].sum()) if not df.empty else 0.0
                total_gas = float(df[df['tipo'] == 'GASTO']['monto'].sum()) if not df.empty else 0.0
                
                insight_dto = InsightAnalitico(
                    modulo=modulo_enum,
                    total_transacciones_analizadas=total_txs,
                    total_ingresos=total_ing,
                    total_gastos=total_gas,
                    balance_neto=round(total_ing - total_gas, 2),
                    hallazgos={"consulta_duplicada": True, "rango": descripcion_rango}
                )
                return RespuestaModulo(
                    usuario_id=peticion.usuario_id,
                    modulo=modulo_enum,
                    consejo=(
                        f"Ya realizaste esta consulta de **{modulo_enum.value.replace('_', ' ').title()}** "
                        f"para el período **{descripcion_rango}** y los datos financieros no han cambiado. "
                        f"Te recomiendo consultar un rango de fechas diferente o registrar nuevas "
                        f"transacciones para obtener un análisis actualizado."
                    ),
                    estado_coach=EstadoCoach.EXITOSO,
                    insight=insight_dto,
                    usando_fallback=False,
                )

            # 6. Lógica Analítica + Prompting
            logger.info("[CACHE-MISS-FIRMA] Ejecutando motor analítico y Gemini para usuario=%s", peticion.usuario_id)
            metricas = servicio.ejecutar_calculos(df, contexto, **kwargs)

            # Calcular totales financieros antes de invocar la IA, para inyectar en métricas para el fallback
            total_txs = len(df) if not df.empty else 0
            total_ing = float(df[df['tipo'] == 'INGRESO']['monto'].sum()) if not df.empty else 0.0
            total_gas = float(df[df['tipo'] == 'GASTO']['monto'].sum()) if not df.empty else 0.0

            metricas["_total_ingresos"] = total_ing
            metricas["_total_gastos"] = total_gas

            prompt = servicio.orquestar_prompt(metricas, contexto)

            # 7. Ejecución en Coach IA (que verifica e incrementa la cuota)
            consejo, estado, fallback = await self._coach.obtener_consejo_ia(
                peticion.usuario_id, 
                modulo_enum, 
                prompt, 
                metricas, 
                contexto.rol, 
                contexto.nombres, 
                contexto=contexto
            )

            insight_dto = InsightAnalitico(
                modulo=modulo_enum,
                total_transacciones_analizadas=total_txs,
                total_ingresos=total_ing,
                total_gastos=total_gas,
                balance_neto=round(total_ing - total_gas, 2),
                hallazgos=metricas
            )

            resultado_final = RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=consejo,
                estado_coach=estado,
                insight=insight_dto,
                usando_fallback=fallback
            )
            
            # Registrar la firma exitosa en Redis (TTL 7 días)
            self._cache_redis.registrar_consulta(clave_firma, descripcion_rango)
            
            return resultado_final

        except HistorialInsuficienteError as e:
            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=str(e),
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                insight=InsightAnalitico(
                    modulo=modulo_enum,
                    hallazgos={"error": "historial_insuficiente"}
                )
            )
        except Exception as e:
            logger.error(f"Fallo crítico en {modulo_enum.value}: {e}", exc_info=True)
            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo="Lo siento, estamos optimizando tu experiencia. Intenta de nuevo en un momento.",
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                insight=InsightAnalitico(
                    modulo=modulo_enum,
                    hallazgos={"error": str(e)}
                )
            )

    # ── Mapeos y Soporte ──────────────────────────────────────────────────────

def obtener_servicio_analisis() -> ServicioAnalisis:
    return ServicioAnalisis()


def _construir_descripcion_rango(mes, anio, mes_inicio, anio_inicio, mes_fin, anio_fin) -> str:
    MESES = {1:"Ene",2:"Feb",3:"Mar",4:"Abr",5:"May",6:"Jun",
             7:"Jul",8:"Ago",9:"Sep",10:"Oct",11:"Nov",12:"Dic"}
    if mes and anio:
        return f"{MESES.get(mes, mes)}-{anio}"
    if mes_inicio and anio_inicio and mes_fin and anio_fin:
        return f"{MESES.get(mes_inicio)}-{anio_inicio}_a_{MESES.get(mes_fin)}-{anio_fin}"
    return "todos-los-periodos"
