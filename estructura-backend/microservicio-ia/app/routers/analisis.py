"""
routers/analisis.py  ·  v7 — FASE 2: Estandarización ResultadoApi (LUKA)
══════════════════════════════════════════════════════════════════════════════
Endpoints REST del Microservicio IA.

Cambios v7 (FASE 2):
  - Todos los endpoints propagan `ruta=request.url.path` a ResultadoApi
    para paridad exacta con ResultadoApi.java (librería común).
  - Arquitectura delegante: cada endpoint usa `procesar_modulo` del servicio.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
import logging
from fastapi import APIRouter, Request, Depends, Security
from app.libreria_comun.seguridad.validador_jwt import validar_token, obtener_usuario_id
from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.modelos.esquemas import (
    PeticionConFiltroFecha,
    PeticionSimularMeta,
    RespuestaModulo,
    NombreModulo,
    SolicitudClasificacionDTO,
    RespuestaClasificacionDTO,
)
from app.servicios.core.servicio_analisis import ServicioAnalisis, obtener_servicio_analisis

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ia", tags=["Módulos de Análisis IA — LUKA"])


def _obtener_ip(request: Request) -> str:
    """Extrae la IP real del cliente considerando proxies."""
    forwarded = request.headers.get("X-Forwarded-For")
    return forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "127.0.0.1")


# ── Módulo 1: Gasto Hormiga ───────────────────────────────────────────────────

@router.post("/gasto-hormiga", response_model=ResultadoApi[RespuestaModulo])
async def detectar_gasto_hormiga(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.GASTO_HORMIGA, peticion, _obtener_ip(request), rol=payload.get("role", "FREE"))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de gasto hormiga completado", ruta=request.url.path)


# ── Módulo 2: Predicción de Gastos ───────────────────────────────────────────

@router.post("/predecir-gastos", response_model=ResultadoApi[RespuestaModulo])
async def predecir_gastos(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.PREDECIR_GASTOS, peticion, _obtener_ip(request), rol=payload.get("role", "FREE"))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Predicción de gastos generada", ruta=request.url.path)


# ── Módulo 3: Hábitos Financieros ────────────────────────────────────────────

@router.post("/habitos-financieros", response_model=ResultadoApi[RespuestaModulo])
async def habitos_financieros(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.HABITOS_FINANCIEROS, peticion, _obtener_ip(request), rol=payload.get("role", "FREE"))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de hábitos completado", ruta=request.url.path)


# ── Módulo 4: Simulación de Meta de Ahorro ───────────────────────────────────

@router.post("/simular-meta", response_model=ResultadoApi[RespuestaModulo])
async def simular_meta(
    request: Request,
    peticion: PeticionSimularMeta,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    extra = {
        "meta_nombre": peticion.nombre_meta,
        "meta_monto": peticion.monto_objetivo,
        "meta_ahorro_previo": peticion.monto_actual_ahorrado,
        "aporte_deseado": peticion.aporte_mensual_deseado,
        "rol": payload.get("role", "FREE"),
    }
    resultado = await servicio.procesar_modulo(NombreModulo.SIMULAR_META, peticion, _obtener_ip(request), **extra)
    return ResultadoApi.exito_res(datos=resultado, mensaje="Simulación de meta finalizada", ruta=request.url.path)


# ── Módulo 5: Reto de Ahorro Dinámico ────────────────────────────────────────

@router.post("/reto-ahorro", response_model=ResultadoApi[RespuestaModulo])
async def reto_ahorro_dinamico(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(
        NombreModulo.RETO_AHORRO_DINAMICO,
        peticion,
        _obtener_ip(request),
        usuario_id=peticion.usuario_id,
        frecuencia=peticion.frecuencia,
        rol=payload.get("role", "FREE"),
    )
    return ResultadoApi.exito_res(datos=resultado, mensaje="Reto de ahorro gestionado", ruta=request.url.path)


# ── Módulo 6: Reporte Completo ────────────────────────────────────────────────

@router.post("/reporte-completo", response_model=ResultadoApi[RespuestaModulo])
async def reporte_completo(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.REPORTE_COMPLETO, peticion, _obtener_ip(request), rol=payload.get("role", "FREE"))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Reporte ejecutivo generado", ruta=request.url.path)


# ── Módulo 7: Análisis de Estilo de Vida ─────────────────────────────────────

@router.post("/estilo-vida", response_model=ResultadoApi[RespuestaModulo])
async def analisis_estilo_vida(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.ANALISIS_ESTILO_VIDA, peticion, _obtener_ip(request), rol=payload.get("role", "FREE"))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de estilo de vida completado", ruta=request.url.path)


# ── Módulo 8: Auto-Clasificación de Transacciones ────────────────────────────

@router.post("/clasificar-transaccion", response_model=ResultadoApi[RespuestaClasificacionDTO])
async def clasificar_transaccion(
    request: Request,
    solicitud: SolicitudClasificacionDTO,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token),
):
    usuario_id = obtener_usuario_id(payload)
    rol = payload.get("role", "FREE")

    # 1. Verificar cuota diaria del módulo de clasificación
    servicio._coach._verificar_cuota_diaria(usuario_id, NombreModulo.AUTO_CLASIFICACION, rol)

    # 2. Ejecutar clasificación vía servicio dedicado
    from app.servicios.ia.clasificador_ia import ClasificadorIAService
    clasificador = ClasificadorIAService()
    resultado = await clasificador.clasificar(solicitud)
    return ResultadoApi.exito_res(
        datos=resultado,
        mensaje="Sugerencias de categorías generadas",
        ruta=request.url.path,
    )


# ── Módulo 9: Limpiar Historial de Consultas ──────────────────────────────────

@router.delete("/mis-consultas/historial", response_model=ResultadoApi[dict])
async def limpiar_historial_consultas(
    request: Request,
    payload: dict = Security(validar_token),
):
    """
    Permite al usuario limpiar su historial de firmas de consultas previas
    para poder forzar una nueva llamada a Gemini en rangos consultados anteriormente.
    """
    usuario_id = obtener_usuario_id(payload)
    from app.persistencia.cache_redis import CacheRedis
    cache = CacheRedis()
    eliminadas = cache.flush_firmas_usuario(usuario_id)
    
    logger.info(
        "[HISTORIAL-CLEAR] Usuario %s eliminó %d firmas de consulta Redis",
        usuario_id, eliminadas
    )
    return ResultadoApi.exito_res(
        datos={"usuario_id": usuario_id, "firmas_eliminadas": eliminadas},
        mensaje="Historial de consultas limpiado exitosamente",
        ruta=request.url.path,
    )


# ── Admin: Mantenimiento de Caché ────────────────────────────────────────────

@router.delete("/admin/cache/flush", response_model=ResultadoApi[dict])
async def flush_cache_ia(
    request: Request,
    patron: str = "ia:*",
    payload: dict = Security(validar_token),
):
    """
    [ADMIN] Limpia todas las claves de caché Redis que coincidan con el patrón.
    Por defecto elimina ia:* (consejos + cuotas). Requiere JWT válido.
    Útil para forzar que Gemini regenere consejos después de correcciones de código.
    """
    from app.persistencia.cache_redis import CacheRedis
    cache = CacheRedis()
    eliminadas = cache.flush_ia_cache(patron=patron)
    logger.warning(
        "[ADMIN-CACHE-FLUSH] Usuario %s eliminó %d claves Redis (patron='%s')",
        obtener_usuario_id(payload), eliminadas, patron
    )
    return ResultadoApi.exito_res(
        datos={"claves_eliminadas": eliminadas, "patron": patron},
        mensaje=f"Cache limpiada: {eliminadas} clave(s) eliminadas",
        ruta=request.url.path,
    )