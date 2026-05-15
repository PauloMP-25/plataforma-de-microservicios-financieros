"""
servicios/modulos/simular_meta.py  ·  v2.0 — PRO
══════════════════════════════════════════════════════════════════════════════
Simulador de Viabilidad de Metas con Regla Híbrida (1 Mes o 30 TXs).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, Any, Optional
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class SimularMetaService(BaseAnalisisService):
    def __init__(self) -> None:
        super().__init__(nombre_modulo="SIMULAR_META", min_transacciones=30)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        # 1. Validación Híbrida: 30 transacciones O al menos 30 días de historial
        df['fecha'] = pd.to_datetime(df['fecha'])
        dias_historial = (df['fecha'].max() - df['fecha'].min()).days
        
        if len(df) < 30 and dias_historial < 30:
            return {
                "es_viable": False, 
                "razon_insuficiencia": f"Necesito al menos 30 transacciones o 1 mes de historial (tienes {len(df)} txs y {dias_historial} días)."
            }

        # 2. Parámetros de la meta
        monto_objetivo = kwargs.get("meta_monto", 0.0)
        ahorro_actual = kwargs.get("meta_ahorro_previo", 0.0)
        fecha_deseada_str = kwargs.get("fecha_objetivo") # Opcional: "YYYY-MM-DD"
        
        faltante = max(0, monto_objetivo - ahorro_actual)
        
        # 3. Cálculo de capacidad de ahorro real (Promedio mensual)
        promedio_ingresos = df[df['tipo'] == 'INGRESO']['monto'].sum() / max(1, (dias_historial / 30))
        promedio_gastos = df[df['tipo'] == 'GASTO']['monto'].sum() / max(1, (dias_historial / 30))
        capacidad_ahorro = max(0, promedio_ingresos - promedio_gastos)

        meses_estimados = 999
        if capacidad_ahorro > 0:
            meses_estimados = faltante / capacidad_ahorro

        # 4. Verificación de fecha objetivo
        es_viable_en_fecha = True
        if fecha_deseada_str:
            fecha_objetivo = datetime.strptime(fecha_deseada_str, "%Y-%m-%d")
            meses_disponibles = (fecha_objetivo - datetime.now()).days / 30
            es_viable_en_fecha = meses_estimados <= meses_disponibles

        return {
            "meta_nombre": kwargs.get("meta_nombre", "Mi Meta"),
            "es_viable": meses_estimados < 48, # Viable si es menos de 4 años
            "meses_para_lograrlo": round(meses_estimados, 1),
            "capacidad_ahorro_mensual": round(capacidad_ahorro, 2),
            "viabilidad_fecha_objetivo": es_viable_en_fecha if fecha_deseada_str else None,
            "ahorro_faltante": round(faltante, 2)
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        if "razon_insuficiencia" in metricas:
            return f"[SKIP_IA] {metricas['razon_insuficiencia']}"

        viable_str = "SÍ" if metricas["es_viable"] else "DIFÍCIL"
        
        return f"""
        Eres LUKA, Consultor de Inversiones. Tono: {contexto.tono_ia}.
        Evalúa la meta '{metricas['meta_nombre']}' de {contexto.nombres}.
        
        DATOS:
        - Monto faltante: S/ {metricas['ahorro_faltante']}.
        - Capacidad de ahorro detectada: S/ {metricas['capacidad_ahorro_mensual']} al mes.
        - Tiempo estimado: {metricas['meses_para_lograrlo']} meses.
        - ¿Viable en fecha deseada?: {metricas['viabilidad_fecha_objetivo']}.
        
        INSTRUCCIONES:
        1. Responde si es viable o no basándote en los números.
        2. Si es viable, enseña una técnica de ahorro (ej: 50/30/20) para acelerar.
        3. Si NO es viable, explica que necesita aumentar ingresos o reducir gastos fijos, y propón un plan de ajuste.
        """
