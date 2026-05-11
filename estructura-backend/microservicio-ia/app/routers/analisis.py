"""
routers/analisis.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Define los 11 endpoints REST del Microservicio IA de LUKA:
  10 módulos independientes + 1 endpoint de análisis completo.

Responsabilidades del router (y SOLO estas):
  - Declarar la ruta, método HTTP y documentación OpenAPI.
  - Extraer la IP del cliente para auditoría.
  - Delegar al ServicioAnalisis.
  - Retornar el resultado en formato ResultadoApi (camelCase).
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
import logging
from fastapi import APIRouter, Request, Depends, Security
from app.libreria_comun.seguridad.validador_jwt import validar_token, obtener_usuario_id
from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.modelos.esquemas import (
    PeticionClasificar,
    PeticionConFiltroFecha,
    PeticionSimularEscenario,
    PeticionSimularMeta,
    RespuestaModulo,
)
from app.servicios.servicio_analisis import ServicioAnalisis, obtener_servicio_analisis

logger = logging.getLogger(__name__)

# ── Router principal ─────────────────────────────────────────────────────────
router = APIRouter(
    prefix="/api/v1/ia",
    tags=["Módulos de Análisis IA — LUKA"]
)

# ── Utilidad: extrae la IP real del cliente ───────────────────────────────────
def _obtener_ip(request: Request) -> str:
    """Extrae la IP del cliente respetando X-Forwarded-For."""
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "127.0.0.1"

# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINTS DE ANÁLISIS
# ══════════════════════════════════════════════════════════════════════════════

@router.post("/clasificar", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 1: Clasificar transacción")
async def clasificar(
    request: Request,
    peticion: PeticionClasificar,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_clasificar(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Clasificación ejecutada con éxito")

@router.post("/predecir-gastos", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 2: Predicción de gastos")
async def predecir_gastos(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_predecir_gastos(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Predicción ejecutada con éxito")

@router.post("/detectar-anomalias", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 3: Detección de anomalías")
async def detectar_anomalias(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_detectar_anomalias(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Detección de anomalías completada")

@router.post("/optimizar-suscripciones", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 4: Gastos hormiga")
async def optimizar_suscripciones(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_optimizar_suscripciones(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de suscripciones finalizado")

@router.post("/capacidad-ahorro", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 5: Capacidad de ahorro")
async def capacidad_ahorro(
    request: Request,
    peticion: PeticionConFiltedFecha, # Note: Fixed typo from peticion: PeticionConFiltroFecha if any, but using PeticionConFiltroFecha
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_capacidad_ahorro(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Cálculo de ahorro completado")

@router.post("/simular-meta", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 6: Simulación de meta")
async def simular_meta(
    request: Request,
    peticion: PeticionSimularMeta,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_simular_meta(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Simulación de meta finalizada")

@router.post("/estacionalidad", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 7: Estacionalidad")
async def estacionalidad(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_estacionalidad(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis estacional completado")

@router.post("/presupuesto-dinamico", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 8: Presupuesto dinámico")
async def presupuesto_dinamico(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_presupuesto_dinamico(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Presupuesto generado con éxito")

@router.post("/simular-escenario", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 9: Simulación escenario")
async def simular_escenario(
    request: Request,
    peticion: PeticionSimularEscenario,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_simular_escenario(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Escenario simulado con éxito")

@router.post("/reporte-completo", response_model=ResultadoApi[RespuestaModulo], summary="Módulo 10: Reporte completo")
async def reporte_completo(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_reporte_completo(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Reporte completo generado")

@router.post("/analisis-completo", response_model=ResultadoApi[RespuestaModulo], summary="Dashboard Dashboard")
async def analisis_completo(
    request: Request,
    peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
) -> ResultadoApi[RespuestaModulo]:
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.ejecutar_reporte_completo(peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Dashboard actualizado")