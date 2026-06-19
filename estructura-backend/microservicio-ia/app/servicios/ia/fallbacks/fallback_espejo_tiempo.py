"""
servicios/ia/fallbacks/fallback_espejo_tiempo.py  ·  v1.0 — ESPEJO DEL TIEMPO
══════════════════════════════════════════════════════════════════════════════
Fallback estructurado para el módulo «Espejo del Tiempo».

Se activa cuando Gemini no está disponible o falla. Devuelve un dict
compatible con ConsejoEstructuradoEspejo para que el frontend no rompa
y el usuario reciba siempre una respuesta coherente.

Autor: microservicio-ia LUKA
"""

from typing import Any, Dict


def generar_fallback_espejo_tiempo(
    datos: Dict[str, Any],
    nombres: str,
    contexto,
) -> Dict[str, Any]:
    """
    Genera el consejo de contingencia para el Espejo del Tiempo.

    Parámetros:
        datos    — Dict de métricas calculadas en FASE 2.
        nombres  — Primer nombre del usuario.
        contexto — ContextoEstrategicoIADTO (puede ser None en modo offline).

    Retorna:
        Dict compatible con ConsejoEstructuradoEspejo (cartaContinuidad
        y cartaTransformacion como texto de contingencia).
    """
    primer_nombre = nombres.split()[0] if nombres else "estudiante"

    ahorro_actual = datos.get("ahorro_mensual_actual", 0.0)
    ahorro_optimizado = datos.get("ahorro_mensual_optimizado", 0.0)
    diferencia_12m = datos.get("diferencia_neta_12m", 0.0)
    score_actual = datos.get("score_actual", 50)

    hitos_cont_12m = datos.get("proyeccion_continuidad", {}).get("hitos12Meses", {})
    hitos_trans_12m = datos.get("proyeccion_transformacion", {}).get("hitos12Meses", {})

    score_cont = hitos_cont_12m.get("scoreProyectado", score_actual)
    score_trans = hitos_trans_12m.get("scoreProyectado", score_actual)

    return {
        "pensamiento_interno_ia": "Servicio Gemini inactivo. Generando cartas automáticas basadas en proyecciones Pandas.",
        "score_salud_espejo": max(1, min(10, int(score_actual / 10))),
        "etiquetas_internas": ["fallback_activado", "espejo_base"],
        "nota_interna_coach": "Monitorear margen de optimización de no esenciales en siguiente sesión.",
        "cartaContinuidad": (
            f"{primer_nombre}, si mantienes tus hábitos actuales durante los próximos 12 meses, "
            f"tu score financiero llegará a {score_cont} puntos. "
            f"Con un ahorro mensual de S/ {ahorro_actual:,.2f}, estás construyendo una base sólida. "
            f"Recuerda que la consistencia es tu mayor aliado financiero."
        ),
        "cartaTransformacion": (
            f"{primer_nombre}, si reduces tus gastos no esenciales, tu capacidad de ahorro "
            f"mejora a S/ {ahorro_optimizado:,.2f} por mes. "
            f"En 12 meses acumularías S/ {diferencia_12m:,.2f} adicionales y tu score "
            f"alcanzaría {score_trans} puntos. "
            f"Ese cambio pequeño hoy puede ser la diferencia que transforma tu futuro."
        ),
    }
