"""
utilidades/excepciones.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Excepciones de dominio del Microservicio IA.
Toda excepción hereda de LukaException para que el handler global
las traduzca automáticamente a ResultadoApi sin condicionales adicionales.

Mapeo HTTP:
  LukaException base          → 400
  HistorialInsuficienteError  → 400  (HISTORIAL_INSUFICIENTE)
  LimiteDiarioExcedidoError   → 429  (LIMITE_DIARIO_EXCEDIDO)
  BrokerComunicacionError     → 502  (ERROR_SERVICIO_EXTERNO)
  ContratoDatosError          → 400  (DATOS_INVALIDOS)
══════════════════════════════════════════════════════════════════════════════
"""
from app.libreria_comun.excepciones.base import (
    LukaException,
    ValidacionError,
    ServicioExternoError,
)


class HistorialInsuficienteError(ValidacionError):
    """
    Usuario no cumple los requisitos de historial para un módulo analítico.
    HTTP 400 — HISTORIAL_INSUFICIENTE
    """
    def __init__(self, modulo: str, actuales: int, requeridos: int):
        self.modulo = modulo
        self.actuales = actuales
        self.requeridos = requeridos
        super().__init__(
            mensaje=(
                f"Necesitamos al menos {requeridos} transacciones para analizar "
                f"{modulo}, pero solo encontramos {actuales}. "
                f"Registra más movimientos y vuelve a intentarlo."
            ),
            codigo_error="HISTORIAL_INSUFICIENTE",
        )


class LimiteDiarioExcedidoError(LukaException):
    """
    Cuota de tokens/consultas semanales agotada.
    HTTP 429 — LIMITE_DIARIO_EXCEDIDO
    """
    def __init__(self, mensaje: str = "Has agotado tu cuota semanal de consultas IA."):
        super().__init__(mensaje=mensaje, codigo_error="LIMITE_DIARIO_EXCEDIDO")


class BrokerComunicacionError(ServicioExternoError):
    """
    Fallo en la comunicación con RabbitMQ.
    HTTP 502 — ERROR_SERVICIO_EXTERNO
    """
    def __init__(self, mensaje: str = "Error de comunicación con el broker de mensajería.", detalles: str = ""):
        super().__init__(
            mensaje=mensaje,
            codigo_error="ERROR_SERVICIO_EXTERNO",
            detalles=[detalles] if detalles else [],
        )


class ContratoDatosError(ValidacionError):
    """
    Los datos recibidos no cumplen el contrato esperado.
    HTTP 400 — DATOS_INVALIDOS
    """
    def __init__(self, mensaje: str = "Los datos recibidos no cumplen el formato requerido."):
        super().__init__(mensaje=mensaje, codigo_error="DATOS_INVALIDOS")
