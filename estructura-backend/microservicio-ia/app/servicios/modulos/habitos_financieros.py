"""
servicios/modulos/habitos_financieros.py  ·  v2.0 — PRO
══════════════════════════════════════════════════════════════════════════════
Análisis de Hábitos basado en frecuencia dinámica (Semanal/Quincenal/Mensual).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from typing import Dict, Any
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class HabitosFinancierosService(BaseAnalisisService):
    def __init__(self) -> None:
        super().__init__(nombre_modulo="HABITOS_FINANCIEROS", min_transacciones=20)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        self.validar_historial(df)
        
        frecuencia = kwargs.get("frecuencia", "SEMANAL").upper()
        df['fecha'] = pd.to_datetime(df['fecha'])
        
        # 1. Definir ventana de análisis
        dias = 7 if frecuencia == "SEMANAL" else 15 if frecuencia == "QUINCENAL" else 30
        ultima_fecha = df['fecha'].max()
        ventana_actual = df[df['fecha'] >= (ultima_fecha - pd.Timedelta(days=dias))]
        
        # 2. Análisis de Patrones (Días de mayor gasto)
        ventana_actual['dia_semana'] = ventana_actual['fecha'].dt.day_name()
        gastos_por_dia = ventana_actual[ventana_actual['tipo'] == 'GASTO'].groupby('dia_semana')['monto'].sum()
        
        dia_pico = "N/A"
        if not gastos_por_dia.empty:
            dia_pico = gastos_por_dia.idxmax()

        # 3. Categoría dominante en el periodo
        cat_dominante = "N/A"
        if not ventana_actual.empty:
            cat_dominante = ventana_actual[ventana_actual['tipo'] == 'GASTO'].groupby('categoria')['monto'].count().idxmax()

        return {
            "frecuencia_analizada": frecuencia,
            "dia_mayor_gasto": dia_pico,
            "categoria_mas_frecuente": cat_dominante,
            "total_transacciones_periodo": len(ventana_actual),
            "es_saludable": len(ventana_actual[ventana_actual['tipo'] == 'INGRESO']) > 0
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        return f"""
        Eres LUKA, experto en Psicología Financiera. Tono: {contexto.tono_ia}.
        Analiza los HÁBITOS {metricas['frecuencia_analizada']}S de {contexto.nombres}.
        
        DATOS:
        - El día que más gasta es el: {metricas['dia_mayor_gasto']}.
        - La categoría donde más transacciona es: {metricas['categoria_mas_frecuente']}.
        - Total movimientos en el periodo: {metricas['total_transacciones_periodo']}.
        
        INSTRUCCIONES:
        1. Comenta sobre el patrón detectado (ej: si gasta más los fines de semana).
        2. Propón un "Hábito Atómico" para mejorar su relación con el dinero.
        3. Mantén el mensaje motivador hacia su meta: {contexto.nombre_meta_principal}.
        """
