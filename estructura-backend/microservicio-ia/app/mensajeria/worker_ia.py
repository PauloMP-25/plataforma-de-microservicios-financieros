import json
import logging
import asyncio
import aio_pika
from uuid import uuid4
from app.libreria_comun.configuracion.settings import settings
from app.libreria_comun.seguridad.contexto import set_correlation_id
from app.clientes.luka_clients import ClienteFinanciero, ClientePerfil
from app.servicios.servicio_analisis import ServicioAnalisis, obtener_servicio_analisis

logger = logging.getLogger("ia_financiera.worker")

class WorkerIA:
    """
    Worker Asíncrono (aio-pika) para procesamiento On-Demand.
    Orquestra la captura de datos y el análisis de IA disparado por eventos.
    """
    def __init__(self):
        self.cliente_financiero = ClienteFinanciero()
        self.cliente_perfil = ClientePerfil()
        # En un worker no tenemos el Dependency Injection de FastAPI automático,
        # así que instanciamos o usamos la utilidad.
        self.servicio = obtener_servicio_analisis()

    async def procesar_solicitud_analisis(self, message: aio_pika.IncomingMessage):
        """
        Callback de procesamiento de mensajes.
        """
        async with message.process():
            try:
                # 1. Trazabilidad: Capturar Correlation-ID del mensaje
                correl_id = message.headers.get("X-Correlation-ID", str(uuid4()))
                set_correlation_id(correl_id)
                
                logger.info(f"[WORKER] Mensaje recibido | Trace: {correl_id}")

                # 2. Deserializar payload (Espejo de lo enviado por ms-nucleo o Dashboard)
                body = json.loads(message.body.decode())
                usuario_id = body.get("usuarioId")
                token = body.get("token")  # Token para llamadas inter-servicios
                
                if not usuario_id or not token:
                    logger.error("[WORKER] Payload incompleto: falta usuarioId o token")
                    return

                # 3. Orquestación: Obtener contexto desde microservicios Java
                logger.info(f"[WORKER] Recuperando contexto para usuario {usuario_id}...")
                
                # Ejecutamos llamadas concurrentes para optimizar tiempo
                transacciones_task = self.cliente_financiero.obtener_transacciones(usuario_id, token)
                perfil_task = self.cliente_perfil.obtener_perfil_estrategico(usuario_id, token)
                
                transacciones, perfil = await asyncio.gather(transacciones_task, perfil_task)

                # 4. Análisis: Ejecutar lógica de IA
                logger.info(f"[WORKER] Ejecutando análisis IA para usuario {usuario_id}")
                
                # Aquí se llamaría al servicio de análisis pasando los datos recuperados
                # resultado = await self.servicio.procesar_analisis_eventos(usuario_id, transacciones, perfil)
                
                logger.info(f"[WORKER] Análisis completado exitosamente para {usuario_id}")

            except Exception as exc:
                logger.error(f"[WORKER] Fallo en el procesamiento: {str(exc)}", exc_info=True)
                # El context manager se encarga del NACK si hay excepción no capturada

    async def iniciar(self):
        """Inicia el bucle de consumo de RabbitMQ."""
        try:
            conexion = await aio_pika.connect_robust(settings.rabbit_url)
            canal = await conexion.channel()
            
            # Prefetch=1 para no saturar el worker con análisis pesados
            await canal.set_qos(prefetch_count=1)
            
            cola = await canal.declare_queue("cola.ia.procesamiento", durable=True)
            
            logger.info("[WORKER] Worker IA iniciado. Escuchando 'cola.ia.procesamiento'...")
            
            await cola.consume(self.procesar_solicitud_analisis)
            
            # Mantener el worker corriendo
            await asyncio.Future()
            
        except Exception as exc:
            logger.critical(f"[WORKER] Error fatal al conectar con RabbitMQ: {exc}")
            raise
