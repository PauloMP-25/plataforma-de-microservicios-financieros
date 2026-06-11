"""
servicios/ia/fallbacks/gestor_fallbacks.py
Gestor modular de fallbacks para la IA.
"""

from typing import Dict, Any, Optional
from app.modelos.esquemas import NombreModulo
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class GestorFallbacks:
    @staticmethod
    def generar_fallback(
        modulo: NombreModulo,
        datos: Dict[str, Any],
        nombres: str,
        contexto: Optional[ContextoEstrategicoIADTO] = None,
    ) -> Any:
        if modulo == NombreModulo.GASTO_HORMIGA:
            # Retorna un diccionario que coincida con ConsejoEstructurado
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
        
        # Fallback genérico para módulos legacy (texto plano)
        return f"Hola {nombres}, por el momento no puedo conectarme a mi motor de IA. Por favor, intenta de nuevo más tarde."
