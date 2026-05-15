"""
servicios/modulos/reto_ahorro_dinamico.py  ·  v2.2 — CORRECCIÓN Y MÍNIMOS
══════════════════════════════════════════════════════════════════════════════
Módulo Gamificado con Persistencia y Seguimiento Temporal.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, Any, Optional
from app.servicios.core.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.persistencia.database import SessionLocal
from app.persistencia.modelos_db import IaRetoAhorro

class RetoAhorroDinamicoService(BaseAnalisisService):
    
    def __init__(self) -> None:
        # 20 transacciones es el mínimo para detectar patrones de retos
        super().__init__(nombre_modulo="RETO_AHORRO_DINAMICO", min_transacciones=20)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO, **kwargs) -> Dict[str, Any]:
        self.validar_historial(df)
        
        usuario_id = kwargs.get("usuario_id")
        frecuencia_solicitada = kwargs.get("frecuencia", "SEMANAL")
        hoy = datetime.now()
        
        with SessionLocal() as db:
            reto = db.query(IaRetoAhorro).filter(
                IaRetoAhorro.usuario_id == usuario_id,
                IaRetoAhorro.estado.in_(["ACTIVO", "PENDIENTE_VEREDICTO"])
            ).first()

            if reto:
                if hoy >= reto.fecha_fin:
                    reto.estado = "PENDIENTE_VEREDICTO"
                    db.commit()
                    return self._evaluar_resultado_final(df, reto, contexto)
                
                total_dias = (reto.fecha_fin - reto.fecha_inicio).days or 1
                dias_pasados = (hoy - reto.fecha_inicio).days
                progreso = min(100, round((dias_pasados / total_dias) * 100))
                
                return {
                    "estado_reto": "ACTIVO",
                    "id_reto": reto.id,
                    "categoria_objetivo": reto.categoria,
                    "progreso_temporal": progreso,
                    "fecha_fin": reto.fecha_fin.strftime("%Y-%m-%d"),
                    "monto_limite": reto.monto_limite,
                    "mensaje": f"Tu misión '{reto.categoria}' está al {progreso}% de tiempo. ¡No cedas ante la tentación!"
                }

            return self._proponer_nuevo_reto(df, usuario_id, frecuencia_solicitada, db, contexto)

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        estado = metricas.get("estado_reto")
        
        if estado == "ACTIVO":
            return f"[SKIP_IA] {metricas['mensaje']}"

        if estado == "VEREDICTO":
            resultado = "LOGRADO" if metricas['exito'] else "FALLIDO"
            return f"""
            Eres LUKA. El usuario terminó su reto de '{metricas['categoria']}'.
            RESULTADO: {resultado}. AHORRO LOGRADO: S/ {metricas['ahorro_real']}.
            GASTO REAL: S/ {metricas['gasto_real']} vs LÍMITE: S/ {metricas['monto_limite']}.
            Tono: {contexto.tono_ia}.
            """

        if estado == "NUEVO":
            return f"""
            Eres LUKA. Propón una nueva MISIÓN DE AHORRO.
            CATEGORÍA: {metricas['categoria_objetivo']}. DURACIÓN: {metricas['frecuencia']}.
            Tono: {contexto.tono_ia}.
            """
        
        return "[SKIP_IA] Sigue registrando tus movimientos."

    def _proponer_nuevo_reto(self, df, usuario_id, frecuencia, db, contexto):
        df['fecha'] = pd.to_datetime(df['fecha'])
        df_gastos = df[df['tipo'] == 'GASTO'].copy()
        if df_gastos.empty: return {"error": "Sin gastos"}
        
        top_cat = df_gastos.groupby('categoria')['monto'].sum().idxmax()
        monto_periodo = df_gastos.groupby('categoria')['monto'].sum().max()
        
        dias = 7 if frecuencia == "SEMANAL" else 15 if frecuencia == "QUINCENAL" else 30
        limite_sugerido = (monto_periodo / 30) * dias * 0.5
        
        nuevo_reto = IaRetoAhorro(
            usuario_id=usuario_id,
            categoria=top_cat,
            monto_limite=limite_sugerido,
            fecha_fin=datetime.now() + timedelta(days=dias),
            frecuencia=frecuencia,
            estado="ACTIVO"
        )
        db.add(nuevo_reto)
        db.commit()
        
        return {
            "estado_reto": "NUEVO",
            "categoria_objetivo": top_cat,
            "frecuencia": frecuencia,
            "ahorro_potencial": round(limite_sugerido, 2)
        }

    def _evaluar_resultado_final(self, df, reto, contexto):
        df['fecha'] = pd.to_datetime(df['fecha'])
        df_periodo = df[(df['fecha'] >= reto.fecha_inicio) & (df['fecha'] <= reto.fecha_fin) & (df['categoria'] == reto.categoria)]
        
        gasto_real = df_periodo[df_periodo['tipo'] == 'GASTO']['monto'].sum()
        exito = gasto_real <= reto.monto_limite
        ahorro = max(0, (reto.monto_limite * 2) - gasto_real)

        with SessionLocal() as db_session:
            db_reto = db_session.query(IaRetoAhorro).get(reto.id)
            if db_reto:
                db_reto.estado = "FINALIZADO"
                db_reto.ahorro_logrado = ahorro
                db_session.commit()

        return {
            "estado_reto": "VEREDICTO",
            "categoria": reto.categoria,
            "exito": exito,
            "gasto_real": round(gasto_real, 2),
            "monto_limite": round(reto.monto_limite, 2),
            "ahorro_real": round(ahorro, 2)
        }
