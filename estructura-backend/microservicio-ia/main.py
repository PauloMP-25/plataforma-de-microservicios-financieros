"""
main.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Punto de entrada del Microservicio IA Financiera de LUKA.
 
Cambios respecto a v3:
  - Router actualizado: 10 endpoints independientes + /analisis-completo.
  - Se elimina el endpoint inline /api/v1/ia/analizar (reemplazado por los
    10 módulos específicos del nuevo router).
  - Se elimina la instancia directa de CoachIA en main.py; toda la lógica
    vive ahora en ServicioAnalisis → motor_ia → CoachIA.
  - El ConsumidorIA de RabbitMQ se mantiene en hilo daemon (sin cambios).
  - Health check enriquecido con estado del consumidor y versión del motor.
  - Manejador global de IA_BaseException adaptado a los nuevos códigos v4.
══════════════════════════════════════════════════════════════════════════════
"""

import logging
import threading
from contextlib import asynccontextmanager
from datetime import datetime
 
import uvicorn
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
 
from app.clientes.cliente_eureka import desregistrar_de_eureka, registrar_en_eureka
from app.configuracion import obtener_configuracion
from app.excepciones import IA_BaseException
from app.mensajeria.consumidor_ia import ConsumidorIA
from app.routers import analisis  # ← Router v4 con los 10 módulos independientes


# ══════════════════════════════════════════════════════════════════════════
# LOGGING: Configuración básica para toda la app
# ══════════════════════════════════════════════════════════════════════════ 
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("ia_financiera")

config = obtener_configuracion()

# ══════════════════════════════════════════════════════════════════════════
# Estado global del consumidor RabbitMQ (accesible desde el health check)
# ══════════════════════════════════════════════════════════════════════════ 
_consumidor: ConsumidorIA | None = None
_consumidor_activo: bool = False

# ══════════════════════════════════════════════════════════════════════════════
# CONSUMIDOR RABBITMQ — hilo daemon
# ══════════════════════════════════════════════════════════════════════════════
def _iniciar_consumidor_rabbitmq() -> None:
    """
    Arranca el ConsumidorIA en un hilo daemon.
 
    Al ser daemon, muere automáticamente cuando el proceso principal termina.
    Si RabbitMQ no está disponible al arrancar, solo se loguea el error;
    el microservicio FastAPI sigue funcionando con los endpoints HTTP.
    """
    global _consumidor, _consumidor_activo
    try:
        _consumidor = ConsumidorIA()
        hilo = threading.Thread(
            target=_consumidor.iniciar,
            name="hilo-consumidor-rabbitmq",
            daemon=True,
        )
        hilo.start()
        _consumidor_activo = True
        logger.info(
            "[MAIN] Consumidor RabbitMQ iniciado en hilo '%s'.", hilo.name
        )
    except Exception as exc:
        _consumidor_activo = False
        logger.error(
            "[MAIN] No se pudo iniciar el ConsumidorIA: %s. "
            "El microservicio HTTP funcionará sin procesamiento de eventos RabbitMQ.",
            str(exc),
        )

