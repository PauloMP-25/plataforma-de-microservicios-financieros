"""
servicios/modulos/espejo_tiempo.py  ·  v1.0 — ESPEJO DEL TIEMPO (LUKA)
══════════════════════════════════════════════════════════════════════════════
Módulo de proyección temporal financiera a 3, 6 y 12 meses.

Arquitectura de 3 Fases:
  FASE 1 — El orquestador (ServicioAnalisis) recopila el DataFrame de
            transacciones y las metas activas del usuario.
  FASE 2 — Este módulo (Pandas/NumPy) calcula KPIs de ahorro mensual,
            tendencia del score y proyecta dos líneas de futuro:
            'Continuidad' (sin cambios) y 'Transformación' (reducción
            del 25 % en gastos no esenciales).
  FASE 3 — Solo los KPIs resumidos se pasan a Gemini; él NO calcula
            nada, solo narra creativamente dos «cartas al futuro».

Mínimo requerido: 30 transacciones históricas.

Autor: microservicio-ia LUKA
"""

import pandas as pd
import numpy as np
import logging
from typing import Any, Dict, List

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.modelos.esquemas import ConsejoEstructuradoEspejo
from app.servicios.ia.prompts.prompt_espejo_tiempo import generar_prompt_espejo_tiempo
from app.utilidades.excepciones import HistorialInsuficienteError

logger = logging.getLogger(__name__)

# Categorías consideradas NO esenciales para la línea de Transformación.
CATEGORIAS_NO_ESENCIALES = {
    "entretenimiento", "restaurantes", "delivery", "comida rapida",
    "suscripciones", "ropa", "accesorios", "juegos", "viajes",
    "salidas", "ocio", "belleza", "caprichos", "snacks", "cafeteria",
}

# Límite mínimo de transacciones para ejecutar el análisis.
MIN_TRANSACCIONES = 30

# Horizonte de proyección en meses.
HITOS_MESES = [3, 6, 12]


