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
    prompt = f"""Eres LUKA, el Psicólogo Financiero. Háblale directo al usuario. Tono: {contexto.tono_ia}.

<perfil>
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Cluster Dominante: {metricas['cluster_dominante']} ({metricas['distribucion'][metricas['cluster_dominante']]['porcentaje']}%)
Otros focos: {metricas['distribucion']}
</hallazgos>

<instrucciones>
Define la "Personalidad Financiera" de {contexto.nombres}.
1. arquetipo: Un nombre creativo (ej: "El Foodie Explorador", "Tecno-Minimalista").
2. significado_arquetipo: Pequeña descripción de qué significa esta personalidad y por qué se le asignó basándote en los datos.
3. descripcion_perfil: Diagnóstico breve de su estilo de vida actual.
4. consejo_tactico: Un "hack" para ahorrar sin renunciar a las cosas de su estilo de vida.
5. alineacion_meta: Cómo este estilo impacta en su meta ({contexto.nombre_meta_principal}).
6. mensaje_estilo_vida: Una frase motivadora de cierre adaptada a su arquetipo.
</instrucciones>"""
    return prompt.strip()
