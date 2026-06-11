"""
servicios/ia/prompts/prompt_simular_meta.py
Generador del prompt para el módulo SIMULAR_META.
"""

from typing import Dict, Any

def generar_prompt_simular_meta(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt de evaluación de la meta."""
    if "razon_insuficiencia" in metricas:
        return f"[SKIP_IA] {metricas['razon_insuficiencia']}"

    prompt = f"""
    Eres LUKA, Consultor de Inversiones. Tono: {contexto.tono_ia}.
    Evalúa la meta '{metricas['meta_nombre']}' de {contexto.nombres}.
    
    DATOS:
    - Monto faltante: S/ {metricas['ahorro_faltante']}.
    - Capacidad de ahorro detectada: S/ {metricas['capacidad_ahorro_mensual']} al mes.
    - Tiempo estimado: {metricas['meses_para_lograrlo']} meses.
    - ¿Viable en fecha deseada?: {metricas['viabilidad_fecha_objetivo']}.
    
    INSTRUCCIONES:
    1. Responde si es viable o no basándote en los números.
    2. Si es viable, enseña una técnica de ahorro (ej: 50/30/20) para acelerar.
    3. Si NO es viable, explica que necesita aumentar ingresos o reducir gastos fijos, y propón un plan de ajuste.
    """
    return prompt.strip()
