"""
servicios/ia/prompts/prompt_predecir_gastos.py
Generador del prompt para el módulo PREDECIR_GASTOS.
"""

from typing import Dict, Any

def generar_prompt_predecir_gastos(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt de predicción y alerta."""
    if not metricas.get("tiene_datos", False):
        return "[SKIP_IA] Aún estoy conociendo tus hábitos. Necesito un par de meses más para predecir tu futuro financiero."

    estado_alerta = "ALERTA ROJA: RIESGO DE INSOLVENCIA" if metricas["riesgo_quiebra"] else "ESTADO: ESTABLE"
    
    return f"""Eres LUKA, Estratega Financiero especializado en PREDICCIÓN. Tono: {contexto.tono_ia.lower()}. Háblale directo al usuario.

<perfil>
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Estado: {estado_alerta}
Gasto promedio actual: S/ {metricas['promedio_historico']:,.2f}
Proyección próximo mes: S/ {metricas['proyeccion_proximo_mes']:,.2f}
Variación de tendencia: {metricas['porcentaje_variacion_mensual']}% mensual
Ingreso mensual: S/ {metricas['ingreso_mensual']:,.2f}
Déficit proyectado: S/ {metricas['deficit_estimado']:,.2f}
</hallazgos>

<instrucciones>
1. Genera una advertencia financiera o felicitación.
2. Explica de forma cruda pero motivadora cómo la tendencia de gastos afecta su meta principal o un objetivo financiero general.
3. Da UNA recomendación matemática exacta basada en los números proporcionados.
</instrucciones>"""
