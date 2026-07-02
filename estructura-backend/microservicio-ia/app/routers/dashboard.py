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
from app.clientes.luka_clients import obtener_cliente_financiero
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
    Usa caché YTD e implementa filtrado en memoria.
    """
    usuario_id = obtener_usuario_id(payload)
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    # 1. Obtener bolsa de transacciones crudas
    datos_raw = await obtener_transacciones_YTD_optimizadas(
        usuario_id=usuario_id,
        token=token,
        fecha_inicio_str=fechaInicio,
        fecha_fin_str=fechaFin
    )

    # 2. Cargar en Pandas y aplicar filtros locales
    df = json_a_dataframe(datos_raw)

    # Parsear fechas de filtro
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

    if not df.empty:
        # A. Filtrar por Rango de Fecha Solicitado (en memoria)
        # La columna 'fecha' en el df ya está formateada como datetime por json_a_dataframe
        df = df[(df['fecha'] >= pd.to_datetime(dt_inicio)) & (df['fecha'] <= pd.to_datetime(dt_fin))]

        # B. Filtrar por metodoPago si se especifica
        if metodoPago:
            df = df[df['metodo_pago'].astype(str).str.upper() == metodoPago.upper()]

        # C. Filtrar por tipoMovimiento si se especifica
        if tipoMovimiento:
            tipo_map = "GASTO" if tipoMovimiento.upper() == "EGRESO" else tipoMovimiento.upper()
            df = df[df['tipo'].astype(str).str.upper() == tipo_map]

    # 3. Calcular KPIs sobre el DataFrame filtrado
    if df.empty:
        total_ing = 0.0
        total_gas = 0.0
        balance = 0.0
        cant_ing = 0
        cant_gas = 0
        tasa_ahorro = 0.0
        recientes = []
        desde_str = dt_inicio.isoformat()
        hasta_str = dt_fin.isoformat()
    else:
        df_ing = df[(df['tipo'] == 'INGRESO') & (df['estado'].astype(str).str.upper() == 'COMPLETED')]
        df_gas = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')]

        total_ing = float(df_ing['monto'].sum())
        total_gas = float(df_gas['monto'].sum())
        balance = total_ing - total_gas
        cant_ing = len(df_ing)
        cant_gas = len(df_gas)
        tasa_ahorro = ((total_ing - total_gas) / total_ing) * 100 if total_ing > 0 else 0.0

        # Para las recientes, filtramos de la lista cruda original (camelCase) para preservar la interfaz del frontend
        recientes_filtradas = []
        for tx in datos_raw:
            try:
                tx_fecha = datetime.fromisoformat(tx.get("fechaTransaccion", "").replace("Z", ""))
            except Exception:
                continue
            
            if not (dt_inicio <= tx_fecha <= dt_fin):
                continue
            
            if metodoPago and tx.get("metodoPago", "").upper() != metodoPago.upper():
                continue
                
            if tipoMovimiento:
                tipo_map = "GASTO" if tipoMovimiento.upper() == "EGRESO" else tipoMovimiento.upper()
                if tx.get("tipo", "").upper() != tipo_map:
                    continue
            
            recientes_filtradas.append(tx)
        
        try:
            recientes_filtradas.sort(key=lambda x: x.get("fechaTransaccion", ""), reverse=True)
        except Exception:
            pass
        recientes = recientes_filtradas[:10]

        if 'fecha_transaccion' in df.columns:
            desde_str = df['fecha_transaccion'].min().isoformat()
            hasta_str = df['fecha_transaccion'].max().isoformat()
        else:
            desde_str = dt_inicio.isoformat()
            hasta_str = dt_fin.isoformat()

    kpis = {
        "resumen": {
            "desde": desde_str,
            "hasta": hasta_str,
            "totalIngresos": round(total_ing, 2),
            "totalGastos": round(total_gas, 2),
            "balance": round(balance, 2),
            "cantidadIngresos": cant_ing,
            "cantidadGastos": cant_gas,
            "tasaAhorro": round(tasa_ahorro, 2)
        },
        "recientes": recientes
    }

    return ResultadoApi.exito_res(datos=kpis, mensaje="KPIs calculados con éxito.", ruta=request.url.path)


@router.get("/graficos", response_model=ResultadoApi[RespuestaGraficos])
async def get_dashboard_graficos(
    request: Request,
    fechaInicio: Optional[str] = None,
    fechaFin: Optional[str] = None,
    metodoPago: Optional[str] = None,
    tipoMovimiento: Optional[str] = None,
    payload: dict = Security(validar_token),
):
    """
    Obtiene y calcula dinámicamente los datos de gráficos.
    Usa caché YTD e implementa filtrado en memoria.
    """
    usuario_id = obtener_usuario_id(payload)
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    # 1. Obtener bolsa de transacciones crudas
    datos_raw = await obtener_transacciones_YTD_optimizadas(
        usuario_id=usuario_id,
        token=token,
        fecha_inicio_str=fechaInicio,
        fecha_fin_str=fechaFin
    )

    # 2. Cargar en Pandas y aplicar filtros locales
    df = json_a_dataframe(datos_raw)

    # Parsear fechas de filtro
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

    if not df.empty:
        # A. Filtrar por Rango de Fecha Solicitado (en memoria)
        df = df[(df['fecha'] >= pd.to_datetime(dt_inicio)) & (df['fecha'] <= pd.to_datetime(dt_fin))]

        # B. Filtrar por metodoPago si se especifica
        if metodoPago:
            df = df[df['metodo_pago'].astype(str).str.upper() == metodoPago.upper()]

        # C. Filtrar por tipoMovimiento si se especifica
        if tipoMovimiento:
            tipo_map = "GASTO" if tipoMovimiento.upper() == "EGRESO" else tipoMovimiento.upper()
            df = df[df['tipo'].astype(str).str.upper() == tipo_map]

    # 3. Flujo de Caja (últimos 5 meses)
    ahora = datetime.now()
    meses_lista = []
    for i in range(4, -1, -1):
        m = ahora.month - i
        y = ahora.year
        while m <= 0:
            m += 12
            y -= 1
        meses_lista.append((y, m))

    NOMBRES_MESES = {
        1: "Ene", 2: "Feb", 3: "Mar", 4: "Abr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Ago", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dic"
    }

    flujo_caja = []
    if df.empty:
        for y, m in meses_lista:
            flujo_caja.append({
                "mes": NOMBRES_MESES[m],
                "ingresos": 0.0,
                "gastos": 0.0
            })
    else:
        for y, m in meses_lista:
            df_mes = df[(df['anio'] == y) & (df['mes'] == m)]
            ing_mes = float(df_mes[(df_mes['tipo'] == 'INGRESO') & (df_mes['estado'].astype(str).str.upper() == 'COMPLETED')]['monto'].sum())
            gas_mes = float(df_mes[(df_mes['tipo'] == 'GASTO') & (df_mes['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
            flujo_caja.append({
                "mes": NOMBRES_MESES[m],
                "ingresos": round(ing_mes, 2),
                "gastos": round(gas_mes, 2)
            })

    # 4. Distribución de Gastos
    distribucion = []
    colores_default = {
        "food": "#FF7043",
        "comida": "#FF7043",
        "transport": "#42A5F5",
        "transporte": "#42A5F5",
        "leisure": "#AB47BC",
        "ocio": "#AB47BC",
        "entretenimiento": "#AB47BC",
        "health": "#26C6DA",
        "salud": "#26C6DA",
        "saas": "#5B6AF0",
        "hogar": "#EC4899",
        "servicios": "#14B8A6",
        "inversiones": "#10B981",
        "transferencia": "#F59E0B",
        "otros": "#859397"
    }

    if not df.empty:
        df_gastos = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')]
        total_gastado = float(df_gastos['monto'].sum())

        if total_gastado > 0:
            grouped = df_gastos.groupby('categoria_nombre')['monto'].sum().reset_index()
            grouped = grouped.sort_values(by='monto', ascending=False)
            for idx, row in grouped.iterrows():
                cat = str(row['categoria_nombre'])
                total_cat = float(row['monto'])
                pct = (total_cat / total_gastado) * 100
                cat_lower = cat.lower()
                color = colores_default.get(cat_lower, "#859397")
                distribucion.append({
                    "categoria": cat,
                    "total": round(total_cat, 2),
                    "porcentaje": round(pct, 2),
                    "color": color
                })

    graficos = {
        "flujoCaja": flujo_caja,
        "distribucionGastos": distribucion
    }

    return ResultadoApi.exito_res(datos=graficos, mensaje="Gráficos calculados con éxito.", ruta=request.url.path)

