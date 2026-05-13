"""
utilidades/preparador_datos.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Capa de transformación: convierte el JSON crudo del microservicio Java
en DataFrames de Pandas limpios y tipados, listos para el Motor Analítico.

Responsabilidades (y solo estas):
  1. Deserializar la respuesta Page<TransaccionDTO> de Spring.
  2. Renombrar columnas Java (camelCase) → español (snake_case).
  3. Forzar tipos de datos correctos (float, datetime, str).
  4. Agregar columnas derivadas útiles (anio, mes, dia, dia_semana).
  5. Proveer funciones de agregación reutilizables por los módulos.
  6. Construir ResumenMensual para cada mes del historial.

NO hace análisis estadístico ni llama a servicios externos.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd

from app.modelos.esquemas import ResumenMensual, TipoMovimiento

logger = logging.getLogger(__name__)


# ── Mapeo canónico: campo Java → nombre en español ───────────────────────────
# Si el microservicio Java cambia algún campo, solo hay que actualizarlo aquí.
MAPA_COLUMNAS: Dict[str, str] = {
    "id":                "id",
    "usuarioId":         "usuario_id",
    "nombreCliente":     "nombre_cliente",
    "monto":             "monto",
    "tipo":              "tipo",
    "categoriaId":       "categoria_id",
    "categoriaNombre":   "categoria_nombre",
    "categoriaIcono":    "categoria_icono",
    "fechaTransaccion":  "fecha_transaccion",
    "metodoPago":        "metodo_pago",
    "etiquetas":         "etiquetas",
    "notas":             "notas",
    "fechaRegistro":     "fecha_registro",
}

# Columnas garantizadas en el DataFrame de salida (sin las derivadas)
COLUMNAS_BASE: List[str] = list(MAPA_COLUMNAS.values())

# Columnas derivadas que se añaden después de parsear fechas
COLUMNAS_DERIVADAS: List[str] = [
    "anio", "mes", "dia", "dia_semana", "semana_anio",
]

TODAS_LAS_COLUMNAS: List[str] = COLUMNAS_BASE + COLUMNAS_DERIVADAS


# ══════════════════════════════════════════════════════════════════════════════
# FUNCIÓN PRINCIPAL DE CONVERSIÓN
# ══════════════════════════════════════════════════════════════════════════════

def json_a_dataframe(respuesta_json: Dict) -> pd.DataFrame:
    """
    Convierte la respuesta paginada de Spring Boot en un DataFrame limpio.

    El microservicio-nucleo-financiero retorna un objeto Page<TransaccionDTO>:
        {
          "content": [ {...}, {...} ],
          "totalElements": 45,
          "totalPages": 1,
          ...
        }

    Parámetros
    ----------
    respuesta_json : dict
        Respuesta cruda del endpoint GET /api/v1/financiero/transacciones/historial.

    Retorna
    -------
    pd.DataFrame con columnas normalizadas. Vacío con esquema completo si no hay datos.
    """
    contenido: List[Dict] = respuesta_json.get("content", [])

    if not contenido:
        logger.warning(
            "[PREPARADOR] La respuesta Java no contiene transacciones (content vacío)."
        )
        return _dataframe_vacio()

    df = pd.DataFrame(contenido)

    # ── 1. Renombrar columnas Java → español ──────────────────────────────────
    df = df.rename(
        columns={java: esp for java, esp in MAPA_COLUMNAS.items() if java in df.columns}
    )

    # ── 2. Agregar columnas que falten con valor nulo ─────────────────────────
    for columna in COLUMNAS_BASE:
        if columna not in df.columns:
            df[columna] = np.nan
            logger.debug("[PREPARADOR] Columna '%s' no encontrada en respuesta Java; se agregó como NaN.", columna)

    # ── 3. Forzar tipos correctos ─────────────────────────────────────────────
    df["monto"] = pd.to_numeric(df["monto"], errors="coerce").fillna(0.0)
    df["tipo"] = df["tipo"].astype(str).str.upper().str.strip()
    df["categoria_nombre"] = df["categoria_nombre"].fillna("Sin categoría").astype(str)
    df["usuario_id"] = df["usuario_id"].astype(str)

    for columna_fecha in ("fecha_transaccion", "fecha_registro"):
        if columna_fecha in df.columns:
            df[columna_fecha] = pd.to_datetime(df[columna_fecha], errors="coerce")

    # ── 4. Columnas derivadas de fecha ────────────────────────────────────────
    if "fecha_transaccion" in df.columns and not df["fecha_transaccion"].isna().all():
        df["anio"] = df["fecha_transaccion"].dt.year.astype("Int64")
        df["mes"] = df["fecha_transaccion"].dt.month.astype("Int64")
        df["dia"] = df["fecha_transaccion"].dt.day.astype("Int64")
        df["dia_semana"] = df["fecha_transaccion"].dt.day_name()
        df["semana_anio"] = (
            df["fecha_transaccion"].dt.isocalendar().week.astype("Int64")
        )
    else:
        for col in COLUMNAS_DERIVADAS:
            df[col] = np.nan

    # ── 5. Eliminar filas con monto nulo o negativo ───────────────────────────
    filas_invalidas = df["monto"] <= 0
    if filas_invalidas.any():
        logger.warning(
            "[PREPARADOR] Se descartaron %d transacciones con monto <= 0.",
            filas_invalidas.sum(),
        )
        df = df[~filas_invalidas].copy()

    logger.info(
        "[PREPARADOR] DataFrame construido: %d filas × %d columnas.",
        len(df),
        len(df.columns),
    )
    return df.reset_index(drop=True)


# ══════════════════════════════════════════════════════════════════════════════
# FUNCIONES DE FILTRADO
# ══════════════════════════════════════════════════════════════════════════════

def filtrar_por_tipo(df: pd.DataFrame, tipo: TipoMovimiento) -> pd.DataFrame:
    """
    Filtra el DataFrame dejando solo transacciones del tipo indicado.

    Parámetros
    ----------
    df   : DataFrame normalizado.
    tipo : TipoMovimiento.GASTO o TipoMovimiento.INGRESO.

    Retorna
    -------
    Copia filtrada del DataFrame.
    """
    if df.empty or "tipo" not in df.columns:
        return df.copy()
    return df[df["tipo"] == tipo.value].copy()


def filtrar_por_mes_anio(
    df: pd.DataFrame,
    mes: Optional[int] = None,
    anio: Optional[int] = None,
) -> pd.DataFrame:
    """
    Aplica filtros de mes y/o año al DataFrame.
    Si ambos son None, retorna el DataFrame completo.
    """
    resultado = df.copy()
    if anio is not None and "anio" in resultado.columns:
        resultado = resultado[resultado["anio"] == anio]
    if mes is not None and "mes" in resultado.columns:
        resultado = resultado[resultado["mes"] == mes]
    return resultado


def filtrar_por_categoria(df: pd.DataFrame, categoria: str) -> pd.DataFrame:
    """Filtra por nombre de categoría (insensible a mayúsculas)."""
    if df.empty or "categoria_nombre" not in df.columns:
        return df.copy()
    return df[
        df["categoria_nombre"].str.lower() == categoria.lower()
    ].copy()


# ══════════════════════════════════════════════════════════════════════════════
# FUNCIONES DE AGREGACIÓN
# ══════════════════════════════════════════════════════════════════════════════

def agrupar_por_mes(df: pd.DataFrame, tipo: Optional[TipoMovimiento] = None) -> pd.DataFrame:
    """
    Agrupa transacciones por año-mes y suma los montos.
    Fundamental para análisis estadístico y predicción.

    Parámetros
    ----------
    df   : DataFrame normalizado.
    tipo : Si se especifica, filtra antes de agrupar.

    Retorna
    -------
    DataFrame con columnas [anio, mes, monto_total] ordenado cronológicamente.
    """
    if df.empty:
        return pd.DataFrame(columns=["anio", "mes", "monto_total"])

    datos = filtrar_por_tipo(df, tipo) if tipo else df.copy()
    if datos.empty:
        return pd.DataFrame(columns=["anio", "mes", "monto_total"])

    return (
        datos.groupby(["anio", "mes"])["monto"]
        .sum()
        .reset_index()
        .rename(columns={"monto": "monto_total"})
        .sort_values(["anio", "mes"])
        .reset_index(drop=True)
    )


def agrupar_por_categoria(
    df: pd.DataFrame,
    tipo: Optional[TipoMovimiento] = None,
) -> pd.DataFrame:
    """
    Agrupa por categoría y calcula suma, cantidad y promedio.
    Útil para los módulos de gastos hormiga y reporte completo.

    Retorna
    -------
    DataFrame con columnas [categoria_nombre, monto_total, cantidad, promedio]
    ordenado por monto_total descendente.
    """
    if df.empty or "categoria_nombre" not in df.columns:
        return pd.DataFrame(columns=["categoria_nombre", "monto_total", "cantidad", "promedio"])

    datos = filtrar_por_tipo(df, tipo) if tipo else df.copy()
    if datos.empty:
        return pd.DataFrame(columns=["categoria_nombre", "monto_total", "cantidad", "promedio"])

    return (
        datos.groupby("categoria_nombre")["monto"]
        .agg(monto_total="sum", cantidad="count", promedio="mean")
        .reset_index()
        .sort_values("monto_total", ascending=False)
        .reset_index(drop=True)
    )


def calcular_estadisticas_basicas(df: pd.DataFrame) -> Dict[str, float]:
    """
    Calcula métricas financieras descriptivas del DataFrame completo.

    Retorna
    -------
    Diccionario con todas las métricas. Si el DataFrame está vacío, retorna ceros.
    """
    if df.empty:
        return {
            "total_transacciones": 0,
            "total_ingresos": 0.0,
            "total_gastos": 0.0,
            "balance_neto": 0.0,
            "promedio_ingreso": 0.0,
            "promedio_gasto": 0.0,
            "monto_maximo": 0.0,
            "monto_minimo": 0.0,
        }

    gastos = filtrar_por_tipo(df, TipoMovimiento.GASTO)
    ingresos = filtrar_por_tipo(df, TipoMovimiento.INGRESO)

    total_ingresos = float(ingresos["monto"].sum()) if not ingresos.empty else 0.0
    total_gastos = float(gastos["monto"].sum()) if not gastos.empty else 0.0

    return {
        "total_transacciones": len(df),
        "total_ingresos": round(total_ingresos, 2),
        "total_gastos": round(total_gastos, 2),
        "balance_neto": round(total_ingresos - total_gastos, 2),
        "promedio_ingreso": round(float(ingresos["monto"].mean()), 2) if not ingresos.empty else 0.0,
        "promedio_gasto": round(float(gastos["monto"].mean()), 2) if not gastos.empty else 0.0,
        "monto_maximo": round(float(df["monto"].max()), 2),
        "monto_minimo": round(float(df["monto"].min()), 2),
    }


# ══════════════════════════════════════════════════════════════════════════════
# CONSTRUCCIÓN DE RESÚMENES MENSUALES
# ══════════════════════════════════════════════════════════════════════════════

def construir_resumenes_mensuales(df: pd.DataFrame) -> List[ResumenMensual]:
    """
    Convierte el DataFrame en una lista de ResumenMensual ordenada cronológicamente.
    Cada ResumenMensual incluye totales y desglose por categoría para ese mes.

    Usado por el Motor Analítico para análisis de tendencias y comparación.

    Parámetros
    ----------
    df : DataFrame normalizado por json_a_dataframe().

    Retorna
    -------
    Lista de ResumenMensual, ordenada de más antiguo a más reciente.
    """
    if df.empty or "anio" not in df.columns or "mes" not in df.columns:
        return []

    resumenes: List[ResumenMensual] = []

    # Obtenemos los períodos únicos presentes en el DataFrame
    periodos: pd.DataFrame = (
        df[["anio", "mes"]]
        .dropna()
        .drop_duplicates()
        .sort_values(["anio", "mes"])
    )

    for _, fila in periodos.iterrows():
        anio = int(fila["anio"])
        mes = int(fila["mes"])

        df_mes = df[(df["anio"] == anio) & (df["mes"] == mes)]

        gastos_mes = filtrar_por_tipo(df_mes, TipoMovimiento.GASTO)
        ingresos_mes = filtrar_por_tipo(df_mes, TipoMovimiento.INGRESO)

        # Desglose de gastos por categoría para este mes
        gastos_categoria: Dict[str, float] = {}
        if not gastos_mes.empty:
            gastos_categoria = (
                gastos_mes.groupby("categoria_nombre")["monto"]
                .sum()
                .round(2)
                .to_dict()
            )

        resumen = ResumenMensual(
            anio=anio,
            mes=mes,
            total_ingresos=round(float(ingresos_mes["monto"].sum()), 2) if not ingresos_mes.empty else 0.0,
            total_gastos=round(float(gastos_mes["monto"].sum()), 2) if not gastos_mes.empty else 0.0,
            gastos_por_categoria=gastos_categoria,
            cantidad_transacciones=len(df_mes),
        )
        resumenes.append(resumen)

    logger.info(
        "[PREPARADOR] Resúmenes mensuales construidos: %d meses.",
        len(resumenes),
    )
    return resumenes


def obtener_periodo_texto(df: pd.DataFrame) -> str:
    """
    Genera una cadena legible del período cubierto por el DataFrame.
    Ejemplo: 'Noviembre 2025 - Abril 2026'
    """
    if df.empty or "fecha_transaccion" not in df.columns:
        return "Período desconocido"

    fechas_validas = df["fecha_transaccion"].dropna()
    if fechas_validas.empty:
        return "Período desconocido"

    MESES_ES = {
        1: "Enero", 2: "Febrero", 3: "Marzo", 4: "Abril",
        5: "Mayo", 6: "Junio", 7: "Julio", 8: "Agosto",
        9: "Septiembre", 10: "Octubre", 11: "Noviembre", 12: "Diciembre",
    }

    fecha_min: pd.Timestamp = fechas_validas.min()
    fecha_max: pd.Timestamp = fechas_validas.max()

    inicio = f"{MESES_ES[fecha_min.month]} {fecha_min.year}"
    fin = f"{MESES_ES[fecha_max.month]} {fecha_max.year}"

    return inicio if inicio == fin else f"{inicio} - {fin}"


# ══════════════════════════════════════════════════════════════════════════════
# UTILIDADES INTERNAS
# ══════════════════════════════════════════════════════════════════════════════

def _dataframe_vacio() -> pd.DataFrame:
    """
    Retorna un DataFrame vacío con el esquema completo esperado.
    Evita errores de KeyError en los módulos cuando no hay transacciones.
    """
    return pd.DataFrame(columns=TODAS_LAS_COLUMNAS)


def validar_datos_suficientes(
    df: pd.DataFrame,
    minimo_filas: int = 1,
    tipo: Optional[TipoMovimiento] = None,
) -> Tuple[bool, str]:
    """
    Valida si el DataFrame tiene suficientes datos para un análisis.

    Parámetros
    ----------
    df           : DataFrame a validar.
    minimo_filas : Mínimo de filas necesarias.
    tipo         : Si se especifica, valida solo ese tipo de movimiento.

    Retorna
    -------
    (True, "") si hay datos suficientes.
    (False, "mensaje de error") si no los hay.
    """
    if df.empty:
        return False, "No se encontraron transacciones para el período solicitado."

    datos = filtrar_por_tipo(df, tipo) if tipo else df

    if len(datos) < minimo_filas:
        tipo_texto = f" de tipo {tipo.value}" if tipo else ""
        return (
            False,
            f"Se necesitan al menos {minimo_filas} transacciones{tipo_texto} "
            f"para este análisis. Solo se encontraron {len(datos)}.",
        )

    return True, ""