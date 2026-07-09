import calendar
from datetime import datetime
import json
import logging
from typing import Optional, List, Dict, Any
import pandas as pd

from fastapi import APIRouter, Request, Depends, Security
from pydantic import BaseModel, Field

from app.libreria_comun.seguridad.validador_jwt import validar_token, obtener_usuario_id
from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.clientes.luka_clients import obtener_cliente_financiero, obtener_cliente_perfil
from app.persistencia.redis.cache_redis import CacheRedis
from app.utilidades.preparador_datos import json_a_dataframe

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ia/dashboard", tags=["Dashboard KPIs — LUKA"])

# ── Schemas de respuesta ──────────────────────────────────────────────────────

class ResumenKPIs(BaseModel):
    desde: str
    hasta: str
    total_ingresos: float = Field(..., alias="totalIngresos")
    total_gastos: float = Field(..., alias="totalGastos")
    balance: float
    cantidad_ingresos: int = Field(..., alias="cantidadIngresos")
    cantidad_gastos: int = Field(..., alias="cantidadGastos")
    tasa_ahorro: float = Field(..., alias="tasaAhorro")
    gasto_promedio_diario: float = Field(..., alias="gastoPromedioDiario")
    proyeccion_fin_de_mes: float = Field(..., alias="proyeccionFinDeMes")
    volumen_transacciones: int = Field(..., alias="volumenTransacciones")

    model_config = {
        "populate_by_name": True,
    }

class RespuestaKPIs(BaseModel):
    resumen: ResumenKPIs
    recientes: List[Dict[str, Any]]

class CashflowPoint(BaseModel):
    mes: str
    ingresos: float
    gastos: float

class CategoriaDistribucion(BaseModel):
    categoria: str
    total: float
    porcentaje: float
    color: str

class RespuestaGraficos(BaseModel):
    flujo_caja: List[CashflowPoint] = Field(..., alias="flujoCaja")
    distribucion_gastos: List[CategoriaDistribucion] = Field(..., alias="distribucionGastos")
    heatmap: List[Dict[str, Any]] = Field(default=[], alias="heatmap")
    transacciones_metodo: List[Dict[str, Any]] = Field(default=[], alias="transaccionesMetodo")
    comparativa: List[Dict[str, Any]] = Field(default=[], alias="comparativa")

    model_config = {
        "populate_by_name": True,
    }


