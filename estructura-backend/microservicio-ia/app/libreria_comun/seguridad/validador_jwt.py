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
        import base64
        # Decodificar la clave Base64 a bytes (el backend Java usa Base64)
        secret_key = base64.b64decode(settings.jwt_secret_key)
        
        payload = jwt.decode(
            token,
            secret_key,
            algorithms=[settings.jwt_algorithm, "HS384", "HS512"],
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

def obtener_rol_usuario(payload: Dict[str, Any]) -> str:
    """
    Extrae el rol del usuario del payload decodificado del JWT.
    Soporta:
      - 'role': string (ej. "PREMIUM")
      - 'roles': list (ej. ["ROLE_PREMIUM"]) -> mapea a "PREMIUM", "PRO", "FREE"
    Por defecto retorna "FREE".
    """
    # 1. Intentar con 'role' directo (ej. "PREMIUM")
    rol = payload.get("role")
    if rol:
        rol_str = str(rol).upper()
        if rol_str.startswith("ROLE_"):
            rol_str = rol_str[5:]
        return rol_str

    # 2. Intentar con la lista 'roles' (ej. ["ROLE_PREMIUM"])
    roles_list = payload.get("roles")
    if isinstance(roles_list, list):
        for r in roles_list:
            r_str = str(r).upper()
            if r_str.startswith("ROLE_"):
                r_str = r_str[5:]
            if r_str in ["PREMIUM", "PRO", "FREE", "BASIC"]:
                return "FREE" if r_str == "BASIC" else r_str

    return "FREE"

