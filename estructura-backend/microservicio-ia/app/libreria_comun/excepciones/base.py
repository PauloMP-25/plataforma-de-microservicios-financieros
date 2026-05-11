from datetime import datetime
from typing import List, Optional

class LukaException(Exception):
    """
    Excepción base para el ecosistema LUKA en Python.
    Espejo funcional de las excepciones de negocio en Spring Boot.
    """
    def __init__(
        self, 
        mensaje: str, 
        codigo_error: str = "ERROR_INTERNO", 
        detalles: Optional[List[str]] = None
    ):
        self.mensaje = mensaje
        self.codigo_error = codigo_error
        self.detalles = detalles or []
        self.timestamp = datetime.now().isoformat()
        super().__init__(self.mensaje)

class ValidacionError(LukaException):
    """Equivalente a errores de validación de campos (400 Bad Request)."""
    def __init__(self, mensaje: str, detalles: Optional[List[str]] = None):
        super().__init__(mensaje, "VALOR_INVALIDO", detalles)

class RecursoNoEncontradoError(LukaException):
    """Equivalente a 404 Not Found."""
    def __init__(self, mensaje: str):
        super().__init__(mensaje, "RECURSO_NO_ENCONTRADO")

class NoAutorizadoError(LukaException):
    """Equivalente a 403 Forbidden."""
    def __init__(self, mensaje: str = "Acceso denegado o permisos insuficientes."):
        super().__init__(mensaje, "ACCESO_DENEGADO")

class ServicioExternoError(LukaException):
    """Error al comunicarse con otros microservicios o APIs (Gemini, etc)."""
    def __init__(self, mensaje: str, codigo_error: str = "ERROR_SERVICIO_EXTERNO"):
        super().__init__(mensaje, codigo_error)
