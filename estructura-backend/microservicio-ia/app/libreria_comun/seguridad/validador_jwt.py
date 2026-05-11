import jwt
import binascii
import logging
from typing import Dict, Any, Optional
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.libreria_comun.configuracion.settings import settings

logger = logging.getLogger("libreria_comun.seguridad")

# Esquema de seguridad Bearer (Authorization: Bearer <token>)
reusable_oauth2 = HTTPBearer(auto_error=False)

def validar_token(
    credenciales: Optional[HTTPAuthorizationCredentials] = Security(reusable_oauth2)
) -> Dict[str, Any]:
    """
    Dependencia de FastAPI para validar el JWT y retornar el payload.
    Utiliza la clave HEX compartida con el ecosistema Java.
    """
    if not credenciales:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "mensaje": "Se requiere un token de autenticación.",
                "codigoError": "ACCESO_NO_AUTORIZADO"
            }
        )

    token = credenciales.credentials

    try:
        # Decodificar la clave HEX a bytes
        secret_key = binascii.unhexlify(settings.jwt_secret_key)
        
        payload = jwt.decode(
            token,
            secret_key,
            algorithms=[settings.jwt_algorithm],
            options={"verify_exp": True}
        )
        
        # Validar presencia de identificador de usuario (usuarioId o sub)
        if "usuarioId" not in payload and "sub" not in payload:
            logger.error("[SEGURIDAD] Token válido pero sin usuarioId ni sub.")
            raise jwt.InvalidTokenError("Token malformado: falta identificación")
            
        return payload

    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "mensaje": "El token ha expirado.",
                "codigoError": "TOKEN_EXPIRADO"
            }
        )
    except (jwt.InvalidTokenError, binascii.Error) as e:
        logger.error(f"[SEGURIDAD] Error al validar token: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "exito": False,
                "mensaje": "Token de seguridad inválido.",
                "codigoError": "TOKEN_INVALIDO"
            }
        )

def obtener_usuario_id(payload: Dict[str, Any]) -> str:
    """
    Extrae el ID único del usuario del payload decodificado.
    Prioriza 'usuarioId' (estándar LUKA) y usa 'sub' como fallback.
    """
    user_id = payload.get("usuarioId") or payload.get("sub")
    if not user_id:
        raise ValueError("No se pudo extraer el usuarioId del payload del token")
    return str(user_id)
