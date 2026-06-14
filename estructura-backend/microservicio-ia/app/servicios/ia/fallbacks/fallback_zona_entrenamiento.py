"""
servicios/ia/fallbacks/fallback_zona_entrenamiento.py
Fallback estructurado para Zona de Entrenamiento.
"""
from typing import Dict, Any

def generar_fallback_zona_entrenamiento(datos: Dict[str, Any], nombres: str, contexto) -> Dict[str, Any]:
    estado = datos.get("estado_fisico", "Sedentario")
    return {
        "pensamiento_interno_ia": "Falla de red con la IA. Generando rutina de contingencia usando Pandas.",
        "estado_fisico": estado,
        "evaluacion_previa": "Resumen rápido base (modo offline).",
        "rutina": [
            {
                "nombre": "Cardio de Bolsillo",
                "descripcion": "Revisa todos tus gastos menores a S/ 20 del mes pasado.",
                "duracion_dias": 30,
                "frecuencia": "1 vez por semana",
                "metrica_exito": "Encontrar al menos 2 gastos innecesarios."
            },
            {
                "nombre": "Ayuno de Suscripciones",
                "descripcion": "Cancela 1 servicio de streaming que no hayas usado en 15 días.",
                "duracion_dias": 30,
                "frecuencia": "Única vez",
                "metrica_exito": "1 suscripción cancelada."
            },
            {
                "nombre": "Levantamiento de Ahorro",
                "descripcion": "Transfiere S/ 10 a tu cuenta de ahorros al final del día.",
                "duracion_dias": 30,
                "frecuencia": "Diario",
                "metrica_exito": "Transferencia completada."
            }
        ]
    }
