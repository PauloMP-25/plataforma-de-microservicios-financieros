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
    cumplimiento_presupuesto: float = Field(default=0.0, alias="cumplimientoPresupuesto")
    presupuesto_activo: Optional[float] = Field(default=None, alias="presupuestoActivo")

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
    También consulta el presupuesto global activo desde ms-cliente para calcular
    el cumplimientoPresupuesto real del usuario.
    """
    usuario_id = obtener_usuario_id(payload)
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    # 1. Obtener bolsa de transacciones crudas y presupuesto activo en paralelo
    import asyncio
    cliente_perfil = obtener_cliente_perfil()
    datos_raw, presupuesto_activo = await asyncio.gather(
        obtener_transacciones_YTD_optimizadas(
            usuario_id=usuario_id,
            token=token,
            fecha_inicio_str=fechaInicio,
            fecha_fin_str=fechaFin
        ),
        cliente_perfil.obtener_presupuesto_activo_async(usuario_id=usuario_id, token=token)
    )
    monto_presupuesto = float(presupuesto_activo.get("montoLimite", 0)) if presupuesto_activo else None
    logger.info(f"[DASHBOARD-KPIs] Presupuesto activo para {usuario_id}: {monto_presupuesto}")

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

        dias_periodo = (dt_fin - dt_inicio).days + 1
        gasto_prom_diario = total_gas / dias_periodo if dias_periodo > 0 else 0.0
        
        import calendar
        dias_en_mes = calendar.monthrange(dt_fin.year, dt_fin.month)[1]
        proyeccion_fin = gasto_prom_diario * dias_en_mes

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

    # Calcular cumplimiento de presupuesto usando el presupuesto global activo del ms-cliente
    if monto_presupuesto and monto_presupuesto > 0:
        cumplimiento_presupuesto = round((total_gas / monto_presupuesto) * 100, 2)
    else:
        # Sin presupuesto configurado: mostrar 0
        cumplimiento_presupuesto = 0.0

    kpis = {
        "resumen": {
            "desde": desde_str,
            "hasta": hasta_str,
            "totalIngresos": round(total_ing, 2),
            "totalGastos": round(total_gas, 2),
            "balance": round(balance, 2),
            "cantidadIngresos": cant_ing,
            "cantidadGastos": cant_gas,
            "tasaAhorro": round(tasa_ahorro, 2),
            "gastoPromedioDiario": round(gasto_prom_diario, 2) if not df.empty else 0.0,
            "proyeccionFinDeMes": round(proyeccion_fin, 2) if not df.empty else 0.0,
            "cumplimientoPresupuesto": cumplimiento_presupuesto,
            "presupuestoActivo": monto_presupuesto
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
    Obtiene y calcula dinámicamente los datos de gráficos:
    - Flujo de Caja (últimos 5 meses)
    - Distribución de Gastos por categoría
    - Heatmap: cantidad de gastos por día de semana
    - Métodos de Pago: desglose de transacciones por método
    - Comparativa Histórica: gastos mes a mes (año actual vs año anterior)
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
        df = df[(df['fecha'] >= pd.to_datetime(dt_inicio)) & (df['fecha'] <= pd.to_datetime(dt_fin))]
        if metodoPago:
            df = df[df['metodo_pago'].astype(str).str.upper() == metodoPago.upper()]
        if tipoMovimiento:
            tipo_map = "GASTO" if tipoMovimiento.upper() == "EGRESO" else tipoMovimiento.upper()
            df = df[df['tipo'].astype(str).str.upper() == tipo_map]

    NOMBRES_MESES = {
        1: "Ene", 2: "Feb", 3: "Mar", 4: "Abr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Ago", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dic"
    }

    # 3. Flujo de Caja (últimos 5 meses)
    meses_lista = []
    for i in range(4, -1, -1):
        m = hoy.month - i
        y = hoy.year
        while m <= 0:
            m += 12
            y -= 1
        meses_lista.append((y, m))

    flujo_caja = []
    for y, m in meses_lista:
        if not df.empty:
            df_mes = df[(df['anio'] == y) & (df['mes'] == m)]
            ing_mes = float(df_mes[(df_mes['tipo'] == 'INGRESO') & (df_mes['estado'].astype(str).str.upper() == 'COMPLETED')]['monto'].sum())
            gas_mes = float(df_mes[(df_mes['tipo'] == 'GASTO') & (df_mes['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
        else:
            ing_mes, gas_mes = 0.0, 0.0
        flujo_caja.append({"mes": NOMBRES_MESES[m], "ingresos": round(ing_mes, 2), "gastos": round(gas_mes, 2)})

    # 4. Distribución de Gastos por categoría
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
        "ropa y calzado": "#F59E0B", "otros gastos": "#859397"
    }
    distribucion = []
    if not df.empty:
        df_gastos = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')]
        total_gastado = float(df_gastos['monto'].sum())
        if total_gastado > 0:
            grouped = df_gastos.groupby('categoria_nombre')['monto'].sum().reset_index()
            grouped = grouped.sort_values(by='monto', ascending=False)
            for _, row in grouped.iterrows():
                cat = str(row['categoria_nombre'])
                total_cat = float(row['monto'])
                distribucion.append({
                    "categoria": cat,
                    "total": round(total_cat, 2),
                    "porcentaje": round((total_cat / total_gastado) * 100, 2),
                    "color": colores_default.get(cat.lower(), "#859397")
                })

    # 5. Heatmap: cantidad de gastos agrupados por día de semana (Lun–Dom)
    DIAS_SEMANA = {0: "Lun", 1: "Mar", 2: "Mié", 3: "Jue", 4: "Vie", 5: "Sáb", 6: "Dom"}
    heatmap = []
    if not df.empty:
        df_gas_hm = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')].copy()
        if not df_gas_hm.empty and 'fecha' in df_gas_hm.columns:
            df_gas_hm['dia_num'] = pd.to_datetime(df_gas_hm['fecha']).dt.dayofweek
            conteo_sem = df_gas_hm.groupby('dia_num').size().reset_index(name='n')
            dias_map = {int(row['dia_num']): int(row['n']) for _, row in conteo_sem.iterrows()}
            for num, nombre in DIAS_SEMANA.items():
                heatmap.append({"dia": nombre, "intensidad": dias_map.get(num, 0)})
    if not heatmap:
        heatmap = [{"dia": n, "intensidad": 0} for n in DIAS_SEMANA.values()]

    # 6. Métodos de Pago: desglose de gastos por método de pago
    COLORES_METODO = {
        "EFECTIVO": "#10B981", "TARJETA": "#5B6AF0",
        "TRANSFERENCIA": "#F59E0B", "DIGITAL": "#EC4899"
    }
    transacciones_metodo = []
    if not df.empty:
        df_met = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')].copy()
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

    # 7. Comparativa Histórica: gastos mes a mes (año actual vs año anterior)
    comparativa = []
    anio_ant = anio_actual - 1
    df_ant = pd.DataFrame()
    try:
        from app.persistencia.redis.cache_redis import CacheRedis as CacheRedisComp
        import json as json_comp
        key_cache_ant = f"ia:raw_tx:{usuario_id}:YTD_{anio_ant}"
        cache_comp = CacheRedisComp()
        cached_ant = cache_comp.obtener(key_cache_ant)
        if cached_ant:
            datos_raw_ant = json_comp.loads(cached_ant)
            logger.info(f"[DASHBOARD-GRAFICOS] Cache HIT comparativa año anterior ({anio_ant})")
        else:
            ytd_inicio_ant = datetime(anio_ant, 1, 1)
            ytd_fin_ant = datetime(anio_ant, 12, 31, 23, 59, 59)
            cliente_fin = obtener_cliente_financiero()
            resp_ant = await cliente_fin.obtener_historial_transacciones_async(
                usuario_id=usuario_id, token=token, tamanio=10000,
                desde_exacto=ytd_inicio_ant.isoformat(), hasta_exacto=ytd_fin_ant.isoformat()
            )
            datos_raw_ant = resp_ant.get("datos", []) if resp_ant else []
            try:
                cache_comp.guardar(key_cache_ant, json_comp.dumps(datos_raw_ant), ex=86400)
                logger.info(f"[DASHBOARD-GRAFICOS] Año anterior ({anio_ant}) cacheado por 24h.")
            except Exception:
                pass
        df_ant = json_a_dataframe(datos_raw_ant)
    except Exception as e:
        logger.warning(f"[DASHBOARD-GRAFICOS] No se pudo cargar historial año anterior: {e}")

    for m in range(1, hoy.month + 1):
        nombre_mes = NOMBRES_MESES[m]
        gas_actual = 0.0
        gas_anterior = 0.0
        if not df.empty and 'anio' in df.columns:
            df_m_act = df[(df['anio'] == anio_actual) & (df['mes'] == m)]
            gas_actual = float(df_m_act[(df_m_act['tipo'] == 'GASTO') & (df_m_act['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
        if not df_ant.empty and 'anio' in df_ant.columns:
            df_m_ant = df_ant[(df_ant['anio'] == anio_ant) & (df_ant['mes'] == m)]
            gas_anterior = float(df_m_ant[(df_m_ant['tipo'] == 'GASTO') & (df_m_ant['estado'].astype(str).str.upper() != 'FAILED')]['monto'].sum())
        comparativa.append({"mes": nombre_mes, "actual": round(gas_actual, 2), "anterior": round(gas_anterior, 2)})

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
