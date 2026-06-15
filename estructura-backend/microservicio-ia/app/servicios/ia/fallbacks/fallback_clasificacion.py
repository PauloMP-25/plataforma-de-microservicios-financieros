"""
servicios/ia/fallbacks/fallback_clasificacion.py
Fallback local en caso de que Gemini falle para el módulo AUTO_CLASIFICACION.
"""

from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_fallback_clasificacion(
    metricas: Dict[str, Any],
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> Dict[str, Any]:
    """Genera una respuesta genérica."""
    return {
        "categorias_sugeridas": ["OTROS", "VARIOS", "GENERAL", "INDEFINIDO"]
    }
