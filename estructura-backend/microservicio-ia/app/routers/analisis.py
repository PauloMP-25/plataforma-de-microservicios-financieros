"""
routers/analisis.py
Endpoints de la API de IA Financiera.
Cada endpoint corresponde a uno de los 10 módulos de análisis.
"""

import fastapi
from fastapi import APIRouter, HTTPException, Request, status, Depends, Header
from typing import Optional
from datetime import datetime
import logging

from app.modelos.esquemas import (
    SolicitudAnalisis, SolicitudSimulacion, SolicitudMetaFinanciera,
    RespuestaClasificacion, RespuestaPrediccion, RespuestaAnomalias,
    RespuestaSuscripciones, RespuestaCapacidadAhorro, RespuestaMetaFinanciera,
    RespuestaEstacionalidad, RespuestaPresupuesto, RespuestaSimulacion,
    RespuestaReporte, RespuestaAnalisisCompleto
)
from app.clientes.cliente_financiero import ClienteNucleoFinanciero, ClienteAuditoria
from app.utilidades.preparador_datos import json_a_dataframe
from app.servicios import motor_ia

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ia", tags=["Análisis IA Financiero"])

# Clientes persistentes
_cliente_financiero = ClienteNucleoFinanciero()
_cliente_auditoria = ClienteAuditoria()

# =========================================================================
# LOGICA DE SOPORTE (Dependencias)
# =========================================================================

def _obtener_dataframe(solicitud: SolicitudAnalisis, token: str):
    """Helper: obtiene las transacciones y las convierte en DataFrame."""
    try:
        datos_json = _cliente_financiero.obtener_historial_transacciones(
            usuario_id=solicitud.usuario_id,
            token=token,
            tamanio=solicitud.tamanio_pagina,
            mes=solicitud.mes,
            anio=solicitud.anio,
        )
        df = json_a_dataframe(datos_json)
        return df
    except ConnectionError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "SERVICIO_NO_DISPONIBLE",
                "mensaje": str(exc),
                "sugerencia": "Verifique que microservicio-nucleo-financiero esté corriendo en el puerto 8085."
            }
        )
    except (ValueError, TimeoutError) as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "ERROR_MICROSERVICIO", "mensaje": str(exc)}
        )


