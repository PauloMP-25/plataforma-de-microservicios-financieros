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
    Retorna las instrucciones explícitas para que Gemini respete
    el esquema JSON ConsejoEstructurado estándar.
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

- analisis_ia         : Análisis detallado del módulo. Incluye números,
                        variación respecto al mes anterior (si aplica)
                        y qué está causando la situación. (2-4 oraciones)

- conexion_emocional  : Una sola frase que conecte directamente el análisis
                        con el impacto en su meta de ahorro activa. Hazla personal.

- plan_accion_titulo  : Título corto y motivador para el plan de acción.

- plan_accion_pasos   : Lista de 2 a 4 pasos CONCRETOS. Cada paso debe ser accionable
                        esta semana, no una recomendación genérica.

- comentario_positivo : Cierre motivador de 1 oración. Si el usuario mejoró respecto
                        a la sesión anterior, reconócelo explícitamente.
"""
