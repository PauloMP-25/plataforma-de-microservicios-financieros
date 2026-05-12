"""
libreria_comun/excepciones/base.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Jerarquía de excepciones de negocio del ecosistema LUKA en Python.
Espejo funcional de la jerarquía ExcepcionGlobal.java (libreria-comun).

Mapeo Java → Python:
  ExcepcionGlobal          → LukaException
  ExcepcionValidacion      → ValidacionError        (HTTP 400)
  ExcepcionRecursoNoEnc.   → RecursoNoEncontrado    (HTTP 404)
  ExcepcionAccesoDenegado  → AccesoDenegado         (HTTP 403)
  ExcepcionNoAutorizado    → NoAutorizado           (HTTP 401)
  ExcepcionServicioExterno → ServicioExternoError   (HTTP 502)

Diseño:
  - Cada excepción lleva un campo `codigo_error` en SCREAMING_SNAKE_CASE,
    igual al CodigoError.java, para que el manejador global (handler.py)
    pueda mapear al HTTP status correcto sin condicionales.
  - El campo `timestamp` facilita la trazabilidad en logs estructurados.
  - `detalles` acepta listas de strings para errores de validación de campos.

Uso:
    raise ValidacionError(
        mensaje="Necesitamos al menos 3 meses de movimientos.",
        codigo_error="HISTORIAL_INSUFICIENTE",
    )
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional


# ══════════════════════════════════════════════════════════════════════════════
# EXCEPCIÓN RAÍZ
# ══════════════════════════════════════════════════════════════════════════════

class LukaException(Exception):
    """
    Excepción base para todo el ecosistema LUKA en Python.
    Espejo funcional de ExcepcionGlobal.java (libreria-comun).

    Todos los manejadores de excepciones (FastAPI, RabbitMQ, pytest)
    capturan esta clase base para garantizar respuestas consistentes.
    """

    def __init__(
        self,
        mensaje: str,
        codigo_error: str = "ERROR_INTERNO",
        detalles: Optional[List[str]] = None,
    ) -> None:
        """
        Args:
            mensaje:      Mensaje amigable en español para el usuario final.
            codigo_error: Código en SCREAMING_SNAKE_CASE. Debe coincidir
                          con CodigoError.java para interoperabilidad.
            detalles:     Lista de strings con información técnica adicional
                          (ej: campos que fallaron en validación).
        """
        super().__init__(mensaje)
        self.mensaje = mensaje
        self.codigo_error = codigo_error
        self.detalles: List[str] = detalles or []
        self.timestamp: str = datetime.now().isoformat()

    def __repr__(self) -> str:
        return (
            f"{self.__class__.__name__}("
            f"codigo={self.codigo_error!r}, "
            f"mensaje={self.mensaje!r})"
        )


# ══════════════════════════════════════════════════════════════════════════════
# EXCEPCIONES DE NEGOCIO (HTTP 4xx)
# ══════════════════════════════════════════════════════════════════════════════

class ValidacionError(LukaException):
    """
    Datos de entrada que no cumplen las reglas de negocio.
    Espejo de ExcepcionValidacion.java → HTTP 400 Bad Request.

    Usada también para pre-condiciones del pipeline:
      - Historial insuficiente (HISTORIAL_INSUFICIENTE)
      - Datos malformados (DATOS_MALFORMADOS)
      - Valores fuera de rango (VALOR_INVALIDO)

    Ejemplo:
        raise ValidacionError(
            mensaje="Necesitamos al menos 3 meses de movimientos "
                    "para detectar tus gastos hormiga con precisión.",
            codigo_error="HISTORIAL_INSUFICIENTE",
        )
    """

    def __init__(
        self,
        mensaje: str,
        codigo_error: str = "ERROR_VALIDACION",
        detalles: Optional[List[str]] = None,
    ) -> None:
        super().__init__(mensaje, codigo_error, detalles)


class RecursoNoEncontrado(LukaException):
    """
    Recurso solicitado que no existe en el sistema.
    Espejo de ExcepcionRecursoNoEncontrado.java → HTTP 404 Not Found.
    """

    def __init__(
        self,
        mensaje: str,
        codigo_error: str = "RECURSO_NO_ENCONTRADO",
    ) -> None:
        super().__init__(mensaje, codigo_error)


class AccesoDenegado(LukaException):
    """
    Usuario autenticado sin permisos sobre el recurso.
    Espejo de ExcepcionAccesoDenegado.java → HTTP 403 Forbidden.
    """

    def __init__(
        self,
        mensaje: str = "No tiene permisos para realizar esta acción.",
        codigo_error: str = "ACCESO_DENEGADO",
    ) -> None:
        super().__init__(mensaje, codigo_error)


class NoAutorizado(LukaException):
    """
    Token JWT ausente, inválido o expirado.
    Espejo de ExcepcionNoAutorizado.java → HTTP 401 Unauthorized.
    """

    def __init__(
        self,
        causa: str = "TOKEN_INVALIDO",
    ) -> None:
        super().__init__(
            mensaje=f"Acceso denegado: {causa}. Por favor, inicie sesión nuevamente.",
            codigo_error="ACCESO_NO_AUTORIZADO",
        )


# ══════════════════════════════════════════════════════════════════════════════
# EXCEPCIONES DE INFRAESTRUCTURA (HTTP 5xx)
# ══════════════════════════════════════════════════════════════════════════════

class ServicioExternoError(LukaException):
    """
    Fallo en la comunicación con otro microservicio o API externa (Gemini, etc.).
    Espejo de ExcepcionServicioExterno.java → HTTP 502 Bad Gateway.

    Diseñada para degradación elegante:
      El coach IA captura esta excepción y retorna estado_coach=NO_DISPONIBLE
      en lugar de propagar el error al usuario.
    """

    def __init__(
        self,
        mensaje: str,
        codigo_error: str = "ERROR_SERVICIO_EXTERNO",
        detalles: Optional[List[str]] = None,
    ) -> None:
        super().__init__(mensaje, codigo_error, detalles)


class ErrorInterno(LukaException):
    """
    Error técnico no controlado dentro del microservicio.
    Equivale a HTTP 500 Internal Server Error.
    """

    def __init__(
        self,
        mensaje: str = "Ha ocurrido un error interno en el servidor.",
        codigo_error: str = "ERROR_INTERNO",
    ) -> None:
        super().__init__(mensaje, codigo_error)