class EspejoTiempoService:
    """
    Servicio del módulo «Espejo del Tiempo».

    Expone la misma interfaz que los demás módulos canónicos del proyecto:
      - ejecutar_calculos(df, contexto, **kwargs) → Dict[str, Any]
      - orquestar_prompt(metricas, contexto)       → str
      - obtener_esquema_salida()                   → Pydantic BaseModel class
    """

    nombre_modulo: str = "ESPEJO_TEMPORAL"

    # ── Interfaz pública ──────────────────────────────────────────────────────

    def ejecutar_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
        **kwargs,
    ) -> Dict[str, Any]:
        """
        FASE 2: Calcula los KPIs de ahorro y proyecta dos líneas de futuro.

        Parámetros:
            df       — DataFrame con columnas: fecha, tipo, monto, categoria.
            contexto — ContextoEstrategicoIADTO con datos del usuario.
            **kwargs — Puede recibir 'metas' (List[dict]) con nombre,
                       monto_objetivo y monto_actual de cada meta activa.

        Lanza:
            HistorialInsuficienteError si len(df) < MIN_TRANSACCIONES.

        Retorna:
            Dict con todos los KPIs y las proyecciones listas para el
            prompt de Gemini y para el campo hallazgos del frontend.
        """
        # Validación mínima de datos
        if len(df) < MIN_TRANSACCIONES:
            raise HistorialInsuficienteError(
                modulo=self.nombre_modulo,
                actuales=len(df),
                requeridos=MIN_TRANSACCIONES,
            )

        # Normalización del DataFrame
        df = df.copy()
        df["fecha"] = pd.to_datetime(df["fecha"])
        df["categoria"] = df["categoria"].str.lower().str.strip()

        # Metas activas pasadas como kwargs (lista de dicts)
        metas: List[Dict[str, Any]] = kwargs.get("metas", [])

        # ── KPI 1: Score actual y tendencia ──────────────────────────────────
        score_actual = int(kwargs.get("score_actual", 50))
        tendencia_score = self._calcular_tendencia_score(df)

        # ── KPI 2: Capacidad de ahorro mensual (últimos 3 meses) ─────────────
        ahorro_mensual, promedio_ingresos_3m, promedio_gastos_3m = (
            self._calcular_ahorro_mensual(df)
        )

        # ── KPI 3: Gastos no esenciales y ahorro optimizado ──────────────────
        gastos_optimizados, reduccion_aplicada = self._calcular_gastos_optimizados(
            df, promedio_gastos_3m
        )
        ahorro_mensual_optimizado = promedio_ingresos_3m - gastos_optimizados

        # ── Proyecciones a 3, 6 y 12 meses ───────────────────────────────────
        proyeccion_continuidad = self._proyectar(
            meses_lista=HITOS_MESES,
            ahorro_mensual=ahorro_mensual,
            score_actual=score_actual,
            tendencia_score=tendencia_score,
            metas=metas,
            linea="continuidad",
        )

        proyeccion_transformacion = self._proyectar(
            meses_lista=HITOS_MESES,
            ahorro_mensual=ahorro_mensual_optimizado,
            score_actual=score_actual,
            tendencia_score=tendencia_score,
            metas=metas,
            linea="transformacion",
        )

        # ── Resumen para Gemini (FASE 3) ─────────────────────────────────────
        diferencia_neta_12m = round(
            (ahorro_mensual_optimizado - ahorro_mensual) * 12, 2
        )

        metricas = {
            # Datos del presente
            "score_actual": score_actual,
            "ahorro_mensual_actual": round(ahorro_mensual, 2),
            "ahorro_mensual_optimizado": round(ahorro_mensual_optimizado, 2),
            "diferencia_neta_12m": diferencia_neta_12m,
            "reduccion_gastos_aplicada": round(reduccion_aplicada, 2),
            "metas_activas_count": len(metas),
            "tendencia_score": round(tendencia_score, 3),
            # Proyecciones completas (usadas para hallazgos y para el prompt)
            "proyeccion_continuidad": proyeccion_continuidad,
            "proyeccion_transformacion": proyeccion_transformacion,
            # Nombres de metas a 12 meses (para el prompt)
            "metas_cumplidas_continuidad_12m": proyeccion_continuidad["hitos12Meses"]["metasCumplidas"],
            "metas_fracasadas_continuidad_12m": proyeccion_continuidad["hitos12Meses"]["metasFracasadas"],
            "metas_cumplidas_transformacion_12m": proyeccion_transformacion["hitos12Meses"]["metasCumplidas"],
            "metas_fracasadas_transformacion_12m": proyeccion_transformacion["hitos12Meses"]["metasFracasadas"],
        }

        logger.info(
            "[ESPEJO-TIEMPO] Usuario procesado — score=%d, ahorro_actual=%.2f, "
            "ahorro_optimizado=%.2f, metas=%d",
            score_actual,
            ahorro_mensual,
            ahorro_mensual_optimizado,
            len(metas),
        )

        return metricas

    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """
        FASE 3: Construye el prompt reducido para Gemini.
        Solo los KPIs clave; Gemini NO recalcula nada.
        """
        return generar_prompt_espejo_tiempo(metricas, contexto)

    def obtener_esquema_salida(self):
        """Retorna el esquema Pydantic para Structured Outputs de Gemini."""
        return ConsejoEstructuradoEspejo

    # ── Métodos privados de cálculo (Pandas/NumPy) ────────────────────────────

    def _calcular_ahorro_mensual(
        self, df: pd.DataFrame
    ):
        """
        Calcula el promedio mensual de ingresos y gastos de los últimos 3 meses
        para obtener la capacidad de ahorro mensual actual.

        Retorna:
            Tupla (ahorro_mensual, promedio_ingresos_3m, promedio_gastos_3m).
        """
        fecha_corte = df["fecha"].max() - pd.DateOffset(months=3)
        df_3m = df[df["fecha"] >= fecha_corte]

        if df_3m.empty:
            return 0.0, 0.0, 0.0

        ingresos_3m = float(
            df_3m[df_3m["tipo"] == "INGRESO"]["monto"].sum()
        )
        gastos_3m = float(
            df_3m[df_3m["tipo"] == "GASTO"]["monto"].sum()
        )

        # Número real de meses distintos en la ventana de 3 meses
        meses_distintos = max(
            df_3m["fecha"].dt.to_period("M").nunique(), 1
        )

        promedio_ingresos = ingresos_3m / meses_distintos
        promedio_gastos = gastos_3m / meses_distintos
        ahorro = promedio_ingresos - promedio_gastos

        return ahorro, promedio_ingresos, promedio_gastos

    def _calcular_tendencia_score(self, df: pd.DataFrame) -> float:
        """
        Estima la tendencia mensual del score financiero usando la tasa de
        ahorro mensual agrupada.

        Una tasa de ahorro positiva y creciente aporta puntos; negativa,
        resta. Retorna la variación media de puntos por mes (puede ser
        negativa).
        """
        df_gastos = df[df["tipo"] == "GASTO"].copy()
        df_ingresos = df[df["tipo"] == "INGRESO"].copy()

        if df_gastos.empty or df_ingresos.empty:
            return 0.0

        # Agrupamos por mes para obtener la tasa de ahorro mensual
        df_gastos["periodo"] = df_gastos["fecha"].dt.to_period("M")
        df_ingresos["periodo"] = df_ingresos["fecha"].dt.to_period("M")

        gastos_por_mes = df_gastos.groupby("periodo")["monto"].sum()
        ingresos_por_mes = df_ingresos.groupby("periodo")["monto"].sum()

        periodos = ingresos_por_mes.index.union(gastos_por_mes.index)
        tasas = []
        for p in periodos:
            ing = float(ingresos_por_mes.get(p, 0.0))
            gas = float(gastos_por_mes.get(p, 0.0))
            if ing > 0:
                tasa = (ing - gas) / ing  # tasa de ahorro [0, 1]
                # Convertir tasa a variación de score: [-3, +3] por mes
                tasas.append(tasa * 3.0)

        if len(tasas) < 2:
            return float(np.mean(tasas)) if tasas else 0.0

        # Regresión lineal simple sobre las tasas para capturar la tendencia
        x = np.arange(len(tasas), dtype=float)
        coef = np.polyfit(x, tasas, 1)  # [pendiente, intercepto]
        return float(coef[0])  # puntos de score por mes

    def _calcular_gastos_optimizados(
        self, df: pd.DataFrame, promedio_gastos_3m: float
    ):
        """
        Identifica el top de categorías no esenciales y aplica una reducción
        conservadora del 25 % sobre su gasto promedio mensual.

        Retorna:
            Tupla (gastos_optimizados, reduccion_monetaria_aplicada).
        """
        fecha_corte = df["fecha"].max() - pd.DateOffset(months=3)
        df_3m = df[
            (df["fecha"] >= fecha_corte) & (df["tipo"] == "GASTO")
        ].copy()

        if df_3m.empty:
            return promedio_gastos_3m, 0.0

        meses_distintos = max(df_3m["fecha"].dt.to_period("M").nunique(), 1)

        # Gasto promedio por categoría al mes
        cat_promedio = (
            df_3m.groupby("categoria")["monto"].sum() / meses_distintos
        )

        # Filtrar las categorías no esenciales presentes en el historial
        cats_no_esenciales_presentes = cat_promedio[
            cat_promedio.index.isin(CATEGORIAS_NO_ESENCIALES)
        ]

        if cats_no_esenciales_presentes.empty:
            # Sin categorías no esenciales: aplicar reducción genérica del 10 %
            reduccion = promedio_gastos_3m * 0.10
            return promedio_gastos_3m - reduccion, reduccion

        # Reducción del 25 % sobre gastos no esenciales
        total_no_esencial = float(cats_no_esenciales_presentes.sum())
        reduccion = total_no_esencial * 0.25
        gastos_optimizados = promedio_gastos_3m - reduccion

        return gastos_optimizados, reduccion

    def _proyectar(
        self,
        meses_lista: List[int],
        ahorro_mensual: float,
        score_actual: int,
        tendencia_score: float,
        metas: List[Dict[str, Any]],
        linea: str,
    ) -> Dict[str, Any]:
        """
        Genera los hitos de proyección para una línea (continuidad o
        transformación) en los horizontes de meses especificados.

        Para cada horizonte calcula:
          - scoreProyectado  : score_actual + tendencia * meses
                               (cap en 100, mínimo 0; +5/mes máx en
                               transformación).
          - ahorroAcumulado  : ahorro_mensual * meses.
          - metasCumplidas   : nombres de metas cuyo monto_restante
                               queda cubierto por el ahorro acumulado.
          - metasFracasadas  : el resto.

        Retorna:
            Dict con claves hitos3Meses, hitos6Meses, hitos12Meses.
        """
        resultado = {}

        for meses in meses_lista:
            ahorro_acumulado = round(ahorro_mensual * meses, 2)

            # Score proyectado con cap en línea de transformación
            if linea == "transformacion":
                delta_mensual = min(tendencia_score + 1.5, 5.0)
            else:
                delta_mensual = tendencia_score

            score_proyectado = int(
                min(max(score_actual + delta_mensual * meses, 0), 100)
            )

            # Evaluación de metas
            cumplidas = []
            fracasadas = []
            for meta in metas:
                nombre = meta.get("nombre", "Meta sin nombre")
                monto_objetivo = float(meta.get("monto_objetivo", 0.0))
                monto_actual = float(meta.get("monto_actual", 0.0))
                monto_restante = max(monto_objetivo - monto_actual, 0.0)

                if ahorro_acumulado >= monto_restante:
                    cumplidas.append(nombre)
                else:
                    fracasadas.append(nombre)

            clave = f"hitos{meses}Meses"
            resultado[clave] = {
                "scoreProyectado": score_proyectado,
                "ahorroAcumulado": ahorro_acumulado,
                "metasCumplidas": cumplidas,
                "metasFracasadas": fracasadas,
            }

        return resultado
