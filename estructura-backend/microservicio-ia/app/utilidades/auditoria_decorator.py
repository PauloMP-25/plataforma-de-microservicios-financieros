import functools
import logging
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria

logger = logging.getLogger(__name__)

def auditar_operacion(accion_base: str):
    """
    Decorador para auditar automáticamente el inicio y resultado (éxito/fallo)
    de operaciones de IA sin acoplar la lógica de negocio.
    """
    def decorador(func):
        @functools.wraps(func)
        async def wrapper(self, *args, **kwargs):
            
            # Intentar extraer usuario_id e ip de peticion y ip, si están como kwargs o args
            # Asumimos que los métodos decorados tienen una firma similar a procesar_modulo(modulo_enum, peticion, ip)
            usuario_id = "UNKNOWN"
            ip = "UNKNOWN"
            modulo_str = accion_base
            
            # Búsqueda rápida heurística de peticion, ip y modulo_enum
            for arg in args + tuple(kwargs.values()):
                if hasattr(arg, "usuario_id"):
                    usuario_id = arg.usuario_id
                # String simple con pinta de IP
                if isinstance(arg, str) and arg.count(".") == 3 and len(arg) <= 15:
                    ip = arg
                # Extraer NombreModulo dinámicamente si se pasa
                if hasattr(arg, "value") and type(arg).__name__ == "NombreModulo":
                    modulo_str = arg.value
            
            try:
                evento_inicio = EventoAuditoriaDTO(
                    usuario_id=usuario_id, accion=f"SOLICITUD_{modulo_str}", modulo="MS-IA", ip_origen=ip, detalles="INICIADO"
                )
                await publicador_auditoria.publicar_evento(evento_inicio)
            except: pass

            try:
                resultado = await func(self, *args, **kwargs)
                
                try:
                    evento_exito = EventoAuditoriaDTO(
                        usuario_id=usuario_id, accion=f"SOLICITUD_{modulo_str}", modulo="MS-IA", ip_origen=ip, detalles="EXITOSO"
                    )
                    await publicador_auditoria.publicar_evento(evento_exito)
                except: pass
                
                return resultado
                
            except Exception as e:
                try:
                    evento_fallo = EventoAuditoriaDTO(
                        usuario_id=usuario_id, accion=f"SOLICITUD_{modulo_str}", modulo="MS-IA", ip_origen=ip, detalles=f"FALLIDO: {str(e)}"
                    )
                    await publicador_auditoria.publicar_evento(evento_fallo)
                except: pass
                raise e
        return wrapper
    return decorador
