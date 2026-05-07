"""
routers/analisis.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Define los 11 endpoints REST del Microservicio IA de LUKA:
  10 módulos independientes + 1 endpoint de análisis completo.

Responsabilidades del router (y SOLO estas):
  - Declarar la ruta, método HTTP y documentación OpenAPI.
  - Extraer la IP del cliente para auditoría.
  - Delegar al ServicioAnalisis.
  - Manejar los errores de negocio y traducirlos a respuestas HTTP correctas.

El router NO hace cálculos, NO llama a Gemini, NO accede a DataFrames.
Todo eso ocurre en ServicioAnalisis → Motor Analítico → CoachIA.

Códigos de respuesta usados:
  200 OK              → análisis exitoso.
  422 Unprocessable   → datos insuficientes o parámetros inválidos.
  503 Service Unavail → Gemini no disponible (cuota o auth).
  500 Internal Error  → error inesperado.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException, Request, status

from app.excepciones import (
    AnalisisFinancieroError,
    GeminiAutenticacionError,
    GeminiCuotaExcedidaError,
)
from app.modelos.esquemas import (
    PeticionClasificar,
    PeticionConFiltroFecha,
    PeticionSimularEscenario,
    PeticionSimularMeta,
    RespuestaModulo,
)
from app.servicios.servicio_analisis import ServicioAnalisis

logger = logging.getLogger(__name__)

# ── Instancia única del servicio (reutilizada en todos los endpoints) ────────
_servicio = ServicioAnalisis()

# ── Router principal ─────────────────────────────────────────────────────────
router = APIRouter(
    prefix="/api/v1/ia",
    tags=["Módulos de Análisis IA — LUKA"],
    responses={
        503: {"description": "Motor de IA no disponible temporalmente."},
        422: {"description": "Datos insuficientes para ejecutar el análisis."},
    },
)


# ── Utilidad: extrae la IP real del cliente ───────────────────────────────────
def _obtener_ip(request: Request) -> str:
    """
    Extrae la IP del cliente respetando el header X-Forwarded-For
    que el API Gateway agrega al reenviar la petición.
    """
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "127.0.0.1"


# ── Manejo centralizado de excepciones del servicio ──────────────────────────
def _manejar_excepcion(exc: Exception, modulo: str) -> None:
    """
    Traduce excepciones de dominio a HTTPException de FastAPI.
    Llamado desde cada endpoint en su bloque except.
    """
    if isinstance(exc, GeminiCuotaExcedidaError):
        logger.warning("[ROUTER] Cuota de Gemini agotada | módulo=%s", modulo)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "codigo": "GEMINI_CUOTA_AGOTADA",
                "mensaje": "El motor de IA está temporalmente no disponible. Intenta en unos minutos.",
            },
        )
    if isinstance(exc, GeminiAutenticacionError):
        logger.error("[ROUTER] Error de autenticación con Gemini | módulo=%s", modulo)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "codigo": "GEMINI_AUTH_ERROR",
                "mensaje": "Error de configuración del motor de IA. Contacta al soporte.",
            },
        )
    if isinstance(exc, (ValueError, AnalisisFinancieroError)):
        logger.warning("[ROUTER] Datos insuficientes o error de negocio | módulo=%s | %s", modulo, str(exc))
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail={
                "codigo": "DATOS_INSUFICIENTES",
                "mensaje": str(exc),
            },
        )
    # Error inesperado
    logger.error("[ROUTER] Error inesperado | módulo=%s | %s", modulo, str(exc), exc_info=True)
    raise HTTPException(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        detail={
            "codigo": "ERROR_INTERNO",
            "mensaje": "Ocurrió un error inesperado. Por favor intenta de nuevo.",
        },
    )


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 1 — Autoclasificación de Transacciones
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/clasificar",
    response_model=RespuestaModulo,
    summary="Módulo 1: Autoclasificación de transacciones",
    description=(
        "Evalúa si la categoría asignada a una transacción es la más precisa "
        "comparándola con el historial del usuario. Si detecta una categoría "
        "más adecuada, la sugiere con justificación."
    ),
)
async def clasificar_transaccion(
    peticion: PeticionClasificar,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_clasificar(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "CLASIFICAR")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2 — Predicción de Gastos
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/predecir-gastos",
    response_model=RespuestaModulo,
    summary="Módulo 2: Predicción de gastos del próximo mes",
    description=(
        "Proyecta el gasto total del próximo mes usando regresión lineal "
        "(si hay ≥ 3 meses de historial) o media móvil. "
        "Incluye detección de tendencia alcista/bajista y alerta de categorías críticas."
    ),
)
async def predecir_gastos(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_predecir_gastos(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "PREDECIR_GASTOS")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 3 — Detección de Anomalías
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/detectar-anomalias",
    response_model=RespuestaModulo,
    summary="Módulo 3: Detección de gastos inusuales",
    description=(
        "Identifica transacciones que se desvían significativamente del "
        "comportamiento habitual del usuario usando Z-Score estadístico. "
        "Clasifica cada anomalía por nivel de riesgo (BAJO / MEDIO / ALTO / CRÍTICO)."
    ),
)
async def detectar_anomalias(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_detectar_anomalias(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "DETECTAR_ANOMALIAS")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 4 — Optimización de Suscripciones / Gastos Hormiga
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/optimizar-suscripciones",
    response_model=RespuestaModulo,
    summary="Módulo 4: Detección de gastos hormiga y suscripciones",
    description=(
        "Detecta gastos pequeños y recurrentes (≤ S/ 30 por defecto) que, "
        "sumados, representan un drenaje significativo del presupuesto mensual. "
        "Calcula el impacto mensual y anual acumulado."
    ),
)
async def optimizar_suscripciones(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_optimizar_suscripciones(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "OPTIMIZAR_SUSCRIPCIONES")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 5 — Capacidad de Ahorro
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/capacidad-ahorro",
    response_model=RespuestaModulo,
    summary="Módulo 5: Cálculo de capacidad de ahorro (regla 50/30/20)",
    description=(
        "Calcula la tasa de ahorro real del universitario y la compara con "
        "la regla 50/30/20. Aplica un factor de seguridad para dar una "
        "proyección conservadora y propone ajustes concretos."
    ),
)
async def capacidad_ahorro(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_capacidad_ahorro(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "CAPACIDAD_AHORRO")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 6 — Simulación de Meta de Ahorro
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/simular-meta",
    response_model=RespuestaModulo,
    summary="Módulo 6: Simulación de tiempo para alcanzar una meta de ahorro",
    description=(
        "Proyecta cuántos meses tomará alcanzar un objetivo financiero específico "
        "(laptop, viaje, fondo de emergencia, etc.) basándose en la capacidad "
        "de ahorro real calculada del historial."
    ),
)
async def simular_meta(
    peticion: PeticionSimularMeta,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_simular_meta(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "SIMULAR_META")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 7 — Análisis de Estacionalidad
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/estacionalidad",
    response_model=RespuestaModulo,
    summary="Módulo 7: Análisis de patrones estacionales de gasto",
    description=(
        "Detecta en qué meses el universitario gasta más y por qué. "
        "Calcula el coeficiente de variación estacional y genera un plan "
        "de anticipación financiera para los meses de mayor gasto."
    ),
)
async def estacionalidad(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_estacionalidad(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "ESTACIONALIDAD")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 8 — Presupuesto Dinámico
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/presupuesto-dinamico",
    response_model=RespuestaModulo,
    summary="Módulo 8: Generación de presupuesto semanal dinámico",
    description=(
        "Calcula un presupuesto semanal y diario realista desglosado por categoría, "
        "basado en el comportamiento histórico real. Incluye cuántos días quedan "
        "en el mes y el gasto promedio diario de referencia."
    ),
)
async def presupuesto_dinamico(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_presupuesto_dinamico(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "PRESUPUESTO_DINAMICO")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 9 — Simulación de Escenario
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/simular-escenario",
    response_model=RespuestaModulo,
    summary="Módulo 9: Simulación de escenario '¿Qué pasaría si...?'",
    description=(
        "Calcula el impacto financiero de un cambio hipotético: "
        "nuevo gasto recurrente, cancelación de suscripción, aumento de ingreso, etc. "
        "Muestra el balance resultante y si el cambio afecta la capacidad de ahorro."
    ),
)
async def simular_escenario(
    peticion: PeticionSimularEscenario,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_simular_escenario(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "SIMULAR_ESCENARIO")


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 10 — Reporte Completo
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/reporte-completo",
    response_model=RespuestaModulo,
    summary="Módulo 10: Reporte ejecutivo mensual completo (CFO Virtual)",
    description=(
        "Genera el análisis financiero más completo: KPIs del período, "
        "score de salud financiera (0-100), top alertas, oportunidades de mejora "
        "y un resumen ejecutivo en lenguaje natural generado por el coach IA."
    ),
)
async def reporte_completo(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    try:
        return await _servicio.ejecutar_reporte_completo(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "REPORTE_COMPLETO")


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINT EXTRA — Análisis Completo (todos los módulos en uno)
# ══════════════════════════════════════════════════════════════════════════════

@router.post(
    "/analisis-completo",
    response_model=RespuestaModulo,
    summary="Dashboard completo: ejecuta el reporte completo + todos los KPIs",
    description=(
        "Alias del módulo 10 optimizado para el Dashboard principal de LUKA. "
        "Retorna el reporte completo con todos los KPIs, gráfico de barras apiladas "
        "del historial y el consejo ejecutivo del coach. "
        "Usar este endpoint para la pantalla de inicio del Dashboard."
    ),
)
async def analisis_completo(
    peticion: PeticionConFiltroFecha,
    request: Request,
) -> RespuestaModulo:
    """
    Ruta conveniente que ejecuta el módulo de reporte completo.
    El Dashboard la llama al cargar la pantalla principal.
    """
    try:
        return await _servicio.ejecutar_reporte_completo(
            peticion=peticion,
            ip_origen=_obtener_ip(request),
        )
    except Exception as exc:
        _manejar_excepcion(exc, "ANALISIS_COMPLETO")