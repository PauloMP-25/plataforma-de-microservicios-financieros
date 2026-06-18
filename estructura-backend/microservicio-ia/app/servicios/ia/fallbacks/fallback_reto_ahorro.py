"""
servicios/ia/fallbacks/fallback_reto_ahorro.py
Fallback local en caso de que Gemini falle para el módulo RETO_AHORRO_DINAMICO.
"""

from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_fallback_reto_ahorro(
    metricas: Dict[str, Any],
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> Dict[str, Any]:
    """Genera una respuesta determinista en base al estado del reto."""
    estado = metricas.get("estado_reto")

    if estado == "VEREDICTO":
        exito = metricas.get('exito', False)
        if exito:
            titulo = "¡Misión Cumplida!"
            diag = f"Lograste mantenerte por debajo del límite en {metricas.get('categoria')}."
            estrategia = "Excelente disciplina. Mantén esta misma regla para la próxima misión."
            motivacional = "¡Sigue así, estás construyendo un futuro sólido!"
        else:
            titulo = "Misión Fallida"
            diag = f"Excediste el límite en la categoría {metricas.get('categoria')}."
            estrategia = "Revisa en qué gastos específicos te excediste y evítalos en la próxima."
            motivacional = "No te desanimes, el próximo reto será tuyo."
            
        return {
            "pensamiento_interno_ia": "Fallback: Veredicto generado por reglas locales.",
            "score_salud_reto": 7 if exito else 4,
            "etiquetas_internas": ["fallback", "reto_veredicto", "logrado" if exito else "fallido"],
            "nota_interna_coach": f"Reto finalizado: {resultado}. Revisar consistencia de ahorro.",
            "titulo_mision": titulo,
            "diagnostico": diag,
            "estrategia": estrategia,
            "mensaje_motivacional": motivacional
        }

    # Si no es VEREDICTO (ni ACTIVO que hace SKIP_IA), es NUEVO
    cat = metricas.get("categoria_objetivo", "gastos generales")
    ahorro = metricas.get("ahorro_potencial", 0)
    
    return {
        "pensamiento_interno_ia": "Fallback: Propuesta de nueva misión generada por reglas locales.",
        "score_salud_reto": 5,
        "etiquetas_internas": ["fallback", "reto_propuesto", cat.lower()],
        "nota_interna_coach": f"Nueva misión propuesta para {cat}.",
        "titulo_mision": f"Operación: Controlar {cat}",
        "diagnostico": f"Hemos detectado que puedes optimizar tus gastos en {cat}.",
        "estrategia": f"Intenta reducir tus compras en esta categoría para ahorrar potencialmente S/ {ahorro}.",
        "mensaje_motivacional": "¡Acepta el reto y demuestra tu disciplina!"
    }