# ══════════════════════════════════════════════════════════════════════════════
# LIFECYCLE — startup / shutdown
# ══════════════════════════════════════════════════════════════════════════════
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestiona lo que sucede cuando el microservicio arranca y se apaga.
 
    Startup:
      1. Imprime el banner de inicio con la configuración activa.
      2. Registra el microservicio en Eureka.
      3. Arranca el ConsumidorIA en hilo daemon.
 
    Shutdown:
      1. Detiene el ConsumidorIA limpiamente.
      2. Desregistra el microservicio de Eureka.
    """
    # ── Banner de inicio ──────────────────────────────────────────────────────
    logger.info("═" * 65)
    logger.info("  LUKA — Microservicio IA Financiera  v%s", config.version_app)
    logger.info("  Puerto         : %d", config.puerto)
    logger.info("  Entorno        : %s", config.entorno)
    logger.info("  Gemini         : %s (temp=%.1f, max_tokens=%d)",
                config.gemini_modelo, config.gemini_temperatura, config.gemini_max_tokens)
    logger.info("  Broker         : %s:%d", config.rabbitmq_host, config.rabbitmq_puerto)
    logger.info("  Cola entrada   : %s", config.cola_ia_procesamiento)
    logger.info("  Cola consejos  : %s", config.cola_dashboard_consejos)
    logger.info("  Cola módulos   : %s", config.cola_dashboard_modulos)
    logger.info("  Historial      : %d meses | Umbral hormiga: S/ %.2f",
                config.meses_historial, config.umbral_monto_hormiga)
    logger.info("  Regla 50/30/20 : %.0f%% / %.0f%% / %.0f%%",
                config.porcentaje_necesidades,
                config.porcentaje_deseos,
                config.porcentaje_ahorro_objetivo)
    logger.info("═" * 65)

    # --- REGISTRO EN EUREKA ---
    await registrar_en_eureka(config)
    _iniciar_consumidor_rabbitmq()
    
    yield # ── La app está corriendo ──────────────────────────────────────────

    # ── Shutdown ──────────────────────────────────────────────────────────────
    logger.info("[MAIN] Iniciando apagado del microservicio...")
 
    if _consumidor:
        try:
            _consumidor.detener()
            logger.info("[MAIN] ConsumidorIA detenido correctamente.")
        except Exception as exc:
            logger.warning("[MAIN] Error al detener el ConsumidorIA: %s", str(exc))
 
    await desregistrar_de_eureka()
    logger.info("[MAIN] Microservicio IA de LUKA detenido correctamente.")

# ══════════════════════════════════════════════════════════════════════════════
# APLICACIÓN FASTAPI
# ══════════════════════════════════════════════════════════════════════════════
app = FastAPI(
    title="LUKA — Microservicio de Inteligencia Artificial Financiera",
    description="""
## Motor de Análisis Financiero para Universitarios
 
Microservicio IA de la plataforma **LUKA**, diseñada para ayudar a
universitarios peruanos a entender y mejorar sus finanzas personales.
 
### Arquitectura: IA Centrada en Datos
El sistema separa claramente dos responsabilidades:
- **Motor Analítico** (Pandas / Scikit-Learn): realiza todos los cálculos matemáticos y estadísticos.
- **Coach Financiero** (Gemini): recibe el resumen técnico y genera consejos en lenguaje natural.
 
