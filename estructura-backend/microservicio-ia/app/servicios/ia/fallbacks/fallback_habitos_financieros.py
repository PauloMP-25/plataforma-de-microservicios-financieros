"""
servicios/ia/fallbacks/fallback_habitos_financieros.py
Fallback en texto plano para Hábitos Financieros.
"""
from typing import Dict, Any
from app.servicios.ia.fallbacks.fallback_generico import generar_encabezado_resiliencia

def generar_fallback_habitos_financieros(modulo, metricas: Dict[str, Any], nombres: str, contexto) -> str:
    header = generar_encabezado_resiliencia(modulo, metricas, nombres, contexto)
    salud = metricas.get('salud_financiera', 50)
    return (
        f"{header}"
        f"📊 **Resultados del Análisis (Hábitos):**\n"
        f"Tu índice de salud financiera calculado es de **{salud}/100**.\n\n"
        f"💡 **Consejo Detective LUKA:**\n"
        f"Intenta seguir la regla básica 50/30/20: mantén tus ahorros en al menos un 20% de tus ingresos totales "
        f"y evita que tus gastos fijos (necesidades) superen el 50% de tus ingresos."
    )
