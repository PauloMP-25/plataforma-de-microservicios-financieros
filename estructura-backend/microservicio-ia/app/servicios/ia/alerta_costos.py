import logging
from datetime import datetime
import asyncio
from app.persistencia.cache_redis import CacheRedis
from app.configuracion import obtener_configuracion

logger = logging.getLogger("ia_financiera.servicios.alerta_costos")

class AlertaCostosIA:
    """
    Servicio encargado de monitorear el gasto de tokens de Gemini
    y emitir alertas si se superan los umbrales definidos, usando Redis.
    """

    @staticmethod
    async def verificar_alerta_costos_diarios():
        config = obtener_configuracion()
        today = datetime.now().date()
        clave_costo = f"ia:costo:diario:{today}"
        
        try:
            cache = CacheRedis()
            costo_str = cache.obtener(clave_costo)
            costo_total = float(costo_str) if costo_str else 0.0
            
            logger.info(f"[MONITOR-COSTOS] Gasto acumulado hoy ({today}): ${costo_total:.4f} USD")
            
            if costo_total > config.umbral_alerta_costo_diario_usd:
                logger.warning(f"⚠️ [ALERTA-GASTO] El costo diario (${costo_total:.2f}) superó el umbral (${config.umbral_alerta_costo_diario_usd:.2f})")
                await AlertaCostosIA._notificar_exceso_costo(costo_total)
                
        except Exception as e:
            logger.error(f"[MONITOR-COSTOS] Error al verificar costos: {e}")

    @staticmethod
    async def _notificar_exceso_costo(costo: float):
        """
        Envía una notificación de alerta vía ms-mensajeria.
        """
        from app.mensajeria.publicador_mensajeria import publicador_mensajeria
        
        asunto = "⚠️ ALERTA CRÍTICA: Límite de Gasto Gemini Excedido"
        cuerpo = (
            f"Hola Paulo,\n\n"
            f"Se ha detectado que el costo acumulado de Gemini hoy es de ${costo:.2f} USD,\n"
            f"superando el umbral diario configurado.\n\n"
            f"Por favor, revisa el dashboard de costos y el uso de los microservicios de IA.\n\n"
            f"Atentamente,\nLUKA Assistant"
        )
        
        await publicador_mensajeria.enviar_email(
            destinatario="paulomoronpoma@gmail.com",
            asunto=asunto,
            cuerpo=cuerpo
        )
        logger.critical(f"🛑 ALERTA DE GASTO ENVIADA - Costo: ${costo:.2f} USD.")

async def job_monitoreo_costos():
    """Bucle infinito para el job de monitoreo (ejecución cada 4 horas)."""
    while True:
        await AlertaCostosIA.verificar_alerta_costos_diarios()
        # Esperar 4 horas entre verificaciones
        await asyncio.sleep(4 * 3600)
