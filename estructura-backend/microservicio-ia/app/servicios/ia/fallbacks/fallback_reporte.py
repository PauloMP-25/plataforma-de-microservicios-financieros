"""
servicios/ia/fallbacks/fallback_reporte.py
Fallback local en caso de que Gemini falle para el módulo REPORTE_COMPLETO.
"""

from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_fallback_reporte(
    metricas: Dict[str, Any],
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> Dict[str, Any]:
    """Genera una respuesta determinista en base al score."""
    score = metricas.get('score_salud', 0)
    
    if score >= 70:
        analisis = f"Tu score de {score}/100 indica una salud financiera Excelente. Estás ahorrando a buen ritmo."
        veredicto = "Excelente desempeño en lo que va del año."
    elif score >= 40:
        analisis = f"Tu score de {score}/100 indica una salud financiera Estable. Gastas casi lo mismo que ganas."
        veredicto = "Desempeño aceptable, pero hay margen de mejora para el resto del año."
    else:
        analisis = f"Tu score de {score}/100 indica un Riesgo financiero. Estás gastando más de lo que ahorras."
        veredicto = "Es urgente reducir gastos en tu categoría crítica para estabilizar tu economía este año."

    cat = metricas.get("categoria_critica", "general")
    impacto = f"El gasto en {cat} representa un impacto importante que puede retrasar tus metas."
    
    score_salud = max(1, min(10, int(score // 10)))
    
    return {
        "pensamiento_interno_ia": "Fallback: Reporte generado por reglas locales basadas en rangos de score.",
        "score_salud_reporte": score_salud,
        "etiquetas_internas": ["fallback", "reporte_completo", f"salud_{score_salud}"],
        "nota_interna_coach": f"Reporte generado con score {score}. Categoría crítica: {cat}.",
        "analisis_score": analisis,
        "impacto_meta": impacto,
        "veredicto_final": veredicto,
        "mensaje_motivacional": "¡Cada sol ahorrado hoy es un paso más hacia tu tranquilidad financiera de este año!"
    }
