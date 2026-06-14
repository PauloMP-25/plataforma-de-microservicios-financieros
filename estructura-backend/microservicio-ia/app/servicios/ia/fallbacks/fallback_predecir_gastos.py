"""
servicios/ia/fallbacks/fallback_predecir_gastos.py
Fallback determinista para el módulo PREDECIR_GASTOS.
Devuelve el esquema exacto que espera el frontend cuando Gemini falla.
"""

from typing import Dict, Any

def generar_fallback_predecir_gastos(datos: Dict[str, Any], nombres: str, contexto: Any = None) -> Dict[str, Any]:
    """Genera la respuesta estructurada desde reglas duras (Pandas)."""
    
    riesgo = datos.get("riesgo_quiebra", False)
    promedio = datos.get("promedio_historico", 0)
    proyeccion = datos.get("proyeccion_proximo_mes", 0)
    deficit = datos.get("deficit_estimado", 0)
    variacion = datos.get("porcentaje_variacion_mensual", 0)
    meta = contexto.nombre_meta_principal if contexto else "tus ahorros"
    
    if riesgo:
        return {
            "pensamiento_interno_ia": "El modelo de Gemini no respondió. Usando fallback determinista de riesgo alto por déficit proyectado.",
            "introduccion": f"Hola {nombres}, este es un aviso automático de emergencia financiera.",
            "analisis_tendencia": f"Tu gasto promedio es S/{promedio:,.2f}, pero la tendencia apunta a que el próximo mes gastarás S/{proyeccion:,.2f} (variación de {variacion}%).",
            "impacto_meta": f"Esta tendencia de déficit de S/{deficit:,.2f} pone en grave riesgo {meta}.",
            "recomendacion_matematica": f"Necesitas reducir tus gastos no esenciales inmediatamente para cubrir el déficit proyectado de S/{deficit:,.2f}.",
            "mensaje_motivacional": "Los números no mienten, pero tú tienes el control para cambiarlos hoy mismo."
        }
    else:
        balance = datos.get("balance_proyectado", 0)
        return {
            "pensamiento_interno_ia": "El modelo de Gemini no respondió. Usando fallback determinista de tendencia estable.",
            "introduccion": f"Hola {nombres}, este es tu resumen predictivo automático.",
            "analisis_tendencia": f"Tus gastos están controlados. La tendencia apunta a un gasto de S/{proyeccion:,.2f} para el próximo mes.",
            "impacto_meta": f"Esta estabilidad te permitirá tener un margen positivo de S/{balance:,.2f} para aportar a {meta}.",
            "recomendacion_matematica": f"Mantén este ritmo y asegura enviar tu saldo a fin de mes directo a tu meta de ahorro.",
            "mensaje_motivacional": "Sigue así, la disciplina financiera rinde frutos reales."
        }
