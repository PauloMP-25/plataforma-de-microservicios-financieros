"""
Módulo de Excepciones Personalizadas para el Microservicio IA.
Permite el rastreo específico de errores en la integración con Gemini,
RabbitMQ y la lógica de negocio financiera.
"""

from datetime import datetime
from typing import Optional, Any

class IA_BaseException(Exception):
    """Excepción raíz para todas las anomalías del microservicio."""
    def __init__(
        self, 
        mensaje: str, 
        codigo: str = "IA_GENERIC_ERROR", 
        detalles: Optional[Any] = None
    ):
        self.mensaje = mensaje
        self.codigo = codigo
        self.detalles = detalles
        self.timestamp = datetime.now().isoformat()
        super().__init__(self.mensaje)

# ── 1. EXCEPCIONES DE INFRAESTRUCTURA ────────────────────────────────────────

class ConfiguracionError(IA_BaseException):
    """Lanzada cuando faltan variables en el .env o son inválidas."""
    def __init__(self, mensaje: str, detalles: Optional[Any] = None):
        super().__init__(mensaje, "IA_CONFIG_001", detalles)

class BrokerComunicacionError(IA_BaseException):
    """Error crítico al conectar o publicar en RabbitMQ."""
    def __init__(self, mensaje: str, detalles: Optional[Any] = None):
        super().__init__(mensaje, "IA_RABBIT_002", detalles)

# ── 2. EXCEPCIONES DE DOMINIO IA (GEMINI) ────────────────────────────────────

class GeminiError(IA_BaseException):
    """Excepción base para errores relacionados con Google Gemini."""
    pass

class GeminiCuotaExcedidaError(GeminiError):
    """Lanzada cuando se alcanza el límite de Rate Limit (Error 429)."""
    def __init__(self, mensaje: str = "Límite de peticiones a Gemini alcanzado."):
        super().__init__(mensaje, "IA_GEMINI_429")

class GeminiFiltroSeguridadError(GeminiError):
    """Lanzada cuando Google bloquea la respuesta por sus políticas de seguridad."""
    def __init__(self, mensaje: str, detalles: Optional[Any] = None):
        super().__init__(mensaje, "IA_GEMINI_SAFETY", detalles)

class GeminiAutenticacionError(GeminiError):
    """Lanzada cuando la API Key es inválida o expiró."""
    def __init__(self, mensaje: str = "API Key de Gemini inválida."):
        super().__init__(mensaje, "IA_GEMINI_401")

# ── 3. EXCEPCIONES DE CONTRATO Y DATOS ───────────────────────────────────────

class ContratoDatosError(IA_BaseException):
    """Lanzada cuando el JSON recibido de Java no cumple con el esquema Pydantic."""
    def __init__(self, mensaje: str, detalles: Optional[Any] = None):
        super().__init__(mensaje, "IA_DATA_400", detalles)

class ModuloNoImplementadoError(IA_BaseException):
    """Lanzada cuando se solicita uno de los 10 módulos que aún está en desarrollo."""
    def __init__(self, modulo: str):
        mensaje = f"El módulo de análisis '{modulo}' aún no está disponible."
        super().__init__(mensaje, "IA_MODULE_404", {"modulo": modulo})

# ── 4. EXCEPCIONES DE NEGOCIO ────────────────────────────────────────────────

class AnalisisFinancieroError(IA_BaseException):
    """Lanzada cuando la lógica del cerebro no puede procesar el consejo."""
    def __init__(self, mensaje: str, detalles: Optional[Any] = None):
        super().__init__(mensaje, "IA_BIZ_500", detalles)