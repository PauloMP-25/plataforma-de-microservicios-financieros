"""
servicios/ia/prompts/prompt_reto_ahorro.py
Generador del prompt para el módulo RETO_AHORRO_DINAMICO.
"""

from typing import Dict, Any

def generar_prompt_reto_ahorro(
    metricas: Dict[str, Any],
    contexto,
) -> str:
    """Construye el prompt de la misión de ahorro."""
    estado = metricas.get("estado_reto")
    
    if estado == "ACTIVO":
        return f"[SKIP_IA] {metricas['mensaje']}"

    if estado == "VEREDICTO":
        resultado = "LOGRADO" if metricas['exito'] else "FALLIDO"
        return f"""Eres LUKA. Háblale directo al usuario. Tono: {contexto.tono_ia}.

<perfil>
Reto de Ahorro: {metricas['categoria']}
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Resultado Final: {resultado}
Gasto Real: S/ {metricas['gasto_real']}
Límite de Gasto: S/ {metricas['monto_limite']}
Ahorro Logrado: S/ {metricas['ahorro_real']}
</hallazgos>

<instrucciones>
Genera un veredicto estructurado siguiendo el esquema de respuesta.
1. score_salud_reto: Califica del 1 al 10 la salud financiera del usuario según el veredicto del reto.
2. etiquetas_internas: Lista de 1 a 3 etiquetas cortas sobre el desempeño.
3. nota_interna_coach: Nota técnica sobre el reto.
4. Diagnóstico: Comenta por qué lo logró o por qué falló basándote en el gasto real vs el límite.
5. Estrategia: Qué debe mejorar para la próxima o qué debe mantener.
</instrucciones>"""

    if estado == "NUEVO":
        return f"""Eres LUKA. Háblale directo al usuario. Tono: {contexto.tono_ia}.

<perfil>
Reto Propuesto: {metricas['categoria_objetivo']}
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Frecuencia: {metricas['frecuencia']}
Ahorro Potencial: S/ {metricas['ahorro_potencial']}
</hallazgos>

<instrucciones>
Propón esta misión de ahorro de manera motivadora siguiendo el esquema de respuesta.
1. score_salud_reto: Califica del 1 al 10 la viabilidad de que logre el reto según sus transacciones previas.
2. etiquetas_internas: Lista de 1 a 3 etiquetas cortas sobre el reto.
3. nota_interna_coach: Nota técnica recomendando monitorear esta categoría.
4. Diagnóstico: Indica por qué es importante reducir el gasto en esa categoría.
5. Estrategia: Dale una o dos reglas claras para no gastar más del límite.
</instrucciones>"""

    return "[SKIP_IA] Sigue registrando tus movimientos."
