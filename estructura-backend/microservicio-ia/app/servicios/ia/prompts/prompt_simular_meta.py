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

    prompt = f"""Eres LUKA, Consultor de Inversiones. Tono: {contexto.tono_ia}. Háblale directo al usuario.

<perfil>
Meta: {metricas['meta_nombre']}
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Monto faltante: S/ {metricas['ahorro_faltante']}
Capacidad ahorro detectada: S/ {metricas['capacidad_ahorro_mensual']}/mes
Tiempo estimado: {metricas['meses_para_lograrlo']} meses
¿Viable en fecha deseada?: {metricas['viabilidad_fecha_objetivo']}
</hallazgos>

<instrucciones>
1. Diagnóstico: Responde si es viable o no basándote en los números.
2. Técnica: Si es viable, sugiere una técnica de ahorro.
3. Plan: Si NO es viable, explica que necesita aumentar ingresos o reducir gastos fijos, y propón un plan de ajuste.
</instrucciones>"""
    return prompt.strip()
