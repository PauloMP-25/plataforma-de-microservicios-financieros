# Refactorización Pendiente — Microservicio IA (LUKA)

> Documento de seguimiento técnico. Registra la arquitectura canónica de 3 fases
> y los módulos que aún no la siguen completamente.
> **Responsable:** Paulo Moron (Cloud Architect / Project Lead)

---

## Arquitectura Canónica: Las 3 Fases del Ciclo de Consulta IA

Esta es la arquitectura correcta y definitiva que deben seguir **todos** los módulos de análisis del `microservicio-ia`. Se toma como referencia de diseño para toda nueva implementación y para la refactorización de los módulos heredados.

---

### FASE 1 — Recolección de Contexto

El usuario solicita una consulta al módulo respectivo. El `ServicioAnalisis` (orquestador) se encarga de:
1. Obtener el **Contexto Personal** del usuario (tono de IA, ingresos declarados, ocupación, nombre, metas activas) desde el cliente `ms-nucleo-financiero`.
2. Recuperar las **Transacciones Financieras** del usuario en el rango de fechas solicitado.
3. Consultar el **Historial de Coaching previo** del módulo en `ia_historial_coaching` para inyectar memoria a la sesión.
4. Ensamblar todo en un `DataFrame` de Pandas listo para ser analizado.

**Clave:** En esta fase no se llama a Gemini. Solo se buscan datos.

---

### FASE 2 — Motor Analítico (Pandas / NumPy)

El módulo específico (por ejemplo `DeteccionGastosHormigaService`) recibe el `DataFrame` y ejecuta `ejecutar_calculos()`. Este motor:
- Aplica estadísticas descriptivas, segmentación, tendencias y comparaciones usando **Pandas y NumPy**.
- Produce un diccionario de **KPIs, Hallazgos e Insights** cuantitativos ya calculados: montos exactos, porcentajes, rankings, proyecciones matemáticas, etc.
- **No describe ni narra nada.** Solo calcula números.

**Clave:** En esta fase tampoco se llama a Gemini. Pandas hace todo el trabajo matemático.

---

### FASE 3 — Coach IA (Gemini — Solo narrativa)

El orquestador toma **únicamente los KPIs y Hallazgos más relevantes** producidos en la Fase 2 (no el DataFrame completo ni datos crudos), y los pasa a Gemini a través del `prompt` del módulo respectivo. El rol de Gemini es estrictamente:
- Actuar como **coach financiero**, no como calculador.
- Leer los datos ya procesados y transformarlos en una **narrativa empática, motivadora y personalizada**.
- Devolver una **Respuesta Estructurada** (Pydantic `BaseModel` via `response_schema`) que el frontend pueda renderizar directamente sin parsear Markdown.

**Gemini NO debe:**
- Recibir transacciones crudas o DataFrames completos.
- Recalcular, inferir o inventar cifras no calculadas por Pandas.
- Recibir instrucciones redundantes de formato (el esquema Pydantic ya lo garantiza).

**Clave:** Gemini es el narrador. Pandas es el analista. Ambos tienen roles separados e irremplazables.

---

## Regla de Oro para Prompts

> **Solo los datos significativos del módulo (KPIs y Hallazgos calculados por Pandas) deben pasarse al prompt de Gemini.**

Esto garantiza:
- **Eficiencia de tokens**: Menos tokens de entrada = menor costo por consulta = mayor margen de rentabilidad por suscripción (plan de S/ 15).
- **Precisión matemática**: Los números son exactos porque Pandas los calculó, no Gemini.
- **Respuestas coherentes**: Gemini no puede "alucinar" un número que nunca le dimos.

---

## Estado de Cumplimiento por Módulo

