"""
servicios/ia/fallbacks/fallback_simular_meta.py
Fallback en texto plano para Simular Meta.
"""
from typing import Dict, Any
from app.servicios.ia.fallbacks.fallback_generico import generar_encabezado_resiliencia

def generar_fallback_simular_meta(modulo, metricas: Dict[str, Any], nombres: str, contexto) -> str:
    header = generar_encabezado_resiliencia(modulo, metricas, nombres, contexto)
    
    meta = metricas.get('nombre_meta', 'tu meta')
    meses = metricas.get('meses_para_meta', 0)
    return (
        f"{header}"
        f"📊 **Resultados del Análisis (Proyecciones de Meta):**\n"
        f"Las proyecciones estáticas indican que lograrás **{meta}** "
        f"en aproximadamente **{meses} meses** si mantienes tu ritmo actual.\n\n"
        f"💡 **Consejo Detective LUKA:**\n"
        f"Si deseas acelerar este plazo, intenta reducir gastos no esenciales este mes para destinar un porcentaje mayor a tus ahorros."
    )
