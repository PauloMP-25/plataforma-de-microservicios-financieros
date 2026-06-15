"""
servicios/modulos/auto_clasificacion.py
Módulo AUTO_CLASIFICACION.
"""

import pandas as pd
from typing import Dict, Any
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.modelos.esquemas import ConsejoEstructuradoAutoClasificacion
from app.servicios.ia.prompts.prompt_clasificacion import generar_prompt_clasificacion

class AutoClasificacionService(BaseAnalisisService):
    def __init__(self) -> None:
        # 0 transacciones mínimas requeridas porque procesa una sola descripcion
        super().__init__(nombre_modulo="AUTO_CLASIFICACION", min_transacciones=0)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        # No usamos el historial (df)
        descripcion = kwargs.get("descripcion", "")
        if not descripcion:
            return {"error": "No se proporcionó descripción para clasificar"}
        
        return {
            "descripcion_transaccion": descripcion
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        return generar_prompt_clasificacion(metricas, contexto)

    def obtener_esquema_salida(self):
        return ConsejoEstructuradoAutoClasificacion
