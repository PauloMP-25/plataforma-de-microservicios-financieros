import pandas as pd
import logging
from abc import ABC, abstractmethod
from typing import Dict, Any, List
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.excepciones.base import ValidacionError

logger = logging.getLogger(__name__)

class BaseAnalisisService(ABC):
    """
    Clase Base Escalable para el Pipeline LUKA-COACH V4.
    Asegura que todos los módulos de análisis sigan el mismo rigor técnico y de seguridad.
    """
    
    def __init__(self, nombre_modulo: str, meses_minimos: int = 3):
        self.nombre_modulo = nombre_modulo
        self.meses_minimos = meses_minimos

    def ingestat_y_validar(self, transacciones: List[Dict[str, Any]]) -> pd.DataFrame:
        """
        Fase 1: Ingestión y Validación (Capa de Datos).
        Convierte a DataFrame y aplica filtros de calidad histórica.
        """
        if not transacciones:
            raise ValidacionError(
                mensaje=f"No hay suficientes datos para iniciar el análisis de {self.nombre_modulo}.",
                codigo_error="SIN_DATOS"
            )

        df = pd.DataFrame(transacciones)
        
        # Estandarización de fechas
        df['fecha'] = pd.to_datetime(df['fecha'])
        
        # Validación de rango histórico
        fecha_min = df['fecha'].min()
        fecha_max = df['fecha'].max()
        dias_historial = (fecha_max - fecha_min).days
        
        dias_requeridos = self.meses_minimos * 30
        
        if dias_historial < dias_requeridos:
            logger.warning(f"[PIPELINE] Historial insuficiente: {dias_historial} días (requerido: {dias_requeridos})")
            raise ValidacionError(
                mensaje=f"Necesitamos al menos {self.meses_minimos} meses de movimientos para analizar tus {self.nombre_modulo.lower()} con precisión.",
                codigo_error="HISTORIAL_INSUFICIENTE"
            )
            
        return df

    @abstractmethod
    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO) -> Dict[str, Any]:
        """Fase 2: Motor de Cálculo Local (Pandas/NumPy)."""
        pass

    @abstractmethod
    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        """Fase 3: Orquestación de Empatía (Capa Gemini)."""
        pass
