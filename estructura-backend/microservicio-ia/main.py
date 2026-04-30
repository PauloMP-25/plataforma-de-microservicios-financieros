"""
main.py  ·  v3 — Dashboard + Persistencia + Multi-Módulos
══════════════════════════════════════════════════════════════════════════════
Punto de entrada del Microservicio IA Financiera v3.
 
Cambios respecto a v2:
  - ConsumidorIA v3 con modo dual (automático + manual)
  - Endpoint HTTP nuevo: POST /api/v1/ia/solicitar-modulo
    (permite al Dashboard disparar un CONSULTA_MODULO directamente via HTTP,
     sin pasar por RabbitMQ, útil para desarrollo y testing)
  - Health check enriquecido con estado del consumidor
══════════════════════════════════════════════════════════════════════════════
"""

import logging
import threading
from contextlib import asynccontextmanager
from datetime import datetime
 
from fastapi import Body, FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn
 
from app.configuracion import obtener_configuracion
from app.clientes.cliente_eureka import registrar_en_eureka, desregistrar_de_eureka
from app.mensajeria.consumidor_ia import ConsumidorIA
from app.modelos.evento_analisis import EventoAnalisisIA, ResultadoAnalisisIA
from app.routers import analisis
from app.excepciones import IA_BaseException
from app.servicios.coach_ia import CoachIA


# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("ia_financiera")

config = obtener_configuracion()

# Estado global del consumidor (accesible desde el health check)
_consumidor: ConsumidorIA | None = None
_consumidor_activo: bool = False

# ── Consumidor RabbitMQ ─────────────────────────────────────────────
def _iniciar_consumidor_rabbitmq() -> None:
    """
    Arranca el ConsumidorIA en un hilo daemon.
    Al ser daemon, muere automáticamente cuando el proceso principal termina.
    Si RabbitMQ no está disponible al arrancar, solo se loguea el error;
    el microservicio FastAPI sigue funcionando con normalidad.
    """
    global _consumidor
    try:
        _consumidor = ConsumidorIA()
        hilo = threading.Thread(
            target=_consumidor.iniciar,
            name="hilo-consumidor-rabbitmq",
            daemon=True,
        )
        hilo.start()
        _consumidor_activo = True
        logger.info("[MAIN] Consumidor RabbitMQ iniciado en hilo '%s'.", hilo.name)
    except Exception as exc:
        logger.error(
            "[MAIN] No se pudo iniciar el ConsumidorIA: %s. "
            "El microservicio HTTP funcionará sin procesamiento de eventos.",
            str(exc),
        )