def _auditar(usuario_id: str, accion: str, detalles: str, request: Request):
    """Helper: envía evento de auditoría de forma no bloqueante."""
    ip = request.client.host if request.client else "unknown"
    _cliente_auditoria.reportar_evento(usuario_id, accion, detalles, ip)


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 1: CLASIFICAR TRANSACCIONES
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/clasificar",
    response_model=RespuestaClasificacion,
    summary="Módulo 1: Clasificar transacciones automáticamente",
    description="""
    Etiqueta automáticamente las transacciones usando análisis semántico
    y lógica difusa. Asigna categorías de comportamiento: `necesidad`,
    `capricho`, `inversión` o `recurrente`.
    """,
)
async def clasificar_transacciones(solicitud: SolicitudAnalisis, 
                                request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    # Extraemos el token limpio
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.clasificar_transaccion_automatica(df)
    _auditar(solicitud.usuario_id, "IA_CLASIFICACION", f"Clasificadas {resultado.get('transacciones_clasificadas', 0)} transacciones", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 2: PREDECIR GASTOS DEL PRÓXIMO MES
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/predecir-gastos",
    response_model=RespuestaPrediccion,
    summary="Módulo 2: Predecir gastos del próximo mes",
    description="""
    Proyecta ingresos y gastos del mes siguiente usando regresión lineal
    o media móvil ponderada (dependiendo de la cantidad de datos históricos).
    """,
)
async def predecir_gastos(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    # Extraemos el token limpio
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.predecir_gastos_proximo_mes(df)
    _auditar(solicitud.usuario_id, "IA_PREDICCION_GASTOS", f"Gasto predicho: S/ {resultado.get('gasto_predicho', 0)}", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 3: DETECTAR ANOMALÍAS
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/detectar-anomalias",
    response_model=RespuestaAnomalias,
    summary="Módulo 3: Detectar anomalías financieras",
    description="""
    Identifica transacciones inusuales usando el método Z-Score.
    Una transacción es anómala si su monto supera 2 desviaciones estándar
    respecto a la media de su categoría.
    """,
)
async def detectar_anomalias(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.detectar_anomalias_financieras(df)
    _auditar(solicitud.usuario_id, "IA_DETECCION_ANOMALIAS", f"Anomalías detectadas: {resultado.get('anomalias_detectadas', 0)}", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 4: OPTIMIZAR SUSCRIPCIONES
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/optimizar-suscripciones",
    response_model=RespuestaSuscripciones,
    summary="Módulo 4: Optimizar suscripciones y gastos hormiga",
    description="""
    Detecta gastos recurrentes de bajo monto (gastos hormiga) y suscripciones
    que podrían cancelarse para liberar capacidad de ahorro.
    """,
)
async def optimizar_suscripciones(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    # Extraemos el token limpio
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.optimizar_suscripciones(df)
    _auditar(solicitud.usuario_id, "IA_OPTIMIZACION_SUSCRIPCIONES", f"Ahorro potencial: S/ {resultado.get('ahorro_potencial', 0)}", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 5: CALCULAR CAPACIDAD DE AHORRO
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/capacidad-ahorro",
    response_model=RespuestaCapacidadAhorro,
    summary="Módulo 5: Calcular capacidad de ahorro",
    description="""
    Calcula la capacidad real de ahorro mensual usando la fórmula:
    `(Ingresos - Gastos Fijos) Factor de Seguridad (0.85)`.
    Clasifica el resultado según la regla 50/30/20.
    """,
)
async def calcular_ahorro(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    # Extraemos el token limpio
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.calcular_capacidad_ahorro(df)
    _auditar(solicitud.usuario_id, "IA_CAPACIDAD_AHORRO", f"Ahorro: S/ {resultado.get('capacidad_ahorro', 0)} ({resultado.get('porcentaje_ahorro', 0)}%)", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 6: SIMULAR METAS FINANCIERAS
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/simular-meta",
    response_model=RespuestaMetaFinanciera,
    summary="Módulo 6: Simular metas financieras",
    description="""
    Proyecta cuántos meses tardará el usuario en alcanzar una meta financiera
    dado su ritmo de ahorro actual. Incluye escenarios optimista y pesimista.
    """,
)
async def simular_meta(solicitud: SolicitudMetaFinanciera, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    token = authorization.replace("Bearer ", "")
    solicitud_analisis = SolicitudAnalisis(
        usuario_id=solicitud.usuario_id,
        mes=solicitud.mes,
        anio=solicitud.anio,
    )
    df = _obtener_dataframe(solicitud_analisis, token)
    resultado = motor_ia.simular_metas_financieras(df, solicitud.monto_meta, solicitud.nombre_meta)
    _auditar(solicitud.usuario_id, "IA_SIMULACION_META", f"Meta '{solicitud.nombre_meta}' en {resultado.get('meses_para_alcanzar', '?')} meses", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 7: ANALIZAR ESTACIONALIDAD
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/estacionalidad",
    response_model=RespuestaEstacionalidad,
    summary="Módulo 7: Analizar estacionalidad",
    description="""
    Agrupa las transacciones por mes para detectar picos históricos de gasto
    e ingreso y el patrón estacional del usuario.
    """,
)
async def analizar_estacionalidad(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    resultado = motor_ia.analizar_estacionalidad(df)
    _auditar(solicitud.usuario_id, "IA_ESTACIONALIDAD", "Análisis de estacionalidad ejecutado", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 8: RECOMENDAR PRESUPUESTO DINÁMICO
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/presupuesto-dinamico",
    response_model=RespuestaPresupuesto,
    summary="Módulo 8: Recomendar presupuesto dinámico",
    description="""
    Calcula el presupuesto semanal recomendado ajustado dinámicamente
    según el gasto de la semana anterior y los límites por categoría.
    """,
)
async def recomendar_presupuesto(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    # Extraemos el token limpio
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    df = _obtener_dataframe(solicitud)
    resultado = motor_ia.recomendar_presupuesto_dinamico(df)
    _auditar(solicitud.usuario_id, "IA_PRESUPUESTO_DINAMICO", f"Presupuesto semanal: S/ {resultado.get('presupuesto_semana_actual', 0)}", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 9: SIMULAR "¿QUÉ PASARÍA SI?"
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/simular-escenario",
    response_model=RespuestaSimulacion,
    summary="Módulo 9: Simular escenario '¿Qué pasaría si?'",
    description="""
    Calcula el impacto financiero de agregar un nuevo gasto fijo mensual
    o un nuevo ingreso sobre la capacidad de ahorro y el balance.
    """,
)
async def simular_escenario(solicitud: SolicitudSimulacion, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    token = authorization.replace("Bearer ", "")
    solicitud_analisis = SolicitudAnalisis(
        usuario_id=solicitud.usuario_id,
        mes=solicitud.mes,
        anio=solicitud.anio,
    )
    df = _obtener_dataframe(solicitud_analisis, token)
    resultado = motor_ia.simular_escenario_que_pasaria_si(
        df,
        nuevo_gasto_fijo=solicitud.nuevo_gasto_fijo,
        nuevo_ingreso=solicitud.nuevo_ingreso,
    )
    _auditar(solicitud.usuario_id, "IA_SIMULACION_ESCENARIO", f"Nuevo gasto fijo: S/ {solicitud.nuevo_gasto_fijo} | Nuevo ingreso: S/ {solicitud.nuevo_ingreso}", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT 10: GENERAR REPORTE EN LENGUAJE NATURAL
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/reporte-completo",
    response_model=RespuestaReporte,
    summary="Módulo 10: Generar reporte ejecutivo en lenguaje natural",
    description="""
    Genera un resumen ejecutivo completo integrando los KPIs de todos los
    módulos anteriores. Actúa como un **CFO virtual** que explica la situación
    financiera del usuario de forma comprensible, con alertas y recomendaciones.
    """,
)
async def generar_reporte(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )
    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    periodo = f"{solicitud.mes or 'todos los meses'} / {solicitud.anio or 'todos los años'}"
    resultado = motor_ia.generar_reporte_lenguaje_natural(df, periodo)
    _auditar(solicitud.usuario_id, "IA_REPORTE_COMPLETO", f"Puntaje salud financiera: {resultado.get('puntaje_salud_financiera', 0)}/100", request)
    return resultado


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT BONUS: ANÁLISIS COMPLETO (todos los módulos en una sola llamada)
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/analisis-completo",
    response_model=RespuestaAnalisisCompleto,
    summary="Análisis completo — todos los módulos",
    description="""
    Ejecuta los 10 módulos de análisis en una sola petición y devuelve
    el resumen consolidado. Ideal para el dashboard principal del frontend.

    Esta llamada puede tardar más tiempo al ejecutar todos los análisis.
    """,
)
async def analisis_completo(solicitud: SolicitudAnalisis, request: Request, authorization: Optional[str] = Header(None)):
    
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Se requiere el token JWT en el encabezado Authorization"
        )

    token = authorization.replace("Bearer ", "")
    df = _obtener_dataframe(solicitud, token)
    periodo = f"{solicitud.mes or 'período completo'} {solicitud.anio or ''}"

    prediccion = motor_ia.predecir_gastos_proximo_mes(df)
    anomalias = motor_ia.detectar_anomalias_financieras(df)
    ahorro = motor_ia.calcular_capacidad_ahorro(df)
    suscripciones = motor_ia.optimizar_suscripciones(df)
    reporte = motor_ia.generar_reporte_lenguaje_natural(df, periodo)

    _auditar(solicitud.usuario_id, "IA_ANALISIS_COMPLETO", f"Análisis completo ejecutado — {len(df)} transacciones procesadas", request)

    return {
        "usuario_id": solicitud.usuario_id,
        "fecha_analisis": datetime.now().isoformat(),
        "total_transacciones": len(df),
        "prediccion": prediccion,
        "anomalias": anomalias,
        "capacidad_ahorro": ahorro,
        "suscripciones": suscripciones,
        "reporte": reporte,
        "mensaje": f"Análisis completo generado para {len(df)} transacciones. Puntaje de salud: {reporte.get('puntaje_salud_financiera', 0)}/100.",
    }