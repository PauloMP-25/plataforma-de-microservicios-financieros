"""
utilidades/preparador_datos.py
Convierte el JSON de respuesta de Java en DataFrames de Pandas
listos para los módulos de análisis.
"""

import pandas as pd
import numpy as np
from typing import Dict, Any, List, Optional
import logging

logger = logging.getLogger(__name__)


def json_a_dataframe(respuesta_json: Dict[str, Any]) -> pd.DataFrame:
    """
    Convierte la respuesta paginada del microservicio Java en un DataFrame.
    Maneja el campo 'content' del objeto Page<TransaccionDTO> de Spring.

    Parámetros
    ----------
    respuesta_json : dict
        Respuesta cruda de la API Java (objeto Page<TransaccionDTO>).

    Retorna
    -------
    pd.DataFrame con columnas normalizadas en español.
    """
    contenido = respuesta_json.get("content", [])
    if not contenido:
        logger.warning("[DATOS] La respuesta no contiene transacciones.")
        return _dataframe_vacio()

    df = pd.DataFrame(contenido)

    # ── Renombrar columnas a español ─────────────────────────────────────────
    mapeo_columnas = {
        "id": "id",
        "usuarioId": "usuario_id",
        "nombreCliente": "nombre_cliente",
        "monto": "monto",
        "tipo": "tipo",
        "categoriaId": "categoria_id",
        "categoriaNombre": "categoria_nombre",
        "categoriaIcono": "categoria_icono",
        "fechaTransaccion": "fecha_transaccion",
        "metodoPago": "metodo_pago",
        "etiquetas": "etiquetas",
        "notas": "notas",
        "fechaRegistro": "fecha_registro",
    }
    df = df.rename(columns={k: v for k, v in mapeo_columnas.items() if k in df.columns})

    # ── Conversión de tipos ───────────────────────────────────────────────────
    if "monto" in df.columns:
        df["monto"] = pd.to_numeric(df["monto"], errors="coerce").fillna(0.0)

    for col_fecha in ["fecha_transaccion", "fecha_registro"]:
        if col_fecha in df.columns:
            df[col_fecha] = pd.to_datetime(df[col_fecha], errors="coerce")

    # ── Columnas derivadas útiles para análisis ───────────────────────────────
    if "fecha_transaccion" in df.columns:
        df["anio"] = df["fecha_transaccion"].dt.year
        df["mes"] = df["fecha_transaccion"].dt.month
        df["dia"] = df["fecha_transaccion"].dt.day
        df["dia_semana"] = df["fecha_transaccion"].dt.day_name()
        df["semana_anio"] = df["fecha_transaccion"].dt.isocalendar().week.astype(int)

    logger.info(
        "[DATOS] DataFrame creado: %d filas, %d columnas",
        len(df),
        len(df.columns),
    )
    return df


def filtrar_por_tipo(df: pd.DataFrame, tipo: str) -> pd.DataFrame:
    """Filtra el DataFrame por tipo de movimiento: 'INGRESO' o 'GASTO'."""
    if df.empty or "tipo" not in df.columns:
        return df
    return df[df["tipo"] == tipo].copy()


def agrupar_por_mes(df: pd.DataFrame, columna_valor: str = "monto") -> pd.DataFrame:
    """
    Agrupa transacciones por año-mes y suma los montos.
    Útil para análisis de estacionalidad y predicciones.
    """
    if df.empty:
        return pd.DataFrame()
    return (
        df.groupby(["anio", "mes"])[columna_valor]
        .sum()
        .reset_index()
        .rename(columns={columna_valor: "monto_total"})
        .sort_values(["anio", "mes"])
    )


def agrupar_por_categoria(df: pd.DataFrame) -> pd.DataFrame:
    """Agrupa por categoría y calcula totales."""
    if df.empty or "categoria_nombre" not in df.columns:
        return pd.DataFrame()
    return (
        df.groupby("categoria_nombre")["monto"]
        .agg(["sum", "count", "mean"])
        .reset_index()
        .rename(columns={"sum": "monto_total", "count": "cantidad", "mean": "promedio"})
        .sort_values("monto_total", ascending=False)
    )


def _dataframe_vacio() -> pd.DataFrame:
    """Retorna un DataFrame vacío con el esquema esperado."""
    return pd.DataFrame(
        columns=[
            "id", "usuario_id", "nombre_cliente", "monto", "tipo",
            "categoria_id", "categoria_nombre", "categoria_icono",
            "fecha_transaccion", "metodo_pago", "etiquetas", "notas",
            "fecha_registro", "anio", "mes", "dia", "dia_semana", "semana_anio",
        ]
    )


def calcular_estadisticas_basicas(df: pd.DataFrame) -> Dict[str, float]:
    """Calcula estadísticas descriptivas rápidas del DataFrame."""
    if df.empty:
        return {}

    gastos = filtrar_por_tipo(df, "GASTO")
    ingresos = filtrar_por_tipo(df, "INGRESO")

    return {
        "total_transacciones": len(df),
        "total_ingresos": float(ingresos["monto"].sum()) if not ingresos.empty else 0.0,
        "total_gastos": float(gastos["monto"].sum()) if not gastos.empty else 0.0,
        "balance": float(ingresos["monto"].sum() - gastos["monto"].sum()) if not df.empty else 0.0,
        "promedio_ingreso": float(ingresos["monto"].mean()) if not ingresos.empty else 0.0,
        "promedio_gasto": float(gastos["monto"].mean()) if not gastos.empty else 0.0,
        "monto_maximo": float(df["monto"].max()) if not df.empty else 0.0,
        "monto_minimo": float(df["monto"].min()) if not df.empty else 0.0,
    }