"""
servicios/ia/fallbacks/fallback_estilo_vida.py
Fallback local en caso de que Gemini falle para el módulo ANALISIS_ESTILO_VIDA.
"""

from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_fallback_estilo_vida(
    metricas: Dict[str, Any],
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> Dict[str, Any]:
    """Genera una respuesta determinista en base al cluster dominante."""
    cluster = metricas.get('cluster_dominante', 'MINIMALISTA')
    
    arquetipos = {
        "FOODIE": ("El Sibarita Explorador", "Alguien que disfruta invertir en buena comida y restaurantes.", "Usa cupones o elige un día a la semana para cocinar algo premium en casa."),
        "DIGITAL": ("El Tecno-Conectado", "Tu mundo gira en torno a las pantallas, suscripciones y gadgets.", "Audita tus suscripciones: cancela la que menos uses este mes."),
        "WELLNESS": ("El Fitness Enthusiast", "Priorizas tu cuerpo, mente y salud por encima de todo.", "Compra suplementos o membresías en planes anuales para ahorrar a largo plazo."),
        "EXPLORER": ("El Trotamundos", "Prefieres coleccionar experiencias y viajes que objetos.", "Planifica tus viajes en temporada baja o usa millas de tarjetas de crédito."),
        "MINIMALISTA": ("El Administrador Zen", "Gastas estrictamente en lo esencial y mantienes el orden.", "Sigue así, pero recuerda que también puedes presupuestar para darte un gusto ocasional.")
    }
    
    arq_nombre, arq_sig, tactica = arquetipos.get(cluster, arquetipos["MINIMALISTA"])
    
    meta_principal = contexto.nombre_meta_principal if contexto else "tu meta actual"
    return {
        "pensamiento_interno_ia": f"Fallback activado. Generando perfil para el cluster {cluster}.",
        "score_salud_estilo": 5,
        "etiquetas_internas": ["fallback", "riesgo_medio"],
        "nota_interna_coach": f"Analizar en detalle el cluster {cluster} en la próxima sesión.",
        "arquetipo": arq_nombre,
        "significado_arquetipo": arq_sig,
        "descripcion_perfil": f"¡Hola {nombres}! Basado en tus gastos recientes, tu foco principal es {cluster.lower()}.",
        "consejo_tactico": tactica,
        "alineacion_meta": f"Al optimizar tu estilo de vida, liberarás fondos más rápido para '{meta_principal}'.",
        "mensaje_estilo_vida": "¡Disfruta lo que amas de forma inteligente!"
    }
