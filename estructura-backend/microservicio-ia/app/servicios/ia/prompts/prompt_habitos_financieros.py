"""
servicios/ia/prompts/prompt_habitos_financieros.py
Generador del prompt para el módulo HABITOS_FINANCIEROS.
"""

from typing import Dict, Any

def generar_prompt_habitos_financieros(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    historial_previo = metricas.get("_historial_previo")
    memoria_interna = ""
    if isinstance(historial_previo, dict):
        nota = historial_previo.get("nota_interna_coach")
        score = historial_previo.get("score_salud_habitos")
        if nota:
            memoria_interna += f"\nTu directiva de la sesión pasada: {nota}"
        if score:
            memoria_interna += f"\nScore de salud de la sesión pasada: {score}/10"

    prompt = f"""Eres LUKA, experto en Psicología Financiera. Tono: {contexto.tono_ia}. Háblale directo al usuario llamándolo por su nombre: {contexto.nombres}.

<perfil>
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Frecuencia analizada: {metricas['frecuencia_analizada']}
Día de mayor gasto: {metricas['dia_mayor_gasto']}
Categoría más frecuente: {metricas['categoria_mas_frecuente']}
Total movimientos: {metricas['total_transacciones_periodo']}{memoria_interna}
</hallazgos>

<instrucciones>
1. En el campo `analisis_patron`, inicia saludando al usuario por su nombre en el tono configurado, y luego comenta sobre el patrón detectado (ej: gasta más los fines de semana). No repitas el saludo en otro campo.
2. Propón un "Hábito Atómico" realizable y pequeño para mejorar su relación con el dinero.
3. Si hay historial o directiva previa, evalúa si se cumplió el objetivo y menciónalo sutilmente.
4. Mantén el mensaje motivador hacia su meta.
5. Genera un `score_salud_habitos` (1-10) evaluando los hábitos mostrados.
6. Genera `etiquetas_internas` cortas y una `nota_interna_coach` clara para guiar tu análisis en la siguiente sesión.
</instrucciones>"""
    return prompt.strip()
