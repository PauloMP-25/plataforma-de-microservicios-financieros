"""
servicios/modulos/reporte_completo.py  ·  v2.0 — PRO
══════════════════════════════════════════════════════════════════════════════
Reporte Ejecutivo Anual (Desde el 1 de enero hasta hoy).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, Any
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.ia.prompts.prompt_reporte_completo import generar_prompt_reporte_completo

class ReporteCompletoService(BaseAnalisisService):
    def __init__(self) -> None:
        super().__init__(nombre_modulo="REPORTE_COMPLETO", min_transacciones=50)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        self.validar_historial(df)
        
        # 1. Ventana Anual: Desde el 1 de enero del año en curso
        anio_actual = datetime.now().year
        df['fecha'] = pd.to_datetime(df['fecha'])
        df_anual = df[df['fecha'].dt.year == anio_actual]
        
        if df_anual.empty:
            return {"error": "No hay datos registrados en el año actual"}

        # 2. Métricas Acumuladas
        total_ingresos = df_anual[df_anual['tipo'] == 'INGRESO']['monto'].sum()
        total_gastos = df_anual[df_anual['tipo'] == 'GASTO']['monto'].sum()
        balance = total_ingresos - total_gastos
        
        # 3. Categoría de Mayor Gasto Anual
        cat_gasto_max = df_anual[df_anual['tipo'] == 'GASTO'].groupby('categoria')['monto'].sum().idxmax()
        monto_cat_max = df_anual[df_anual['tipo'] == 'GASTO'].groupby('categoria')['monto'].sum().max()

        # 4. Score de Salud LUKA (0-100)
        # Ratio Ahorro: (Balance / Ingresos) * 100
        score = 0
        if total_ingresos > 0:
            ratio_ahorro = (balance / total_ingresos) * 100
            score = min(100, max(0, 50 + (ratio_ahorro * 2)))

        return {
            "anio": anio_actual,
            "total_ingresos": round(float(total_ingresos), 2),
            "total_gastos": round(float(total_gastos), 2),
            "balance_anual": round(float(balance), 2),
            "score_salud": int(score),
            "categoria_critica": cat_gasto_max,
            "porcentaje_gasto_critico": round((monto_cat_max / total_gastos) * 100, 1) if total_gastos > 0 else 0
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        return generar_prompt_reporte_completo(metricas, contexto)
