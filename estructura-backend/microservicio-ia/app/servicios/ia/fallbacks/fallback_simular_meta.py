"""
servicios/ia/fallbacks/fallback_simular_meta.py
Fallback determinista estructurado para Simular Meta.
"""
from typing import Dict, Any

def generar_fallback_simular_meta(datos: Dict[str, Any], nombres: str, contexto: Any = None) -> Dict[str, Any]:
    meta = datos.get('meta_nombre', 'tu meta')
    meses = datos.get('meses_para_lograrlo', 0)
    ahorro = datos.get('capacidad_ahorro_mensual', 0)
    faltante = datos.get('ahorro_faltante', 0)
    es_viable = datos.get('es_viable', False)

    if "razon_insuficiencia" in datos:
        return {
            "pensamiento_interno_ia": "Falta historial para simular.",
            "introduccion": f"Hola {nombres}, no tenemos datos suficientes aún.",
            "diagnostico_viabilidad": datos['razon_insuficiencia'],
            "plan_accion": "Registra tus gastos e ingresos regularmente durante este mes.",
            "tecnica_sugerida": None,
            "mensaje_motivacional": "¡El primer paso es registrar, tú puedes!"
        }

    if es_viable:
        return {
            "pensamiento_interno_ia": "El modelo no respondió. Fallback: meta viable.",
            "introduccion": f"Hola {nombres}, las proyecciones muestran que {meta} es alcanzable.",
            "diagnostico_viabilidad": f"Con tu ahorro mensual de S/{ahorro:,.2f}, tardarás aproximadamente {meses} meses en juntar los S/{faltante:,.2f} restantes.",
            "plan_accion": "Mantén tu nivel de gastos actual y asegúrate de guardar el excedente cada mes.",
            "tecnica_sugerida": "Regla 50/30/20: Destina 20% a tus ahorros de forma estricta.",
            "mensaje_motivacional": "¡Sigue así, el tiempo pasa rápido cuando tienes una meta clara!"
        }
    else:
        return {
            "pensamiento_interno_ia": "El modelo no respondió. Fallback: meta no viable en el corto plazo.",
            "introduccion": f"Hola {nombres}, {meta} requerirá un ajuste en tus finanzas.",
            "diagnostico_viabilidad": f"Tu capacidad de ahorro de S/{ahorro:,.2f} no es suficiente para alcanzar la meta en un plazo razonable.",
            "plan_accion": "Necesitamos reducir tus gastos no esenciales o buscar un ingreso extra temporal.",
            "tecnica_sugerida": "Microahorro semanal: ahorra pequeñas cantidades semanales en vez de montos fijos grandes.",
            "mensaje_motivacional": "Todo gran objetivo requiere sacrificios, ¡yo sé que puedes lograrlo!"
        }
