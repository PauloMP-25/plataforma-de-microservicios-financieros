from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

class Settings(BaseSettings):
    """
    Configuración centralizada para la librería común,
    mapeando variables del .env de LUKA.
    """
    
    # Seguridad JWT
    jwt_secret_key: str = Field(alias="JWT_CLAVE_SECRETA")
    jwt_algorithm: str = Field(default="HS256", alias="JWT_ALGORITHM")
    
    # RabbitMQ
    rabbit_host: str = Field(default="localhost", alias="RABBITMQ_HOST")
    rabbit_port: int = Field(default=5672, alias="RABBITMQ_PORT")
    rabbit_user: str = Field(default="guest", alias="RABBITMQ_USERNAME")
    rabbit_pass: str = Field(default="guest", alias="RABBITMQ_PASSWORD")
    
    @property
    def rabbit_url(self) -> str:
        """Retorna la URL de conexión para RabbitMQ."""
        return f"amqp://{self.rabbit_user}:{self.rabbit_pass}@{self.rabbit_host}:{self.rabbit_port}/"

    model_config = SettingsConfigDict(
        env_file=".env.book.env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True
    )

settings = Settings()
