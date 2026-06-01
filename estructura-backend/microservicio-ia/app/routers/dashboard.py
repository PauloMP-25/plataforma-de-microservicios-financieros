import calendar
from datetime import datetime
import json
import logging
from typing import Optional, List, Dict, Any

from fastapi import APIRouter, Request, Depends, Security
from pydantic import BaseModel, Field

from app.libreria_comun.seguridad.validador_jwt import validar_token, obtener_usuario_id
from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.clientes.luka_clients import obtener_cliente_financiero
from app.persistencia.cache_redis import CacheRedis
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


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/kpis", response_model=ResultadoApi[RespuestaKPIs])
async def get_dashboard_kpis(
    request: Request,
    payload: dict = Security(validar_token),
):
    """
    Obtiene y calcula dinámicamente los KPIs principales y las últimas 10 transacciones.
    Almacena el resultado en Redis (TTL 15 min) antes de responder.
    """
    usuario_id = obtener_usuario_id(payload)
    
    # Extraer el token crudo de los cabeceras de request
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    cache = CacheRedis()
    key_resumen = f"dashboard:resumen:{usuario_id}"
    
    # Intentar obtener de caché (si por alguna razón se llama directamente, aunque Gateway lee primero)
    cached_data = cache.obtener(key_resumen)
    if cached_data:
        try:
            parsed = json.loads(cached_data)
            logger.info(f"[DASHBOARD-IA] Cache HIT para resumen de usuario_id={usuario_id}")
            return ResultadoApi.exito_res(datos=parsed, mensaje="KPIs recuperados de la caché.", ruta=request.url.path)
        except Exception as e:
            logger.error(f"[DASHBOARD-IA] Error parseando cache de resumen: {e}")

    logger.info(f"[DASHBOARD-IA] Cache MISS para resumen de usuario_id={usuario_id}. Calculando...")

    # Consultar transacciones de ms-nucleo-financiero
    cliente = obtener_cliente_financiero()
    try:
        resp = await cliente.obtener_historial_transacciones_async(usuario_id, token, tamanio=200)
        datos_raw = resp.get("datos", []) if resp else []
    except Exception as e:
        logger.error(f"[DASHBOARD-IA] Error consultando transacciones: {e}")
        datos_raw = []

    if not datos_raw:
        logger.info(f"[DASHBOARD-IA] Cargando datos fallback mockups en KPIs para usuario_id={usuario_id}")
        datos_raw = MOCK_TRANSACCIONES

    df = json_a_dataframe(datos_raw)

    # Filtrar transacciones y calcular métricas
    if df.empty:
        total_ing = 0.0
        total_gas = 0.0
        balance = 0.0
        cant_ing = 0
        cant_gas = 0
        tasa_ahorro = 0.0
        recientes = []
        desde_str = datetime.now().isoformat()
        hasta_str = datetime.now().isoformat()
    else:
        df_ing = df[(df['tipo'] == 'INGRESO') & (df['estado'].astype(str).str.upper() == 'COMPLETED')]
        df_gas = df[(df['tipo'] == 'GASTO') & (df['estado'].astype(str).str.upper() != 'FAILED')]

        total_ing = float(df_ing['monto'].sum())
        total_gas = float(df_gas['monto'].sum())
        balance = total_ing - total_gas
        cant_ing = len(df_ing)
        cant_gas = len(df_gas)
        tasa_ahorro = ((total_ing - total_gas) / total_ing) * 100 if total_ing > 0 else 0.0

        # Sort original raw records by transaction date descending for the recent table
        try:
            datos_raw.sort(key=lambda x: x.get("fechaTransaccion", ""), reverse=True)
        except Exception:
            pass
        recientes = datos_raw[:10]

        # Fechas extremas
        if 'fecha_transaccion' in df.columns:
            desde_str = df['fecha_transaccion'].min().isoformat()
            hasta_str = df['fecha_transaccion'].max().isoformat()
        else:
            desde_str = datetime.now().isoformat()
            hasta_str = datetime.now().isoformat()

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

    # Guardar en Redis (TTL 15 min / 900 segundos)
    try:
        cache.guardar(key_resumen, json.dumps(kpis), ex=900)
        logger.info(f"[DASHBOARD-IA] Resumen cacheado en Redis para usuario_id={usuario_id}")
    except Exception as e:
        logger.warning(f"[DASHBOARD-IA] No se pudo guardar resumen en Redis: {e}")

    return ResultadoApi.exito_res(datos=kpis, mensaje="KPIs calculados con éxito.", ruta=request.url.path)


@router.get("/graficos", response_model=ResultadoApi[RespuestaGraficos])
async def get_dashboard_graficos(
    request: Request,
    payload: dict = Security(validar_token),
):
    """
    Obtiene y calcula dinámicamente los datos de gráficos SVG (flujo de caja e ingresos/egresos).
    Almacena el resultado en Redis (TTL 15 min) antes de responder.
    """
    usuario_id = obtener_usuario_id(payload)
    
    auth_header = request.headers.get("Authorization", "")
    token = auth_header.replace("Bearer ", "") if auth_header.startswith("Bearer ") else ""

    cache = CacheRedis()
    key_graficos = f"dashboard:graficos:{usuario_id}"
    
    cached_data = cache.obtener(key_graficos)
    if cached_data:
        try:
            parsed = json.loads(cached_data)
            logger.info(f"[DASHBOARD-IA] Cache HIT para graficos de usuario_id={usuario_id}")
            return ResultadoApi.exito_res(datos=parsed, mensaje="Gráficos recuperados de la caché.", ruta=request.url.path)
        except Exception as e:
            logger.error(f"[DASHBOARD-IA] Error parseando cache de gráficos: {e}")

    logger.info(f"[DASHBOARD-IA] Cache MISS para graficos de usuario_id={usuario_id}. Calculando...")

    # Consultar transacciones
    cliente = obtener_cliente_financiero()
    try:
        resp = await cliente.obtener_historial_transacciones_async(usuario_id, token, tamanio=200)
        datos_raw = resp.get("datos", []) if resp else []
    except Exception as e:
        logger.error(f"[DASHBOARD-IA] Error consultando transacciones para gráficos (usando fallback mockups): {e}")
        datos_raw = []

    if not datos_raw:
        logger.info(f"[DASHBOARD-IA] Cargando datos fallback mockups en Gráficos para usuario_id={usuario_id}")
        datos_raw = MOCK_TRANSACCIONES

    df = json_a_dataframe(datos_raw)

    # 1. Flujo de Caja (últimos 5 meses)
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

    # 2. Distribución de Gastos
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

    # Guardar en Redis (TTL 15 min / 900 segundos)
    try:
        cache.guardar(key_graficos, json.dumps(graficos), ex=900)
        logger.info(f"[DASHBOARD-IA] Gráficos cacheados en Redis para usuario_id={usuario_id}")
    except Exception as e:
        logger.warning(f"[DASHBOARD-IA] No se pudo guardar gráficos en Redis: {e}")

    return ResultadoApi.exito_res(datos=graficos, mensaje="Gráficos calculados con éxito.", ruta=request.url.path)