# ── Fallback Mockups (Localhost / Testing) ──────────────────────────────────
MOCK_TRANSACCIONES = [
    # Junio 2026
    {
        "id": "t1",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 5000.0,
        "tipo": "INGRESO",
        "categoria": "Sueldo",
        "categoriaIcono": "wallet",
        "fechaTransaccion": "2026-06-01T09:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mensual",
        "descripcion": "Nómina de Junio",
        "estado": "Completed"
    },
    {
        "id": "t2",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 200.0,
        "tipo": "GASTO",
        "categoria": "Comida",
        "categoriaIcono": "utensils",
        "fechaTransaccion": "2026-06-01T13:15:00",
        "metodoPago": "TARJETA",
        "etiquetas": "Almuerzo",
        "descripcion": "Restaurante La Lucha",
        "estado": "Completed"
    },
    # Mayo 2026
    {
        "id": "t3",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 4800.0,
        "tipo": "INGRESO",
        "categoria": "Sueldo",
        "categoriaIcono": "wallet",
        "fechaTransaccion": "2026-05-30T09:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mensual",
        "descripcion": "Nómina de Mayo",
        "estado": "Completed"
    },
    {
        "id": "t4",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 1500.0,
        "tipo": "GASTO",
        "categoria": "Hogar",
        "categoriaIcono": "house",
        "fechaTransaccion": "2026-05-05T10:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Alquiler",
        "descripcion": "Pago de Alquiler Mayo",
        "estado": "Completed"
    },
    {
        "id": "t5",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 300.0,
        "tipo": "GASTO",
        "categoria": "Ocio",
        "categoriaIcono": "film",
        "fechaTransaccion": "2026-05-15T21:00:00",
        "metodoPago": "TARJETA",
        "etiquetas": "Entretenimiento",
        "descripcion": "Concierto",
        "estado": "Completed"
    },
    # Abril 2026
    {
        "id": "t6",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 4800.0,
        "tipo": "INGRESO",
        "categoria": "Sueldo",
        "categoriaIcono": "wallet",
        "fechaTransaccion": "2026-04-30T09:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mensual",
        "descripcion": "Nómina de Abril",
        "estado": "Completed"
    },
    {
        "id": "t7",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 1800.0,
        "tipo": "GASTO",
        "categoria": "Servicios",
        "categoriaIcono": "wifi",
        "fechaTransaccion": "2026-04-10T12:00:00",
        "metodoPago": "DIGITAL",
        "etiquetas": "Luz-Agua-Internet",
        "descripcion": "Recibos de Hogar",
        "estado": "Completed"
    },
    {
        "id": "t8",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 400.0,
        "tipo": "GASTO",
        "categoria": "Ocio",
        "categoriaIcono": "film",
        "fechaTransaccion": "2026-04-18T19:30:00",
        "metodoPago": "TARJETA",
        "etiquetas": "Cine",
        "descripcion": "Cine y Cena Familiar",
        "estado": "Completed"
    },
    # Marzo 2026
    {
        "id": "t9",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 5200.0,
        "tipo": "INGRESO",
        "categoria": "Sueldo",
        "categoriaIcono": "wallet",
        "fechaTransaccion": "2026-03-31T09:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mensual-Bono",
        "descripcion": "Nómina + Bono Marzo",
        "estado": "Completed"
    },
    {
        "id": "t10",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 1200.0,
        "tipo": "GASTO",
        "categoria": "Comida",
        "categoriaIcono": "utensils",
        "fechaTransaccion": "2026-03-05T14:00:00",
        "metodoPago": "EFECTIVO",
        "etiquetas": "Compras",
        "descripcion": "Supermercado Mensual",
        "estado": "Completed"
    },
    {
        "id": "t11",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 600.0,
        "tipo": "GASTO",
        "categoria": "Saas",
        "categoriaIcono": "laptop-code",
        "fechaTransaccion": "2026-03-12T10:00:00",
        "metodoPago": "TARJETA",
        "etiquetas": "Cursos-Herramientas",
        "descripcion": "Suscripciones Cloud",
        "estado": "Completed"
    },
    # Febrero 2026
    {
        "id": "t12",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 4500.0,
        "tipo": "INGRESO",
        "categoria": "Sueldo",
        "categoriaIcono": "wallet",
        "fechaTransaccion": "2026-02-28T09:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mensual",
        "descripcion": "Nómina de Febrero",
        "estado": "Completed"
    },
    {
        "id": "t13",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 1100.0,
        "tipo": "GASTO",
        "categoria": "Hogar",
        "categoriaIcono": "house",
        "fechaTransaccion": "2026-02-05T11:00:00",
        "metodoPago": "TRANSFERENCIA",
        "etiquetas": "Mantenimiento",
        "descripcion": "Reparaciones e insumos",
        "estado": "Completed"
    },
    {
        "id": "t14",
        "usuarioId": "usr-123",
        "nombreCliente": "Usuario Luka",
        "monto": 200.0,
        "tipo": "GASTO",
        "categoria": "Transporte",
        "categoriaIcono": "bus",
        "fechaTransaccion": "2026-02-15T08:00:00",
        "metodoPago": "DIGITAL",
        "etiquetas": "Movilidad",
        "descripcion": "Pasajes y Taxis",
        "estado": "Completed"
    }
]


# ── Helper para Obtención Optimizada (Caché YTD) ───────────────────────────────

