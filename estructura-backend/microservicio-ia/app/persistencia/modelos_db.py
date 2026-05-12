from sqlalchemy import Column, String, Integer, Text, Boolean, DateTime, Float
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class IaAnalisisCache(Base):
    """
    Entidad para persistir los consejos generados por Gemini.
    Funciona como una 'segunda memoria' para evitar re-procesar 
    los mismos datos y ahorrar tokens.
    """
    __tablename__ = "ia_analisis_cache"

    # Hash único generado a partir de los datos de entrada del prompt
    hash_datos = Column(String(64), primary_key=True)
    
    cliente_id = Column(String(50), nullable=False, index=True)
    modulo = Column(String(50), nullable=False)
    version_modulo = Column(Integer, default=1)
    
    prompt_usado = Column(Text, nullable=False)
    consejo_gemini = Column(Text, nullable=False)
    tokens_usados = Column(Integer, default=0)
    
    # Indica si el consejo fue generado por Gemini o por el fallback local
    usando_fallback = Column(Boolean, default=False)
    
    fecha_creacion = Column(DateTime, default=datetime.now)

    def __repr__(self):
        return f"<IaAnalisisCache(hash={self.hash_datos}, cliente={self.cliente_id}, modulo={self.modulo})>"
