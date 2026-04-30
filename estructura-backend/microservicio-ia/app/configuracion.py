"""
configuracion.py  ·  v3 — Dashboard + Persistencia + Multi-Módulos
══════════════════════════════════════════════════════════════════════════════
Extiende la configuración v2 añadiendo:
  - Parámetros de historial (MESES_HISTORIAL = 6)
  - Colas de respuesta hacia el Dashboard
  - Umbrales para detección de gastos hormiga
  - URL de persistencia (microservicio-dashboard o bd directa)
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
    url_dashboard:         str = "http://localhost:8087"

    # ── Parámetros de análisis IA ────────────────────────────
    umbral_anomalia: float = 2.0
    factor_seguridad_ahorro: float = 0.85
    monto_minimo_suscripcion: float = 5.0

    # ── Historial comparativo ─────────────────────────────────────────────────
    meses_historial:              int   = 3      # ventana temporal para todos los módulos
    meses_historial_prediccion:   int   = 3      # alias explícito para predicción
    umbral_gasto_hormiga:         float = 30.0   # monto máximo para clasificar como hormiga
    min_recurrencias_hormiga:     int   = 3      # veces mínimas en el historial para ser hormiga



    # ── Google Generative AI (Gemini) ─────────────────────────────────────────
    gemini_api_key: str = "AIzaSyAN1g22ckI7Mb3SBh140yJBsB9Fs-aWs_0"
    gemini_modelo:  str = "gemini-1.5-flash"
    gemini_max_tokens:        int = 500
    gemini_temperatura:       float = 0.7        # creatividad del coach
 
    # ── RabbitMQ ──────────────────────────────────────────────────────────────
    rabbitmq_host:     str = "localhost"
    rabbitmq_puerto:   int = 5672
    rabbitmq_usuario:  str = "guest"
    rabbitmq_password: str = "guest"
    rabbitmq_vhost:    str = "/"

    # ── Colas RabbitMQ ────────────────────────────────────────────────────────
    # Entrada
    cola_ia_procesamiento:   str = "cola.ia.procesamiento"
    exchange_ia:             str = "exchange.ia"
 
    # Salida hacia Dashboard (reemplaza cola.mensajeria.whatsapp)
    cola_dashboard_consejos: str = "cola.dashboard.consejos"      # consejos automáticos
    cola_dashboard_modulos:  str = "cola.dashboard.modulos"       # respuestas a módulos manuales
    exchange_dashboard:      str = "exchange.dashboard"
    
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