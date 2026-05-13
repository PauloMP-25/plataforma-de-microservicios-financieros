"""
servicios/core/base_analisis.py  ·  v2.0 — VALIDACIÓN POR TRANSACCIONES
══════════════════════════════════════════════════════════════════════════════
Base Pro para módulos de análisis. Soporta validación híbrida (meses/txs).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
from typing import Dict, Any
from abc import ABC, abstractmethod
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

class BaseAnalisisService(ABC):
    def __init__(self, nombre_modulo: str, min_transacciones: int = 20):
        self.nombre_modulo = nombre_modulo
        self.min_transacciones = min_transacciones

    def validar_historial(self, df: pd.DataFrame, custom_min: int = None):
        """
        Valida si el DataFrame tiene suficientes transacciones para el análisis.
        """
        requisito = custom_min if custom_min is not None else self.min_transacciones
        total_txs = len(df)
        
        if total_txs < requisito:
            raise HistorialInsuficienteError(
                modulo=self.nombre_modulo,
                mes_actual=total_txs,
                minimo_requerido=requisito
            )

    @abstractmethod
    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        """Lógica de Numpy/Pandas."""
        pass

    @abstractmethod
    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        """Construcción del prompt para Gemini."""
        pass