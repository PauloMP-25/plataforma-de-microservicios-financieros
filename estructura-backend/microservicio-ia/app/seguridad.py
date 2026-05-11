"""
seguridad.py — Validación JWT para FastAPI

Valida el mismo token JWT emitido por microservicio-usuario.
Usa la misma clave secreta compartida (JWT_SECRET_KEY).

El Gateway ya valida el JWT antes de llegar aquí, pero es
buena práctica validarlo de nuevo en el microservicio
(defensa en profundidad: no confiar ciegamente en el Gateway).
"""

import jwt
import binascii
import logging
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.configuracion import obtener_configuracion

logger = logging.getLogger("ia_financiera.seguridad")

# Extrae el token del header 'Authorization: Bearer <token>'
esquema_bearer = HTTPBearer(auto_error=False)

def validar_token(
    credenciales: HTTPAuthorizationCredentials = Security(esquema_bearer)
) -> dict:
    """
    Valida el JWT usando la clave secreta compartida con el microservicio de Java.
    """
    if credenciales is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "estado": 401,
                "error": "ACCESO_NO_AUTORIZADO",
                "mensaje": "Se requiere un token JWT válido."
            }
        )

    config = obtener_configuracion()
    token = credenciales.credentials

    try:
        # Convertimos la clave HEX de Java a bytes que Python entiende
        # Java genera el secreto como una cadena Hexadecimal
        clave_bytes = binascii.unhexlify(config.jwt_clave_secreta)

        payload = jwt.decode(
            token,
            clave_bytes,
            algorithms=[config.jwt_algoritmo],
            options={"verify_exp": True}
        )
        
        # Validación extra: el payload debe contener usuarioId
        if "usuarioId" not in payload and "sub" not in payload:
            logger.error("[SEGURIDAD] Token válido pero sin identificación de usuario.")
            raise jwt.InvalidTokenError("Token malformado: falta usuarioId")

        return payload

    except jwt.ExpiredSignatureError:
        logger.warning("[SEGURIDAD] Intento de acceso con token expirado.")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "estado": 401,
                "error": "TOKEN_EXPIRADO",
                "mensaje": "Su sesión ha expirado. Por favor, inicie sesión nuevamente."
            }
        )
    except (jwt.InvalidTokenError, binascii.Error) as e:
        logger.error(f"[SEGURIDAD] Token inválido o clave secreta mal configurada: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "estado": 401,
                "error": "TOKEN_INVALIDO",
                "mensaje": "El token de seguridad es inválido."
            }
        )

def obtener_usuario_id(payload: dict) -> str:
    """
    Extrae el ID del usuario del token decodificado.
    Busca 'usuarioId' (estándar LUKA) o 'sub' como fallback.
    """
    return payload.get("usuarioId") or payload.get("sub")

def requerir_rol(rol_necesario: str):
    """
    Verifica si el usuario tiene el rol requerido (ej. ROLE_ADMIN, ROLE_PREMIUM).
    """
    def verificador(payload: dict = Security(validar_token)):
        roles = payload.get("roles", [])
        if rol_necesario not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={"error": "ACCESO_DENEGADO", "mensaje": f"Se requiere el rol {rol_necesario}"}
            )
        return payload
    return verificador