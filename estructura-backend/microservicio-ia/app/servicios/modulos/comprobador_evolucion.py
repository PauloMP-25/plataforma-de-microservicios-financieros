import pandas as pd
import numpy as np
import logging
from typing import Dict, Any
from app.modelos.esquemas import ConsejoEstructuradoEvolucion
from app.servicios.ia.prompts.prompt_comprobador_evolucion import generar_prompt_comprobador_evolucion

logger = logging.getLogger(__name__)

class ComprobadorEvolucion:
    def ejecutar_calculos(self, df_a: pd.DataFrame, df_b: pd.DataFrame, contexto, **kwargs) -> Dict[str, Any]:
        """
        Calcula los 5 KPIs matemáticos comparando el Periodo A y el Periodo B.
        """
        metricas = {}
        
        # 1. Normalización de Días (evita división por cero y compara periodos de distinta longitud)
        dias_a = (df_a["fecha"].max() - df_a["fecha"].min()).days + 1 if not df_a.empty else 15
        dias_b = (df_b["fecha"].max() - df_b["fecha"].min()).days + 1 if not df_b.empty else 15
        dias_a = max(dias_a, 1)
        dias_b = max(dias_b, 1)

        # Totales Periodo A
        ingresos_a = float(df_a[df_a["tipo"] == "INGRESO"]["monto"].sum()) if not df_a.empty else 0.0
        gastos_a = float(df_a[df_a["tipo"] == "GASTO"]["monto"].sum()) if not df_a.empty else 0.0
        
        # Totales Periodo B
        ingresos_b = float(df_b[df_b["tipo"] == "INGRESO"]["monto"].sum()) if not df_b.empty else 0.0
        gastos_b = float(df_b[df_b["tipo"] == "GASTO"]["monto"].sum()) if not df_b.empty else 0.0
        
        # KPI 1: Delta Tasa de Ahorro (ΔTS)
        ts_a = ((ingresos_a - gastos_a) / ingresos_a * 100) if ingresos_a > 0 else 0.0
        ts_b = ((ingresos_b - gastos_b) / ingresos_b * 100) if ingresos_b > 0 else 0.0
        delta_ts = ts_b - ts_a
        
        # KPI 2: Índice de Volatilidad del Gasto (IVG)
        def calc_ivg(df, total_gastos, dias):
            if df.empty or total_gastos == 0: return 0.0
            gastos_df = df[df["tipo"] == "GASTO"].copy()
            # Convertimos a string de fecha para agrupar por día
            gastos_df["fecha_str"] = gastos_df["fecha"].dt.date
            gasto_diario = gastos_df.groupby("fecha_str")["monto"].sum()
            std_diaria = float(gasto_diario.std(ddof=0)) if len(gasto_diario) > 1 else 0.0
            media_diaria = total_gastos / dias
            return (std_diaria / media_diaria * 100) if media_diaria > 0 else 0.0

        ivg_a = calc_ivg(df_a, gastos_a, dias_a)
        ivg_b = calc_ivg(df_b, gastos_b, dias_b)
        
        # KPI 3 & 4: Categorías Conquistadas y Reincidentes
        cat_a = df_a[df_a["tipo"] == "GASTO"].groupby("categoria")["monto"].sum() / dias_a if not df_a.empty else pd.Series()
        cat_b = df_b[df_b["tipo"] == "GASTO"].groupby("categoria")["monto"].sum() / dias_b if not df_b.empty else pd.Series()
        
        conquistadas = []
        reincidentes = []
        
        all_cats = set(cat_a.index).union(set(cat_b.index))
        for c in all_cats:
            g_a = float(cat_a.get(c, 0.0))
            g_b = float(cat_b.get(c, 0.0))
            
            if g_a > 0 and g_b > 0:
                diff_pct = ((g_b - g_a) / g_a) * 100
                if diff_pct <= -10:
                    estado = "Dominada" if diff_pct <= -50 else ("Victoria Sólida" if diff_pct <= -25 else "Victoria Parcial")
                    conquistadas.append({
                        "categoria": c, 
                        "reduccion_pct": round(abs(diff_pct), 1), 
                        "ahorro_absoluto": round((g_a - g_b) * dias_b, 2), 
                        "estado": estado
                    })
                elif diff_pct >= 10:
                    reincidentes.append({
                        "categoria": c, 
                        "aumento_pct": round(diff_pct, 1), 
                        "gasto_extra": round((g_b - g_a) * dias_b, 2)
                    })
            elif g_a == 0 and g_b > 0:
                # Exceso nuevo
                reincidentes.append({
                    "categoria": c, 
                    "aumento_pct": 100.0, 
                    "gasto_extra": round(g_b * dias_b, 2)
                })
        
        # Ordenamos las listas por el mayor impacto
        conquistadas.sort(key=lambda x: x["reduccion_pct"], reverse=True)
        reincidentes.sort(key=lambda x: x["aumento_pct"], reverse=True)

        # KPI 5: Ponderación IMF (Índice de Madurez Financiera)
        # 1. Delta Tasa de Ahorro (Max 35 pts)
        score_ts = 35 if delta_ts > 5 else (20 if delta_ts >= 0 else 0)
        
        # 2. Volatilidad (Max 25 pts)
        score_ivg = 25 if ivg_b <= 20 else (15 if ivg_b <= 40 else (5 if ivg_b <= 70 else 0))
        if ivg_b > ivg_a: 
            score_ivg = max(0, score_ivg - 5)
            
        # 3. Categorías Conquistadas (Max 25 pts)
        score_conq = min(25, len(conquistadas) * 8)
        
        # 4. Categorías Reincidentes (Max 15 pts) - Penalización
        # Si el usuario NO tiene reincidencias, gana sus 15 puntos perfectos.
        score_reinc = max(0, 15 - (len(reincidentes) * 5))
        
        imf = score_ts + score_ivg + score_conq + score_reinc
        
        # Clasificación Diagnóstica Final
        if imf <= 25:
            diagnostico = "REGRESIÓN DETECTADA"
        elif imf <= 45:
            diagnostico = "ESTANCAMIENTO"
        elif imf <= 65:
            diagnostico = "EVOLUCIÓN INCIPIENTE"
        elif imf <= 85:
            diagnostico = "MADUREZ EN DESARROLLO"
        else:
            diagnostico = "SALTO CUALITATIVO"

        metricas = {
            "delta_tasa_ahorro": round(delta_ts, 2),
            "tasa_ahorro_a": round(ts_a, 2),
            "tasa_ahorro_b": round(ts_b, 2),
            "ivg_a": round(ivg_a, 2),
            "ivg_b": round(ivg_b, 2),
            "categorias_conquistadas": conquistadas,
            "categorias_reincidentes": reincidentes,
            "score_imf": imf,
            "diagnostico_imf": diagnostico
        }
        return metricas

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto) -> str:
        """
        Construye el contexto médico-financiero para Gemini basado en los KPIs calculados.
        """
        return generar_prompt_comprobador_evolucion(metricas, contexto)

    def obtener_esquema_salida(self):
        return ConsejoEstructuradoEvolucion
