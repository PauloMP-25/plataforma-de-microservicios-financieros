"""
servicios/motor_ia.py
═══════════════════════════════════════════════════════════════
Motor principal de Inteligencia Artificial Financiera.
Contiene los 10 módulos de análisis inteligente.
Usa Pandas para manipulación de datos y Scikit-Learn para modelos.
═══════════════════════════════════════════════════════════════
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional, Tuple
import logging
import calendar

from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler

from app.configuracion import obtener_configuracion
from app.utilidades.preparador_datos import (
    filtrar_por_tipo,
    agrupar_por_mes,
    agrupar_por_categoria,
    calcular_estadisticas_basicas,
)

logger = logging.getLogger(__name__)
config = obtener_configuracion()


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 1: CLASIFICAR TRANSACCIÓN AUTOMÁTICA
# ══════════════════════════════════════════════════════════════════════════════

def clasificar_transaccion_automatica(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 1: Etiqueta automáticamente las transacciones usando reglas semánticas
    y análisis de patrones en nombre_cliente, notas y categoría.

    Lógica: Combina reglas difusas (keyword matching con pesos) para asignar
    sub-etiquetas de comportamiento de consumo: 'necesidad', 'capricho',
    'inversión', 'recurrente', 'estacional'.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones del usuario.

    Retorna
    -------
    dict con las clasificaciones y métricas de precisión.
    """
    if df.empty:
        return {"error": "No hay transacciones para clasificar", "transacciones_clasificadas": 0}

    # ── Diccionarios de palabras clave por categoría de comportamiento ────────
    palabras_necesidad = [
        "supermercado", "farmacia", "salud", "alquiler", "hipoteca",
        "servicios", "agua", "luz", "gas", "internet", "alimentación",
        "transporte", "medic", "doctor", "hospital", "colegio"
    ]
    palabras_capricho = [
        "netflix", "spotify", "cine", "restaurante", "ropa", "juego",
        "viaje", "hotel", "bar", "entretenimiento", "amazon", "tienda"
    ]
    palabras_inversion = [
        "curso", "libro", "educación", "inversión", "acción", "cripto",
        "ahorro", "fondo", "freelance", "negocio", "capacitación"
    ]
    palabras_recurrente = [
        "suscripción", "mensual", "cuota", "servicio", "factura",
        "pago automático", "débito", "cargo fijo"
    ]

    def _clasificar_fila(fila: pd.Series) -> Tuple[str, float]:
        """Clasifica una transacción individual con un puntaje de confianza."""
        texto = " ".join(filter(None, [
            str(fila.get("nombre_cliente", "")),
            str(fila.get("categoria_nombre", "")),
            str(fila.get("notas", "")),
            str(fila.get("etiquetas", "")),
        ])).lower()

        puntajes = {
            "necesidad": sum(1 for w in palabras_necesidad if w in texto),
            "capricho": sum(1 for w in palabras_capricho if w in texto),
            "inversión": sum(1 for w in palabras_inversion if w in texto),
            "recurrente": sum(1 for w in palabras_recurrente if w in texto),
        }

        # Si es INGRESO, la clasificación es diferente
        if fila.get("tipo") == "INGRESO":
            return "ingreso_activo", 0.9

        mejor_categoria = max(puntajes, key=lambda k: puntajes[k])
        max_puntaje = puntajes[mejor_categoria]

        if max_puntaje == 0:
            # Sin palabras clave: clasificar por monto y categoría
            monto = fila.get("monto", 0)
            if monto > 500:
                return "necesidad", 0.6
            elif monto < 30:
                return "capricho", 0.55
            else:
                return "necesidad", 0.5
        
        confianza = min(0.5 + (max_puntaje * 0.15), 0.95)
        return mejor_categoria, round(confianza, 2)

    # ── Aplicar clasificación a todo el DataFrame ─────────────────────────────
    resultados = df.apply(_clasificar_fila, axis=1)
    df_resultado = df.copy()
    df_resultado["etiqueta_ia"] = resultados.apply(lambda x: x[0])
    df_resultado["confianza_ia"] = resultados.apply(lambda x: x[1])

    # ── Estadísticas de clasificación ────────────────────────────────────────
    distribucion = df_resultado["etiqueta_ia"].value_counts().to_dict()
    precision_promedio = float(df_resultado["confianza_ia"].mean())

    # ── Top 5 transacciones clasificadas para la respuesta ───────────────────
    muestra = df_resultado.head(10)[
        ["nombre_cliente", "monto", "tipo", "categoria_nombre", "etiqueta_ia", "confianza_ia"]
    ].fillna("").to_dict(orient="records")

    return {
        "total_transacciones": len(df),
        "transacciones_clasificadas": len(df_resultado),
        "categorias_detectadas": distribucion,
        "etiquetas_asignadas": muestra,
        "precision_estimada": round(precision_promedio, 3),
        "mensaje": f"Se etiquetaron {len(df_resultado)} transacciones con una confianza promedio de {precision_promedio:.0%}.",
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2: PREDECIR GASTOS DEL PRÓXIMO MES
# ══════════════════════════════════════════════════════════════════════════════

def predecir_gastos_proximo_mes(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 2: Predice ingresos y gastos del próximo mes usando regresión lineal
    y media móvil ponderada. Selecciona el mejor método según la cantidad
    de datos disponibles.

    Parámetros
    ----------
    df : pd.DataFrame con historial de transacciones.

    Retorna
    -------
    dict con la predicción y el método utilizado.
    """
    if df.empty:
        return {"error": "Sin datos para predecir"}

    gastos_df = filtrar_por_tipo(df, "GASTO")
    ingresos_df = filtrar_por_tipo(df, "INGRESO")

    historico_gastos = agrupar_por_mes(gastos_df).tail(config.meses_historial_prediccion + 3)
    historico_ingresos = agrupar_por_mes(ingresos_df).tail(config.meses_historial_prediccion + 3)

    def _predecir_serie(serie_mensual: pd.DataFrame) -> Tuple[float, str]:
        """Predice el siguiente valor de una serie temporal mensual."""
        if serie_mensual.empty:
            return 0.0, "sin_datos"

        montos = serie_mensual["monto_total"].values

        if len(montos) < 2:
            return float(montos[0]), "valor_unico"

        if len(montos) >= 4:
            # Regresión lineal para detectar tendencia
            X = np.arange(len(montos)).reshape(-1, 1)
            y = montos
            modelo = LinearRegression()
            modelo.fit(X, y)
            prediccion = float(modelo.predict([[len(montos)]])[0])
            return max(prediccion, 0.0), "regresion_lineal"
        else:
            # Media móvil ponderada (más peso a datos recientes)
            pesos = np.array([1, 2, 3][: len(montos)])
            prediccion = float(np.average(montos[-len(pesos) :], weights=pesos))
            return max(prediccion, 0.0), "media_movil_ponderada"

    gasto_predicho, metodo_gasto = _predecir_serie(historico_gastos)
    ingreso_predicho, metodo_ingreso = _predecir_serie(historico_ingresos)

    # ── Mes siguiente ────────────────────────────────────────────────────────
    hoy = datetime.now()
    if hoy.month == 12:
        mes_siguiente = 1
        anio_siguiente = hoy.year + 1
    else:
        mes_siguiente = hoy.month + 1
        anio_siguiente = hoy.year

    nombre_mes = calendar.month_name[mes_siguiente]
    balance_predicho = ingreso_predicho - gasto_predicho

    # ── Histórico para el gráfico ────────────────────────────────────────────
    historico_combinado = []
    for _, fila in historico_gastos.iterrows():
        historico_combinado.append({
            "periodo": f"{int(fila['anio'])}-{int(fila['mes']):02d}",
            "gastos": round(float(fila["monto_total"]), 2),
            "ingresos": 0.0,
        })

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "mes_predicho": f"{nombre_mes} {anio_siguiente}",
        "gasto_predicho": round(gasto_predicho, 2),
        "ingreso_predicho": round(ingreso_predicho, 2),
        "balance_predicho": round(balance_predicho, 2),
        "metodo_utilizado": metodo_gasto,
        "confianza": "alta" if len(historico_gastos) >= 4 else "media" if len(historico_gastos) >= 2 else "baja",
        "historico_meses": historico_combinado[-6:],
        "mensaje": (
            f"Proyección para {nombre_mes}: gastos ~S/ {gasto_predicho:,.2f}, "
            f"ingresos ~S/ {ingreso_predicho:,.2f}. "
            f"Balance estimado: S/ {balance_predicho:,.2f}."
        ),
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 3: DETECTAR ANOMALÍAS FINANCIERAS
# ══════════════════════════════════════════════════════════════════════════════

def detectar_anomalias_financieras(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 3: Detecta transacciones anómalas usando el método de Z-Score.
    Una transacción es anómala si su monto supera N desviaciones estándar
    respecto a la media de su categoría.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.

    Retorna
    -------
    dict con lista de anomalías y monto en riesgo.
    """
    if df.empty or len(df) < 3:
        return {
            "anomalias_detectadas": 0,
            "mensaje": "Se necesitan al menos 3 transacciones para detectar anomalías.",
        }

    umbral = config.umbral_anomalia
    anomalias = []
    monto_en_riesgo = 0.0

    # ── Detectar anomalías por categoría ─────────────────────────────────────
    for categoria in df["categoria_nombre"].dropna().unique():
        grupo = df[df["categoria_nombre"] == categoria]["monto"]
        if len(grupo) < 2:
            continue

        media = grupo.mean()
        desviacion = grupo.std()
        if desviacion == 0:
            continue

        z_scores = (grupo - media) / desviacion
        indices_anomalos = z_scores[z_scores.abs() > umbral].index

        for idx in indices_anomalos:
            fila = df.loc[idx]
            monto_en_riesgo += float(fila["monto"])
            anomalias.append({
                "id": str(fila.get("id", "")),
                "fecha": str(fila["fecha_transaccion"])[:10] if pd.notna(fila.get("fecha_transaccion")) else "N/A",
                "categoria": categoria,
                "nombre_cliente": str(fila.get("nombre_cliente", "")),
                "monto": round(float(fila["monto"]), 2),
                "monto_promedio_categoria": round(float(media), 2),
                "z_score": round(float(z_scores[idx]), 2),
                "tipo": str(fila.get("tipo", "")),
                "nivel_riesgo": "alto" if abs(z_scores[idx]) > 3 else "medio",
            })

    # ── Ordenar por z-score descendente ──────────────────────────────────────
    anomalias.sort(key=lambda x: abs(x["z_score"]), reverse=True)

    mensaje = (
        f"Se detectaron {len(anomalias)} transacciones inusuales "
        f"(umbral: {umbral}σ). Monto total en revisión: S/ {monto_en_riesgo:,.2f}."
        if anomalias
        else "No se detectaron anomalías. El patrón de gastos es consistente."
    )

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "total_transacciones_analizadas": len(df),
        "anomalias_detectadas": len(anomalias),
        "umbral_utilizado": umbral,
        "detalles_anomalias": anomalias[:10],  # Top 10
        "monto_en_riesgo": round(monto_en_riesgo, 2),
        "mensaje": mensaje,
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 4: OPTIMIZAR SUSCRIPCIONES (GASTOS HORMIGA)
# ══════════════════════════════════════════════════════════════════════════════

def optimizar_suscripciones(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 4: Identifica gastos recurrentes de bajo monto (gastos hormiga)
    y suscripciones que podrían optimizarse.
    Detecta patrones de repetición en nombre_cliente y monto similar.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.

    Retorna
    -------
    dict con lista de suscripciones detectadas y ahorro potencial.
    """
    if df.empty:
        return {"suscripciones_detectadas": [], "ahorro_potencial": 0.0}

    gastos = filtrar_por_tipo(df, "GASTO")
    if gastos.empty:
        return {"suscripciones_detectadas": [], "ahorro_potencial": 0.0}

    # ── Detectar gastos hormiga: recurrentes de monto bajo ───────────────────
    palabras_suscripcion = [
        "netflix", "spotify", "disney", "amazon prime", "youtube premium",
        "hbo", "apple", "microsoft", "google", "dropbox", "canva",
        "chatgpt", "openai", "adobe", "zoom", "slack"
    ]
    categorias_suscripcion = ["suscripciones streaming", "tecnología", "entretenimiento"]

    def _es_suscripcion(fila: pd.Series) -> bool:
        texto = " ".join(filter(None, [
            str(fila.get("nombre_cliente", "")),
            str(fila.get("categoria_nombre", "")),
            str(fila.get("notas", "")),
        ])).lower()
        return (
            any(p in texto for p in palabras_suscripcion)
            or str(fila.get("categoria_nombre", "")).lower() in categorias_suscripcion
        )

    gastos["es_suscripcion"] = gastos.apply(_es_suscripcion, axis=1)

    # ── Detectar gastos hormiga por monto ────────────────────────────────────
    monto_umbral = config.monto_minimo_suscripcion
    gastos_hormiga = gastos[
        (gastos["monto"] < 50) & (gastos["monto"] > monto_umbral)
    ]

    # ── Agrupar por nombre para ver frecuencia ────────────────────────────────
    resumen_suscripciones = (
        gastos[gastos["es_suscripcion"]]
        .groupby("nombre_cliente")
        .agg(
            cantidad=("monto", "count"),
            monto_promedio=("monto", "mean"),
            monto_total=("monto", "sum"),
            categoria=("categoria_nombre", "first"),
        )
        .reset_index()
        .sort_values("monto_total", ascending=False)
    )

    lista_suscripciones = []
    for _, fila in resumen_suscripciones.iterrows():
        lista_suscripciones.append({
            "nombre": str(fila["nombre_cliente"]),
            "categoria": str(fila["categoria"]),
            "cantidad_cargos": int(fila["cantidad"]),
            "monto_promedio": round(float(fila["monto_promedio"]), 2),
            "monto_total": round(float(fila["monto_total"]), 2),
            "recomendacion": "Evaluar cancelación" if fila["monto_total"] > 50 else "Bajo impacto",
        })

    monto_total_suscripciones = float(resumen_suscripciones["monto_total"].sum()) if not resumen_suscripciones.empty else 0.0
    ahorro_potencial = monto_total_suscripciones * 0.30  # 30% de ahorro potencial

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "total_gastos_recurrentes": len(lista_suscripciones),
        "monto_total_mensual": round(monto_total_suscripciones, 2),
        "suscripciones_detectadas": lista_suscripciones,
        "ahorro_potencial": round(ahorro_potencial, 2),
        "recomendacion": (
            f"Revisando {len(lista_suscripciones)} suscripciones podrías ahorrar "
            f"hasta S/ {ahorro_potencial:,.2f} mensuales cancelando las que no usas."
            if lista_suscripciones
            else "No se detectaron suscripciones activas. ¡Excelente control de gastos fijos!"
        ),
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 5: CALCULAR CAPACIDAD DE AHORRO
# ══════════════════════════════════════════════════════════════════════════════

def calcular_capacidad_ahorro(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 5: Calcula la capacidad real de ahorro aplicando la fórmula:
    Capacidad Ahorro = (Ingresos - Gastos Fijos) × Factor de Seguridad

    Los gastos fijos se identifican como los que se repiten mensualmente.
    El factor de seguridad (0.85) crea un colchón para imprevistos.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.

    Retorna
    -------
    dict con el desglose del cálculo y clasificación.
    """
    if df.empty:
        return {"error": "Sin datos disponibles para calcular capacidad de ahorro."}

    estadisticas = calcular_estadisticas_basicas(df)
    total_ingresos = estadisticas.get("total_ingresos", 0.0)
    total_gastos = estadisticas.get("total_gastos", 0.0)

    # ── Identificar gastos fijos (recurrentes en múltiples meses) ────────────
    gastos = filtrar_por_tipo(df, "GASTO")
    gastos_fijos = 0.0

    if not gastos.empty and "mes" in gastos.columns:
        gastos_por_cliente_mes = (
            gastos.groupby(["nombre_cliente", "mes"])["monto"].sum().reset_index()
        )
        clientes_recurrentes = (
            gastos_por_cliente_mes.groupby("nombre_cliente")["mes"]
            .nunique()
            .reset_index(name="meses_activos")
        )
        # Si aparece en 2+ meses distintos, es recurrente/fijo
        fijos_mask = clientes_recurrentes["meses_activos"] >= 2
        clientes_fijos = clientes_recurrentes[fijos_mask]["nombre_cliente"].tolist()
        gastos_fijos = float(
            gastos[gastos["nombre_cliente"].isin(clientes_fijos)]["monto"].sum()
        )

    factor = config.factor_seguridad_ahorro
    margen_bruto = total_ingresos - total_gastos
    capacidad_ahorro = max((total_ingresos - gastos_fijos) * factor - (total_gastos - gastos_fijos), 0)
    porcentaje_ahorro = (capacidad_ahorro / total_ingresos * 100) if total_ingresos > 0 else 0

    # ── Clasificación según regla 50/30/20 ───────────────────────────────────
    if porcentaje_ahorro >= 20:
        clasificacion = "Excelente 🟢"
        recomendacion = "¡Excelente disciplina! Considera invertir el excedente en un fondo de inversión."
    elif porcentaje_ahorro >= 10:
        clasificacion = "Buena 🟡"
        recomendacion = "Buen avance. Intenta aumentar gradualmente hasta el 20% de ahorro mensual."
    elif porcentaje_ahorro >= 5:
        clasificacion = "Regular 🟠"
        recomendacion = "Revisa los gastos no esenciales para llegar al 10% de ahorro mínimo recomendado."
    else:
        clasificacion = "Crítica 🔴"
        recomendacion = "Ingresos insuficientes o gastos excesivos. Prioriza reducir deudas y gastos variables."

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "total_ingresos": round(total_ingresos, 2),
        "total_gastos": round(total_gastos, 2),
        "gastos_fijos": round(gastos_fijos, 2),
        "margen_bruto": round(margen_bruto, 2),
        "capacidad_ahorro": round(capacidad_ahorro, 2),
        "porcentaje_ahorro": round(porcentaje_ahorro, 1),
        "factor_seguridad_aplicado": factor,
        "clasificacion": clasificacion,
        "recomendacion": recomendacion,
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 6: SIMULAR METAS FINANCIERAS
# ══════════════════════════════════════════════════════════════════════════════

def simular_metas_financieras(
    df: pd.DataFrame,
    monto_meta: float,
    nombre_meta: str,
) -> Dict[str, Any]:
    """
    Módulo 6: Proyecta cuántos meses tardará el usuario en alcanzar una meta
    dado su ahorro mensual actual. Calcula escenarios optimista y pesimista.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.
    monto_meta : float — Monto objetivo de la meta (S/).
    nombre_meta : str — Nombre de la meta (ej: "Fondo de emergencia").

    Retorna
    -------
    dict con proyección de tiempo y escenarios.
    """
    if df.empty:
        return {"error": "Sin datos para simular la meta."}

    # Reutilizamos el módulo 5 para obtener la capacidad de ahorro
    datos_ahorro = calcular_capacidad_ahorro(df)
    capacidad_mensual = datos_ahorro.get("capacidad_ahorro", 0.0)

    if capacidad_mensual <= 0:
        return {
            "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
            "nombre_meta": nombre_meta,
            "monto_meta": monto_meta,
            "capacidad_ahorro_mensual": 0.0,
            "meses_para_alcanzar": -1,
            "fecha_estimada": "No disponible — aumenta tus ingresos o reduce gastos primero.",
            "escenario_optimista_meses": -1,
            "escenario_pesimista_meses": -1,
            "mensaje": "Con tu capacidad de ahorro actual (S/ 0), no es posible alcanzar la meta. Revisa tus gastos.",
        }

    meses_base = int(np.ceil(monto_meta / capacidad_mensual))
    meses_optimista = int(np.ceil(monto_meta / (capacidad_mensual * 1.25)))  # Ahorra 25% más
    meses_pesimista = int(np.ceil(monto_meta / (capacidad_mensual * 0.75)))  # Ahorra 25% menos

    hoy = datetime.now()
    fecha_estimada = hoy + timedelta(days=meses_base * 30)

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "nombre_meta": nombre_meta,
        "monto_meta": round(monto_meta, 2),
        "capacidad_ahorro_mensual": round(capacidad_mensual, 2),
        "meses_para_alcanzar": meses_base,
        "fecha_estimada": fecha_estimada.strftime("%B %Y"),
        "escenario_optimista_meses": meses_optimista,
        "escenario_pesimista_meses": meses_pesimista,
        "mensaje": (
            f"Para '{nombre_meta}' (S/ {monto_meta:,.2f}), ahorrando S/ {capacidad_mensual:,.2f}/mes "
            f"lo lograrás en aproximadamente {meses_base} meses ({fecha_estimada.strftime('%B %Y')}). "
            f"Escenario optimista: {meses_optimista} meses | Pesimista: {meses_pesimista} meses."
        ),
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 7: ANALIZAR ESTACIONALIDAD
# ══════════════════════════════════════════════════════════════════════════════

def analizar_estacionalidad(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 7: Detecta patrones estacionales agrupando transacciones por mes
    para identificar picos históricos de gasto e ingreso.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.

    Retorna
    -------
    dict con análisis de estacionalidad mensual.
    """
    if df.empty or "mes" not in df.columns:
        return {"error": "Sin suficientes datos para análisis de estacionalidad."}

    nombres_meses = {
        1: "Enero", 2: "Febrero", 3: "Marzo", 4: "Abril",
        5: "Mayo", 6: "Junio", 7: "Julio", 8: "Agosto",
        9: "Septiembre", 10: "Octubre", 11: "Noviembre", 12: "Diciembre"
    }

    gastos = filtrar_por_tipo(df, "GASTO")
    ingresos = filtrar_por_tipo(df, "INGRESO")

    resumen_mensual = []
    for mes_num in range(1, 13):
        gasto_mes = float(gastos[gastos["mes"] == mes_num]["monto"].sum()) if not gastos.empty else 0.0
        ingreso_mes = float(ingresos[ingresos["mes"] == mes_num]["monto"].sum()) if not ingresos.empty else 0.0
        cant_transacciones = len(df[df["mes"] == mes_num])

        resumen_mensual.append({
            "mes_numero": mes_num,
            "mes_nombre": nombres_meses[mes_num],
            "total_gastos": round(gasto_mes, 2),
            "total_ingresos": round(ingreso_mes, 2),
            "balance": round(ingreso_mes - gasto_mes, 2),
            "cantidad_transacciones": cant_transacciones,
        })

    # ── Identificar picos ────────────────────────────────────────────────────
    meses_con_datos = [m for m in resumen_mensual if m["cantidad_transacciones"] > 0]
    if not meses_con_datos:
        return {"error": "Sin datos mensuales suficientes."}

    mes_mayor_gasto = max(meses_con_datos, key=lambda x: x["total_gastos"])
    mes_menor_gasto = min(
        [m for m in meses_con_datos if m["total_gastos"] > 0],
        key=lambda x: x["total_gastos"],
        default=meses_con_datos[0],
    )
    mes_mayor_ingreso = max(meses_con_datos, key=lambda x: x["total_ingresos"])

    # ── Detectar patrón ───────────────────────────────────────────────────────
    gastos_mensuales = [m["total_gastos"] for m in meses_con_datos]
    coeficiente_variacion = (np.std(gastos_mensuales) / np.mean(gastos_mensuales)) if np.mean(gastos_mensuales) > 0 else 0
    if coeficiente_variacion < 0.2:
        patron = "Gastos muy estables — patrón regular y predecible"
    elif coeficiente_variacion < 0.5:
        patron = "Ligera variabilidad mensual — algunos meses pico identificados"
    else:
        patron = "Alta variabilidad — gastos irregulares con picos pronunciados"

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "mes_mayor_gasto": f"{mes_mayor_gasto['mes_nombre']} (S/ {mes_mayor_gasto['total_gastos']:,.2f})",
        "mes_mayor_ingreso": f"{mes_mayor_ingreso['mes_nombre']} (S/ {mes_mayor_ingreso['total_ingresos']:,.2f})",
        "mes_menor_gasto": f"{mes_menor_gasto['mes_nombre']} (S/ {mes_menor_gasto['total_gastos']:,.2f})",
        "picos_detectados": [
            m for m in resumen_mensual
            if m["total_gastos"] > np.mean([x["total_gastos"] for x in meses_con_datos]) * 1.3
        ],
        "patron_detectado": patron,
        "distribucion_mensual": resumen_mensual,
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 8: RECOMENDAR PRESUPUESTO DINÁMICO
# ══════════════════════════════════════════════════════════════════════════════

def recomendar_presupuesto_dinamico(df: pd.DataFrame) -> Dict[str, Any]:
    """
    Módulo 8: Calcula el presupuesto semanal dinámico basado en el gasto
    de la semana anterior y los límites por categoría.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.

    Retorna
    -------
    dict con el presupuesto recomendado y límites por categoría.
    """
    if df.empty:
        return {"error": "Sin datos para recomendar presupuesto."}

    gastos = filtrar_por_tipo(df, "GASTO")
    if gastos.empty:
        return {"error": "Sin gastos registrados para calcular presupuesto."}

    # ── Gasto de la semana anterior ───────────────────────────────────────────
    hoy = datetime.now()
    inicio_semana_anterior = hoy - timedelta(days=hoy.weekday() + 7)
    fin_semana_anterior = inicio_semana_anterior + timedelta(days=6)

    gastos_semana_anterior = gastos[
        (gastos["fecha_transaccion"] >= pd.Timestamp(inicio_semana_anterior))
        & (gastos["fecha_transaccion"] <= pd.Timestamp(fin_semana_anterior))
    ] if "fecha_transaccion" in gastos.columns else pd.DataFrame()

    gasto_semana_anterior = float(gastos_semana_anterior["monto"].sum()) if not gastos_semana_anterior.empty else float(gastos["monto"].mean() * 7)

    # ── Calcular presupuesto semanal ──────────────────────────────────────────
    datos_ahorro = calcular_capacidad_ahorro(df)
    ingreso_mensual = datos_ahorro.get("total_ingresos", 0)
    presupuesto_mensual = ingreso_mensual * 0.80  # 80% del ingreso para gastos
    presupuesto_semanal = presupuesto_mensual / 4.3  # Semanas promedio por mes
    limite_diario = presupuesto_semanal / 7

    # Ajuste dinámico: si gastó de más la semana pasada, reduce el presupuesto esta semana
    factor_ajuste = 1.0
    advertencias = []
    if gasto_semana_anterior > presupuesto_semanal * 1.2:
        factor_ajuste = 0.90
        advertencias.append(f"⚠️ La semana anterior superaste el presupuesto en S/ {gasto_semana_anterior - presupuesto_semanal:,.2f}. Presupuesto reducido 10%.")
    elif gasto_semana_anterior < presupuesto_semanal * 0.6:
        factor_ajuste = 1.10
        advertencias.append(f"✅ Ahorraste bien la semana pasada. Presupuesto aumentado 10%.")

    presupuesto_ajustado = presupuesto_semanal * factor_ajuste

    # ── Límites por categoría (basado en distribución histórica) ─────────────
    distribucion_categorias = agrupar_por_categoria(gastos)
    total_gastos_historico = float(distribucion_categorias["monto_total"].sum()) if not distribucion_categorias.empty else 1.0

    categorias_con_limite = []
    for _, cat in distribucion_categorias.head(5).iterrows():
        porcentaje = float(cat["monto_total"]) / total_gastos_historico
        limite_categoria = presupuesto_ajustado * porcentaje
        categorias_con_limite.append({
            "categoria": str(cat["categoria_nombre"]),
            "porcentaje_historico": round(porcentaje * 100, 1),
            "limite_semanal": round(limite_categoria, 2),
            "limite_diario": round(limite_categoria / 7, 2),
        })

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "gasto_semana_anterior": round(gasto_semana_anterior, 2),
        "presupuesto_semana_actual": round(presupuesto_ajustado, 2),
        "limite_diario_recomendado": round(limite_diario * factor_ajuste, 2),
        "categorias_con_limite": categorias_con_limite,
        "advertencias": advertencias,
        "mensaje": (
            f"Presupuesto semanal recomendado: S/ {presupuesto_ajustado:,.2f} "
            f"(S/ {limite_diario * factor_ajuste:,.2f}/día). "
            f"Basado en ingresos de S/ {ingreso_mensual:,.2f}/mes."
        ),
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 9: SIMULAR ESCENARIO "¿QUÉ PASARÍA SI?"
# ══════════════════════════════════════════════════════════════════════════════

def simular_escenario_que_pasaria_si(
    df: pd.DataFrame,
    nuevo_gasto_fijo: float = 0.0,
    nuevo_ingreso: float = 0.0,
) -> Dict[str, Any]:
    """
    Módulo 9: Simula el impacto de agregar un nuevo gasto fijo o ingreso
    sobre la capacidad de ahorro, presupuesto y metas financieras.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.
    nuevo_gasto_fijo : float — Nuevo gasto fijo mensual a simular.
    nuevo_ingreso : float — Nuevo ingreso mensual a simular.

    Retorna
    -------
    dict con comparación de escenario actual vs simulado.
    """
    if df.empty:
        return {"error": "Sin datos para la simulación."}

    estadisticas = calcular_estadisticas_basicas(df)
    datos_ahorro_actual = calcular_capacidad_ahorro(df)

    ingresos_actuales = estadisticas.get("total_ingresos", 0)
    gastos_actuales = estadisticas.get("total_gastos", 0)
    ahorro_actual = datos_ahorro_actual.get("capacidad_ahorro", 0)
    porcentaje_actual = datos_ahorro_actual.get("porcentaje_ahorro", 0)

    # ── Escenario simulado ───────────────────────────────────────────────────
    ingresos_simulados = ingresos_actuales + nuevo_ingreso
    gastos_simulados = gastos_actuales + nuevo_gasto_fijo
    margen_simulado = ingresos_simulados - gastos_simulados
    ahorro_simulado = max(margen_simulado * config.factor_seguridad_ahorro, 0)
    porcentaje_simulado = (ahorro_simulado / ingresos_simulados * 100) if ingresos_simulados > 0 else 0

    # ── Impacto ───────────────────────────────────────────────────────────────
    delta_ahorro = ahorro_simulado - ahorro_actual
    delta_porcentaje = porcentaje_simulado - porcentaje_actual

    if delta_ahorro > 0:
        viabilidad = "✅ Viable — mejora tu situación financiera"
        recomendacion = f"Este cambio aumenta tu ahorro mensual en S/ {delta_ahorro:,.2f}. ¡Adelante!"
    elif delta_ahorro > -100:
        viabilidad = "⚠️ Moderadamente viable — impacto leve"
        recomendacion = f"Reducirías tu ahorro en S/ {abs(delta_ahorro):,.2f}/mes. Evalúa si es necesario."
    else:
        viabilidad = "🔴 No recomendado — impacto significativo en tus ahorros"
        recomendacion = f"Este cambio reduciría tu ahorro en S/ {abs(delta_ahorro):,.2f}/mes. Busca alternativas."

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "escenario_actual": {
            "ingresos_mensuales": round(ingresos_actuales, 2),
            "gastos_mensuales": round(gastos_actuales, 2),
            "capacidad_ahorro": round(ahorro_actual, 2),
            "porcentaje_ahorro": round(porcentaje_actual, 1),
        },
        "escenario_simulado": {
            "ingresos_mensuales": round(ingresos_simulados, 2),
            "gastos_mensuales": round(gastos_simulados, 2),
            "capacidad_ahorro": round(ahorro_simulado, 2),
            "porcentaje_ahorro": round(porcentaje_simulado, 1),
        },
        "impacto": {
            "delta_ahorro_mensual": round(delta_ahorro, 2),
            "delta_porcentaje": round(delta_porcentaje, 1),
            "nuevo_gasto_fijo_ingresado": nuevo_gasto_fijo,
            "nuevo_ingreso_ingresado": nuevo_ingreso,
        },
        "viabilidad": viabilidad,
        "recomendacion": recomendacion,
    }


# ══════════════════════════════════════════════════════════════════════════════
# MÓDULO 10: GENERAR REPORTE EN LENGUAJE NATURAL
# ══════════════════════════════════════════════════════════════════════════════

def generar_reporte_lenguaje_natural(df: pd.DataFrame, periodo: str = "este período") -> Dict[str, Any]:
    """
    Módulo 10: Genera un resumen ejecutivo en texto natural basado en los
    KPIs calculados por los módulos anteriores. Funciona como un "CFO virtual"
    que explica la situación financiera del usuario de forma comprensible.

    Parámetros
    ----------
    df : pd.DataFrame con las transacciones.
    periodo : str — Descripción del período analizado.

    Retorna
    -------
    dict con el reporte narrativo completo.
    """
    if df.empty:
        return {"error": "Sin datos suficientes para generar el reporte."}

    # ── Recopilar KPIs de todos los módulos ───────────────────────────────────
    estadisticas = calcular_estadisticas_basicas(df)
    ahorro = calcular_capacidad_ahorro(df)
    anomalias = detectar_anomalias_financieras(df)
    suscripciones = optimizar_suscripciones(df)
    prediccion = predecir_gastos_proximo_mes(df)

    total_ingresos = estadisticas.get("total_ingresos", 0)
    total_gastos = estadisticas.get("total_gastos", 0)
    balance = estadisticas.get("balance", 0)
    capacidad_ahorro = ahorro.get("capacidad_ahorro", 0)
    porcentaje_ahorro = ahorro.get("porcentaje_ahorro", 0)
    num_anomalias = anomalias.get("anomalias_detectadas", 0)
    monto_riesgo = anomalias.get("monto_en_riesgo", 0)
    ahorro_potencial_suscripciones = suscripciones.get("ahorro_potencial", 0)
    gasto_predicho = prediccion.get("gasto_predicho", 0)

    # ── Puntaje de salud financiera (0-100) ────────────────────────────────────
    puntaje = 50.0
    if porcentaje_ahorro >= 20:
        puntaje += 25
    elif porcentaje_ahorro >= 10:
        puntaje += 15
    elif porcentaje_ahorro >= 5:
        puntaje += 5
    else:
        puntaje -= 10

    if balance > 0:
        puntaje += 15
    else:
        puntaje -= 15

    if num_anomalias == 0:
        puntaje += 10
    elif num_anomalias <= 2:
        puntaje += 5
    else:
        puntaje -= 10

    puntaje = max(0, min(100, puntaje))

    if puntaje >= 80:
        clasificacion_salud = "Excelente 🟢"
    elif puntaje >= 60:
        clasificacion_salud = "Buena 🟡"
    elif puntaje >= 40:
        clasificacion_salud = "Regular 🟠"
    else:
        clasificacion_salud = "Crítica 🔴"

    # ── Construir el texto del reporte ────────────────────────────────────────
    resumen = (
        f"Durante {periodo}, registraste {len(df)} transacciones con ingresos de "
        f"S/ {total_ingresos:,.2f} y gastos de S/ {total_gastos:,.2f}, "
        f"resultando en un {'superávit' if balance >= 0 else 'déficit'} de "
        f"S/ {abs(balance):,.2f}. "
        f"Tu capacidad de ahorro real es S/ {capacidad_ahorro:,.2f} mensuales "
        f"({porcentaje_ahorro:.1f}% de tus ingresos), lo cual se clasifica como "
        f"{ahorro.get('clasificacion', 'N/A')}. "
    )

    if num_anomalias > 0:
        resumen += (
            f"Se identificaron {num_anomalias} transacciones inusuales "
            f"por un total de S/ {monto_riesgo:,.2f} que merecen revisión. "
        )

    if ahorro_potencial_suscripciones > 0:
        resumen += (
            f"Optimizando suscripciones podrías liberar hasta "
            f"S/ {ahorro_potencial_suscripciones:,.2f} adicionales por mes. "
        )

    resumen += (
        f"Para el próximo mes se proyectan gastos de S/ {gasto_predicho:,.2f}. "
        f"Tu puntaje de salud financiera es {puntaje:.0f}/100 — {clasificacion_salud}."
    )

    # ── Alertas y recomendaciones ─────────────────────────────────────────────
    alertas = []
    recomendaciones = []

    if balance < 0:
        alertas.append(f"🔴 Déficit de S/ {abs(balance):,.2f} — gastas más de lo que ingresas")
    if num_anomalias > 3:
        alertas.append(f"⚠️ {num_anomalias} transacciones anómalas detectadas — revisión necesaria")
    if porcentaje_ahorro < 5:
        alertas.append("🔴 Capacidad de ahorro crítica — menos del 5% de tus ingresos")

    if porcentaje_ahorro < 10:
        recomendaciones.append("Aplica la regla 50/30/20: 50% necesidades, 30% deseos, 20% ahorro.")
    if ahorro_potencial_suscripciones > 30:
        recomendaciones.append(f"Cancela suscripciones no usadas: liberas S/ {ahorro_potencial_suscripciones:,.2f}/mes.")
    if gasto_predicho > total_ingresos * 0.9:
        recomendaciones.append("El próximo mes los gastos proyectados son muy altos. Planifica con anticipación.")
    recomendaciones.append("Registra TODAS tus transacciones para mejorar la precisión del análisis.")

    return {
        "usuario_id": str(df["usuario_id"].iloc[0]) if "usuario_id" in df.columns else "N/A",
        "fecha_generacion": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "periodo_analizado": periodo,
        "resumen_ejecutivo": resumen,
        "kpis": {
            "total_ingresos": round(total_ingresos, 2),
            "total_gastos": round(total_gastos, 2),
            "balance_neto": round(balance, 2),
            "capacidad_ahorro": round(capacidad_ahorro, 2),
            "porcentaje_ahorro": round(porcentaje_ahorro, 1),
            "total_transacciones": len(df),
            "anomalias_detectadas": num_anomalias,
            "ahorro_potencial_suscripciones": round(ahorro_potencial_suscripciones, 2),
            "gasto_proyectado_proximo_mes": round(gasto_predicho, 2),
        },
        "alertas": alertas,
        "recomendaciones": recomendaciones,
        "puntaje_salud_financiera": round(puntaje, 1),
        "clasificacion_salud": clasificacion_salud,
    }