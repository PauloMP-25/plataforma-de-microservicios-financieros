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
 
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
 
from app.configuracion import obtener_configuracion
from app.clientes.cliente_eureka import registrar_en_eureka, desregistrar_de_eureka
from app.mensajeria.consumidor_ia import ConsumidorIA
from app.routers import analisis
from app.excepciones import IA_BaseException


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
    title="Microservicio IA Financiera",
    description="""

## Motor de Inteligencia Artificial para Análisis Financiero
### Novedades v3
- **Análisis histórico comparativo**: historial de 6 meses incluido en cada evento
- **10 módulos especializados** con System Prompts propios para Gemini
- **Salida al Dashboard**: `ResultadoAnalisisIA` con `metadata_grafico` para charts
- **Modo dual**: eventos automáticos (por transacción) y manuales (botón del Dashboard)
- **KPIs por módulo**: valor destacado para el header de cada widget
 
### Flujo de eventos
```
nucleo-financiero → exchange.ia → cola.ia.procesamiento
    ├── TRANSACCION_RECIENTE → Gemini → cola.dashboard.consejos
    └── CONSULTA_MODULO      → Gemini → cola.dashboard.modulos
```
Este microservicio actúa como el **cerebro analítico** del sistema SaaS Financiero.
Consume datos del `microservicio-nucleo-financiero` y procesa eventos de transacciones 
desde **RabbitMQ** (`exchange.ia`) y expone 10 módulos de análisis financiero vía HTTP.

### Módulos HTTP disponibles
| # | Modulo | Descripción | Tipo Modulo | Gráfico
|---|----------|-------------|
| 1 | Analisis Automatico | Etiqueta transacciones automáticamente | -- |
| 2 | Prediccion de Gastos | Proyecta gastos del próximo mes | Lineas |
| 3 | Detección de Anomalías | Identifica transacciones inusuales | -- |
| 4 | Gastos Hormiga | Detecta gastos hormiga | Barras |
| 5 | Capacidad de Ahorro | Calcula capacidad real de ahorro | Dona |
| 6 | Metas Financieras | Proyecta tiempo para alcanzar metas | -- |
| 7 | Estacionalidad | Detecta patrones mensuales | Lineas |
| 8 | Presupuesto Dinámico | Presupuesto semanal recomendado | -- |
| 9 | Comparacion Mensual | Compara gastos e ingresos mensuales | Barras Agrupadas |
| 10 | Reporte Dinamico | Reporte ejecutivo en lenguaje natural | Barras Apiladas |

### Dependencias
- **microservicio-nucleo-financiero** — Puerto 8085 (fuente de datos)
- **microservicio-auditoria** — Puerto 8082 (registro de eventos)
    """,
    version=config.version_app,
    contact={
        "name": "Paulo Cesar Moron Poma",
        "email": "paulomoronpoma@gmail.com",
    },
    license_info={
        "name": "Privado — Sistema SaaS Financiero",
    },
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
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


@app.get("/actuator/health", tags=["Sistema"], summary="Health check del servicio")
async def health_check():
    """
    Este endpoint es el que Eureka consultará cada 10-30 segundos 
    para verificar que Python sigue vivo.
    """
    # Verificamos si el hilo existe y está activo
    hilo_vivo = False
    for thread in threading.enumerate():
        if thread.name == "hilo-consumidor-rabbitmq" and thread.is_alive():
            hilo_vivo = True
            break
            
    estado_general = "UP" if hilo_vivo else "DOWN"
    
    return {
        "status": estado_general,
        "servicio": config.nombre_app,
        "version": config.version_app,
        "gemini": config.gemini_modelo,
        "broker": {
            "host":            f"{config.rabbitmq_host}:{config.rabbitmq_puerto}",
            "consumidor_activo": _consumidor_activo,
            "cola_entrada":    config.cola_ia_procesamiento,
            "cola_consejos":   config.cola_dashboard_consejos,
            "cola_modulos":    config.cola_dashboard_modulos,
        },
        "configuracion": {
            "meses_historial":       config.meses_historial,
            "umbral_gasto_hormiga":  config.umbral_gasto_hormiga,
            "factor_ahorro":         config.factor_seguridad_ahorro,
        },
        "timestamp": datetime.now().isoformat()
    }


@app.get(
    "/",
    tags=["Sistema"],
    summary="Información del microservicio",
    include_in_schema=False,
)
async def raiz():
    return {
        "servicio": "Microservicio IA Financiera",
        "version": config.version_app,
        "documentacion": "/docs",
        "health": "/actuator/health",
        "endpoints_ia": [
            "POST /api/v1/ia/clasificar",
            "POST /api/v1/ia/predecir-gastos",
            "POST /api/v1/ia/detectar-anomalias",
            "POST /api/v1/ia/optimizar-suscripciones",
            "POST /api/v1/ia/capacidad-ahorro",
            "POST /api/v1/ia/simular-meta",
            "POST /api/v1/ia/estacionalidad",
            "POST /api/v1/ia/presupuesto-dinamico",
            "POST /api/v1/ia/simular-escenario",
            "POST /api/v1/ia/reporte-completo",
            "POST /api/v1/ia/analisis-completo",
        ],
    }