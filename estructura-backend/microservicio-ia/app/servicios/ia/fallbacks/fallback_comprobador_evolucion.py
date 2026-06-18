"""
servicios/ia/fallbacks/fallback_comprobador_evolucion.py
Fallback estructurado para Comprobador de Evolución.
"""
from typing import Dict, Any

def generar_fallback_comprobador_evolucion(datos: Dict[str, Any], nombres: str, contexto) -> Dict[str, Any]:
    imf = datos.get("score_imf", 0)
    diag = datos.get("diagnostico_imf", "DATOS INCOMPLETOS")
    reincidentes = datos.get("categorias_reincidentes", [])
    
    recetas = []
    for r in reincidentes[:2]: # Máximo 2 para el fallback
        recetas.append({
            "categoria": r.get("categoria", "Desconocida"),
            "diagnostico": f"Aumento matemático del {r.get('aumento_pct', 0)}% detectado por el sistema base.",
            "posologia": [
                "1. Revisar transacciones recientes de esta categoría.",
                "2. Evitar gastos impulsivos en esta área esta semana.",
                "3. Establecer un límite temporal."
            ],
            "pronostico": f"Ahorro preventivo de S/ {r.get('gasto_extra', 0)} estimado."
        })
    
    if not recetas:
        recetas.append({
            "categoria": "Salud Financiera General",
            "diagnostico": "El sistema base no detectó reincidencias graves matemáticamente.",
            "posologia": [
                "1. Mantener los buenos hábitos actuales.",
                "2. Revisar el balance al final del mes.",
                "3. Disfrutar de tu estabilidad financiera."
            ],
            "pronostico": "Crecimiento patrimonial sostenido estimado."
        })

    return {
        "pensamiento_interno_ia": "Servicio Gemini inactivo. Generando diagnóstico básico estadístico usando Pandas.",
        "score_salud_evolucion": max(1, min(10, int(imf / 10))),
        "etiquetas_internas": ["fallback_activado", "diagnostico_base"],
        "nota_interna_coach": "Monitorear categorías reincidentes en la siguiente interacción.",
        "veredicto_narrativo": f"Basado puramente en cálculo estadístico, tu Índice de Madurez es de {imf}/100 ({diag}). Mis funciones avanzadas están pausadas, pero el motor analítico sugiere lo siguiente:",
        "recetas_medicas": recetas
    }
