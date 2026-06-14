"""
servicios/ia/prompts/prompt_habitos_financieros.py
Generador del prompt para el módulo HABITOS_FINANCIEROS.
"""

from typing import Dict, Any

def generar_prompt_habitos_financieros(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt para el análisis de hábitos financieros."""
    prompt = f"""
    Eres LUKA, experto en Psicología Financiera. Tono: {contexto.tono_ia}.
    Analiza los HÁBITOS {metricas['frecuencia_analizada']}S de {contexto.nombres}.
    
    DATOS:
    - El día que más gasta es el: {metricas['dia_mayor_gasto']}.
    - La categoría donde más transacciona es: {metricas['categoria_mas_frecuente']}.
    - Total movimientos en el periodo: {metricas['total_transacciones_periodo']}.
    
    INSTRUCCIONES:
    1. Comenta sobre el patrón detectado (ej: si gasta más los fines de semana).
    2. Propón un "Hábito Atómico" para mejorar su relación con el dinero.
    3. Mantén el mensaje motivador hacia su meta: {contexto.nombre_meta_principal}.
    """
    return prompt.strip()
