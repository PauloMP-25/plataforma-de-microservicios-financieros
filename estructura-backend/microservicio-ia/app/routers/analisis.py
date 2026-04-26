"""
analisis.py — Endpoints del Motor de IA Financiera.
Integrado con Seguridad JWT y Auditoría Asíncrona.
"""

import logging
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, HTTPException, Request, status, Depends, BackgroundTasks
from httpcore import request

# Modelos y esquemas
from app.modelos.esquemas import (
    SolicitudAnalisis, SolicitudSimulacion, SolicitudMetaFinanciera,
    RespuestaClasificacion, RespuestaPrediccion, RespuestaAnomalias,
    RespuestaSuscripciones, RespuestaCapacidadAhorro, RespuestaMetaFinanciera,
    RespuestaEstacionalidad, RespuestaPresupuesto, RespuestaSimulacion,
    RespuestaReporte, RespuestaAnalisisCompleto
)

# Clientes y Seguridad
from app.clientes.cliente_financiero import ClienteNucleoFinanciero
from app.clientes.cliente_auditoria import enviar_evento_auditoria
from app.seguridad import validar_token, obtener_usuario_id
from app.utilidades.preparador_datos import json_a_dataframe
from app.servicios import motor_ia

logger = logging.getLogger("ia_financiera.router")
router = APIRouter(prefix="/api/v1/ia", tags=["Análisis IA Financiero"])

# Cliente persistente para datos financieros
_cliente_financiero = ClienteNucleoFinanciero()

# Definición de dependencia de seguridad para limpieza visual
UsuarioAutenticado = Annotated[dict, Depends(validar_token)]

# =========================================================================
# LOGICA DE SOPORTE (Dependencias)
# =========================================================================

async def _obtener_datos_as_df(solicitud: SolicitudAnalisis, token: str):
    """Helper: Obtiene transacciones del Core y las convierte a DataFrame."""
    try:
        # Nota: Idealmente cliente_financiero también debería ser async
        datos_json = _cliente_financiero.obtener_historial_transacciones(
            usuario_id=solicitud.usuario_id,
            token=token,
            tamanio=solicitud.tamanio_pagina,
            mes=solicitud.mes,
            anio=solicitud.anio,
        )
        return json_a_dataframe(datos_json)
    except Exception as exc:
        logger.error(f"Error al conectar con Nucleo Financiero: {exc}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "CORE_NO_DISPONIBLE", "mensaje": "No se pudo obtener datos del núcleo financiero."}
        )


