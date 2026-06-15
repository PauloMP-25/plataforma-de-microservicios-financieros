"""
servicios/modulos/deteccion_gastos_hormiga.py  ·  v3.0 — FASE 5 (LUKA)
══════════════════════════════════════════════════════════════════════════════
Análisis Dinámico de Gastos Hormiga con Regla 20/20.

Cambios v3 (FASE 5):
  - orquestar_prompt lee metricas["_historial_previo"] y, si existe, inyecta
    una sección "CONTEXTO DE SESIÓN ANTERIOR" para que Gemini compare
    la evolución del usuario entre sesiones.
  - Se eliminó la instrucción "Responde en Markdown". En su lugar, el prompt
    instruye explícitamente a Gemini a generar JSON con el esquema
    ConsejoEstructuradoHormiga (introduccion, analisis_ia, conexion_emocional,
    plan_accion_titulo, plan_accion_pasos, comentario_positivo).
  - ejecutar_calculos: SIN CAMBIOS — la lógica Pandas es intocable.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import json
import logging
from typing import Any, Dict, Optional

import pandas as pd

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.core.base_analisis import BaseAnalisisService
from app.utilidades.excepciones import HistorialInsuficienteError
from app.modelos.esquemas import ConsejoEstructuradoHormiga
from app.servicios.ia.prompts.prompt_gasto_hormiga import generar_prompt_gasto_hormiga

logger = logging.getLogger(__name__)


class DeteccionGastosHormigaService(BaseAnalisisService):
    def __init__(self) -> None:
        super().__init__(nombre_modulo="GASTO_HORMIGA", min_transacciones=20)

    # ── Motor Analítico (INTOCABLE) ───────────────────────────────────────────

    def ejecutar_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
        **kwargs,
    ) -> Dict[str, Any]:
        """
        Lógica Pandas de detección de gastos hormiga.
        Esta función NO se modificó en ninguna fase. Es intocable.
        """
        df["fecha"] = pd.to_datetime(df["fecha"], format="mixed")
        mes_actual = df["fecha"].max().month
        anio_actual = df["fecha"].max().year

        df_mes_actual = df[
            (df["fecha"].dt.month == mes_actual)
            & (df["fecha"].dt.year == anio_actual)
        ]
        df_meses_anteriores = df[
            ~(
                (df["fecha"].dt.month == mes_actual)
                & (df["fecha"].dt.year == anio_actual)
            )
        ]

        if len(df) < 20:
            raise HistorialInsuficienteError("GASTO_HORMIGA", len(df), 20)

        umbral_hormiga = 25.0
        hormigas_actual = df_mes_actual[
            (df_mes_actual["tipo"] == "GASTO")
            & (df_mes_actual["monto"] <= umbral_hormiga)
        ]

        total_hormiga_actual = float(hormigas_actual["monto"].sum())

        comparacion_disponible = len(df_meses_anteriores) >= 20
        variacion = 0.0

        if comparacion_disponible:
            hormigas_anterior = df_meses_anteriores[
                (df_meses_anteriores["tipo"] == "GASTO")
                & (df_meses_anteriores["monto"] <= umbral_hormiga)
            ]
            total_hormiga_anterior = float(hormigas_anterior["monto"].sum())
            if total_hormiga_anterior > 0:
                variacion = (
                    (total_hormiga_actual - total_hormiga_anterior)
                    / total_hormiga_anterior
                ) * 100

        top_hormiga = "N/A"
        list_detectados = []
        if not hormigas_actual.empty:
            top_hormiga = (
                hormigas_actual.groupby("categoria")["monto"].sum().idxmax()
            )
            
            # Agrupar por descripción para encontrar compras repetidas
            col_grupo = "descripcion" if "descripcion" in hormigas_actual.columns else "categoria"
            for name, group in hormigas_actual.groupby(col_grupo):
                total_grupo = float(group["monto"].sum())
                count_grupo = len(group)
                promedio = float(group["monto"].mean())
                categoria_grupo = str(group["categoria"].iloc[0]) if "categoria" in group.columns else "Otros"
                
                # Obtener día de mayor gasto
                dia_pico = "N/A"
                if "fecha" in group.columns:
                    try:
                        dias_es = {
                            "Monday": "Lunes", "Tuesday": "Martes", "Wednesday": "Miércoles",
                            "Thursday": "Jueves", "Friday": "Viernes", "Saturday": "Sábado", "Sunday": "Domingo"
                        }
                        dia_name = group.loc[group["monto"].idxmax()]["fecha"].strftime("%A")
                        dia_pico = dias_es.get(dia_name, dia_name)
                    except Exception:
                        pass
                
                list_detectados.append({
                    "descripcion": str(name),
                    "frecuencia": f"{count_grupo} veces/mes" if count_grupo > 1 else "1 vez/mes",
                    "total": round(total_grupo, 2),
                    "categoria": categoria_grupo,
                    "promedio_por_compra": round(promedio, 2),
                    "dia_mayor_gasto": dia_pico
                })
            
            # Ordenar por total y tomar top 5
            list_detectados = sorted(list_detectados, key=lambda x: x["total"], reverse=True)[:5]

        return {
            "total_gastos_hormiga": round(total_hormiga_actual, 2),
            "principal_gasto_hormiga": top_hormiga,
            "variacion_vs_mes_anterior": round(variacion, 2),
            "proyeccion_fuga_anual": round(total_hormiga_actual * 12, 2),
            "hay_hormigas": total_hormiga_actual > 0,
            "comparacion_disponible": comparacion_disponible,
            "gastos_detectados": list_detectados
        }

    # ── Prompting ─────────────────────────────────────────────────────────────

    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """Construye el prompt delegando al generador específico."""
        return generar_prompt_gasto_hormiga(metricas, contexto)

    def obtener_esquema_salida(self):
        return ConsejoEstructuradoHormiga
