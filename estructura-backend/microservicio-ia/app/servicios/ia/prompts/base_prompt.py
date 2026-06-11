"""
servicios/ia/prompts/base_prompt.py
Utilidades compartidas para la construcción de prompts de los módulos de análisis.
"""

from typing import Optional, Any


def construir_seccion_historial(
    historial_previo: Optional[Any],
    historial_insight: dict,
    kpi_anterior_key: Optional[str] = None,
    kpi_anterior_label: str = "KPI Anterior",
    categoria_anterior_key: Optional[str] = None,
    categoria_anterior_label: str = "Categoría Anterior",
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

    lineas = [
        "",
        "════════════════════════════════════════",
        "CONTEXTO DE SESIÓN ANTERIOR",
        "════════════════════════════════════════",
    ]

    # KPIs comparativos
    if kpi_anterior_key:
        valor_anterior = historial_insight.get(kpi_anterior_key)
        if valor_anterior is not None:
            if isinstance(valor_anterior, float):
                lineas.append(f"- {kpi_anterior_label}: {valor_anterior:.2f}")
            else:
                lineas.append(f"- {kpi_anterior_label}: {valor_anterior}")

    if categoria_anterior_key:
        cat_anterior = historial_insight.get(categoria_anterior_key)
        if cat_anterior:
            lineas.append(f"- {categoria_anterior_label}: {cat_anterior}")

    # Extracto del consejo previo para dar contexto de qué se recomendó
    if isinstance(historial_previo, dict):
        plan_titulo = historial_previo.get("plan_accion_titulo", "")
        plan_pasos = historial_previo.get("plan_accion_pasos", [])
        if plan_titulo:
            lineas.append(f"- En la sesión anterior se propuso el plan: \"{plan_titulo}\"")
        if plan_pasos:
            pasos_resumidos = "; ".join(plan_pasos[:2])  # Máximo 2 pasos
            lineas.append(f"  Los primeros pasos eran: {pasos_resumidos}")
    elif isinstance(historial_previo, str) and len(historial_previo) > 20:
        extracto = historial_previo[:200].replace("\n", " ").strip()
        lineas.append(f"- Resumen del consejo anterior: \"{extracto}...\"")

    lineas.append(
        "Usa este contexto para verificar si el usuario mejoró o empeoró. "
        "Menciona el progreso de forma sutil y motivadora en tu análisis."
    )
    lineas.append("")

    return "\n".join(lineas)


def construir_instrucciones_esquema_estandar() -> str:
    """
    Retorna instrucciones breves para complementar el Structured Output de Gemini.
    """
    return """
Analiza los datos y responde directamente rellenando el esquema JSON requerido.
Sé conciso, usa un tono motivador y da pasos de acción muy concretos para esta semana.
"""