def _registrar_auditoria_segundo_plano(tareas: BackgroundTasks, payload: UsuarioAutenticado, 
                                      accion: str, detalles: str, request: Request):
    """
    Registra el evento en el microservicio de auditoría sin bloquear la respuesta principal.
    """
    usuario_nombre = payload.get("sub", "desconocido")
    ip = request.client.host if request.client else "127.0.0.1"
    
    # Agregamos la tarea para que FastAPI la ejecute DESPUÉS de enviar la respuesta
    tareas.add_task(enviar_evento_auditoria, usuario_nombre, accion, detalles, ip)

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
async def clasificar_transacciones(
    solicitud: SolicitudAnalisis, 
    request: Request, 
    tareas: BackgroundTasks,
    payload: UsuarioAutenticado):
    
    # El token ya viene validado en el payload si llegamos aquí
    token = request.headers.get("Authorization").replace("Bearer ", "")
    
    df = await _obtener_datos_as_df(solicitud, token)
    resultado = motor_ia.clasificar_transaccion_automatica(df)

    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_CLASIFICACION", 
        f"Clasificadas {len(df)} transacciones para {solicitud.usuario_id}", request
    )

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
async def predecir_gastos(
    solicitud: SolicitudAnalisis, 
    tareas: BackgroundTasks,
    request: Request, 
    payload: UsuarioAutenticado):

    # El token ya viene validado en el payload si llegamos aquí
    token = request.headers.get("Authorization").replace("Bearer ", "")
    
    df = await _obtener_datos_as_df(solicitud, token)    
    resultado = motor_ia.predecir_gastos_proximo_mes(df)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_PREDICCION_GASTOS", 
        f"Gasto predicho: S/ {resultado.get('gasto_predicho', 0)}", request
    )
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
async def detectar_anomalias(
    solicitud: SolicitudAnalisis, 
    request: Request, 
    payload: UsuarioAutenticado,
    tareas: BackgroundTasks):

    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    resultado = motor_ia.detectar_anomalias_financieras(df)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_DETECCION_ANOMALIAS", 
        f"Anomalías detectadas: {resultado.get('anomalias_detectadas', 0)}", request
    )
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
async def optimizar_suscripciones(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    # Extraemos el token limpio
    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    resultado = motor_ia.optimizar_suscripciones(df)

        # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_OPTIMIZACION_SUSCRIPCIONES", 
        f"Ahorro potencial: S/ {resultado.get('ahorro_potencial', 0)}", request
    )
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
async def calcular_ahorro(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    # Extraemos el token limpio
    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    resultado = motor_ia.calcular_capacidad_ahorro(df)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_CAPACIDAD_AHORRO", 
        f"Ahorro: S/ {resultado.get('capacidad_ahorro', 0)} ({resultado.get('porcentaje_ahorro', 0)}%)", request
    )
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
async def simular_meta(solicitud: SolicitudMetaFinanciera, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    token = request.headers.get("Authorization").replace("Bearer ", "")

    solicitud_analisis = SolicitudAnalisis(
        usuario_id=solicitud.usuario_id,
        mes=solicitud.mes,
        anio=solicitud.anio,
    )
    df =await _obtener_datos_as_df(solicitud_analisis, token)
    resultado = motor_ia.simular_metas_financieras(df, solicitud.monto_meta, solicitud.nombre_meta)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_SIMULACION_META", 
        f"Meta '{solicitud.nombre_meta}' en {resultado.get('meses_para_alcanzar', '?')} meses", request
    )
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
async def analizar_estacionalidad(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    resultado = motor_ia.analizar_estacionalidad(df)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_ESTACIONALIDAD", 
        f"Estacionalidad: {resultado.get('patron_estacional', 'No detectado')}  ", request
    )
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
async def recomendar_presupuesto(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    # Extraemos el token limpio
    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    resultado = motor_ia.recomendar_presupuesto_dinamico(df)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_PRESUPUESTO_DINAMICO", 
        f"Presupuesto semanal: S/ {resultado.get('presupuesto_semana_actual', 0)}", request
    )
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
async def simular_escenario(solicitud: SolicitudSimulacion, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    token = request.headers.get("Authorization").replace("Bearer ", "")

    solicitud_analisis = SolicitudAnalisis(
        usuario_id=solicitud.usuario_id,
        mes=solicitud.mes,
        anio=solicitud.anio,
    )
    df =await _obtener_datos_as_df(solicitud_analisis, token)
    resultado = motor_ia.simular_escenario_que_pasaria_si(
        df,
        nuevo_gasto_fijo=solicitud.nuevo_gasto_fijo,
        nuevo_ingreso=solicitud.nuevo_ingreso,
    )
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_SIMULACION_ESCENARIO", 
        f"Nuevo gasto fijo: S/ {solicitud.nuevo_gasto_fijo} | Nuevo ingreso: S/ {solicitud.nuevo_ingreso}", request
    )
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
async def generar_reporte(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):

    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    periodo = f"{solicitud.mes or 'todos los meses'} / {solicitud.anio or 'todos los años'}"
    resultado = motor_ia.generar_reporte_lenguaje_natural(df, periodo)
    
    # Auditoría en segundo plano
    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_REPORTE_COMPLETO", 
        f"Puntaje salud financiera: {resultado.get('puntaje_salud_financiera', 0)}/100", request
    )
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
async def analisis_completo(solicitud: SolicitudAnalisis, request: Request, payload: UsuarioAutenticado,
tareas: BackgroundTasks):
    


    token = request.headers.get("Authorization").replace("Bearer ", "")

    df = await _obtener_datos_as_df(solicitud, token)  
    periodo = f"{solicitud.mes or 'período completo'} {solicitud.anio or ''}"

    prediccion = motor_ia.predecir_gastos_proximo_mes(df)
    anomalias = motor_ia.detectar_anomalias_financieras(df)
    ahorro = motor_ia.calcular_capacidad_ahorro(df)
    suscripciones = motor_ia.optimizar_suscripciones(df)
    reporte = motor_ia.generar_reporte_lenguaje_natural(df, periodo)

    # Auditoría en segundo plano

    _registrar_auditoria_segundo_plano(
        tareas, payload, "IA_ANALISIS_COMPLETO", 
        f"Análisis completo ejecutado — {len(df)} transacciones procesadas", request
    )

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