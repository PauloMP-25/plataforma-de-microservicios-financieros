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
            import urllib.parse
            import os
            import ssl
            
            user_escaped = urllib.parse.quote_plus(config.rabbitmq_usuario)
            pass_escaped = urllib.parse.quote_plus(config.rabbitmq_password)
            vhost_escaped = urllib.parse.quote(config.rabbitmq_vhost, safe='')
            
            es_ssl = os.getenv("RABBITMQ_SSL_ENABLED", "true").lower() == "true"
            scheme = "amqps" if es_ssl else "amqp"
            puerto = os.getenv("RABBITMQ_PUERTO", str(config.rabbitmq_puerto))
            
            url = f"{scheme}://{user_escaped}:{pass_escaped}@{config.rabbitmq_host}:{puerto}/{vhost_escaped}"
            
            ssl_context = None
            if es_ssl:
                ssl_context = ssl.create_default_context()
                ssl_context.check_hostname = False
                ssl_context.verify_mode = ssl.CERT_NONE
                
            self._conexion = await aio_pika.connect_robust(url, ssl_context=ssl_context)
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
