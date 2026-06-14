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
        return f"""
        Eres LUKA. El usuario terminó su reto de '{metricas['categoria']}'.
        RESULTADO: {resultado}. AHORRO LOGRADO: S/ {metricas['ahorro_real']}.
        GASTO REAL: S/ {metricas['gasto_real']} vs LÍMITE: S/ {metricas['monto_limite']}.
        Tono: {contexto.tono_ia}.
        """

    if estado == "NUEVO":
        return f"""
        Eres LUKA. Propón una nueva MISIÓN DE AHORRO.
        CATEGORÍA: {metricas['categoria_objetivo']}. DURACIÓN: {metricas['frecuencia']}.
        Tono: {contexto.tono_ia}.
        """
    
    return "[SKIP_IA] Sigue registrando tus movimientos."
