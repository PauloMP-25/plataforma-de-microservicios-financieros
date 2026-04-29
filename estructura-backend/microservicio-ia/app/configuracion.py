"""
configuracion.py
══════════════════════════════════════════════════════════════════════════════
Carga centralizada de variables de entorno para el Microservicio IA.
Utiliza pydantic-settings para validación automática y soporte de Eureka.
Extiende la configuración existente añadiendo soporte para:
  - Google Generative AI (Gemini)
  - RabbitMQ (broker de mensajería)
══════════════════════════════════════════════════════════════════════════════
"""

from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

class Configuracion(BaseSettings):
    # ── Configuración de la App ───────────────────────────────    
    nombre_app: str = "Microservicio IA Financiera"
    id_app_eureka: str = "microservicio-ia"
    version_app: str = "1.0.0"
    puerto: int = 8086
    entorno: str = "desarrollo"
    
    # ── Configuración de Eureka ───────────────────────────────    
    eureka_servidor_url: str = "http://admin:admin123@localhost:8761/eureka"
    eureka_instancia_host: str = "127.0.0.1"
    eureka_instancia_puerto: int = 8086

    # ── Configuración de Seguridad ────────────────────────────
    jwt_clave_secreta: str = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    jwt_algoritmo: str = "HS256"

    # ── URLs de microservicios Java ──────────────────────────
    url_nucleo_financiero: str = "http://localhost:8085"
    url_auditoria:         str = "http://localhost:8082"
    url_cliente:           str = "http://localhost:8083"

    # ── Parámetros de análisis IA ────────────────────────────
    umbral_anomalia: float = 2.0
    factor_seguridad_ahorro: float = 0.85
    meses_historial_prediccion: int = 3
    monto_minimo_suscripcion: float = 5.0

    # ── Google Generative AI (Gemini) ─────────────────────────────────────────
    gemini_api_key: str = "AIzaSyAN1g22ckI7Mb3SBh140yJBsB9Fs-aWs_0"
    gemini_modelo:  str = "gemini-1.5-flash"
 
    # ── RabbitMQ ──────────────────────────────────────────────────────────────
    rabbitmq_host:     str = "localhost"
    rabbitmq_puerto:   int = 5672
    rabbitmq_usuario:  str = "guest"
    rabbitmq_password: str = "guest"
    rabbitmq_vhost:    str = "/"

    # Nombres de colas y exchanges (Sincronizado con consumidor_ia.py)
    rabbitmq_exchange_ia: str = "exchange.ia"
    rabbitmq_cola_entrada: str = "cola.ia.procesamiento"
    rabbitmq_cola_salida_ws: str = "cola.mensajeria.whatsapp"
    
    # Tiempos de vida de la conexión
    rabbitmq_heartbeat: int = 60
    rabbitmq_timeout: int = 300

    # ── Configuración de Pydantic ─────────────────────────────────────────
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"  # Ignora variables extrañas en el .env
    )

    @property
    def nombre_eureka_mayusculas(self) -> str:
        """Retorna el ID de la app en mayúsculas para Eureka."""
        return self.id_app_eureka.upper()


@lru_cache()
def obtener_configuracion() -> Configuracion:
    """
    Singleton con caché. 
    Lru_cache garantiza que el archivo .env se lea una sola vez.
    """
    return Configuracion()