async def obtener_transacciones_YTD_optimizadas(
    usuario_id: str,
    token: str,
    fecha_inicio_str: Optional[str] = None,
    fecha_fin_str: Optional[str] = None,
) -> List[Dict[str, Any]]:
    """
    Obtiene las transacciones aplicando la estrategia de Caché Superset (YTD):
    - Carga por defecto todo el año actual (1 de Enero hasta Hoy) de ms-financiero.
    - Almacena en Redis con TTL de 3 horas (10800 segundos).
    - Si el rango solicitado está dentro del año actual, reutiliza la caché YTD.
    - Si está fuera, hace una consulta específica y la cachea por separado.
    """
    hoy = datetime.now()
    anio_actual = hoy.year
    ytd_inicio = datetime(anio_actual, 1, 1)
    ytd_fin = hoy

    # Parsear fechas de solicitud
    try:
        dt_inicio = datetime.strptime(fecha_inicio_str, "%Y-%m-%d") if fecha_inicio_str else ytd_inicio
    except Exception:
        dt_inicio = ytd_inicio

    try:
        dt_fin = datetime.strptime(fecha_fin_str, "%Y-%m-%d") if fecha_fin_str else ytd_fin
        dt_fin = dt_fin.replace(hour=23, minute=59, second=59)
    except Exception:
        dt_fin = ytd_fin

    # Determinar si el rango solicitado está dentro del YTD del año actual
    # (inicio >= 1 de enero y fin <= fin del día de hoy)
    dentro_ytd = (dt_inicio >= ytd_inicio) and (dt_fin <= ytd_fin.replace(hour=23, minute=59, second=59))

    cache = CacheRedis()

    if dentro_ytd:
        key_cache = f"ia:raw_tx:{usuario_id}:YTD_{anio_actual}"
        desde_query = ytd_inicio.isoformat()
        hasta_query = ytd_fin.isoformat()
        logger.info(f"[DASHBOARD-IA] Solicitud dentro de rango YTD. Clave de caché maestra: {key_cache}")
    else:
        inicio_fmt = dt_inicio.strftime("%Y-%m-%d")
        fin_fmt = dt_fin.strftime("%Y-%m-%d")
        key_cache = f"ia:raw_tx:{usuario_id}:{inicio_fmt}_{fin_fmt}"
        desde_query = dt_inicio.isoformat()
        hasta_query = dt_fin.isoformat()
        logger.info(f"[DASHBOARD-IA] Solicitud fuera de rango YTD. Clave de caché específica: {key_cache}")

    cached_data = cache.obtener(key_cache)
    if cached_data:
        try:
            logger.info(f"[DASHBOARD-IA] Cache HIT para transacciones crudas ({key_cache})")
            return json.loads(cached_data)
        except Exception as e:
            logger.error(f"[DASHBOARD-IA] Error parseando cache de transacciones crudas: {e}")

    logger.info(f"[DASHBOARD-IA] Cache MISS. Consultando ms-financiero desde {desde_query} hasta {hasta_query}...")
    
    error_conexion = False
    cliente = obtener_cliente_financiero()
    try:
        resp = await cliente.obtener_historial_transacciones_async(
            usuario_id=usuario_id,
            token=token,
            tamanio=10000,
            desde_exacto=desde_query,
            hasta_exacto=hasta_query
        )
        datos_raw = resp.get("datos", []) if resp else []
    except Exception as e:
        logger.error(f"[DASHBOARD-IA] Error consultando transacciones de ms-financiero: {e}")
        datos_raw = []
        error_conexion = True

    if error_conexion:
        logger.warning("[DASHBOARD-IA] Retornando MOCK_TRANSACCIONES debido a fallo en conexión")
        datos_raw = MOCK_TRANSACCIONES

    # Guardar en Redis con TTL de 3 horas (10800 segundos)
    try:
        cache.guardar(key_cache, json.dumps(datos_raw), ex=10800)
        logger.info(f"[DASHBOARD-IA] Transacciones crudas guardadas en caché Redis ({key_cache}) por 3 horas.")
    except Exception as e:
        logger.warning(f"[DASHBOARD-IA] No se pudo guardar en Redis: {e}")

    return datos_raw


# Helper Functions for Dashboard calculation

def filtrar_df_kpis(df: pd.DataFrame, dt_inicio: datetime, dt_fin: datetime, metodo_pago: Optional[str], tipo_movimiento: Optional[str]) -> pd.DataFrame:
    """Aplica los filtros de fecha, método de pago y tipo de movimiento al DataFrame."""
    if df.empty:
        return df
    
    # Rango de fecha
    df_filtered = df[(df['fecha'] >= pd.to_datetime(dt_inicio)) & (df['fecha'] <= pd.to_datetime(dt_fin))]
    
    # Método de pago
    if metodo_pago:
        df_filtered = df_filtered[df_filtered['metodo_pago'].astype(str).str.upper() == metodo_pago.upper()]
        
    # Tipo de movimiento
    if tipo_movimiento:
        tipo_map = "GASTO" if tipo_movimiento.upper() == "EGRESO" else tipo_movimiento.upper()
        df_filtered = df_filtered[df_filtered['tipo'].astype(str).str.upper() == tipo_map]
        
    return df_filtered


