from enum import Enum
from fastapi import status

class CodigoError(str, Enum):
    """
    Catálogo centralizado de errores para LUKA APP (Versión Python).
    Mapeo 1:1 con CodigoError.java en libreria-comun.
    """
    # 404 - Not Found
    RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO"
    CLIENTE_NO_ENCONTRADO = "CLIENTE_NO_ENCONTRADO"
    META_NO_ENCONTRADA = "META_NO_ENCONTRADA"
    CODIGO_INVALIDO = "CODIGO_INVALIDO"

    # 403 - Forbidden
    ACCESO_DENEGADO = "ACCESO_DENEGADO"
    CUENTA_NO_ACTIVADA = "CUENTA_NO_ACTIVADA"
    CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA"

    # 401 - Unauthorized
    ACCESO_NO_AUTORIZADO = "ACCESO_NO_AUTORIZADO"
    USUARIO_NO_REGISTRADO = "USUARIO_NO_REGISTRADO"
    TOKEN_EXPIRADO = "TOKEN_EXPIRADO"
    CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS"

    # 409 - Conflict
    CONFLICTO_DE_DATOS = "CONFLICTO_DE_DATOS"
    DNI_DUPLICADO = "DNI_DUPLICADO"
    USUARIO_DUPLICADO = "USUARIO_DUPLICADO"
    LIMITE_GLOBAL_EXISTENTE = "LIMITE_GLOBAL_EXISTENTE"

    # 429 - Too Many Requests
    IP_BLOQUEADA = "IP_BLOQUEADA"
    LIMITE_DIARIO_EXCEDIDO = "LIMITE_DIARIO_EXCEDIDO"

    # 410 - Gone
    CODIGO_VENCIDO = "CODIGO_VENCIDO"

    # 400 - Bad Request
    ERROR_VALIDACION = "ERROR_VALIDACION"
    TOKEN_INVALIDO = "TOKEN_INVALIDO"
    PASSWORD_MISMATCH = "PASSWORD_MISMATCH"
    SOLICITUD_INCORRECTA = "SOLICITUD_INCORRECTA"

    # 502 - External Error
    ERROR_SERVICIO_EXTERNO = "ERROR_SERVICIO_EXTERNO"

    # 500 - Internal
    ERROR_INTERNO = "ERROR_INTERNO"

    @property
    def status_code(self) -> int:
        """Retorna el código HTTP asociado al error."""
        mapeo = {
            # 404
            CodigoError.RECURSO_NO_ENCONTRADO: status.HTTP_404_NOT_FOUND,
            CodigoError.CLIENTE_NO_ENCONTRADO: status.HTTP_404_NOT_FOUND,
            CodigoError.META_NO_ENCONTRADA: status.HTTP_404_NOT_FOUND,
            CodigoError.CODIGO_INVALIDO: status.HTTP_404_NOT_FOUND,
            # 403
            CodigoError.ACCESO_DENEGADO: status.HTTP_403_FORBIDDEN,
            CodigoError.CUENTA_NO_ACTIVADA: status.HTTP_403_FORBIDDEN,
            CodigoError.CUENTA_BLOQUEADA: status.HTTP_403_FORBIDDEN,
            # 401
            CodigoError.ACCESO_NO_AUTORIZADO: status.HTTP_401_UNAUTHORIZED,
            CodigoError.USUARIO_NO_REGISTRADO: status.HTTP_401_UNAUTHORIZED,
            CodigoError.TOKEN_EXPIRADO: status.HTTP_401_UNAUTHORIZED,
            CodigoError.CREDENCIALES_INVALIDAS: status.HTTP_401_UNAUTHORIZED,
            # 409
            CodigoError.CONFLICTO_DE_DATOS: status.HTTP_409_CONFLICT,
            CodigoError.DNI_DUPLICADO: status.HTTP_409_CONFLICT,
            CodigoError.USUARIO_DUPLICADO: status.HTTP_409_CONFLICT,
            CodigoError.LIMITE_GLOBAL_EXISTENTE: status.HTTP_409_CONFLICT,
            # 429
            CodigoError.IP_BLOQUEADA: status.HTTP_429_TOO_MANY_REQUESTS,
            CodigoError.LIMITE_DIARIO_EXCEDIDO: status.HTTP_429_TOO_MANY_REQUESTS,
            # 410
            CodigoError.CODIGO_VENCIDO: status.HTTP_410_GONE,
            # 400
            CodigoError.ERROR_VALIDACION: status.HTTP_400_BAD_REQUEST,
            CodigoError.TOKEN_INVALIDO: status.HTTP_400_BAD_REQUEST,
            CodigoError.PASSWORD_MISMATCH: status.HTTP_400_BAD_REQUEST,
            CodigoError.SOLICITUD_INCORRECTA: status.HTTP_400_BAD_REQUEST,
            # 502
            CodigoError.ERROR_SERVICIO_EXTERNO: status.HTTP_502_BAD_GATEWAY,
            # 500
            CodigoError.ERROR_INTERNO: status.HTTP_500_INTERNAL_SERVER_ERROR,
        }
        return mapeo.get(self, status.HTTP_500_INTERNAL_SERVER_ERROR)
