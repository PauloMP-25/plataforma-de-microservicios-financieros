"""
servicios/ia/prompts/prompt_clasificacion.py
Generador del prompt para el módulo AUTO_CLASIFICACION.
"""

from typing import Dict, Any

def generar_prompt_clasificacion(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt estricto para auto clasificar."""
    descripcion = metricas.get('descripcion_transaccion', 'Desconocido')
    
    prompt = f"""<rol>Clasificador de Transacciones Financieras</rol>
<descripcion_usuario> {descripcion} </descripcion_usuario>
<tarea>Genera exactamente 4 categorías únicas (de 1 sola palabra cada una) que mejor definan esta descripción de gasto o ingreso.</tarea>
<restricciones>
- No justifiques. 
- No expliques. 
- Solo devuelve las 4 palabras de forma precisa.
- Las categorías deben estar en mayúsculas.
- La primera debe ser la más precisa.
</restricciones>"""
    return prompt
