from contextvars import ContextVar
from uuid import uuid4

# Variable de contexto para el ID de correlación (Thread-safe y Async-safe)
correlation_id: ContextVar[str] = ContextVar("correlation_id", default=str(uuid4()))

def get_correlation_id() -> str:
    """Obtiene el ID de correlación actual."""
    return correlation_id.get()

def set_correlation_id(value: str) -> None:
    """Establece un nuevo ID de correlación."""
    correlation_id.set(value)
