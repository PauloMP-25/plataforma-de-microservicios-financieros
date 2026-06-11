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

    # ── Prompting con Memoria + Instrucciones de Esquema JSON ─────────────────

    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """
        Construye el prompt para el módulo GASTO_HORMIGA.

        v3 — Cambios:
          1. Lee metricas["_historial_previo"] para inyectar contexto histórico.
          2. Elimina instrucción de Markdown.
          3. Instruye a Gemini a respetar el esquema JSON ConsejoEstructurado.
        """
        # ── Caso especial: sin hormigas detectadas ────────────────────────────
        if not metricas["hay_hormigas"]:
            return (
                "[SKIP_IA] ¡Felicidades "
                + contexto.nombres
                + "! No he detectado gastos hormiga este mes. Sigue así."
            )

        # ── Sección 1: Variación respecto a mes anterior ──────────────────────
        if metricas.get("comparacion_disponible", True):
            variacion_pct = metricas["variacion_vs_mes_anterior"]
            if variacion_pct > 0:
                variacion_str = f"Los gastos hormiga AUMENTARON un {variacion_pct:.1f}% vs el mes anterior."
            elif variacion_pct < 0:
                variacion_str = f"Los gastos hormiga DISMINUYERON un {abs(variacion_pct):.1f}% vs el mes anterior. ¡Buen progreso!"
            else:
                variacion_str = "Los gastos hormiga se mantuvieron estables respecto al mes anterior."
        else:
            variacion_str = "No hay datos históricos suficientes para comparar con el mes anterior."

        # ── Sección 2: Contexto de sesión anterior (NUEVO v3) ─────────────────
        # Recuperamos el historial previo inyectado por el orquestador.
        # Puede ser: dict (ConsejoEstructurado anterior), str (consejo legacy), o None.
        historial_previo = metricas.get("_historial_previo")
        historial_insight = metricas.get("_historial_insight") or {}

        seccion_historial = _construir_seccion_historial(
            historial_previo, historial_insight
        )

        # ── Sección 3: Instrucciones de esquema JSON (NUEVO v3) ───────────────
        instrucciones_esquema = _construir_instrucciones_esquema()

        # ── Prompt final ──────────────────────────────────────────────────────
        prompt = f"""
Eres LUKA, el Detective Financiero de la app de finanzas personales "Luka App".
Tu personalidad: {contexto.tono_ia}. Dirígete siempre al usuario por su nombre.

════════════════════════════════════════
PERFIL DEL USUARIO
════════════════════════════════════════
{contexto.resumen_para_prompt}

════════════════════════════════════════
HALLAZGOS DEL MOTOR ANALÍTICO (este mes)
════════════════════════════════════════
- Fuga acumulada en gastos hormiga: S/ {metricas['total_gastos_hormiga']:.2f}
- Categoría con mayor fuga: {metricas['principal_gasto_hormiga']}
- {variacion_str}
- Proyección de fuga anual si no actúa: S/ {metricas['proyeccion_fuga_anual']:.2f}
- Meta de ahorro activa: {contexto.nombre_meta_principal} (progreso: {contexto.porcentaje_meta_principal}%)
{seccion_historial}
════════════════════════════════════════
INSTRUCCIONES DE ANÁLISIS
════════════════════════════════════════
1. Sé directo y concreto. Usa los datos numéricos reales del análisis.
2. Conecta la fuga de dinero con el impacto real en su meta "{contexto.nombre_meta_principal}".
3. Propón exactamente entre 2 y 4 pasos de acción concretos y accionables esta semana.
4. Si el historial previo indica que el usuario ya tuvo gastos hormiga antes,
   menciona sutilmente si mejoró o empeoró, sin ser repetitivo ni condescendiente.
5. El tono debe ser: {contexto.tono_ia}.
{instrucciones_esquema}
"""
        return prompt.strip()


# ── Funciones auxiliares del módulo ───────────────────────────────────────────

