"""
cliente_eureka.py — Integración con el Servidor Eureka.
Gestiona el ciclo de vida del registro:
  1. Registro al iniciar (startup).
  2. Latidos (heartbeats) automáticos para mantenerse "vivo".
  3. Desregistro al apagar (shutdown) para evitar tráfico muerto.
"""

import py_eureka_client.eureka_client as eureka_client
import logging
import app.configuracion as Configuracion

# Configuración de logs para ver el estado de la conexión en consola
logger = logging.getLogger(__name__)

async def registrar_en_eureka(config: Configuracion) -> None:
    """
    Realiza el registro del microservicio en el servidor Java Eureka.
    Se ejecuta al arrancar la aplicación FastAPI.
    """
    logger.info(
        "Intentando registrar '%s' en el servidor Eureka: %s",
        config.nombre_eureka_mayusculas,
        config.eureka_servidor_url
    )

    try:
        # Iniciamos el cliente de forma asíncrona
        await eureka_client.init_async(
            # ── Servidor de Descubrimiento ───────────────────────────────────
            eureka_server=config.eureka_servidor_url,

            # ── Identidad del Servicio ───────────────────────────────────────
            # Este nombre aparecerá en el Dashboard de Eureka
            app_name=config.nombre_eureka_mayusculas,

            # ── Ubicación de la Instancia ────────────────────────────────────
            # El Gateway usará este host y puerto para enviarnos peticiones
            instance_host=config.eureka_instancia_host,
            instance_port=config.eureka_instancia_puerto,

            # ── Rutas de Estado (Health Checks) ──────────────────────────────
            # Ruta que Eureka consultará para saber si estamos saludables
            health_check_url=f"http://{config.eureka_instancia_host}:{config.eureka_instancia_puerto}/actuator/health",
            home_page_url=f"http://{config.eureka_instancia_host}:{config.eureka_instancia_puerto}/",

            # ── Tiempos de Latido (Heartbeats) ───────────────────────────────
            # renewal_interval: cada cuánto avisamos que estamos vivos (10s)
            # duration: cuánto tiempo nos espera Eureka antes de darnos de baja (30s)
            renewal_interval_in_secs=10, 
            duration_in_secs=30,

            # ── Información Adicional (Metadata) ─────────────────────────────
            metadata={
                "version": config.version_app,
                "lenguaje": "Python (FastAPI)",
                "entorno": config.entorno,
                "proyecto": "SaaS Financiero"
            }
        )

        logger.info(
            "Registro exitoso en Eureka como '%s' (%s:%s)",
            config.nombre_eureka_mayusculas,
            config.eureka_instancia_host,
            config.eureka_instancia_puerto
        )

    except Exception as error:
        # No detenemos la app si Eureka falla; el cliente reintentará solo.
        logger.error(
            "No se pudo registrar en Eureka: %s. "
            "El servicio continuará funcionando localmente y reintentará el registro.",
            str(error)
        )

async def desregistrar_de_eureka() -> None:
    """
    Avisa al servidor Eureka que este microservicio se está apagando.
    Esto evita que el Gateway envíe peticiones a un servicio que ya no existe.
    """
    logger.info("Solicitando desregistro del servidor Eureka...")
    try:
        await eureka_client.stop_async()
        logger.info("Microservicio desregistrado correctamente.")
    except Exception as error:
        logger.warning("Hubo un problema al intentar desregistrar: %s", str(error))