### 10 Módulos de Análisis Disponibles
| # | Endpoint | Descripción |
|---|----------|-------------|
| 1 | `POST /clasificar` | Valida y sugiere la categoría correcta de una transacción |
| 2 | `POST /predecir-gastos` | Proyección del gasto del próximo mes (regresión lineal) |
| 3 | `POST /detectar-anomalias` | Gastos inusuales por Z-Score estadístico |
| 4 | `POST /optimizar-suscripciones` | Gastos hormiga y suscripciones olvidadas |
| 5 | `POST /capacidad-ahorro` | Tasa de ahorro real vs. regla 50/30/20 |
| 6 | `POST /simular-meta` | Tiempo estimado para alcanzar una meta de ahorro |
| 7 | `POST /estacionalidad` | Patrones cíclicos de gasto por mes |
| 8 | `POST /presupuesto-dinamico` | Presupuesto semanal por categoría |
| 9 | `POST /simular-escenario` | Impacto de un cambio hipotético en el presupuesto |
| 10 | `POST /reporte-completo` | Reporte ejecutivo mensual con score de salud financiera |
| — | `POST /analisis-completo` | Dashboard principal: reporte completo con todos los KPIs |
    """,
    version="4.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# ══════════════════════════════════════════════════════════════════════════════
# CORS: Permitir todas las orígenes en desarrollo, restringir en producción
# ══════════════════════════════════════════════════════════════════════════════
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if not config.es_produccion else [],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ══════════════════════════════════════════════════════════════════════════════
# MANEJADORES GLOBALES DE EXCEPCIONES
# ══════════════════════════════════════════════════════════════════════════════
@app.exception_handler(IA_BaseException)
async def manejador_ia_especifico(request: Request, exc: IA_BaseException) -> JSONResponse:
    """
    Captura todas las excepciones que heredan de IA_BaseException.
    Determina el código HTTP según el código de error interno.
    """
    logger.error(
        "[EXCEPTION] [%s] en %s: %s",
        exc.codigo, request.url.path, exc.mensaje,
    )
 
    # Mapeo de códigos internos → HTTP status
    codigo_http = status.HTTP_500_INTERNAL_SERVER_ERROR
    if "GEMINI_429" in exc.codigo:
        codigo_http = status.HTTP_503_SERVICE_UNAVAILABLE
    elif "GEMINI_SAFETY" in exc.codigo:
        codigo_http = status.HTTP_422_UNPROCESSABLE_ENTITY
    elif "GEMINI_401" in exc.codigo:
        codigo_http = status.HTTP_503_SERVICE_UNAVAILABLE
    elif "DATA_400" in exc.codigo:
        codigo_http = status.HTTP_422_UNPROCESSABLE_ENTITY
    elif "MODULE_404" in exc.codigo:
        codigo_http = status.HTTP_404_NOT_FOUND
    elif "CONFIG" in exc.codigo or "RABBIT" in exc.codigo:
        codigo_http = status.HTTP_500_INTERNAL_SERVER_ERROR
 
    return JSONResponse(
        status_code=codigo_http,
        content={
            "estado": codigo_http,
            "codigo_error": exc.codigo,
            "mensaje": exc.mensaje,
            "detalles": str(exc.detalles) if exc.detalles else None,
            "timestamp": exc.timestamp,
            "ruta": str(request.url.path),
        },
    )

@app.exception_handler(Exception)
async def manejador_global_fallback(request: Request, exc: Exception) -> JSONResponse:
    """Manejador de última instancia para errores no controlados."""
    logger.error(
        "[EXCEPTION] FALLO CRÍTICO en %s: %s",
        request.url.path, str(exc), exc_info=True,
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "estado": 500,
            "codigo_error": "IA_UNKNOWN_CRITICAL",
            "mensaje": "Error interno no controlado en el motor de IA.",
            "timestamp": datetime.now().isoformat(),
            "ruta": str(request.url.path),
        },
    )


# ══════════════════════════════════════════════════════════════════════════════
# RUTAS
# ══════════════════════════════════════════════════════════════════════════════

# ── Router principal con los 10 módulos + /analisis-completo ─────────────────
app.include_router(analisis.router)

# ── Health Check ─────────────────────────────────────────────────────────────
@app.get(
    "/actuator/health",
    tags=["Sistema"],
    summary="Estado del microservicio",
    description="Verifica que el microservicio, el motor analítico y el consumidor RabbitMQ estén operativos.",
)

async def health_check() -> dict:
    """
    Endpoint de health check compatible con el servidor Eureka y el API Gateway.
 
    Retorna el estado de cada componente del microservicio:
      - motor_analitico : siempre UP (Pandas/Scikit-Learn son locales).
      - coach_ia        : UP si la API Key de Gemini está configurada.
      - consumidor_rmq  : UP si el hilo del ConsumidorIA arrancó correctamente.
    """
    gemini_configurado = bool(config.gemini_api_key)
 
    componentes = {
        "motor_analitico": {
            "estado": "UP",
            "descripcion": "Pandas + Scikit-Learn operativos",
        },
        "coach_ia": {
            "estado": "UP" if gemini_configurado else "DOWN",
            "modelo": config.gemini_modelo,
            "descripcion": "Gemini configurado" if gemini_configurado else "API Key no configurada",
        },
        "consumidor_rabbitmq": {
            "estado": "UP" if _consumidor_activo else "DEGRADADO",
            "broker": f"{config.rabbitmq_host}:{config.rabbitmq_puerto}",
            "cola": config.cola_ia_procesamiento,
            "descripcion": "Procesando eventos" if _consumidor_activo else "Sin conexión al broker",
        },
    }
 
    # El servicio está UP si al menos el motor y el coach funcionan.
    # RabbitMQ caído es degradado pero no crítico (los endpoints HTTP siguen funcionando).
    estado_global = "UP" if gemini_configurado else "DOWN"
    if not _consumidor_activo:
        estado_global = "DEGRADADO"
 
    return {
        "estado": estado_global,
        "servicio": config.nombre_app,
        "version": config.version_app,
        "entorno": config.entorno,
        "timestamp": datetime.now().isoformat(),
        "componentes": componentes,
        "endpoints_disponibles": 11,
    }



# ══════════════════════════════════════════════════════════════════════════════
# PUNTO DE ENTRADA DIRECTO
# ══════════════════════════════════════════════════════════════════════════════
 
if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=config.puerto,
        reload=config.entorno == "desarrollo",
        log_level="info",
    )