| Módulo | Fase 1 ✅ | Fase 2 (Pandas) ✅ | Fase 3 (Solo KPIs a Gemini) | Structured Output | Fallback Propio | Persistencia| Técnicas Prompt | Integración Frontend | Responsable |
|---|---|---|---|---|---|---|---|---|---|
| `GASTO_HORMIGA` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoHormiga` | ✅ | ✅ | ✅ | ✅ | Paulo |
| `PREDECIR_GASTOS` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoPredecir` | ✅ | ✅ | ✅ | ✅ | Paulo |
| `HABITOS_FINANCIEROS` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoHabitos` | ✅ | ✅ | ✅ | ✅ | Paulo |
| `SIMULAR_META` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoSimularMeta` | ✅ | ✅ | ✅ | ✅ | Paulo |
| `RETO_AHORRO_DINAMICO` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoReto` | ✅ | ✅ | ✅ | ✅ | Paulo |
| `REPORTE_COMPLETO` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoReporte` | ✅ Fallback Propio | ✅ Tabla propia | ✅ Aplicadas | ✅ | Paulo |
| `ANALISIS_ESTILO_VIDA` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoEstilo` | ✅ Fallback Propio | ✅ Tabla propia | ✅ Aplicadas | ✅ | Paulo |
| `AUTO_CLASIFICACION` | ✅ | N/A | ✅ Zero History | ✅ `ConsejoEstructuradoAutoClasificacion` | ✅ Fallback Propio | ✅ | ✅ Aplicadas | ✅ | Paulo |
| `COMPROBADOR_EVOLUCION` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoEvolucion` | ✅ | ✅ | ✅ Aplicadas | ✅ | Paulo |
| `ZONA_ENTRENAMIENTO` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoEntrenamiento` | ✅ | ✅ | ✅ Aplicadas | ✅ | Paulo |
| `ESPEJO_TEMPORAL` | ✅ | ✅ | ✅ Solo KPIs | ✅ `ConsejoEstructuradoEspejo` | ✅ | ✅ | ✅ Aplicadas | ✅ | Paulo |

**Leyenda:** ✅ Implementado | ⚠️ Pendiente | ❌ No implementado | 🔄 En progreso | N/A No aplica

---

## Tareas de Refactorización Pendientes (Backlog)

### Alta Prioridad (Impacto en Costos)
- **Auditar todos los prompts** (`prompt_habitos_financieros.py`, `prompt_reto_ahorro.py`, `prompt_reporte_completo.py`, `prompt_estilo_vida.py`) para verificar que **solo inyectan KPIs resumidos** y no datos crudos ni instrucciones de formato redundantes.
- **Migrar módulos a Structured Output** (los 3 que aún devuelven `String plano`): `HABITOS_FINANCIEROS`, `RETO_AHORRO_DINAMICO`, `REPORTE_COMPLETO`, `ANALISIS_ESTILO_VIDA`. Cada uno necesita:
  1. Su propio `ConsejoEstructurado[NombreModulo]` en `esquemas.py`.
  2. Su propia clase que implemente `obtener_esquema_salida()`.
  3. Su propio `fallback_[modulo].py` que devuelva el mismo dict estructurado.

### Media Prioridad (Calidad)
- **Agregar `ConsejoEstructurado` a `AUTO_CLASIFICACION`**: Actualmente la clasificación devuelve una lista de strings. Podría encapsularse en un schema Pydantic.
- **Revisar que los `ejecutar_calculos()` de cada módulo** no envíen datos innecesarios en el dict de métricas que llega al prompt (limpiar claves con prefijo `_` que son solo para historial).

### Baja Prioridad (Deuda técnica)
- Añadir índice compuesto `(usuario_id, modulo, fecha_generacion)` en `ia_historial_coaching` para optimizar consultas de historial cuando la tabla crezca.
- Agregar `__init__.py` a las carpetas `prompts/` y `fallbacks/` para hacerlas paquetes explícitos.

---

## PRUEBAS DE RESPUESTA DE GEMINI POR MODULO

| Módulo | Fecha de Prueba | ¿Exitoso? | Estado |
|---|---|---|---|
| `GASTO_HORMIGA` | 2026-06-15 | ✅ | Exitoso. Estructura unificada: sin `introduccion`, con saludo personalizado bajo `analisis_ia` y exactamente 5 pasos. |

### Respuesta de Gemini — GASTO_HORMIGA

```json
{
  "pensamiento_interno_ia": "He analizado los gastos hormiga de Cesar, notando una mejora en la reducción de fugas. Mi objetivo es ofrecer 5 pasos prácticos y conectar la fuga con su meta.",
  "analisis_ia": "¡Hola, Cesar! He estado revisando tus movimientos este mes. Tus gastos hormiga suman S/ 181.95, con el transporte siendo la categoría principal de fuga. ¡Pero hay buenas noticias! Has logrado disminuir tus gastos hormiga en un 11.2% respecto al mes anterior. Sin embargo, si esta tendencia de fuga se mantiene, podrías estar perdiendo S/ 2,183.40 al año.",
  "conexion_emocional": "Esos S/ 181.95 que se te escaparon este mes, y la proyección anual de S/ 2,183.40, podrían ser una parte de tus ahorros para tu 'Viaje a Japón'. ¡Imagina todo lo que harías con ese dinero allá!",
  "plan_accion_titulo": "Plan de Acción: ¡Ahorra en Transporte para Japón!",
  "plan_accion_pasos": [
    "Identifica rutas alternativas o considera caminar/bicicleta para trayectos cortos.",
    "Revisa tus gastos de transporte diarios y busca al menos una forma de optimizarlos esta semana (ej. usar transporte público en lugar de taxi).",
    "Establece un presupuesto semanal específico para transporte y monitorea que no lo excedas.",
    "Organiza viajes compartidos con compañeros que tengan rutas similares para dividir los costos.",
    "Camina en los tramos cortos de menos de 10 cuadras; además de ahorrar, beneficiará tu salud diaria."
  ],
  "comentario_positivo": "¡Sigue así, Cesar! Cada pequeño ajuste te acerca más a tu increíble viaje a Japón. ¡Estoy aquí para apoyarte en cada paso!"
}
```
