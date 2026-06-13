"""
servicios/servicio_analisis.py  ·  v8.0 — FASE 4: Memoria de Coaching (LUKA)
══════════════════════════════════════════════════════════════════════════════
Orquestador Principal — Conecta el Motor Analítico con el Coach IA.

Cambios v8 (FASE 4):
  - Consulta IaHistorialCoaching ANTES de ejecutar los cálculos Pandas.
    Si existe historial previo, se inyecta en metricas["_historial_previo"]
    para que el módulo lo incluya en su prompt.
  - GASTO_HORMIGA recibe esquema_salida=ConsejoEstructuradoHormiga en la llamada
    a obtener_consejo_ia → activa Structured Output en Gemini.
    Los otros 9 módulos reciben esquema_salida=None → sin cambios.
  - Después de una respuesta exitosa, persiste la interacción en
    IaHistorialCoaching vía RepositorioHistorialCoaching.guardar().
    Si la escritura falla, se loguea y NO se degrada la experiencia.
  - asyncio.gather para concurrencia HTTP: intacto.
  - Lógica del Motor Analítico (Pandas): intacta.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import asyncio
import hashlib
import json
import logging
from datetime import datetime
from typing import Any, Optional

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import (
    ConsejoEstructuradoHormiga,
    EstadoCoach,
    InsightAnalitico,
    NombreModulo,
    PeticionBase,
    PeticionComparacionDTO,
    RespuestaModulo,
    ConsejoEstructuradoEvolucion,
)
from app.persistencia.redis.cache_redis import CacheRedis
from app.persistencia.postgres.database import SessionLocal
from app.persistencia.postgres.repositorio_historial import RepositorioHistorialCoaching
from app.servicios.core.fabrica_modulos import FabricaModulosAnalisis
from app.servicios.ia.coach_ia import CoachIA
from app.clientes.luka_clients import obtener_cliente_financiero, obtener_cliente_perfil
from app.utilidades.excepciones import HistorialInsuficienteError, LimiteDiarioExcedidoError
from app.utilidades.preparador_datos import json_a_dataframe
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
        **kwargs,
    ) -> RespuestaModulo:
        """
        Método ÚNICO y GENÉRICO para procesar cualquier módulo (v8).

        Flujo ampliado (cambios respecto a v7 marcados con ── NUEVO ──):
          1. Verificar cuota semanal (early exit sin llamadas HTTP).
          2. ── NUEVO ── Consultar historial previo en DB.
          3. Obtener módulo de la Fábrica.
          4. Concurrencia HTTP (asyncio.gather).
          5. Hash estable + detección de consulta duplicada (Redis).
          6. ── NUEVO ── Inyectar historial en metricas.
          7. Lógica Analítica (Pandas) + Prompting.
          8. ── NUEVO ── Activar Structured Output solo para GASTO_HORMIGA.
          9. Ejecución en CoachIA.
          10. ── NUEVO ── Persistir interacción en historial.
          11. Construir y retornar RespuestaModulo.
        """
        try:
            # ── 1. Verificar cuota semanal (early exit) ───────────────────────
            tipo_pool = (
                "clasificacion"
                if modulo_enum == NombreModulo.AUTO_CLASIFICACION
                else "analitica"
            )
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
                    mensaje=(
                        f"Has agotado tus {limite} consultas semanales de {tipo_pool}. "
                        f"Tu plan {rol_upper} se renueva el próximo lunes."
                    )
                )

            # ── 2. NUEVO: Consultar historial previo ──────────────────────────
            # Solo para GASTO_HORMIGA en esta fase.
            # Para los otros módulos historial_previo = None (sin overhead de DB).
            historial_previo = None
            if modulo_enum == NombreModulo.GASTO_HORMIGA:
                historial_previo = await asyncio.to_thread(
                    self._obtener_historial, peticion.usuario_id, modulo_enum.value
                )

            # ── 3. Obtener módulo de la Fábrica ──────────────────────────────
            servicio = FabricaModulosAnalisis.obtener_modulo(modulo_enum)

            # ── 4. Concurrencia HTTP (asyncio.gather) ─────────────────────────
            mes = getattr(peticion, "mes", None)
            anio = getattr(peticion, "anio", None)
            dia_inicio = getattr(peticion, "dia_inicio", None)
            mes_inicio = getattr(peticion, "mes_inicio", None)
            anio_inicio = getattr(peticion, "anio_inicio", None)
            dia_fin = getattr(peticion, "dia_fin", None)
            mes_fin = getattr(peticion, "mes_fin", None)
            anio_fin = getattr(peticion, "anio_fin", None)

            tarea_financiera = self._cliente_financiero.obtener_historial_transacciones_async(
                peticion.usuario_id, peticion.token, peticion.tamanio_pagina,
                mes, anio, dia_inicio, mes_inicio, anio_inicio, dia_fin, mes_fin, anio_fin,
            )
            tarea_perfil = self._cliente_perfil.obtener_perfil_usuario_async(
                peticion.usuario_id, peticion.token
            )

            resp_financiero, dict_perfil = await asyncio.gather(
                tarea_financiera, tarea_perfil
            )

            datos_raw = resp_financiero.get("datos", [])
            df = json_a_dataframe(datos_raw)
            contexto = ContextoEstrategicoIADTO.model_validate(dict_perfil)
            contexto.rol = kwargs.get("rol", "FREE")

            # ── 5. Hash + detección de consulta duplicada (Redis) ─────────────
            datos_normalizados = sorted(
                [json.dumps(t, sort_keys=True) for t in datos_raw]
            )
            hash_txs = hashlib.sha256(
                json.dumps(datos_normalizados).encode()
            ).hexdigest()[:16]

            descripcion_rango = _construir_descripcion_rango(
                mes, anio, mes_inicio, anio_inicio, mes_fin, anio_fin
            )
            clave_firma = (
                f"ia:firma:{peticion.usuario_id}:"
                f"{modulo_enum.value}:{descripcion_rango}:{hash_txs}"
            )

            descripcion_previa = self._cache_redis.obtener_firma(clave_firma)
            if descripcion_previa:
                logger.info(
                    "[CACHE-HIT-FIRMA] Consulta duplicada usuario=%s modulo=%s rango=%s",
                    peticion.usuario_id, modulo_enum.value, descripcion_rango,
                )
                return self._respuesta_duplicada(
                    peticion.usuario_id, modulo_enum, df, descripcion_rango
                )

            # ── 6. NUEVO: Inyectar historial previo en métricas ───────────────
            # La inyección ocurre ANTES de ejecutar_calculos para que el módulo
            # pueda leerlo en orquestar_prompt. La lógica Pandas es intocable:
            # ejecutar_calculos solo lee las métricas que conoce, ignora _historial_previo.
            metricas = servicio.ejecutar_calculos(df, contexto, **kwargs)

            if historial_previo is not None:
                metricas["_historial_previo"] = historial_previo.get_consejo()
                metricas["_historial_insight"] = historial_previo.get_insight()
                logger.info(
                    "[HISTORIAL] Historial previo inyectado para usuario=%s modulo=%s (id=%d)",
                    peticion.usuario_id, modulo_enum.value, historial_previo.id,
                )
            else:
                metricas["_historial_previo"] = None
                metricas["_historial_insight"] = None

            # Totales financieros para fallback
            total_txs = len(df) if not df.empty else 0
            total_ing = float(df[df["tipo"] == "INGRESO"]["monto"].sum()) if not df.empty else 0.0
            total_gas = float(df[df["tipo"] == "GASTO"]["monto"].sum()) if not df.empty else 0.0
            metricas["_total_ingresos"] = total_ing
            metricas["_total_gastos"] = total_gas

            prompt = servicio.orquestar_prompt(metricas, contexto)

            # ── 7. NUEVO: Activar Structured Output dinámicamente según el módulo ───
            esquema_salida = servicio.obtener_esquema_salida()

            # ── 8. Ejecución en CoachIA ───────────────────────────────────────
            consejo, estado, fallback = await self._coach.obtener_consejo_ia(
                peticion.usuario_id,
                modulo_enum,
                prompt,
                metricas,
                contexto.rol,
                contexto.nombres,
                contexto=contexto,
                esquema_salida=esquema_salida,   # ← NUEVO v8
            )

            # ── 9. NUEVO: Persistir interacción en historial ──────────────────
            # Solo si Gemini respondió con éxito (no fallback).
            # Errores de escritura no degradan la experiencia del usuario.
            if not fallback and estado == EstadoCoach.EXITOSO:
                asyncio.create_task(
                    asyncio.to_thread(
                        self._persistir_historial,
                        usuario_id=peticion.usuario_id,
                        modulo=modulo_enum.value,
                        metricas=metricas,
                        consejo=consejo,
                        estado_coach=estado.value,
                    )
                )

            # ── 10. Construir respuesta final ─────────────────────────────────
            consejo_final = consejo
            if isinstance(consejo, dict) and esquema_salida:
                try:
                    consejo_final = esquema_salida(**consejo)
                except Exception as e:
                    logger.warning(
                        "[ORQUESTADOR] No se pudo convertir dict a %s: %s "
                        "— usando dict raw como fallback de conversión.",
                        esquema_salida.__name__,
                        e,
                    )
                    # Si la conversión falla, almacenar como str serializado
                    # para no romper RespuestaModulo (Union acepta str también).
                    consejo_final = json.dumps(consejo, ensure_ascii=False)

            insight_dto = InsightAnalitico(
                modulo=modulo_enum,
                total_transacciones_analizadas=total_txs,
                total_ingresos=total_ing,
                total_gastos=total_gas,
                balance_neto=round(total_ing - total_gas, 2),
                hallazgos=metricas,
            )

            resultado_final = RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=consejo_final,
                estado_coach=estado,
                insight=insight_dto,
                usando_fallback=fallback,
            )

            # Registrar firma exitosa en Redis (TTL 7 días)
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
                    hallazgos={"error": "historial_insuficiente"},
                ),
            )
        except Exception as e:
            logger.error("Fallo crítico en %s: %s", modulo_enum.value, e, exc_info=True)
            return RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo="Lo siento, estamos optimizando tu experiencia. Intenta de nuevo en un momento.",
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                insight=InsightAnalitico(
                    modulo=modulo_enum,
                    hallazgos={"error": str(e)},
                ),
            )

    @auditar_operacion("MODULO_COMPARACION")
    async def procesar_comparacion(
        self,
        modulo_enum: NombreModulo,
        peticion: PeticionComparacionDTO,
        ip: str,
        **kwargs,
    ) -> RespuestaModulo:
        try:
            rol_upper = kwargs.get("rol", "FREE").upper()
            limite = 20 if rol_upper == "PRO" else (50 if rol_upper == "PREMIUM" else 5)
            
            fecha_actual = datetime.now()
            anio_act, semana_act, _ = fecha_actual.isocalendar()
            clave_cuota = f"ia:cuota:analitica:{peticion.usuario_id}:{anio_act}:W{semana_act}"

            cuota_actual = self._cache_redis.obtener_cuota_actual(clave_cuota)
            if cuota_actual >= limite:
                raise LimiteDiarioExcedidoError(
                    mensaje=(
                        f"Has agotado tus {limite} consultas semanales de analitica. "
                        f"Tu plan {rol_upper} se renueva el próximo lunes."
                    )
                )

            servicio = FabricaModulosAnalisis.obtener_modulo(modulo_enum)

            tarea_financiera_a = self._cliente_financiero.obtener_historial_transacciones_async(
                peticion.usuario_id, peticion.token, peticion.tamanio_pagina,
                desde_exacto=peticion.rango_a_inicio.isoformat(),
                hasta_exacto=peticion.rango_a_fin.isoformat()
            )
            
            tarea_financiera_b = self._cliente_financiero.obtener_historial_transacciones_async(
                peticion.usuario_id, peticion.token, peticion.tamanio_pagina,
                desde_exacto=peticion.rango_b_inicio.isoformat(),
                hasta_exacto=peticion.rango_b_fin.isoformat()
            )

            tarea_perfil = self._cliente_perfil.obtener_perfil_usuario_async(
                peticion.usuario_id, peticion.token
            )

            resp_financiera_a, resp_financiera_b, dict_perfil = await asyncio.gather(
                tarea_financiera_a, tarea_financiera_b, tarea_perfil
            )

            datos_raw_a = resp_financiera_a.get("datos", [])
            datos_raw_b = resp_financiera_b.get("datos", [])
            df_a = json_a_dataframe(datos_raw_a)
            df_b = json_a_dataframe(datos_raw_b)
            
            contexto = ContextoEstrategicoIADTO.model_validate(dict_perfil)
            contexto.rol = kwargs.get("rol", "FREE")

            # Redis cache key (combining both hashes)
            datos_norm_a = sorted([json.dumps(t, sort_keys=True) for t in datos_raw_a])
            datos_norm_b = sorted([json.dumps(t, sort_keys=True) for t in datos_raw_b])
            hash_txs = hashlib.sha256(json.dumps(datos_norm_a + datos_norm_b).encode()).hexdigest()[:16]

            descripcion_rango = (f"{peticion.rango_a_inicio.strftime('%Y-%m-%d')}_vs_"
                                 f"{peticion.rango_b_fin.strftime('%Y-%m-%d')}")
            clave_firma = f"ia:firma:{peticion.usuario_id}:{modulo_enum.value}:{descripcion_rango}:{hash_txs}"

            descripcion_previa = self._cache_redis.obtener_firma(clave_firma)
            if descripcion_previa:
                return self._respuesta_duplicada(peticion.usuario_id, modulo_enum, df_b, descripcion_rango)

            metricas = servicio.ejecutar_calculos(df_a, df_b, contexto, **kwargs)

            total_txs = len(df_a) + len(df_b)
            total_ing_a = float(df_a[df_a["tipo"] == "INGRESO"]["monto"].sum()) if not df_a.empty else 0.0
            total_gas_a = float(df_a[df_a["tipo"] == "GASTO"]["monto"].sum()) if not df_a.empty else 0.0
            total_ing_b = float(df_b[df_b["tipo"] == "INGRESO"]["monto"].sum()) if not df_b.empty else 0.0
            total_gas_b = float(df_b[df_b["tipo"] == "GASTO"]["monto"].sum()) if not df_b.empty else 0.0
            
            metricas["_total_ingresos"] = total_ing_b
            metricas["_total_gastos"] = total_gas_b
            metricas["_historial_previo"] = None
            metricas["_historial_insight"] = None

            prompt = servicio.orquestar_prompt(metricas, contexto)

            esquema_comparacion = servicio.obtener_esquema_salida()

            consejo, estado, fallback = await self._coach.obtener_consejo_ia(
                peticion.usuario_id, modulo_enum, prompt, metricas,
                contexto.rol, contexto.nombres, contexto=contexto,
                esquema_salida=esquema_comparacion
            )

            if isinstance(consejo, dict) and esquema_comparacion:
                try:
                    consejo = esquema_comparacion(**consejo)
                except Exception as ex:
                    logger.warning("[ORQUESTADOR] Error convirtiendo dict a %s: %s", esquema_comparacion.__name__, ex)
                    consejo = json.dumps(consejo, ensure_ascii=False)

            if not fallback and estado == EstadoCoach.EXITOSO:
                asyncio.create_task(
                    asyncio.to_thread(
                        self._persistir_historial,
                        usuario_id=peticion.usuario_id,
                        modulo=modulo_enum.value,
                        metricas=metricas,
                        consejo=consejo,
                        estado_coach=estado.value,
                    )
                )

            insight_dto = InsightAnalitico(
                modulo=modulo_enum,
                total_transacciones_analizadas=total_txs,
                total_ingresos=total_ing_b,
                total_gastos=total_gas_b,
                balance_neto=round(total_ing_b - total_gas_b, 2),
                hallazgos=metricas,
            )

            resultado_final = RespuestaModulo(
                usuario_id=peticion.usuario_id,
                modulo=modulo_enum,
                consejo=consejo,
                estado_coach=estado,
                insight=insight_dto,
                usando_fallback=fallback,
            )

            self._cache_redis.registrar_consulta(clave_firma, descripcion_rango)
            return resultado_final

        except HistorialInsuficienteError as e:
            return RespuestaModulo(
                usuario_id=peticion.usuario_id, modulo=modulo_enum,
                consejo=str(e), estado_coach=EstadoCoach.NO_DISPONIBLE,
                insight=InsightAnalitico(modulo=modulo_enum, hallazgos={"error": "historial_insuficiente"})
            )
        except Exception as e:
            logger.error("Fallo crítico en %s: %s", modulo_enum.value, e, exc_info=True)
            return RespuestaModulo(
                usuario_id=peticion.usuario_id, modulo=modulo_enum,
                consejo="Lo siento, estamos optimizando tu experiencia. Intenta de nuevo en un momento.",
                estado_coach=EstadoCoach.NO_DISPONIBLE,
                insight=InsightAnalitico(modulo=modulo_enum, hallazgos={"error": str(e)})
            )

    # ── Métodos de soporte privados ───────────────────────────────────────────

    def _obtener_historial(self, usuario_id: str, modulo: str):
        """
        Consulta el último historial de coaching para (usuario_id, modulo).
        Abre y cierra su propia sesión DB para no contaminar el flujo principal.
        Retorna None si no hay historial o si la DB falla.
        """
        try:
            with SessionLocal() as db:
                repo = RepositorioHistorialCoaching(db)
                return repo.obtener_ultimo(usuario_id, modulo)
        except Exception as e:
            logger.warning(
                "[ORQUESTADOR] No se pudo obtener historial para usuario=%s: %s",
                usuario_id, e,
            )
            return None

    def _persistir_historial(
        self,
        usuario_id: str,
        modulo: str,
        metricas: dict,
        consejo,
        estado_coach: str,
    ) -> None:
        """
        Guarda la interacción de coaching en DB y la rutina mensual si aplica.
        Tolerante a fallos: errores solo se loguean, no se propagan.
        """
        try:
            with SessionLocal() as db:
                repo = RepositorioHistorialCoaching(db)
                repo.guardar(
                    usuario_id=usuario_id,
                    modulo=modulo,
                    metricas=metricas,
                    consejo=consejo,
                    estado_coach=estado_coach,
                )
                
                from app.modelos.esquemas import NombreModulo
                if modulo == NombreModulo.ZONA_ENTRENAMIENTO.value and estado_coach == "EXITOSO":
                    from app.persistencia.postgres.repositorio_rutinas import RepositorioRutinas
                    repo_rutinas = RepositorioRutinas(db)
                    
                    if hasattr(consejo, "model_dump"):
                        dict_consejo = consejo.model_dump()
                    elif isinstance(consejo, dict):
                        dict_consejo = consejo
                    else:
                        dict_consejo = json.loads(consejo) if isinstance(consejo, str) else {}
                        
                    estado_fisico = dict_consejo.get("estado_fisico", "Desconocido")
                    ejercicios = dict_consejo.get("rutina", [])
                    repo_rutinas.guardar_rutina(
                        usuario_id=usuario_id,
                        estado_fisico=estado_fisico,
                        ejercicios_json=json.dumps(ejercicios, ensure_ascii=False)
                    )
        except Exception as e:
            logger.error(
                "[ORQUESTADOR] Fallo al persistir historial para usuario=%s modulo=%s: %s",
                usuario_id, modulo, e,
            )

    def _respuesta_duplicada(
        self,
        usuario_id: str,
        modulo_enum: NombreModulo,
        df,
        descripcion_rango: str,
    ) -> RespuestaModulo:
        """Construye la respuesta estándar para consultas duplicadas (cache hit)."""
        total_txs = len(df) if not df.empty else 0
        total_ing = float(df[df["tipo"] == "INGRESO"]["monto"].sum()) if not df.empty else 0.0
        total_gas = float(df[df["tipo"] == "GASTO"]["monto"].sum()) if not df.empty else 0.0

        insight_dto = InsightAnalitico(
            modulo=modulo_enum,
            total_transacciones_analizadas=total_txs,
            total_ingresos=total_ing,
            total_gastos=total_gas,
            balance_neto=round(total_ing - total_gas, 2),
            hallazgos={"consulta_duplicada": True, "rango": descripcion_rango},
        )
        return RespuestaModulo(
            usuario_id=usuario_id,
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


# ── Fábrica de instancias ─────────────────────────────────────────────────────

def obtener_servicio_analisis() -> ServicioAnalisis:
    return ServicioAnalisis()


# ── Utilidades ────────────────────────────────────────────────────────────────

def _construir_descripcion_rango(
    mes, anio, mes_inicio, anio_inicio, mes_fin, anio_fin
) -> str:
    MESES = {
        1: "Ene", 2: "Feb", 3: "Mar", 4: "Abr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Ago", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dic",
    }
    if mes and anio:
        return f"{MESES.get(mes, mes)}-{anio}"
    if mes_inicio and anio_inicio and mes_fin and anio_fin:
        return (
            f"{MESES.get(mes_inicio)}-{anio_inicio}"
            f"_a_"
            f"{MESES.get(mes_fin)}-{anio_fin}"
        )
    return "todos-los-periodos"
