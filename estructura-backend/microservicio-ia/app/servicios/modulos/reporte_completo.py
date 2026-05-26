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
        return f"""
        Eres LUKA, el Auditor Estratégico. Tono: {contexto.tono_ia}.
        Presenta el REPORTE ANUAL {metricas['anio']} para {contexto.nombres}.
        
        RESUMEN EJECUTIVO:
        - Score de Salud Financiera: {metricas['score_salud']}/100.
        - Balance Acumulado: S/ {metricas['balance_anual']}.
        - Categóría de mayor impacto: {metricas['categoria_critica']} ({metricas['porcentaje_gasto_critico']}% del total).
        
        INSTRUCCIONES:
        1. Explica qué significa su Score (ej: 0-40 Riesgo, 40-70 Estable, 70+ Excelente).
        2. Analiza su balance anual y cómo esto impacta en su meta: {contexto.nombre_meta_principal}.
        3. Da un veredicto final para el cierre del año.
        """
