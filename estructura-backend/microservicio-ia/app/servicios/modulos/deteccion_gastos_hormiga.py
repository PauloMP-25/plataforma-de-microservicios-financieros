"""
servicios/modulos/deteccion_gastos_hormiga.py  ·  v2.0 — PRO
══════════════════════════════════════════════════════════════════════════════
Análisis Dinámico de Gastos Hormiga con Regla 20/20.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from typing import Dict, Any
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

class DeteccionGastosHormigaService(BaseAnalisisService):
    def __init__(self) -> None:
        super().__init__(nombre_modulo="GASTO_HORMIGA", min_transacciones=20)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        # 1. Validación Regla 20/20
        df['fecha'] = pd.to_datetime(df['fecha'])
        mes_actual = df['fecha'].max().month
        anio_actual = df['fecha'].max().year
        
        df_actual = df[(df['fecha'].dt.month == mes_actual) & (df['fecha'].dt.year == anio_actual)]
        df_anterior = df[~((df['fecha'].dt.month == mes_actual) & (df['fecha'].dt.year == anio_actual))]
        
        if len(df_actual) < 20:
            raise HistorialInsuficienteError("GASTO_HORMIGA (Mes Actual)", len(df_actual), 20)
        if len(df_anterior) < 20:
            raise HistorialInsuficienteError("GASTO_HORMIGA (Historial Anterior)", len(df_anterior), 20)

        # 2. Análisis de Pequeños Gastos (Menores a S/ 25 por defecto)
        umbral_hormiga = 25.0
        hormigas_actual = df_actual[(df_actual['tipo'] == 'GASTO') & (df_actual['monto'] <= umbral_hormiga)]
        hormigas_anterior = df_anterior[(df_anterior['tipo'] == 'GASTO') & (df_anterior['monto'] <= umbral_hormiga)]
        
        total_hormiga_actual = float(hormigas_actual['monto'].sum())
        total_hormiga_anterior = float(hormigas_anterior['monto'].sum())
        
        variacion = 0.0
        if total_hormiga_anterior > 0:
            variacion = ((total_hormiga_actual - total_hormiga_anterior) / total_hormiga_anterior) * 100

        top_hormiga = "N/A"
        if not hormigas_actual.empty:
            top_hormiga = hormigas_actual.groupby('categoria')['monto'].sum().idxmax()

        return {
            "total_gastos_hormiga": round(total_hormiga_actual, 2),
            "principal_gasto_hormiga": top_hormiga,
            "variacion_vs_mes_anterior": round(variacion, 2),
            "proyeccion_fuga_anual": round(total_hormiga_actual * 12, 2),
            "hay_hormigas": total_hormiga_actual > 0
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        if not metricas["hay_hormigas"]:
            return f"[SKIP_IA] ¡Felicidades {contexto.nombres}! No he detectado gastos hormiga este mes. Sigue así."

        return f"""
        Eres LUKA, el Detective Financiero. Tu tono es {contexto.tono_ia}.
        Analiza estos GASTOS HORMIGA del usuario {contexto.nombres}:
        
        HALLAZGOS:
        - Fuga este mes: S/ {metricas['total_gastos_hormiga']}.
        - Principal sospechoso: {metricas['principal_gasto_hormiga']}.
        - Variación: {metricas['variacion_vs_mes_anterior']}% vs mes anterior.
        - Si no se detiene, perderá S/ {metricas['proyeccion_fuga_anual']} al año.
        
        INSTRUCCIONES:
        1. Sé directo. Explica cuánto dinero está perdiendo en cosas pequeñas.
        2. Conecta esa pérdida con su meta: {contexto.nombre_meta_principal}.
        3. Propón una técnica de ahorro inmediata.
        """
