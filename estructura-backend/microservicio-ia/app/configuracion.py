"""
configuracion.py  ·  v5 — Alineación con Ecosystem Luka (LUKA)
══════════════════════════════════════════════════════════════════════════════
Singleton de configuración para el Microservicio IA Financiera de LUKA.
Gestiona todas las variables de entorno con validación Pydantic v2.

Cambios v5 (alineación con ecosystem):
  - Variables de RabbitMQ renombradas a SPRING_RABBITMQ_* (estándar compartido)
  - Variables de Redis mapeadas desde SPRING_DATA_REDIS_* / REDIS_*
  - Variables de BD mapeadas desde SPRING_DATASOURCE_* (usuario/password)
  - Eureka controlado por EUREKA_CLIENT_ENABLED (mismo que los ms Java)
  - Entorno controlado por SPRING_PROFILES_ACTIVE
══════════════════════════════════════════════════════════════════════════════
"""

from functools import lru_cache
 
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

class Configuracion(BaseSettings):
    
    """
    Centraliza toda la configuración del microservicio.
    Leer una sola vez al arrancar gracias a @lru_cache.
    """

    # ══════════════════════════════════════════════════════════════════════════
    # Configuracion de la App
    # ══════════════════════════════════════════════════════════════════════════
    nombre_app: str = "Microservicio IA Financiera - LUKA"
    id_app_eureka: str = "microservicio-ia"
    version_app: str = "4.0.0"
    puerto: int = Field(default=8086, ge=1024, le=65535)
    entorno: str = "desarrollo"

    # ══════════════════════════════════════════════════════════════════════════
    # SEGURIDAD JWT
    # ══════════════════════════════════════════════════════════════════════════
    jwt_clave_secreta: str = Field(
        validation_alias="JWT_SECRET_KEY",
        description="Clave secreta compartida con microservicio-usuario (formato HEX).",
    )

    jwt_algoritmo: str = "HS256"

    # ══════════════════════════════════════════════════════════════════════════
    # DESCUBRIMIENTO DE SERVICIOS (EUREKA)
    # Usa la misma variable EUREKA_CLIENT_ENABLED que los microservicios Java
    # ══════════════════════════════════════════════════════════════════════════
    eureka_servidor_url: str = Field(
        default="http://localhost:8761/eureka",
        validation_alias="EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE",
        description="URL del servidor Eureka (compartida con ms Java).",
    )
    eureka_instancia_host: str = Field(
        default="microservicio-ia",
        validation_alias="EUREKA_INSTANCE_HOSTNAME",
    )
    eureka_instancia_puerto: int = Field(default=8086, ge=1024, le=65535)
    eureka_enable: bool = Field(
        default=False,
        validation_alias="EUREKA_CLIENT_ENABLED",
        description="Habilita/deshabilita Eureka. Usar False en Render.",
    )
    # ══════════════════════════════════════════════════════════════════════════
    # URLs DE MICROSERVICIOS JAVA
    # ══════════════════════════════════════════════════════════════════════════
    url_nucleo_financiero: str = Field(
        default="http://localhost:8085",
        description="Puerto 8085: fuente de transacciones.",
    )
    url_auditoria: str = Field(
        default="http://localhost:8082",
        description="Puerto 8082: registro de eventos (no bloqueante).",
    )
    url_cliente: str = Field(
        default="http://localhost:8083",
        description="Puerto 8083: perfil del usuario universitario.",
    )
    url_dashboard: str = Field(
        default="http://localhost:8087",
        description="Puerto 8087: visualización de resultados.",
    )

    # ══════════════════════════════════════════════════════════════════════════
    # GOOGLE GEMINI
    # ══════════════════════════════════════════════════════════════════════════
    gemini_api_key: str = Field(
        validation_alias="GEMINI_API_KEY",
        description="Clave API para autenticación con Google Gemini (formato HEX).",
    )
    gemini_modelo:  str = "gemini-2.0-flash"
    gemini_max_tokens: int = Field(
        default=500,
        ge=100,
        le=2048,
        description="Tokens máximos en la respuesta del coach.",
    )
    gemini_temperatura: float = Field(
        default=0.7,
        ge=0.0,
        le=1.0,
        description="Creatividad del coach (0=preciso, 1=creativo).",
    )

    # ── Control de Costos ──────────────────────────────────────────────────────
    gemini_costo_input_1m: float = 0.35   # USD por 1M tokens (Flash 1.5)
    gemini_costo_output_1m: float = 1.05  # USD por 1M tokens (Flash 1.5)
    umbral_alerta_costo_diario_usd: float = 2.70 # S/10 aprox

    # ══════════════════════════════════════════════════════════════════════════
    # CIRCUIT BREAKER (Resilience4j style)
    # ══════════════════════════════════════════════════════════════════════════
    cb_failure_rate_threshold: float = 50.0
    cb_wait_duration_open_state_seconds: int = 30
    cb_sliding_window_size: int = 10

    # ══════════════════════════════════════════════════════════════════════════
    # MOTOR ANALÍTICO — Parámetros para Pandas / Scikit-Learn / SciPy
    # ══════════════════════════════════════════════════════════════════════════
    
    # ── Historial general ─────────────────────────────────────────────────────
    meses_historial: int = Field(
        default=6,
        ge=1,
        le=24,
        description="Ventana temporal máxima para análisis histórico.",
    )
    tamanio_pagina_transacciones: int = Field(
        default=200,
        ge=10,
        le=1000,
        description="Transacciones a consultar al núcleo financiero por petición.",
    )

    # ── Módulo 2: Predicción de Gastos (Regresión Lineal / Media Móvil) ──────
    meses_ventana_prediccion: int = Field(
        default=3,
        ge=2,
        le=12,
        description="Meses de histórico usados para la regresión/media móvil.",
    )
    min_datos_regresion: int = Field(
        default=3,
        ge=2,
        description="Mínimo de puntos de datos para usar regresión lineal.",
    )

    # ── Módulo 3: Detección de Anomalías (Z-Score) ───────────────────────────
    umbral_zscore_anomalia: float = Field(
        default=2.5,
        ge=1.0,
        le=5.0,
        description="Desviaciones estándar para clasificar un gasto como anómalo.",
    )

    # ── Módulo 4: Gastos Hormiga / Suscripciones ─────────────────────────────
    umbral_monto_hormiga: float = Field(
        default=30.0,
        ge=1.0,
        description="Monto máximo (S/) para considerar una transacción 'hormiga'.",
    )
    min_recurrencias_hormiga: int = Field(
        default=3,
        ge=2,
        description="Veces mínimas que debe repetirse para ser gasto hormiga.",
    )

        # ── Módulo 5: Capacidad de Ahorro (Regla 50/30/20) ───────────────────────
    porcentaje_necesidades: float = Field(
        default=50.0,
        description="% del ingreso destinado a necesidades básicas (regla 50/30/20).",
    )
    porcentaje_deseos: float = Field(
        default=30.0,
        description="% del ingreso destinado a deseos/ocio.",
    )
    porcentaje_ahorro_objetivo: float = Field(
        default=20.0,
        description="% del ingreso objetivo de ahorro.",
    )
    factor_seguridad_ahorro: float = Field(
        default=0.85,
        ge=0.5,
        le=1.0,
        description="Factor de prudencia aplicado al ahorro proyectado.",
    )

    # ── Cuotas Módulo Clasificación (On-the-Fly) ──────────────────────────────
    cuota_clasif_premium_semanal: int = 20
    cuota_clasif_pro_semanal: int = 10

    # ── Módulo 8: Presupuesto Dinámico ────────────────────────────────────────
    dias_semana_presupuesto: int = Field(
        default=7,
        description="Días de la semana para distribuir el presupuesto.",
    )
    
    # ══════════════════════════════════════════════════════════════════════════
    # MENSAJERÍA (RABBITMQ)
    # Usa los mismos nombres de variable que todos los microservicios Java
    # ══════════════════════════════════════════════════════════════════════════
    rabbitmq_host: str = Field(
        default="localhost",
        validation_alias="SPRING_RABBITMQ_HOST",
        description="Host de RabbitMQ. Misma variable que los ms Java.",
    )
    rabbitmq_puerto: int = Field(
        default=5672,
        validation_alias="SPRING_RABBITMQ_PORT",
    )
    rabbitmq_usuario: str = Field(
        default="guest",
        validation_alias="SPRING_RABBITMQ_USERNAME",
    )
    rabbitmq_password: str = Field(
        default="guest",
        validation_alias="SPRING_RABBITMQ_PASSWORD",
    )
    rabbitmq_vhost: str = Field(
        default="/",
        validation_alias="SPRING_RABBITMQ_VIRTUAL_HOST",
    )

    # ── Colas RabbitMQ ────────────────────────────────────────────────────────
    # Entrada
    cola_ia_procesamiento:   str = "cola.ia.procesamiento"
    cola_ia_clasificacion:   str = "q.ia.clasificacion"  # Nueva cola para On-the-Fly
    exchange_ia:             str = "exchange.ia"

    # ── Colas de salida hacia Dashboard ──────────────────────────────────────
    cola_dashboard_consejos: str = "cola.dashboard.consejos"      # consejos automáticos
    cola_dashboard_modulos:  str = "cola.dashboard.modulos"       # respuestas a módulos manuales
    exchange_dashboard:      str = "exchange.dashboard"

    # Tiempos de vida de la conexión
    rabbitmq_heartbeat: int = 60
    rabbitmq_timeout: int = 300

    # ── Cola de sincronización (recepción de contexto desde ms-cliente) ───────
    cola_ia_sincronizacion_contexto: str = "cola.ia.sincronizacion.contexto"
    exchange_cliente_actualizaciones: str = "exchange.cliente.actualizaciones"
    rk_perfil_actualizado: str = "cliente.perfil.actualizado"

    # ══════════════════════════════════════════════════════════════════════════
    # BASE DE DATOS (Persistencia de Caché IA)
    # Reutiliza las credenciales compartidas del grupo Render (SPRING_DATASOURCE_*)
    # La URL específica de la BD de IA se construye internamente.
    # ══════════════════════════════════════════════════════════════════════════
    db_host: str = Field(
        default="localhost",
        validation_alias="SPRING_DATASOURCE_CONNECTION",
        description="Host del servidor de BD. Lee SPRING_DATASOURCE_CONNECTION del grupo compartido.",
    )
    db_port: int = Field(default=5432)
    db_usuario: str = Field(
        default="postgres",
        validation_alias="SPRING_DATASOURCE_USERNAME",
        description="Usuario de BD. Lee SPRING_DATASOURCE_USERNAME del grupo compartido.",
    )
    db_password: str = Field(
        default="postgres",
        validation_alias="SPRING_DATASOURCE_PASSWORD",
        description="Password de BD. Lee SPRING_DATASOURCE_PASSWORD del grupo compartido.",
    )
    db_nombre: str = Field(
        default="db_microservicio_ia",
        validation_alias="DB_NAME_IA",
        description="Nombre de la BD exclusiva de IA. Única variable propia de BD.",
    )

    # ══════════════════════════════════════════════════════════════════════════
    # REDIS (Caché de Contexto IA)
    # Reutiliza las credenciales compartidas del grupo Render (SPRING_DATA_REDIS_*)
    # ══════════════════════════════════════════════════════════════════════════
    redis_host: str = Field(
        default="localhost",
        validation_alias="SPRING_DATA_REDIS_HOST",
        description="Host Redis. Lee SPRING_DATA_REDIS_HOST del grupo compartido.",
    )
    redis_port: int = Field(
        default=6379,
        validation_alias="SPRING_DATA_REDIS_PORT",
    )
    redis_db: int = 0
    redis_password: str = Field(
        default="",
        validation_alias="SPRING_REDIS_PASSWORD",
        description="Password Redis. Lee SPRING_REDIS_PASSWORD del grupo compartido.",
    )
    redis_ssl: bool = Field(
        default=False,
        validation_alias="SPRING_DATA_REDIS_SSL_ENABLED",
    )
    redis_ttl_segundos: int = 3600  # 1 hora por defecto

    # ══════════════════════════════════════════════════════════════════════════
    # PYDANTIC SETTINGS
    # ══════════════════════════════════════════════════════════════════════════
    # Nombre de conexión para identificar el servicio en el panel de RabbitMQ
    rabbitmq_connection_name: str = "microservicio-ia"

    model_config = SettingsConfigDict(
        env_file=["../.env", ".env"],  # .env centralizado del backend, con fallback local
        env_file_encoding="utf-8-sig",  # Maneja BOM (UTF-8 y UTF-16 con BOM)
        case_sensitive=False,
        populate_by_name=True,
        extra="ignore"  # Ignora variables extras del .env
    )

    # ── Validadores ───────────────────────────────────────────────────────────
 
    @field_validator("entorno")
    @classmethod
    def validar_entorno(cls, valor: str) -> str:
        """Restringe los entornos válidos."""
        entornos_validos = {"desarrollo", "produccion", "pruebas"}
        if valor.lower() not in entornos_validos:
            raise ValueError(f"Entorno inválido. Opciones: {entornos_validos}")
        return valor.lower()
 
    @field_validator("porcentaje_necesidades", "porcentaje_deseos", "porcentaje_ahorro_objetivo")
    @classmethod
    def validar_porcentajes_regla(cls, valor: float) -> float:
        """Los porcentajes deben ser positivos."""
        if valor <= 0 or valor >= 100:
            raise ValueError("Los porcentajes deben estar entre 0 y 100.")
        return valor
        # ── Propiedades calculadas ────────────────────────────────────────────────
 
    @property
    def nombre_eureka_mayusculas(self) -> str:
        """ID del servicio en mayúsculas para el dashboard de Eureka."""
        return self.id_app_eureka.upper()
 
    @property
    def suma_porcentajes_regla(self) -> float:
        """Suma de los tres porcentajes (debería ser 100)."""
        return self.porcentaje_necesidades + self.porcentaje_deseos + self.porcentaje_ahorro_objetivo
 
    @property
    def es_produccion(self) -> bool:
        """Acceso rápido para condicionales críticos."""
        return self.entorno == "produccion"


@lru_cache()
def obtener_configuracion() -> Configuracion:
    """
    Singleton con caché LRU.
    El archivo .env se lee UNA sola vez en todo el ciclo de vida del proceso.
 
    Uso:
        config = obtener_configuracion()
        print(config.gemini_modelo)
    """
    return Configuracion()