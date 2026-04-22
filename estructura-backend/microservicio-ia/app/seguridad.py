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
            detail={"error": "NO_AUTORIZADO", "mensaje": "Se requiere un token JWT válido."}
        )

    config = obtener_configuracion()
    token = credenciales.credentials

    try:
        # Convertimos la clave HEX de Java a bytes que Python entiende
        clave_bytes = binascii.unhexlify(config.jwt_clave_secreta)

        payload = jwt.decode(
            token,
            clave_bytes,
            algorithms=[config.jwt_algoritmo],
            options={"verify_exp": True}
        )
        return payload

    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": "TOKEN_EXPIRADO", "mensaje": "El token ha expirado."}
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"error": "TOKEN_INVALIDO", "mensaje": "El token es inválido."}
        )

def obtener_usuario_id(payload: dict) -> str:
    """Extrae el ID del usuario del token decodificado."""
    return payload.get("usuarioId")

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