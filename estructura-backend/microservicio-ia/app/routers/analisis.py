"""
routers/analisis.py  ·  v6 — ARQUITECTURA SOLID (LUKA)
══════════════════════════════════════════════════════════════════════════════
Endpoints REST del Microservicio IA. 

Arquitectura:
  Cada endpoint delega al método genérico 'procesar_modulo' del servicio.
  Añadir un nuevo módulo ya no requiere cambiar la lógica del servicio core.
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
    RespuestaClasificacionDTO
)
from app.servicios.core.servicio_analisis import ServicioAnalisis, obtener_servicio_analisis

router = APIRouter(prefix="/api/v1/ia", tags=["Módulos de Análisis IA — LUKA"])

def _obtener_ip(request: Request) -> str:
    """Utilidad para extraer la IP real del cliente."""
    forwarded = request.headers.get("X-Forwarded-For")
    return forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "127.0.0.1")

@router.post("/gasto-hormiga", response_model=ResultadoApi[RespuestaModulo])
async def detectar_gasto_hormiga(
    request: Request, peticion: PeticionConFiltroFecha, 
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.GASTO_HORMIGA, peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de gasto hormiga completado")

@router.post("/predecir-gastos", response_model=ResultadoApi[RespuestaModulo])
async def predecir_gastos(
    request: Request, peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.PREDECIR_GASTOS, peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Predicción de gastos generada")

@router.post("/habitos-financieros", response_model=ResultadoApi[RespuestaModulo])
async def habitos_financieros(
    request: Request, peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.HABITOS_FINANCIEROS, peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de hábitos completado")

@router.post("/simular-meta", response_model=ResultadoApi[RespuestaModulo])
async def simular_meta(
    request: Request, peticion: PeticionSimularMeta,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    # Parámetros específicos para la simulación
    extra = {
        "meta_nombre": peticion.nombre_meta,
        "meta_monto": peticion.monto_objetivo,
        "meta_ahorro_previo": peticion.monto_actual_ahorrado,
        "aporte_deseado": peticion.aporte_mensual_deseado
    }
    resultado = await servicio.procesar_modulo(NombreModulo.SIMULAR_META, peticion, _obtener_ip(request), **extra)
    return ResultadoApi.exito_res(datos=resultado, mensaje="Simulación de meta finalizada")

@router.post("/reto-ahorro", response_model=ResultadoApi[RespuestaModulo])
async def reto_ahorro_dinamico(
    request: Request, peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(
        NombreModulo.RETO_AHORRO_DINAMICO, 
        peticion, 
        _obtener_ip(request),
        usuario_id=peticion.usuario_id,
        frecuencia=peticion.frecuencia
    )
    return ResultadoApi.exito_res(datos=resultado, mensaje="Reto de ahorro gestionado")

@router.post("/reporte-completo", response_model=ResultadoApi[RespuestaModulo])
async def reporte_completo(
    request: Request, peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.REPORTE_COMPLETO, peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Reporte ejecutivo generado")

@router.post("/estilo-vida", response_model=ResultadoApi[RespuestaModulo])
async def analisis_estilo_vida(
    request: Request, peticion: PeticionConFiltroFecha,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    peticion.usuario_id = obtener_usuario_id(payload)
    resultado = await servicio.procesar_modulo(NombreModulo.ANALISIS_ESTILO_VIDA, peticion, _obtener_ip(request))
    return ResultadoApi.exito_res(datos=resultado, mensaje="Análisis de estilo de vida completado")

@router.post("/clasificar-transaccion", response_model=ResultadoApi[RespuestaClasificacionDTO])
async def clasificar_transaccion(
    solicitud: SolicitudClasificacionDTO,
    servicio: ServicioAnalisis = Depends(obtener_servicio_analisis),
    payload: dict = Security(validar_token)
):
    usuario_id = obtener_usuario_id(payload)
    rol = payload.get("role", "FREE")
    
    # 1. Verificar Cuota (Módulo 8)
    servicio._coach._verificar_cuota_diaria(usuario_id, NombreModulo.AUTO_CLASIFICACION, rol)
    
    # 2. Ejecutar clasificación
    from app.servicios.ia.clasificador_ia import ClasificadorIAService
    clasificador = ClasificadorIAService()
    resultado = await clasificador.clasificar(solicitud)
    return ResultadoApi.exito_res(datos=resultado, mensaje="Sugerencias de categorías generadas")