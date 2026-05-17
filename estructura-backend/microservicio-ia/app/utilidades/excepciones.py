"""
utilidades/excepciones.py  ·  v1.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Excepciones personalizadas para el flujo de Inteligencia Artificial.
══════════════════════════════════════════════════════════════════════════════
"""

class IAError(Exception):
    """Base para errores del microservicio IA."""
    pass

class HistorialInsuficienteError(IAError):
    """Lanzada cuando el usuario no cumple los requisitos de transacciones para un módulo."""
    def __init__(self, modulo: str, actuales: int, requeridos: int):
        self.modulo = modulo
        self.actuales = actuales
        self.requeridos = requeridos
        super().__init__(f"Historial insuficiente para {modulo}: tiene {actuales}, requiere {requeridos}.")

class BrokerComunicacionError(IAError):
    """Lanzada cuando hay fallos en la comunicación con RabbitMQ."""
    pass

class ContratoDatosError(IAError):
    """Lanzada cuando los datos recibidos no cumplen con el contrato esperado."""
    pass

class BrokerComunicacionError(IAError):
    pass

class ContratoDatosError(IAError):
    pass
