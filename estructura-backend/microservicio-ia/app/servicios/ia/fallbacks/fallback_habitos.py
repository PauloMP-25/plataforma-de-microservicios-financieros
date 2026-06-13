"""
servicios/ia/fallbacks/fallback_habitos.py
Fallback local en caso de que Gemini falle para el módulo HABITOS_FINANCIEROS.
"""

from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_fallback_habitos(
    metricas: Dict[str, Any],
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> Dict[str, Any]:
    """Genera una respuesta determinista en base a las métricas del motor local (Pandas)
    adaptada al esquema ConsejoEstructuradoHabitos."""
    dia = metricas.get("dia_mayor_gasto", "N/A")
    cat = metricas.get("categoria_mas_frecuente", "N/A")
    es_saludable = metricas.get("es_saludable", True)

    patron = f"Observamos que tu mayor actividad de gastos es el día {dia}, enfocada en la categoría '{cat}'."
    
    if es_saludable:
        habito = "Asigna un límite fijo semanal para esa categoría y transfiere el excedente a tu cuenta de ahorro al inicio de la semana."
        motivacional = "¡Vas por buen camino! Sigue manteniendo ingresos superiores a tus gastos."
    else:
        habito = "Antes de realizar compras en tu categoría principal, espera 24 horas. Esto reducirá las compras impulsivas."
        motivacional = "Pequeños ajustes diarios crean grandes resultados. ¡Tú puedes retomar el control!"

    return {
        "pensamiento_interno_ia": "Fallback activado. Generando respuesta basada en reglas predefinidas y estadísticas de Pandas.",
        "introduccion": "Hola, aquí tienes un resumen rápido de tus hábitos recientes.",
        "analisis_patron": patron,
        "habito_atomico_sugerido": habito,
        "mensaje_motivacional": motivacional
    }
