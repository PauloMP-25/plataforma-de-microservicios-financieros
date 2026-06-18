"""
servicios/ia/prompts/prompt_estilo_vida.py
Generador del prompt para el módulo ANALISIS_ESTILO_VIDA.
"""

from typing import Dict, Any

def generar_prompt_estilo_vida(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt para asignar una personalidad financiera."""
    
    # Manejo de historial previo (Memoria del Coach)
    historial_previo = metricas.get("_historial_previo", {})
    if historial_previo is None:
        historial_previo = {}
        
    nota_pasada = historial_previo.get("nota_interna_coach", "No hay notas previas.")
    score_pasado = historial_previo.get("score_salud_estilo", "N/A")
    
    bloque_memoria = ""
    if nota_pasada != "No hay notas previas." or score_pasado != "N/A":
        bloque_memoria = f"""
<memoria_coach>
En la sesión anterior, anotaste esto para ti mismo: "{nota_pasada}"
Su Score de Salud de Estilo pasado fue: {score_pasado}/10.
</memoria_coach>
"""

    prompt = f"""Eres LUKA, el Psicólogo Financiero. Háblale directo al usuario. Tono: {contexto.tono_ia}.

<perfil>
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Cluster Dominante: {metricas.get('cluster_dominante', 'N/A')} ({metricas.get('distribucion', {}).get(metricas.get('cluster_dominante'), {}).get('porcentaje', 0)}%)
Otros focos: {metricas.get('distribucion', {})}
</hallazgos>
{bloque_memoria}
<instrucciones>
Define la "Personalidad Financiera" de {contexto.nombres}.
1. score_salud_estilo: Evalúa del 1 al 10 cuán saludable es su estilo de vida financiero. Si bajó o subió respecto al score pasado, tenlo en cuenta.
2. etiquetas_internas: Genera de 1 a 3 palabras clave sobre su situación actual.
3. nota_interna_coach: Escribe un recordatorio u objetivo corto que revisarás en la próxima sesión.
4. arquetipo: Un nombre creativo (ej: "El Foodie Explorador", "Tecno-Minimalista").
5. significado_arquetipo: Pequeña descripción de qué significa esta personalidad y por qué se le asignó basándote en los datos.
6. descripcion_perfil: SALUDO INICIAL al usuario por su nombre según el tono de IA configurado, y luego un diagnóstico breve de su estilo de vida actual. NO SALUDES EN NINGÚN OTRO CAMPO.
7. consejo_tactico: Un "hack" para ahorrar sin renunciar a las cosas de su estilo de vida. Evalúa si mejoró respecto a tu nota anterior si existía.
8. alineacion_meta: Cómo este estilo impacta en su meta ({contexto.nombre_meta_principal}).
9. mensaje_estilo_vida: Una frase motivadora de cierre adaptada a su arquetipo (sin saludar).
</instrucciones>"""
    return prompt.strip()
