import json
import logging
import aio_pika
from typing import Optional, Dict, Any
from app.configuracion import obtener_configuracion
from app.libreria_comun.seguridad.contexto import get_correlation_id

logger = logging.getLogger("ia_financiera.mensajeria.publicador_mensajeria")
config = obtener_configuracion()

class PublicadorMensajeria:
    """
    Publicador para envío de mensajes (Email/SMS) vía microservicio-mensajeria.
    """
    
    _instancia: Optional['PublicadorMensajeria'] = None
    _conexion: Optional[aio_pika.RobustConnection] = None
    _canal: Optional[aio_pika.RobustChannel] = None

    def __new__(cls):
        if cls._instancia is None:
            cls._instancia = super(PublicadorMensajeria, cls).__new__(cls)
        return cls._instancia

    async def conectar(self):
        if self._conexion is None or self._conexion.is_closed:
            # Reutilizamos la URL de RabbitMQ de la configuración
            url = f"amqp://{config.rabbitmq_usuario}:{config.rabbitmq_password}@{config.rabbitmq_host}:{config.rabbitmq_puerto}/{config.rabbitmq_vhost}"
            self._conexion = await aio_pika.connect_robust(url)
            self._canal = await self._conexion.channel()
            logger.info("[AMQP] Conexión establecida con RabbitMQ para mensajería.")

    async def enviar_email(self, destinatario: str, asunto: str, cuerpo: str, es_html: bool = False):
        """Publica una solicitud de email en la cola correspondiente."""
        try:
            await self.conectar()
            
            # El Exchange de mensajería definido en Java es "exchange.mensajeria"
            # Pero las colas están ligadas a routing keys.
            # En Java: COLA_EMAIL_ENVIAR = "cola.mensajeria.email.enviar"
            # Vamos a publicar directamente a la cola o al exchange con una RK genérica si existe.
            # Según ConfiguracionRabbitMQ.java, no hay RK para email genérico, solo para OTP.
            # Así que publicaremos directamente a la cola.
            
            payload = {
                "destinatario": destinatario,
                "asunto": asunto,
                "cuerpo": cuerpo,
                "esHtml": es_html,
                "variables": {}
            }
            
            cuerpo_bin = json.dumps(payload).encode()
            
            mensaje = aio_pika.Message(
                body=cuerpo_bin,
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers={"X-Correlation-ID": get_correlation_id() or "ALERTA-SISTEMA"}
            )
            
            # Publicar directamente a la cola para simplicidad
            await self._canal.default_exchange.publish(
                mensaje, 
                routing_key="cola.mensajeria.email.enviar"
            )
            logger.info(f"[AMQP] Solicitud de email alerta enviada a {destinatario}")
            
        except Exception as e:
            logger.error(f"[AMQP] Error al publicar email: {str(e)}")

publicador_mensajeria = PublicadorMensajeria()
