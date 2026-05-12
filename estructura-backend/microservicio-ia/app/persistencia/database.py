from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from app.configuracion import obtener_configuracion
from app.persistencia.modelos_db import Base

config = obtener_configuracion()

# Construcción de la URL de conexión (PostgreSQL con psycopg2)
DATABASE_URL = (
    f"postgresql://{config.db_usuario}:{config.db_password}@"
    f"{config.db_host}:{config.db_port}/{config.db_nombre}"
)

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def inicializar_db():
    """Crea las tablas si no existen."""
    Base.metadata.create_all(bind=engine)

def get_db() -> Session:
    """Provee una sesión de base de datos."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
