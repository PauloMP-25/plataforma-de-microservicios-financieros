"""
configuracion.py — Carga centralizada de variables de entorno.
Utiliza pydantic-settings para validación automática.
"""

from pydantic_settings import BaseSettings
from functools import lru_cache


class Configuracion(BaseSettings):
    # ── App ──────────────────────────────────────────────────
    nombre_app: str = "Microservicio IA Financiera"
    version_app: str = "1.0.0"
    puerto: int = 8086
    entorno: str = "desarrollo"

    # ── URLs de microservicios Java ──────────────────────────
    url_nucleo_financiero: str = "http://localhost:8085"
    url_auditoria: str = "http://localhost:8082"
    url_cliente: str = "http://localhost:8083"

    # ── Seguridad ────────────────────────────────────────────
    jwt_clave_secreta: str = "clave_secreta_defecto"

    # ── Parámetros de análisis IA ────────────────────────────
    umbral_anomalia: float = 2.0
    factor_seguridad_ahorro: float = 0.85
    meses_historial_prediccion: int = 3
    monto_minimo_suscripcion: float = 5.0

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def obtener_configuracion() -> Configuracion:
    """Singleton con caché para no recargar el archivo .env en cada request."""
    return Configuracion()