"""
servicios/modulos/analisis_estilo_de_vida.py  ·  v1.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Módulo ANALISIS_ESTILO_VIDA (The Persona Finder).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
from typing import Dict, Any, List
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.ia.prompts.prompt_estilo_vida import generar_prompt_estilo_vida

class AnalisisEstiloVidaService(BaseAnalisisService):
    def __init__(self) -> None:
        # Se requieren 20 transacciones para tener una muestra representativa de estilo de vida
        super().__init__(nombre_modulo="ANALISIS_ESTILO_VIDA", min_transacciones=20)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        self.validar_historial(df)
        
        # 1. Definir Grupos de Estilo de Vida
        clusters = {
            "FOODIE": ["RESTAURANTE", "DELIVERY", "CAFETERIA", "BAR", "GASTRONOMIA"],
            "DIGITAL": ["TECNOLOGIA", "SUSCRIPCIONES", "STREAMING", "GAMING", "APPS"],
            "WELLNESS": ["SALUD", "GYM", "DEPORTE", "CUIDADO PERSONAL", "FARMACIA"],
            "EXPLORER": ["VIAJES", "TRANSPORTE", "HOTELES", "TURISMO"],
            "MINIMALISTA": ["HOGAR", "SUPERMERCADO", "SERVICIOS"]
        }
        
        df['categoria'] = df['categoria'].str.upper()
        total_gasto = df[df['tipo'] == 'GASTO']['monto'].sum()
        
        if total_gasto == 0:
            return {"error": "Sin gastos registrados para definir perfil"}

        # 2. Calcular distribución por cluster
        analisis_clusters = {}
        for nombre, cats in clusters.items():
            monto_cluster = df[(df['tipo'] == 'GASTO') & (df['categoria'].isin(cats))]['monto'].sum()
            analisis_clusters[nombre] = {
                "monto": float(monto_cluster),
                "porcentaje": round((monto_cluster / total_gasto) * 100, 1)
            }
        
        # 3. Identificar cluster dominante
        cluster_dominante = max(analisis_clusters, key=lambda k: analisis_clusters[k]['monto'])
        
        return {
            "cluster_dominante": cluster_dominante,
            "distribucion": analisis_clusters,
            "total_analizado": float(total_gasto),
            "categorias_detectadas": df['categoria'].unique().tolist()
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        return generar_prompt_estilo_vida(metricas, contexto)
