"""
servicios/ia/prompts/prompt_reporte_completo.py
Generador del prompt para el módulo REPORTE_COMPLETO.
"""

from typing import Dict, Any

def generar_prompt_reporte_completo(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt para el reporte ejecutivo anual."""
    prompt = f"""
    Eres LUKA, el Auditor Estratégico. Tono: {contexto.tono_ia}.
    Presenta el REPORTE ANUAL {metricas['anio']} para {contexto.nombres}.
    
    RESUMEN EJECUTIVO:
    - Score de Salud Financiera: {metricas['score_salud']}/100.
    - Balance Acumulado: S/ {metricas['balance_anual']}.
    - Categóría de mayor impacto: {metricas['categoria_critica']} ({metricas['porcentaje_gasto_critico']}% del total).
    
    INSTRUCCIONES:
    1. Explica qué significa su Score (ej: 0-40 Riesgo, 40-70 Estable, 70+ Excelente).
    2. Analiza su balance anual y cómo esto impacta en su meta: {contexto.nombre_meta_principal}.
    3. Da un veredicto final para el cierre del año.
    """
    return prompt.strip()