def calcular_gasto_presupuesto(df_completo: pd.DataFrame, presupuesto_activo: Optional[dict], monto_presupuesto: Optional[float]) -> float:
    """Calcula la suma de los gastos que caen dentro del periodo del presupuesto activo (o mes actual por defecto)."""
    if not monto_presupuesto or monto_presupuesto <= 0 or df_completo.empty:
        return 0.0
        
    hoy = datetime.now()
    inicio_def = pd.to_datetime(datetime(hoy.year, hoy.month, 1))
    fin_def = pd.to_datetime(hoy)
    
    # Determinar rango
    def_inicio_val = presupuesto_activo.get("fechaInicio") if presupuesto_activo else None
    if def_inicio_val:
        limite_inicio = pd.to_datetime(def_inicio_val)
        if limite_inicio.tzinfo is not None:
            limite_inicio = limite_inicio.tz_localize(None)
    else:
        limite_inicio = inicio_def
        
    def_fin_val = presupuesto_activo.get("fechaFin") if presupuesto_activo else None
    if def_fin_val:
        limite_fin = pd.to_datetime(def_fin_val)
        if limite_fin.tzinfo is not None:
            limite_fin = limite_fin.tz_localize(None)
    else:
        limite_fin = fin_def
        
    # Filtrar gastos en el periodo del presupuesto
    df_gas_pres = df_completo[
        (df_completo['fecha'] >= limite_inicio) & 
        (df_completo['fecha'] <= limite_fin) &
        (df_completo['tipo'] == 'GASTO') & 
        (df_completo['estado'].astype(str).str.upper() != 'FAILED')
    ]
    return float(df_gas_pres['monto'].sum())


def calcular_metricas_kpis(df_filtrado: pd.DataFrame, dt_inicio: datetime, dt_fin: datetime) -> dict:
    """Calcula las métricas e indicadores a partir del DataFrame filtrado."""
    if df_filtrado.empty:
        return {
            "totalIngresos": 0.0,
            "totalGastos": 0.0,
            "balance": 0.0,
            "cantidadIngresos": 0,
            "cantidadGastos": 0,
            "tasaAhorro": 0.0,
            "gastoPromedioDiario": 0.0,
            "proyeccionFinDeMes": 0.0,
            "volumenTransacciones": 0
        }

    df_ing = df_filtrado[(df_filtrado['tipo'] == 'INGRESO') & (df_filtrado['estado'].astype(str).str.upper() == 'COMPLETED')]
    df_gas = df_filtrado[(df_filtrado['tipo'] == 'GASTO') & (df_filtrado['estado'].astype(str).str.upper() != 'FAILED')]

    total_ing = float(df_ing['monto'].sum())
    total_gas = float(df_gas['monto'].sum())
    balance = total_ing - total_gas
    cant_ing = len(df_ing)
    cant_gas = len(df_gas)
    tasa_ahorro = ((total_ing - total_gas) / total_ing) * 100 if total_ing > 0 else 0.0

    dias_periodo = (dt_fin - dt_inicio).days + 1

    import calendar
    dias_en_mes = calendar.monthrange(dt_fin.year, dt_fin.month)[1]

    # Si el filtro es solo INGRESOS (sin gastos), calculamos promedio e proyección sobre ingresos
    solo_ingresos = (total_gas == 0.0 and total_ing > 0.0 and cant_gas == 0)
    if solo_ingresos:
        gasto_prom_diario = total_ing / dias_periodo if dias_periodo > 0 else 0.0
        proyeccion_fin = gasto_prom_diario * dias_en_mes
    else:
        gasto_prom_diario = total_gas / dias_periodo if dias_periodo > 0 else 0.0
        proyeccion_fin = gasto_prom_diario * dias_en_mes

    return {
        "totalIngresos": total_ing,
        "totalGastos": total_gas,
        "balance": balance,
        "cantidadIngresos": cant_ing,
        "cantidadGastos": cant_gas,
        "tasaAhorro": tasa_ahorro,
        "gastoPromedioDiario": gasto_prom_diario,
        "proyeccionFinDeMes": proyeccion_fin,
        "volumenTransacciones": cant_ing + cant_gas
    }


def filtrar_transacciones_recientes(datos_raw: list, dt_inicio: datetime, dt_fin: datetime, metodo_pago: Optional[str], tipo_movimiento: Optional[str]) -> list:
    """Filtra y ordena las últimas 10 transacciones en base a los criterios del dashboard."""
    recientes_filtradas = []
    for tx in datos_raw:
        try:
            tx_fecha = datetime.fromisoformat(tx.get("fechaTransaccion", "").replace("Z", ""))
        except Exception:
            continue
        
        if not (dt_inicio <= tx_fecha <= dt_fin):
            continue
        
        if metodo_pago and tx.get("metodoPago", "").upper() != metodo_pago.upper():
            continue
            
        if tipo_movimiento:
            tipo_map = "GASTO" if tipo_movimiento.upper() == "EGRESO" else tipo_movimiento.upper()
            if tx.get("tipo", "").upper() != tipo_map:
                continue
        
        recientes_filtradas.append(tx)
    
    try:
        recientes_filtradas.sort(key=lambda x: x.get("fechaTransaccion", ""), reverse=True)
    except Exception:
        pass
        
    return recientes_filtradas[:10]


