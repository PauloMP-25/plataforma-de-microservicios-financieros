import pandas as pd
from typing import Any, Dict
from datetime import datetime

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.modulos.base import ModuloBase
from app.servicios.ia.prompts.prompt_entrenamiento import generar_prompt_entrenamiento

class ZonaEntrenamiento(ModuloBase):
    def ejecutar_calculos(
        self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs
    ) -> Dict[str, Any]:
        """Calcula los 4 signos vitales financieros y clasifica el estado físico."""
        metricas = {}
        if df.empty:
            metricas["error"] = "Sin datos en el último mes."
            metricas["estado_fisico"] = "Sedentario"
            return metricas

        hoy = datetime.now()
        df['fecha'] = pd.to_datetime(df['fecha'], format="mixed")
        df_gastos = df[df['tipo'] == "GASTO"]
        df_ingresos = df[df['tipo'] == "INGRESO"]

        # 1. Frecuencia Cardíaca Financiera
        df_ultimos_7 = df[df['fecha'] >= hoy - pd.Timedelta(days=7)]
        df_previos_7 = df[(df['fecha'] >= hoy - pd.Timedelta(days=14)) & (df['fecha'] < hoy - pd.Timedelta(days=7))]
        
        freq_actual = len(df_ultimos_7) / 7.0
        freq_previa = len(df_previos_7) / 7.0
        metricas["frecuencia_cardiaca"] = round(freq_actual, 2)
        metricas["frecuencia_previa"] = round(freq_previa, 2)

        # 2. Presión Arterial del Presupuesto
        dia_actual = hoy.day
        # Simplificación de días en mes
        dias_mes = pd.Period(hoy.strftime("%Y-%m-%d")).days_in_month if hasattr(pd.Period(hoy.strftime("%Y-%m-%d")), 'days_in_month') else 30
        pct_tiempo = dia_actual / dias_mes
        
        gastos_mes = df_gastos[df_gastos['fecha'].dt.month == hoy.month]['monto'].sum()
        ingresos_mes = df_ingresos[df_ingresos['fecha'].dt.month == hoy.month]['monto'].sum()
        
        presupuesto = ingresos_mes if ingresos_mes > 0 else 1000.0
        pct_gasto = gastos_mes / presupuesto if presupuesto > 0 else 1.0
        
        presion = pct_gasto / pct_tiempo if pct_tiempo > 0 else 1.0
        metricas["presion_arterial"] = round(presion, 2)

        # 3. Temperatura de Ahorro
        ahorro_mes = ingresos_mes - gastos_mes
        temperatura = (ahorro_mes / ingresos_mes) if ingresos_mes > 0 else 0.0
        metricas["temperatura_ahorro"] = round(temperatura * 100, 2)

        # 4. Saturación de Categorías
        if not df_gastos.empty and gastos_mes > 0:
            cat_agrupado = df_gastos[df_gastos['fecha'].dt.month == hoy.month].groupby('categoria')['monto'].sum()
            if not cat_agrupado.empty:
                cat_max = cat_agrupado.idxmax()
                pct_max = cat_agrupado.max() / gastos_mes
                metricas["saturacion_categoria"] = cat_max
                metricas["saturacion_pct"] = round(pct_max * 100, 2)
            else:
                metricas["saturacion_categoria"] = "Ninguna"
                metricas["saturacion_pct"] = 0.0
        else:
            metricas["saturacion_categoria"] = "Ninguna"
            metricas["saturacion_pct"] = 0.0

        # Clasificación
        estado = "En Forma"
        if temperatura > 20 and presion <= 1.0 and freq_actual <= 2.0:
            estado = "Atleta de Élite"
        elif presion > 1.5 or (temperatura < 0 and pct_gasto > 1.0):
            estado = "UCI Financiera"
        elif temperatura < 5 and metricas["saturacion_pct"] > 50:
            estado = "Lesionado"
        elif freq_actual < 0.5 and gastos_mes < 100:
            estado = "Sedentario"

        metricas["estado_fisico"] = estado
        
        return metricas

    def orquestar_prompt(
        self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO
    ) -> str:
        return generar_prompt_entrenamiento(metricas, contexto)
