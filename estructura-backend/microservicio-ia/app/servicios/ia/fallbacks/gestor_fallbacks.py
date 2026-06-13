"""
servicios/ia/fallbacks/gestor_fallbacks.py
Gestor modular de fallbacks para la IA.
"""

from typing import Dict, Any, Optional
from app.modelos.esquemas import NombreModulo
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

from app.servicios.ia.fallbacks.fallback_gasto_hormiga import generar_fallback_gasto_hormiga
from app.servicios.ia.fallbacks.fallback_comprobador_evolucion import generar_fallback_comprobador_evolucion
from app.servicios.ia.fallbacks.fallback_zona_entrenamiento import generar_fallback_zona_entrenamiento
from app.servicios.ia.fallbacks.fallback_simular_meta import generar_fallback_simular_meta

from app.servicios.ia.fallbacks.fallback_espejo_tiempo import generar_fallback_espejo_tiempo
from app.servicios.ia.fallbacks.fallback_generico import generar_fallback_generico
from app.servicios.ia.fallbacks.fallback_predecir_gastos import generar_fallback_predecir_gastos
from app.servicios.ia.fallbacks.fallback_habitos import generar_fallback_habitos

class GestorFallbacks:
    @staticmethod
    def generar_fallback(
        modulo: NombreModulo,
        datos: Dict[str, Any],
        nombres: str,
        contexto: Optional[ContextoEstrategicoIADTO] = None,
    ) -> Any:
        
        # Módulos Estructurados (Devuelven Dict/Pydantic Compatible)
        if modulo == NombreModulo.GASTO_HORMIGA:
            return generar_fallback_gasto_hormiga(datos, nombres, contexto)
        elif modulo == NombreModulo.COMPROBADOR_EVOLUCION:
            return generar_fallback_comprobador_evolucion(datos, nombres, contexto)
        elif modulo == NombreModulo.ZONA_ENTRENAMIENTO:
            return generar_fallback_zona_entrenamiento(datos, nombres, contexto)
        elif modulo == NombreModulo.ESPEJO_TEMPORAL:
            return generar_fallback_espejo_tiempo(datos, nombres, contexto)
        elif modulo == NombreModulo.PREDECIR_GASTOS:
            return generar_fallback_predecir_gastos(datos, nombres, contexto)
        elif modulo == NombreModulo.SIMULAR_META:
            return generar_fallback_simular_meta(datos, nombres, contexto)
        elif modulo == NombreModulo.HABITOS_FINANCIEROS:
            return generar_fallback_habitos(datos, nombres, contexto)
            
        # Módulos Texto Plano (Devuelven String)
        
        # Módulos Texto Plano sin fallback específico
        return generar_fallback_generico(modulo, datos, nombres, contexto)