def calcular_flujo_caja(df: pd.DataFrame, meses_lista: list) -> list:
    """Calcula el ingresos/gastos mensuales para el flujo de caja."""
    flujo_caja = []
    NOMBRES_MESES = {
        1: "Ene", 2: "Feb", 3: "Mar", 4: "Abr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Ago", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dic"
    }
    for y, m in meses_lista:
        if not df.empty:
            df_mes = df[(df['anio'] == y) & (df['mes'] == m)]
            ing_mes = float(df_mes[(df_mes['tipo'] == 'INGRESO') & (df_mes['estado'].astype(str).str.upper() == 'COMPLETED')]['monto'].sum())
            gas_mes = float(df_mes[(df_mes['tipo'] == 'GASTO') & (df_mes['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
        else:
            ing_mes, gas_mes = 0.0, 0.0
        flujo_caja.append({"mes": NOMBRES_MESES[m], "ingresos": round(ing_mes, 2), "gastos": round(gas_mes, 2)})
    return flujo_caja


def calcular_distribucion_gastos(df: pd.DataFrame) -> list:
    """Agrupa gastos o ingresos por categoría (top 6, excluye sin categoría)."""
    colores_default = {
        "food": "#FF7043", "comida": "#FF7043", "alimentación": "#FF7043", "alimentacion": "#FF7043",
        "transport": "#42A5F5", "transporte": "#42A5F5", "pasaje moto": "#26C6DA",
        "leisure": "#AB47BC", "ocio": "#AB47BC", "entretenimiento": "#AB47BC",
        "health": "#10B981", "salud": "#10B981",
        "saas": "#6366F1", "hogar": "#EC4899", "vivienda": "#EC4899",
        "suscripciones": "#6366F1", "suscripciones streaming": "#8B5CF6",
        "servicios": "#14B8A6", "inversiones": "#10B981",
        "transferencia": "#F59E0B", "otros": "#859397",
        "tecnología": "#3B82F6", "tecnologia": "#3B82F6",
        "viajes": "#EF4444", "educación": "#10B981", "educacion": "#10B981",
        "ropa y calzado": "#F59E0B", "otros gastos": "#859397",
        "salario": "#8B5CF6", "freelance": "#14B8A6", "ventas": "#F59E0B"
    }
    distribucion = []
    if not df.empty:
        # Determinar si mostrar GASTOS o INGRESOS dependiendo del df
        tiene_gastos = not df[df['tipo'] == 'GASTO'].empty
        tiene_ingresos = not df[df['tipo'] == 'INGRESO'].empty
        
        if tiene_ingresos and not tiene_gastos:
            filtro_tipo = 'INGRESO'
            estados_validos = ['COMPLETED']
        else:
            filtro_tipo = 'GASTO'
            estados_validos = None # Para gastos, cualquier cosa != FAILED
            
        if filtro_tipo == 'INGRESO':
            df_gastos = df[
                (df['tipo'] == 'INGRESO') &
                (df['estado'].astype(str).str.upper() == 'COMPLETED')
            ].copy()
        else:
            df_gastos = df[
                (df['tipo'] == 'GASTO') &
                (df['estado'].astype(str).str.upper() != 'FAILED')
            ].copy()
            
        # Excluir filas sin categoría (NaN, "nan", "none", vacío)
        mask_invalida = (
            df_gastos['categoria_nombre'].isna() |
            df_gastos['categoria_nombre'].astype(str).str.strip().str.lower().isin(['nan', 'none', ''])
        )
        df_gastos = df_gastos[~mask_invalida]
        total_gastado = float(df_gastos['monto'].sum())
        if total_gastado > 0:
            grouped = df_gastos.groupby('categoria_nombre')['monto'].sum().reset_index()
            grouped = grouped.sort_values(by='monto', ascending=False).head(6)
            for _, row in grouped.iterrows():
                cat = str(row['categoria_nombre'])
                total_cat = float(row['monto'])
                distribucion.append({
                    "categoria": cat,
                    "total": round(total_cat, 2),
                    "porcentaje": round((total_cat / total_gastado) * 100, 2),
                    "color": colores_default.get(cat.lower(), "#859397")
                })
    return distribucion


def calcular_heatmap_gastos(df: pd.DataFrame) -> list:
    """Agrupa cantidad de gastos por día de semana (Lun-Dom)."""
    DIAS_SEMANA = {0: "Lun", 1: "Mar", 2: "Mié", 3: "Jue", 4: "Vie", 5: "Sáb", 6: "Dom"}
    heatmap = []
    if not df.empty:
        df_gas_hm = df[((df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')) | ((df['tipo'] == 'INGRESO') & (df['estado'].astype(str).str.upper() == 'COMPLETED'))].copy()
        if not df_gas_hm.empty and 'fecha' in df_gas_hm.columns:
            df_gas_hm['dia_num'] = pd.to_datetime(df_gas_hm['fecha']).dt.dayofweek
            conteo_sem = df_gas_hm.groupby('dia_num').size().reset_index(name='n')
            dias_map = {int(row['dia_num']): int(row['n']) for _, row in conteo_sem.iterrows()}
            for num, nombre in DIAS_SEMANA.items():
                heatmap.append({"dia": nombre, "intensidad": dias_map.get(num, 0)})
    if not heatmap:
        heatmap = [{"dia": n, "intensidad": 0} for n in DIAS_SEMANA.values()]
    return heatmap


def calcular_metodos_pago(df: pd.DataFrame) -> list:
    """Calcula el desglose de gastos por método de pago."""
    COLORES_METODO = {
        "EFECTIVO": "#10B981", "TARJETA": "#5B6AF0",
        "TRANSFERENCIA": "#F59E0B", "DIGITAL": "#EC4899"
    }
    transacciones_metodo = []
    if not df.empty:
        df_met = df[((df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')) | ((df['tipo'] == 'INGRESO') & (df['estado'].astype(str).str.upper() == 'COMPLETED'))].copy()
        if not df_met.empty and 'metodo_pago' in df_met.columns:
            conteo = df_met.groupby('metodo_pago').size().reset_index(name='cantidad')
            conteo = conteo.sort_values('cantidad', ascending=False)
            for _, row in conteo.iterrows():
                m_str = str(row['metodo_pago']).upper()
                transacciones_metodo.append({
                    "metodo": m_str,
                    "cantidad": int(row['cantidad']),
                    "color": COLORES_METODO.get(m_str, "#859397")
                })
    return transacciones_metodo


async def calcular_comparativa_historica(
    df: pd.DataFrame, 
    datos_raw: list, 
    dt_inicio: datetime, 
    dt_fin: datetime, 
    meses_lista_comp: list, 
    anio_actual: int, 
    usuario_id: str, 
    token: str
) -> list:
    """Calcula la comparativa histórica semanal o mensual del usuario."""
    comparativa = []
    dias_periodo = (dt_fin - dt_inicio).days + 1
    es_semanal = (dias_periodo <= 7)
    NOMBRES_MESES = {
        1: "Ene", 2: "Feb", 3: "Mar", 4: "Abr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Ago", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dic"
    }

    # Determinar si comparamos gastos o ingresos (según filtro)
    filtro_tipo = 'GASTO'
    if not df.empty and not df[df['tipo'] == 'INGRESO'].empty and df[df['tipo'] == 'GASTO'].empty:
        filtro_tipo = 'INGRESO'

    if es_semanal:
        # Calcular semana actual
        gas_actual = 0.0
        if not df.empty:
            df_semana_act = df[(df['fecha'] >= pd.to_datetime(dt_inicio)) & (df['fecha'] <= pd.to_datetime(dt_fin))]
            if filtro_tipo == 'INGRESO':
                gas_actual = float(df_semana_act[(df_semana_act['tipo'] == 'INGRESO') & (df_semana_act['estado'].astype(str).str.upper() == 'COMPLETED')]['monto'].sum())
            else:
                gas_actual = float(df_semana_act[(df_semana_act['tipo'] == 'GASTO') & (df_semana_act['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
        
        comparativa.append({
            "mes": "Semana",
            "actual": round(gas_actual, 2),
            "anterior": 0.0
        })
    else:
        # Comparativa Mensual / Anual (Solo mes actual, sin comparar con año anterior)
        for y, m in meses_lista_comp:
            nombre_mes = NOMBRES_MESES[m]
            gas_actual = 0.0
            if not df.empty:
                df_m_act = df[(df['anio'] == y) & (df['mes'] == m)]
                if filtro_tipo == 'INGRESO':
                    gas_actual = float(df_m_act[(df_m_act['tipo'] == 'INGRESO') & (df_m_act['estado'].astype(str).str.upper() == 'COMPLETED')]['monto'].sum())
                else:
                    gas_actual = float(df_m_act[(df_m_act['tipo'] == 'GASTO') & (df_m_act['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
            
            comparativa.append({"mes": nombre_mes, "actual": round(gas_actual, 2), "anterior": 0.0})
            
    return comparativa


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/kpis", response_model=ResultadoApi[RespuestaKPIs])
async def get_dashboard_kpis(
    request: Request,
    fechaInicio: Optional[str] = None,
    fechaFin: Optional[str] = None,
    metodoPago: Optional[str] = None,
    tipoMovimiento: Optional[str] = None,
    payload: dict = Security(validar_token),
):
    """
    Obtiene y calcula dinámicamente los KPIs principales y las últimas 10 transacciones.
    Orquestador modular de KPIs.
    """
    usuario_id = obtener_usuario_id(payload)
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    # 1. Obtener bolsa de transacciones crudas y presupuesto activo en paralelo
    # 1. Obtener bolsa de transacciones crudas
    datos_raw = await obtener_transacciones_YTD_optimizadas(
        usuario_id=usuario_id,
        token=token,
        fecha_inicio_str=fechaInicio,
        fecha_fin_str=fechaFin
    )

    # 2. Cargar DataFrame principal
    df_completo = json_a_dataframe(datos_raw)

    # 3. Parsear fechas de rango
    hoy = datetime.now()
    anio_actual = hoy.year
    ytd_inicio = datetime(anio_actual, 1, 1)
    ytd_fin = hoy

    try:
        dt_inicio = datetime.strptime(fechaInicio, "%Y-%m-%d") if fechaInicio else ytd_inicio
    except Exception:
        dt_inicio = ytd_inicio

    try:
        dt_fin = datetime.strptime(fechaFin, "%Y-%m-%d") if fechaFin else ytd_fin
        dt_fin = dt_fin.replace(hour=23, minute=59, second=59)
    except Exception:
        dt_fin = ytd_fin



    # 5. Filtrar DataFrame para los KPIs del periodo
    df_filtrado = filtrar_df_kpis(df_completo, dt_inicio, dt_fin, metodoPago, tipoMovimiento)

    # 6. Calcular métricas básicas
    metricas = calcular_metricas_kpis(df_filtrado, dt_inicio, dt_fin)

    # 7. Obtener transacciones recientes
    recientes = filtrar_transacciones_recientes(datos_raw, dt_inicio, dt_fin, metodoPago, tipoMovimiento)



    # 9. Fechas de rango reales
    if not df_filtrado.empty and 'fecha_transaccion' in df_filtrado.columns:
        desde_str = df_filtrado['fecha_transaccion'].min().isoformat()
        hasta_str = df_filtrado['fecha_transaccion'].max().isoformat()
    else:
        desde_str = dt_inicio.isoformat()
        hasta_str = dt_fin.isoformat()

    kpis = {
        "resumen": {
            "desde": desde_str,
            "hasta": hasta_str,
            "totalIngresos": round(metricas["totalIngresos"], 2),
            "totalGastos": round(metricas["totalGastos"], 2),
            "balance": round(metricas["balance"], 2),
            "cantidadIngresos": metricas["cantidadIngresos"],
            "cantidadGastos": metricas["cantidadGastos"],
            "tasaAhorro": round(metricas["tasaAhorro"], 2),
            "gastoPromedioDiario": round(metricas["gastoPromedioDiario"], 2),
            "proyeccionFinDeMes": round(metricas["proyeccionFinDeMes"], 2),
            "volumenTransacciones": metricas["volumenTransacciones"]
        },
        "recientes": recientes
    }

    return ResultadoApi.exito_res(datos=kpis, mensaje="KPIs calculados con éxito.", ruta=request.url.path)


@router.get("/graficos")
async def get_dashboard_graficos(
    request: Request,
    fechaInicio: Optional[str] = None,
    fechaFin: Optional[str] = None,
    metodoPago: Optional[str] = None,
    tipoMovimiento: Optional[str] = None,
    payload: dict = Security(validar_token),
):
    """
    Obtiene y calcula dinámicamente los datos de gráficos de manera modular.
    """
    usuario_id = obtener_usuario_id(payload)
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    # 1. Obtener transacciones crudas
    datos_raw = await obtener_transacciones_YTD_optimizadas(
        usuario_id=usuario_id,
        token=token,
        fecha_inicio_str=fechaInicio,
        fecha_fin_str=fechaFin
    )

    # 2. Cargar en DataFrame
    df = json_a_dataframe(datos_raw)

    hoy = datetime.now()
    anio_actual = hoy.year
    ytd_inicio = datetime(anio_actual, 1, 1)
    ytd_fin = hoy

    try:
        dt_inicio = datetime.strptime(fechaInicio, "%Y-%m-%d") if fechaInicio else ytd_inicio
    except Exception:
        dt_inicio = ytd_inicio

    try:
        dt_fin = datetime.strptime(fechaFin, "%Y-%m-%d") if fechaFin else ytd_fin
        dt_fin = dt_fin.replace(hour=23, minute=59, second=59)
    except Exception:
        dt_fin = ytd_fin

    # A. DataFrame con TODOS los filtros (para la mayoría de gráficos)
    df_filtrado = df.copy()
    if not df_filtrado.empty:
        df_filtrado = df_filtrado[(df_filtrado['fecha'] >= pd.to_datetime(dt_inicio)) & (df_filtrado['fecha'] <= pd.to_datetime(dt_fin))]
        if metodoPago:
            df_filtrado = df_filtrado[df_filtrado['metodo_pago'].astype(str).str.upper() == metodoPago.upper()]
        if tipoMovimiento:
            tipo_map = "GASTO" if tipoMovimiento.upper() == "EGRESO" else tipoMovimiento.upper()
            df_filtrado = df_filtrado[df_filtrado['tipo'].astype(str).str.upper() == tipo_map]

    # A2. DataFrame solo con filtro de FECHA (para Relación Gasto-Ingreso — ignora tipo y método)
    df_ratio = df.copy()
    if not df_ratio.empty:
        df_ratio = df_ratio[(df_ratio['fecha'] >= pd.to_datetime(dt_inicio)) & (df_ratio['fecha'] <= pd.to_datetime(dt_fin))]

    # alias para compatibilidad con el resto del código
    df = df_filtrado

    # B. Determinar rango de meses (para flujo de caja)
    if not fechaInicio and not fechaFin:
        meses_lista = [(hoy.year, m) for m in range(1, hoy.month + 1)]
        meses_lista_comp = [(hoy.year, m) for m in range(1, hoy.month + 1)]
    else:
        # Con filtros: meses en el rango [dt_inicio, dt_fin]
        meses_lista = []
        cur_y, cur_m = dt_inicio.year, dt_inicio.month
        while (cur_y < dt_fin.year) or (cur_y == dt_fin.year and cur_m <= dt_fin.month):
            meses_lista.append((cur_y, cur_m))
            cur_m += 1
            if cur_m > 12:
                cur_m = 1
                cur_y += 1

        # meses_lista_comp contiene exactamente los meses del rango sin el mes anterior
        meses_lista_comp = list(meses_lista)

        # Si el rango tiene solo 1 mes, agregar el mes anterior para comparación
        if len(meses_lista) == 1:
            y, m = meses_lista[0]
            prev_m = m - 1
            prev_y = y
            if prev_m <= 0:
                prev_m = 12
                prev_y -= 1
            meses_lista.insert(0, (prev_y, prev_m))

    # C. Cómputo modular de gráficos
    flujo_caja = calcular_flujo_caja(df_ratio, meses_lista)   # Relación Gasto-Ingreso: solo fecha
    distribucion = calcular_distribucion_gastos(df)
    heatmap = calcular_heatmap_gastos(df)
    transacciones_metodo = calcular_metodos_pago(df)
    comparativa = await calcular_comparativa_historica(
        df=df,
        datos_raw=datos_raw,
        dt_inicio=dt_inicio,
        dt_fin=dt_fin,
        meses_lista_comp=meses_lista_comp,
        anio_actual=anio_actual,
        usuario_id=usuario_id,
        token=token
    )

    graficos = {
        "flujoCaja": flujo_caja,
        "distribucionGastos": distribucion,
        "heatmap": heatmap,
        "transaccionesMetodo": transacciones_metodo,
        "comparativa": comparativa
    }

    payload_out = ResultadoApi.exito_res(
        datos=graficos,
        mensaje="Gráficos calculados con éxito.",
        ruta=str(request.url.path)
    )
    return payload_out