def _construir_seccion_historial(
    historial_previo: Optional[Any],
    historial_insight: dict,
) -> str:
    """
    Construye el bloque de contexto histórico para inyectar en el prompt.

    Soporta tres formatos de historial_previo:
      - dict  : ConsejoEstructurado serializado de una sesión anterior.
      - str   : Consejo legacy de texto plano de una sesión anterior.
      - None  : Primera sesión del usuario, sin historial.
    """
    if historial_previo is None:
        return ""

    # Extraer KPI clave del insight histórico para comparación
    fuga_anterior = historial_insight.get("total_gastos_hormiga")
    categoria_anterior = historial_insight.get("principal_gasto_hormiga")

    lineas = [
        "",
        "════════════════════════════════════════",
        "CONTEXTO DE SESIÓN ANTERIOR",
        "════════════════════════════════════════",
    ]

    # KPIs comparativos (si están disponibles en el insight guardado)
    if fuga_anterior is not None:
        lineas.append(
            f"- En la sesión anterior, la fuga por gastos hormiga fue: S/ {fuga_anterior:.2f}"
        )
    if categoria_anterior:
        lineas.append(
            f"- La categoría con mayor fuga en la sesión anterior fue: {categoria_anterior}"
        )

    # Extracto del consejo previo para dar contexto de qué se recomendó
    if isinstance(historial_previo, dict):
        # ConsejoEstructurado → leer el plan de acción anterior
        plan_titulo = historial_previo.get("plan_accion_titulo", "")
        plan_pasos = historial_previo.get("plan_accion_pasos", [])
        if plan_titulo:
            lineas.append(f"- En la sesión anterior se propuso el plan: \"{plan_titulo}\"")
        if plan_pasos:
            pasos_resumidos = "; ".join(plan_pasos[:2])  # Máximo 2 pasos para no sobrecargar el prompt
            lineas.append(f"  Los primeros pasos eran: {pasos_resumidos}")
    elif isinstance(historial_previo, str) and len(historial_previo) > 20:
        # Consejo legacy — incluir solo los primeros 200 chars para no inflar el prompt
        extracto = historial_previo[:200].replace("\n", " ").strip()
        lineas.append(f"- Resumen del consejo anterior: \"{extracto}...\"")

    lineas.append(
        "Usa este contexto para verificar si el usuario mejoró o empeoró. "
        "Menciona el progreso de forma sutil y motivadora en tu análisis."
    )
    lineas.append("")

    return "\n".join(lineas)


def _construir_instrucciones_esquema() -> str:
    """
    Retorna las instrucciones explícitas para que Gemini respete
    el esquema JSON ConsejoEstructurado.

    Aunque response_schema ya fuerza el formato a nivel de API,
    estas instrucciones mejoran la calidad del contenido por campo.
    """
    return """
════════════════════════════════════════
FORMATO DE RESPUESTA OBLIGATORIO
════════════════════════════════════════
Debes generar la respuesta cumpliendo ESTRICTAMENTE con el esquema JSON proporcionado.
NO incluyas texto libre fuera del JSON. NO uses bloques de código markdown.

Distribuye el análisis así en cada campo:

- introduccion        : Saludo personalizado con el nombre del usuario y una frase
                        que resuma el hallazgo principal. (1-2 oraciones)

- analisis_ia         : Análisis detallado de los gastos hormiga. Incluye montos,
                        categoría principal, variación respecto al mes anterior (si aplica)
                        y qué está causando la fuga. (2-4 oraciones)

- conexion_emocional  : Una sola frase que conecte directamente la fuga de dinero
                        con el impacto en su meta de ahorro activa. Hazla personal.

- plan_accion_titulo  : Título corto y motivador para el plan de acción.
                        Ejemplos: "Operación Hormiga Cero", "Plan 30 Días Sin Fugas"

- plan_accion_pasos   : Lista de 2 a 4 pasos CONCRETOS. Cada paso debe ser accionable
                        esta semana, no una recomendación genérica.
                        Ejemplo correcto: "Elimina la suscripción de S/ 15 de X esta semana"
                        Ejemplo incorrecto: "Reduce gastos innecesarios"

- comentario_positivo : Cierre motivador de 1 oración. Si el usuario mejoró respecto
                        a la sesión anterior, reconócelo explícitamente.
"""
