import json
import logging
import aio_pika
from typing import Optional, Union
from app.libreria_comun.configuracion.settings import settings
from app.libreria_comun.configuracion.routing_keys import RoutingKeys
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO, EventoTransaccionalDTO
from app.libreria_comun.seguridad.contexto import get_correlation_id

logger = logging.getLogger("ia_financiera.mensajeria.publicador")

class PublicadorAuditoria:
    """
    Publicador asíncrono (no bloqueante) para eventos de auditoría y trazabilidad.
    Usa aio-pika para integrarse correctamente con el loop de FastAPI.
    """
    
    _instancia: Optional['PublicadorAuditoria'] = None
    _conexion: Optional[aio_pika.RobustConnection] = None
    _canal: Optional[aio_pika.RobustChannel] = None

    def __new__(cls):
        if cls._instancia is None:
            cls._instancia = super(PublicadorAuditoria, cls).__new__(cls)
        return cls._instancia

    async def conectar(self):
        """Establece la conexión persistente con RabbitMQ."""
        if self._conexion is None or self._conexion.is_closed:
            from app.configuracion import obtener_configuracion
            import urllib.parse
            import os
            import ssl
            
            config = obtener_configuracion()
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
            logger.info("[AMQP] Conexión establecida con RabbitMQ para auditoría.")

    async def publicar_evento(
        self, 
        evento: Union[EventoAuditoriaDTO, EventoTransaccionalDTO], 
        routing_key: Optional[str] = None
    ):
        """
        Publica un evento de auditoría o transacción de forma asíncrona.
        """
        try:
            await self.conectar()
            
            # Determinar routing key por defecto según el tipo de evento
            if routing_key is None:
                if isinstance(evento, EventoTransaccionalDTO):
                    routing_key = RoutingKeys.AUDITORIA_TRANSACCION
                else:
                    routing_key = RoutingKeys.AUDITORIA_EVENTO

            # El Exchange ya debe estar creado por los microservicios de Java
            exchange = await self._canal.get_exchange("exchange.auditoria", ensure=False)
            
            # Serializar el modelo a JSON (usando Pydantic v2 model_dump)
            cuerpo = json.dumps(evento.model_dump(by_alias=True, mode="json")).encode()
            
            mensaje = aio_pika.Message(
                body=cuerpo,
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers={"X-Correlation-ID": get_correlation_id() or "SISTEMA-IA"}
            )
            
            await exchange.publish(mensaje, routing_key=routing_key)
            logger.debug(f"[AMQP] Evento publicado en {routing_key}")
            
        except Exception as e:
            logger.error(f"[AMQP] Error al publicar evento de auditoría: {str(e)}")

    async def cerrar(self):
        """Cierra la conexión limpiamente."""
        if self._conexion and not self._conexion.is_closed:
            await self._conexion.close()
            logger.info("[AMQP] Conexión de publicación cerrada.")

# Instancia global
publicador_auditoria = PublicadorAuditoria()
