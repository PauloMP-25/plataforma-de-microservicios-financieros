"""
servicios/ia/fallbacks/fallback_gasto_hormiga.py
Fallback estructurado para Gasto Hormiga.
"""
from typing import Dict, Any

def generar_fallback_gasto_hormiga(datos: Dict[str, Any], nombres: str, contexto) -> Dict[str, Any]:
    return {
        "pensamiento_interno_ia": "El servicio no está disponible en este momento. Generando fallback.",
        "introduccion": f"Hola {nombres}, por el momento mis sistemas principales de análisis están descansando.",
        "analisis_ia": "He revisado rápidamente tus gastos y detectado algunas salidas hormiga que suman S/ " + str(datos.get('total_gastos_hormiga', 0)),
        "conexion_emocional": "Recuerda que cada pequeño ahorro nos acerca más a tu meta principal.",
        "plan_accion_titulo": "Plan de Contingencia Rápido",
        "plan_accion_pasos": [
            "Revisa tus gastos más pequeños de la última semana",
            "Evita gastos innecesarios por hoy"
        ],
        "comentario_positivo": "¡Sigue así! Pronto tendré un análisis más profundo para ti."
    }
