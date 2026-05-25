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
            # 1. Obtener módulo de la Fábrica (Lazy Loading FASE 7)
            servicio = FabricaModulosAnalisis.obtener_modulo(modulo_enum)

            # 2. Concurrencia HTTP (asyncio.gather) — FASE 5
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
            df = json_a_dataframe(resp_financiero.get("datos", []))
            perfil_full = PerfilUsuario.model_validate(dict_perfil)
            contexto = MapperContextoIA.mapear_perfil(perfil_full)

            # 3. Caché de Resultado Completo — FASE 5
            # Construimos hash único: usuario + modulo + (mes/anio/rango) + ultima transaccion
            if not df.empty:
                ultima_fecha = str(df['fecha'].max())
            else:
                ultima_fecha = "SIN_DATOS"
                
            hash_str = f"{peticion.usuario_id}_{modulo_enum.value}_{mes}_{anio}_{mes_inicio}_{anio_inicio}_{mes_fin}_{anio_fin}_{ultima_fecha}"
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
                peticion.usuario_id, modulo_enum, prompt, metricas, contexto.rol, contexto.nombres
            )

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
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                hallazgos={"error": str(e)}
            )

    # ── Mapeos y Soporte ──────────────────────────────────────────────────────

def obtener_servicio_analisis() -> ServicioAnalisis:
    return ServicioAnalisis()