# ── Lifecycle: startup / shutdown ─────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestiona lo que sucede cuando el microservicio nace y muere.
    """
    logger.info("═" * 65)
    logger.info("  Microservicio IA Financiera  v%s  iniciando...", config.version_app)
    logger.info("  Puerto         : %d", config.puerto)
    logger.info("  Entorno        : %s", config.entorno)
    logger.info("  Gemini         : %s (temp=%.1f)", config.gemini_modelo, config.gemini_temperatura)
    logger.info("  Broker         : %s:%d", config.rabbitmq_host, config.rabbitmq_puerto)
    logger.info("  Cola entrada   : %s", config.cola_ia_procesamiento)
    logger.info("  Cola consejos  : %s", config.cola_dashboard_consejos)
    logger.info("  Cola módulos   : %s", config.cola_dashboard_modulos)
    logger.info("  Historial      : %d meses", config.meses_historial)
    logger.info("═" * 65)

    # --- REGISTRO EN EUREKA ---
    await registrar_en_eureka(config)
    await _iniciar_consumidor_rabbitmq()
    yield # ── La app está corriendo ──────────────────────────────────────────
    # --- DESREGISTRO DE EUREKA ---
    await desregistrar_de_eureka()
    logger.info("Microservicio IA Financiera detenido correctamente.")

    # Shutdown
    if _consumidor:
        _consumidor.detener()
    await desregistrar_de_eureka()
    logger.info("[MAIN] Microservicio IA detenido correctamente.")

# ── Aplicación FastAPI ────────────────────────────────────────────────────────
app = FastAPI(
    title="Microservicio de Inteligencia Artificial Financiera",
    description="""
    ## Motor de Análisis Predictivo y Correctivo.
    
    Este servicio procesa datos financieros mediante Google Gemini 1.5 Flash para ofrecer:
    * **Análisis por Transacción:** Consejos inmediatos tras un gasto.
    * **Módulos bajo demanda:** 10 motores de análisis (Predicción, Gasto Hormiga, etc.).
    
    ### Flujo de Eventos:
    1. **Síncrono (HTTP):** El Dashboard solicita un análisis y recibe respuesta inmediata.
    2. **Asíncrono (RabbitMQ):** El Núcleo Financiero envía una transacción, la IA procesa y publica el resultado directamente hacia el Dashboard.
    """,
    version="3.0.0"
)

# ── CORS ──────────────────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # En producción, reemplazar con dominios específicos
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Manejador global de excepciones ──────────────────────────────────────────
@app.exception_handler(IA_BaseException)
async def manejador_ia_especifico(request: Request, exc: IA_BaseException):
    """
    Captura todas las excepciones que heredan de IA_BaseException.
    Muestra el código de error específico (IA_CONFIG, IA_GEMINI, etc.)
    """
    logger.error(f"Error de Dominio [{exc.codigo}] en {request.url.path}: {exc.mensaje}")
    
    # Determinamos el status code basado en el código de error
    status_code = status.HTTP_400_BAD_REQUEST
    if "GEMINI_429" in exc.codigo or "BIZ_500" in exc.codigo:
        status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    elif "CONFIG" in exc.codigo or "RABBIT" in exc.codigo:
        status_code = status.HTTP_500_INTERNAL_SERVER_ERROR

    return JSONResponse(
        status_code=status_code,
        content={
            "estado": status_code,
            "codigo_error": exc.codigo,
            "mensaje": exc.mensaje,
            "detalles": exc.detalles,
            "timestamp": exc.timestamp,
            "ruta": str(request.url.path),
        },
    )

@app.exception_handler(Exception)
async def manejador_global_fallback(request: Request, exc: Exception):
    """Manejador de última instancia para errores no controlados."""
    logger.error(f"FALLO CRÍTICO NO CONTROLADO en {request.url.path}: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "estado": 500,
            "codigo_error": "IA_UNKNOWN_CRITICAL",
            "mensaje": "Error interno no controlado en el motor de IA.",
            "fecha_hora": datetime.now().isoformat(),
        },
    )


# ── Rutas ─────────────────────────────────────────────────────────────────────
app.include_router(analisis.router)

# --- MODULO HTTP (APIs Directas) ---
@app.post(
    "/api/v1/ia/analizar", 
    response_model=ResultadoAnalisisIA,
    tags=["Módulos de IA"],
    summary="Ejecutar análisis bajo demanda",
    description="Permite invocar cualquiera de los 10 módulos de IA pasando el historial de 6 meses."
)
async def analizar_modulo_http(
    evento: EventoAnalisisIA = Body(
        ...,
        example={
            "id_usuario": "user_uuid_12345",
            "tipo_solicitud": "CONSULTA_MODULO",
            "modulo_solicitado": "GASTO_HORMIGA",
            "historial_mensual": [
                {
                    "anio": 2026, "mes": 3, 
                    "totalIngresos": 3000, "totalGastos": 2500,
                    "gastosPorCategoria": {"Alimentación": 800, "Suscripciones": 150}
                }
            ]
        }
    )
):
    resultado = coach.analizar(evento)
    if not resultado:
        raise HTTPException(status_code=500, detail="Error generando respuesta de IA")
    return resultado



if __name__ == "__main__":
    import uvicorn
    # host 0.0.0.0 permite que el microservicio sea visto por otros 
    # contenedores de Docker o servicios en tu red local.
    uvicorn.run(app, host="0.0.0.0", port=8000)