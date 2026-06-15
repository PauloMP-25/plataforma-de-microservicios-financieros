"""
servicios/ia/fallbacks/fallback_gasto_hormiga.py
Fallback estructurado para Gasto Hormiga.
"""
from typing import Dict, Any

def generar_fallback_gasto_hormiga(datos: Dict[str, Any], nombres: str, contexto) -> Dict[str, Any]:
    return {
        "pensamiento_interno_ia": "Análisis local estructurado de patrones de consumo para identificar fugas menores.",
        "introduccion": f"Hola {nombres}, he completado la revisión de tus transacciones para identificar consumos habituales que podrían representar fugas de dinero.",
        "analisis_ia": "He analizado tus transacciones y he detectado consumos hormiga recurrentes en categorías como entretenimiento y compras diarias, acumulando un total aproximado de S/ " + str(round(datos.get('total_gastos_hormiga', 0), 2)),
        "conexion_emocional": "Reducir estas fugas recurrentes te permitirá acelerar de forma significativa el logro de tus metas de ahorro.",
        "plan_accion_titulo": "Plan de Optimización de Gastos",
        "plan_accion_pasos": [
            "Registra tus pequeños consumos diarios durante una semana para tomar consciencia de ellos.",
            "Establece un presupuesto máximo semanal para gastos no esenciales y adhiérete a él.",
            "Automatiza un porcentaje mínimo de ahorro el mismo día que recibas tus ingresos."
        ],
        "comentario_positivo": "¡Felicidades por dar este paso! Monitorear y tomar el control de tus finanzas es la clave para la tranquilidad futura."
    }
