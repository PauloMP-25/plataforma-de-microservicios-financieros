"""
servicios/predecir_gastos.py  ·  v1.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Módulo PREDECIR_GASTOS — Análisis Predictivo de Flujo de Caja.

Responsabilidad única:
  Proyectar el gasto del próximo mes basándose en la tendencia histórica
  y alertar si el usuario corre riesgo de insolvencia (quedarse sin saldo).
══════════════════════════════════════════════════════════════════════════════
"""

import logging
from typing import Any, Dict, List
import pandas as pd
import numpy as np

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.core.base_analisis import BaseAnalisisService
from app.servicios.ia.prompts.prompt_predecir_gastos import generar_prompt_predecir_gastos

logger = logging.getLogger(__name__)

class PrediccionGastosService(BaseAnalisisService):
    """
    Implementación del Módulo PREDECIR_GASTOS.
    Utiliza regresión lineal simple sobre los totales mensuales para proyectar.
    """

    def __init__(self) -> None:
        super().__init__(
            nombre_modulo="PREDECIR_GASTOS",
            min_transacciones=50,
        )

    def ejecutar_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
        **kwargs
    ) -> Dict[str, Any]:
        """
        Calcula la tendencia de gasto y proyecta el cierre del próximo mes.
        """
        self.validar_historial(df)
        
        # 1. Preparar datos temporales
        df['fecha_dt'] = pd.to_datetime(df['fecha'])
        
        # Agrupar gastos por mes
        df_gastos = df[df['tipo'].str.upper() == "GASTO"].copy()
        if df_gastos.empty:
            return {"tiene_datos": False, "razon": "sin_gastos"}

        mensual = (
            df_gastos.resample('ME', on='fecha_dt')['monto']
            .sum()
            .reset_index()
            .rename(columns={'monto': 'total_mensual'})
        )

        if len(mensual) < 2:
            return {"tiene_datos": False, "razon": "historial_insuficiente"}

        # 2. Calcular Tendencia (Regresión Lineal Simple)
        # x = índices (0, 1, 2...), y = montos
        x = np.arange(len(mensual))
        y = mensual['total_mensual'].values
        
        coef = np.polyfit(x, y, 1)  # [pendiente, intercepto]
        pendiente = float(coef[0])
        proyeccion_proximo_mes = float(np.polyval(coef, len(mensual)))
        
        # 3. Análisis de Riesgo
        ingreso_mensual = float(contexto.ingreso_mensual)
        riesgo_quiebra = proyeccion_proximo_mes > ingreso_mensual
        deficit_estimado = max(0, proyeccion_proximo_mes - ingreso_mensual)
        
        porcentaje_variacion = (pendiente / y.mean()) * 100 if y.mean() != 0 else 0

        logger.info(
            f"[PREDICCION] Pendiente: {pendiente:.2f} | Proyección: {proyeccion_proximo_mes:.2f} | "
            f"Riesgo Quiebra: {riesgo_quiebra}"
        )

        return {
            "tiene_datos": True,
            "promedio_historico": float(y.mean()),
            "proyeccion_proximo_mes": round(proyeccion_proximo_mes, 2),
            "gasto_proyectado": round(proyeccion_proximo_mes, 2),
            "balance_proyectado": round(ingreso_mensual - proyeccion_proximo_mes, 2),
            "pendiente": round(pendiente, 2),
            "porcentaje_variacion_mensual": round(porcentaje_variacion, 1),
            "ingreso_mensual": ingreso_mensual,
            "riesgo_quiebra": riesgo_quiebra,
            "deficit_estimado": round(deficit_estimado, 2),
            "meses_analizados": len(mensual),
            "tiene_tendencia_alcista": pendiente > 0
        }

    def obtener_esquema_salida(self) -> Any:
        from app.modelos.esquemas import ConsejoEstructuradoPredecir
        return ConsejoEstructuradoPredecir

    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """
        Construye el prompt de predicción y alerta delegando al generador.
        """
        return generar_prompt_predecir_gastos(metricas, contexto)

# Alias de compatibilidad
PredecirGastosService = PrediccionGastosService

