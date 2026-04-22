"""
main.py — Punto de entrada del Microservicio IA Financiera. 
FastAPI con documentación automática Swagger/OpenAPI en /docs e integración con Eureka Server..
Puerto: 8086
"""

import logging
from contextlib import asynccontextmanager
from datetime import datetime

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.configuracion import obtener_configuracion
from app.clientes.cliente_eureka import registrar_en_eureka, desregistrar_de_eureka
from app.routers import analisis


# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("ia_financiera")

config = obtener_configuracion()


# ── Lifecycle: startup / shutdown ─────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestiona lo que sucede cuando el microservicio nace y muere.
    """
    logger.info("═" * 60)
    logger.info("  Microservicio IA Financiera iniciando...")
    logger.info("  Puerto         : %d", config.puerto)
    logger.info("  Entorno        : %s", config.entorno)
    logger.info("═" * 60)

    # --- REGISTRO EN EUREKA ---
    await registrar_en_eureka(config)
    yield
    # --- DESREGISTRO DE EUREKA ---
    await desregistrar_de_eureka()
    logger.info("Microservicio IA Financiera detenido correctamente.")


# ── Aplicación FastAPI ────────────────────────────────────────────────────────
app = FastAPI(
    title="Microservicio IA Financiera",
    description="""
## Motor de Inteligencia Artificial para Análisis Financiero

Este microservicio actúa como el **cerebro analítico** del sistema SaaS Financiero.
Consume datos del `microservicio-nucleo-financiero` y ejecuta 10 módulos de análisis inteligente.
### Módulos disponibles

| # | Módulo | Descripción |
|---|--------|-------------|
| 1 | `clasificar` | Etiqueta transacciones automáticamente |
| 2 | `predecir-gastos` | Proyecta gastos del próximo mes |
| 3 | `detectar-anomalias` | Identifica transacciones inusuales |
| 4 | `optimizar-suscripciones` | Detecta gastos hormiga y suscripciones |
| 5 | `capacidad-ahorro` | Calcula capacidad real de ahorro |
| 6 | `simular-meta` | Proyecta tiempo para alcanzar metas |
| 7 | `estacionalidad` | Detecta patrones mensuales |
| 8 | `presupuesto-dinamico` | Recomienda presupuesto semanal |
| 9 | `simular-escenario` | Simula impacto de nuevos gastos/ingresos |
| 10 | `reporte-completo` | Genera reporte ejecutivo en lenguaje natural |

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
@app.exception_handler(Exception)
async def manejador_global(request: Request, exc: Exception):
    logger.error("Error inesperado en %s: %s", request.url.path, str(exc), exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "estado": 500,
            "error": "ERROR_INTERNO",
            "mensaje": "Ha ocurrido un error inesperado en el motor de IA.",
            "ruta": str(request.url.path),
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
    return {
        "estado": "UP",
        "servicio": config.nombre_app,
        "version": config.version_app,
        "instancia_eureka": config.id_app_eureka,
        "timestamp": datetime.now().isoformat(),
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