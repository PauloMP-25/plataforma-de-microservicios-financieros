# Microservicio IA Financiera
**Puerto:** `8086` | **Framework:** FastAPI | **Lenguaje:** Python 3.11+

---

## Estado del Proyecto — Abril 2026

### Primera versión completa
**Fecha:** 19 de Abril, 2026
**Responsable:** Paulo Cesar Moron Poma

---

## Responsabilidad

Actúa como el **cerebro analítico** del sistema SaaS Financiero.
Consume datos del `microservicio-nucleo-financiero` (Puerto 8085) y ejecuta
10 módulos de análisis inteligente con Python, Pandas y Scikit-Learn.

---

## Arquitectura

```
Frontend / API Gateway
       │
       ▼
POST /api/v1/ia/{modulo}
       │  usuario_id + parámetros opcionales
       │
       ├─► ClienteNucleoFinanciero → GET /api/v1/financiero/transacciones/historial
       │     └── Convierte JSON → DataFrame de Pandas
       │
       ├─► MotorIA.{modulo}(df) → Ejecuta análisis
       │
       ├─► ClienteAuditoria → POST /api/v1/auditoria/registrar (async, no bloqueante)
       │
       └─► Retorna RespuestaDTO en JSON
```

---

## 10 Módulos de Análisis

| # | Endpoint | Módulo | Tecnología |
|---|----------|--------|------------|
| 1 | `POST /clasificar` | `clasificar_transaccion_automatica` | Keyword Matching + Lógica Difusa |
| 2 | `POST /predecir-gastos` | `predecir_gastos_proximo_mes` | Regresión Lineal / Media Móvil (Scikit-Learn) |
| 3 | `POST /detectar-anomalias` | `detectar_anomalias_financieras` | Z-Score (NumPy/SciPy) |
| 4 | `POST /optimizar-suscripciones` | `optimizar_suscripciones` | Pattern Matching + Agrupación Pandas |
| 5 | `POST /capacidad-ahorro` | `calcular_capacidad_ahorro` | Regla 50/30/20 + Factor de Seguridad |
| 6 | `POST /simular-meta` | `simular_metas_financieras` | Proyección Temporal |
| 7 | `POST /estacionalidad` | `analizar_estacionalidad` | Agrupación Mensual + CV |
| 8 | `POST /presupuesto-dinamico` | `recomendar_presupuesto_dinamico` | Presupuesto Dinámico Semanal |
| 9 | `POST /simular-escenario` | `simular_escenario_que_pasaria_si` | Análisis de Impacto |
| 10 | `POST /reporte-completo` | `generar_reporte_lenguaje_natural` | CFO Virtual — Narrativa Inteligente |
| `POST /analisis-completo` | Todos los módulos | Dashboard completo |

---

## Instalación y Ejecución

### 1. Crear entorno virtual
```bash
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate
```

### 2. Instalar dependencias
```bash
pip install -r requirements.txt
```

### 3. Configurar variables de entorno
Editar `.env` con las URLs de los microservicios Java:
```properties
URL_NUCLEO_FINANCIERO=http://localhost:8085
URL_AUDITORIA=http://localhost:8082
```

### 4. Ejecutar el servidor
```bash
# Modo desarrollo (recarga automática)
uvicorn main:app --reload --port 8086

# Modo producción
uvicorn main:app --host 0.0.0.0 --port 8086 --workers 2
```

### 5. Acceder a la documentación
- **Swagger UI:** http://localhost:8086/docs
- **ReDoc:** http://localhost:8086/redoc
- **Health Check:** http://localhost:8086/actuator/health

---

## Ejemplo de Uso

### POST /api/v1/ia/reporte-completo
```json
{
  "usuario_id": "550e8400-e29b-41d4-a716-446655440000",
  "mes": 4,
  "anio": 2026,
  "tamanio_pagina": 200
}
```

### Respuesta
```json
{
  "usuario_id": "550e8400-...",
  "fecha_generacion": "2026-04-19 10:30:00",
  "resumen_ejecutivo": "Durante abril 2026, registraste 45 transacciones...",
  "kpis": {
    "total_ingresos": 5000.00,
    "total_gastos": 3200.00,
    "balance_neto": 1800.00,
    "capacidad_ahorro": 1530.00,
    "porcentaje_ahorro": 30.6
  },
  "puntaje_salud_financiera": 85.0,
  "clasificacion_salud": "Excelente 🟢"
}
```

---

## Dependencias con otros microservicios

| Servicio | Puerto | Comunicación | Uso |
|----------|--------|--------------|-----|
| microservicio-nucleo-financiero | 8085 | HTTP GET síncrono | Fuente de transacciones |
| microservicio-auditoria | 8082 | HTTP POST asíncrono | Registro de eventos IA |
| microservicio-pago | 8087 | HTTP GET síncrono | Validación de estado de suscripción |

---

## Estructura del Proyecto

```
microservicio-ia-financiera/
├── main.py                          # Punto de entrada FastAPI
├── requirements.txt                 # Dependencias Python
├── .env                             # Variables de entorno
└── app/
    ├── configuracion.py             # Pydantic Settings
    ├── modelos/
    │   └── esquemas.py              # DTOs Pydantic (entrada/salida)
    ├── clientes/
    │   └── cliente_financiero.py    # HTTP clients (httpx)
    ├── utilidades/
    │   └── preparador_datos.py      # JSON → DataFrame helpers
    ├── servicios/
    │   └── motor_ia.py              # 10 módulos de análisis IA
    └── routers/
        └── analisis.py              # Endpoints FastAPI
```

---

## Hitos implementados

- [x] Arquitectura Limpia (Configuración / Clientes / Utilidades / Servicios / Routers)
- [x] 10 módulos de análisis IA con Pandas + Scikit-Learn
- [x] Cliente HTTP robusto con manejo de errores para ms-financiero
- [x] Auditoría no bloqueante hacia ms-auditoria
- [x] Documentación automática Swagger/OpenAPI en `/docs`
- [x] Health check en `/actuator/health`
- [x] Variables de entorno con Pydantic Settings
- [x] Endpoint `/analisis-completo` que ejecuta todos los módulos

## Próximos pasos

- [ ] Agregar caché Redis para no reconsultar datos frecuentes
- [ ] Integrar modelo NLP real (spaCy) para clasificación más precisa
- [ ] Agregar JWT validation para endpoints protegidos
- [ ] Dockerizar el microservicio
- [ ] Tests unitarios con pytest para cada módulo IA