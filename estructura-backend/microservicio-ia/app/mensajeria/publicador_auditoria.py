import json
import logging
import aio_pika
from typing import Optional
from app.libreria_comun.configuracion.settings import settings
from app.libreria_comun.configuracion.routing_keys import RoutingKeys
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO
from app.libreria_comun.seguridad.contexto import get_correlation_id

logger = logging.getLogger("ia_financiera.mensajeria.publicador")
config = obtener_configuracion()

class PublicadorAuditoria:
    """
    Publicador asíncrono (no bloqueante) para eventos de auditoría.
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
            url = settings.rabbit_url
            self._conexion = await aio_pika.connect_robust(url)
            self._canal = await self._conexion.channel()
            logger.info("[AMQP] Conexión establecida con RabbitMQ para auditoría.")

    async def publicar_evento(self, evento: EventoAuditoriaDTO, routing_key: str = RoutingKeys.AUDITORIA_EVENTO):
        """
        Publica un evento de auditoría de forma asíncrona.
        """
        try:
            await self.conectar()
            
            # El Exchange ya debe estar creado por los microservicios de Java
            exchange = await self._canal.get_exchange("exchange.auditoria", ensure=False)
            
            cuerpo = json.dumps(evento.model_dump(by_alias=True, mode="json")).encode()
            
            mensaje = aio_pika.Message(
                body=cuerpo,
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers={"X-Correlation-ID": get_correlation_id()}
            )
            
            await exchange.publish(mensaje, routing_key=routing_key)
            logger.debug(f"[AMQP] Evento de auditoría publicado: {routing_key}")
            
        except Exception as e:
            logger.error(f"[AMQP] Error al publicar evento de auditoría: {str(e)}")
            # No relanzamos la excepción para no bloquear el flujo principal del microservicio
            # La auditoría es importante pero no debe tumbar el servicio de IA

    async def cerrar(self):
        """Cierra la conexión limpiamente."""
        if self._conexion and not self._conexion.is_closed:
            await self._conexion.close()
            logger.info("[AMQP] Conexión de publicación cerrada.")

# Instancia global para ser usada con Depends o directamente
publicador_auditoria = PublicadorAuditoria()
