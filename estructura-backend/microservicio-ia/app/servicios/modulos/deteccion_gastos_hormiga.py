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
    ConsejoEstructurado (introduccion, analisis_ia, conexion_emocional,
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
from app.modelos.esquemas import ConsejoEstructurado
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
        df["fecha"] = pd.to_datetime(df["fecha"])
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
        if not hormigas_actual.empty:
            top_hormiga = (
                hormigas_actual.groupby("categoria")["monto"].sum().idxmax()
            )

        return {
            "total_gastos_hormiga": round(total_hormiga_actual, 2),
            "principal_gasto_hormiga": top_hormiga,
            "variacion_vs_mes_anterior": round(variacion, 2),
            "proyeccion_fuga_anual": round(total_hormiga_actual * 12, 2),
            "hay_hormigas": total_hormiga_actual > 0,
            "comparacion_disponible": comparacion_disponible,
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
        return ConsejoEstructurado
