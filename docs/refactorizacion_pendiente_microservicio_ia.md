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

| Módulo | Fase 1 ✅ | Fase 2 (Pandas) ✅ | Fase 3 (Solo KPIs a Gemini) | Structured Output | Fallback Propio | Persistencia| Técnicas Prompt | Integración Frontend |
|---|---|---|---|---|---|---|---|---|---|
| `GASTO_HORMIGA` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoHormiga` | ✅ | ✅ | ✅ | ✅ |
| `PREDECIR_GASTOS` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoPredecir` | ✅ | ✅ | ✅ | ✅ |
| `HABITOS_FINANCIEROS` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoHabitos` | ✅ | ✅ | ✅ | ✅ |
| `SIMULAR_META` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoSimularMeta` | ✅ | ✅ | ✅ | ✅ |
| `RETO_AHORRO_DINAMICO` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoReto` | ✅ | ✅ | ✅ | ✅ |
| `REPORTE_COMPLETO` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoReporte` | ✅ | ✅ | ✅ | ✅ |
| `ANALISIS_ESTILO_VIDA` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoEstilo` | ✅ | ✅ | ✅ | ✅ |
| `AUTO_CLASIFICACION` | ✅ | N/A | ✅ Zero History| ✅ `ConsejoEstructuradoAutoClasificacion` | ✅ | ✅ | ✅ | ✅ |
| `COMPROBADOR_EVOLUCION` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoEvolucion` | ✅ | ✅ | ✅ | ✅ |
| `ZONA_ENTRENAMIENTO` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoEntrenamiento` | ✅ | ✅ | ✅ | ✅ |
| `ESPEJO_TEMPORAL` | ✅ | ✅ | ✅ | ✅ `ConsejoEstructuradoEspejo` | ✅ | ✅ | ✅ | ✅ |

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
| `HABITOS_FINANCIEROS` | 2026-06-15 | ✅ | Exitoso. Estructura refactorizada: con `score_salud_habitos`, `etiquetas_internas`, `nota_interna_coach` y `analisis_patron` que incluye saludo inicial. |
| `ANALISIS_ESTILO_VIDA` | 2026-06-16 | ✅ | Exitoso. Schema fix aplicado para Gemini. Saludo único en `descripcion_perfil`. Se integraron campos de State Tracking. |
| `PREDECIR_GASTOS` | 2026-06-16 | ✅ | Exitoso. Se eliminó la introducción redundante y se aplicó State Tracking en backend. Testeado con Fallback. |
| `SIMULAR_META` | 2026-06-16 | ✅ | Exitoso. Se eliminó la introducción redundante y se aplicó State Tracking en backend. Testeado con Fallback. Bug de tamanio_pagina resuelto. |
| `RETO_AHORRO_DINAMICO` | 2026-06-16 | ✅ | Exitoso. Se eliminó la introducción redundante y se aplicó State Tracking en backend. Testeado con éxito. |
| `REPORTE_COMPLETO` | 2026-06-16 | ✅ | Exitoso. Se eliminó la introducción redundante y se aplicó State Tracking en backend. Testeado con éxito. |
| `COMPROBADOR_EVOLUCION` | 2026-06-16 | ✅ | Exitoso. Estructura refactorizada: con `score_salud_evolucion`, `etiquetas_internas`, `nota_interna_coach` y `veredicto_narrativo` (sin introducción). |
| `ESPEJO_TEMPORAL` | 2026-06-16 | ✅ | Exitoso. Estructura refactorizada: con `score_salud_espejo`, `etiquetas_internas`, `nota_interna_coach` y cartas en prosa. |
| `ZONA_ENTRENAMIENTO` | 2026-06-16 | ✅ | Exitoso. Estructura refactorizada: con `score_salud_entrenamiento`, `etiquetas_internas`, `nota_interna_coach` y rutina cyberpunk. |

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

### Respuesta de Gemini — HABITOS_FINANCIEROS

```json
{
  "pensamiento_interno_ia": "Fallback activado. Generando respuesta basada en reglas predefinidas y estadísticas de Pandas.",
  "score_salud_habitos": 5,
  "etiquetas_internas": [
    "fallback",
    "riesgo_medio"
  ],
  "nota_interna_coach": "Revisar hábitos de la categoría dominante en la próxima sesión.",
  "analisis_patron": "Hola cesar paulo, aquí tienes un resumen rápido. Observamos que tu mayor actividad de gastos es el día Monday, enfocada en la categoría 'Alimentación'.",
  "habito_atomico_sugerido": "Asigna un límite fijo semanal para esa categoría y transfiere el excedente a tu cuenta de ahorro al inicio de la semana.",
  "mensaje_motivacional": "¡Vas por buen camino! Sigue manteniendo ingresos superiores a tus gastos."
}
```

### Respuesta de Gemini — ANALISIS_ESTILO_VIDA

```json
{
  "pensamiento_interno_ia": "Se detecta un fuerte enfoque en el cluster EXPLORER (15.9%) y un interés secundario en WELLNESS (5.2%). Esto sugiere un perfil que valora las experiencias y el autocuidado. Es importante monitorear si el gasto en EXPLORER es un facilitador o un obstáculo para la meta de 'viaje a japon'.",
  "score_salud_estilo": 6,
  "etiquetas_internas": [
    "Viajero",
    "Aventurero",
    "Bienestar"
  ],
  "nota_interna_coach": "Revisar equilibrio entre gastos de exploración actuales y ahorro para Japón.",
  "arquetipo": "El Explorador Consciente",
  "significado_arquetipo": "Significa que te encanta descubrir nuevos lugares y experiencias, pero también valoras mucho tu bienestar y cuidado personal. Buscas un equilibrio entre la aventura y el autocuidado.",
  "descripcion_perfil": "¡Hola, Cesar! Qué gusto verte por aquí. Analizando tus movimientos, veo que eres una persona que valora mucho las experiencias y el bienestar. Tus gastos muestran una clara inclinación a explorar el mundo y a invertir en tu cuidado personal, lo cual es genial para una vida plena.",
  "consejo_tactico": "Para seguir explorando sin afectar tu meta, ¿qué tal si buscas aventuras más cercanas o escapadas de fin de semana que no requieran grandes inversiones? También puedes explorar opciones de bienestar al aire libre o actividades gratuitas que te recarguen.",
  "alineacion_meta": "Tu estilo de vida explorador está muy alineado con tu meta de viajar a Japón, ¡es la esencia de lo que te mueve! Sin embargo, es clave que tus exploraciones actuales no compitan demasiado con el ahorro para ese gran viaje. Cada pequeña aventura de hoy puede ser un paso hacia la gran aventura de mañana.",
  "mensaje_estilo_vida": "Sigue explorando el mundo y cuidando de ti, Cesar. Cada paso consciente te acerca a tu próxima gran aventura."
}
```

### Respuesta de Gemini — PREDECIR_GASTOS

```json
{
  "pensamiento_interno_ia": "El riesgo de insolvencia es extremadamente alto. Con un ingreso de S/ 3,500.00 y gastos proyectados de S/ 17,427.48, el déficit de S/ 13,927.48 es insostenible. La tendencia de gastos está completamente desalineada con los ingresos, requiriendo una intervención drástica.",
  "analisis_tendencia": "¡Hola, Cesar! LUKA aquí, tu estratega financiero. Mira, la proyección para el próximo mes nos muestra un panorama que necesita nuestra atención. Tus gastos proyectados se disparan a S/ 17,427.48, lo que representa un aumento del 84.7% mensual.",
  "impacto_meta": "Con esta tendencia, tu emocionante viaje a Japón, que ya lleva un 15% de progreso, se ve seriamente comprometido. Un déficit proyectado de S/ 13,927.48 significa que no solo no podrás ahorrar para tu meta, sino que estarías gastando mucho más de lo que ingresas, alejándote de ese sueño.",
  "recomendacion_matematica": "Para evitar el déficit proyectado y al menos igualar tus ingresos, necesitas reducir tus gastos proyectados en al menos S/ 13,927.48, lo que representa aproximadamente un 80% de tus gastos actuales proyectados.",
  "mensaje_motivacional": "Sé que suena fuerte, pero estoy aquí para ayudarte a retomar el control y redirigir tu camino hacia tus metas. ¡Juntos podemos hacerlo!"
}
```

### Respuesta de Gemini — SIMULAR_META

```json
{
  "pensamiento_interno_ia": "El monto faltante para la Laptop Gamer es S/ 5000.0. La capacidad de ahorro mensual detectada es S/ 14145.66. Dado que la capacidad de ahorro excede significativamente el monto faltante, la meta es altamente viable y se puede alcanzar en menos de un mes (0.4 meses).",
  "diagnostico_viabilidad": "¡Hola, Cesar! Soy LUKA y tengo excelentes noticias para ti. ¡Tu meta de la Laptop Gamer es completamente viable! Con tu capacidad de ahorro actual, podrás alcanzarla en un abrir y cerrar de ojos.",
  "plan_accion": "Para alcanzar tu Laptop Gamer, simplemente mantén tu ritmo de ahorro actual. Te sugiero destinar S/ 5000.0 de tu capacidad de ahorro mensual directamente a esta meta. ¡Lo lograrás muy pronto!",
  "tecnica_sugerida": "Ahorro Automático: Configura una transferencia automática desde tu cuenta principal a una cuenta de ahorro específica para tu Laptop Gamer tan pronto recibas tu ingreso. Así, el ahorro se convierte en una prioridad y no en lo que 'sobra'.",
  "mensaje_motivacional": "¡Sigue así, Cesar! Tu disciplina financiera te está llevando a cumplir tus sueños más rápido de lo que imaginas. ¡Estoy emocionado por ver esa nueva laptop en tus manos!"
}
```

### Respuesta de Gemini — RETO_AHORRO_DINAMICO

```json
{
  "pensamiento_interno_ia": "El usuario Cesar tiene un ingreso de S/ 3,500.00 y una meta activa de 'viaje a japon' con 15% de progreso. El hallazgo indica un ahorro potencial de S/ 575.23 semanal en la categoría de alimentación. Este monto es extremadamente significativo y representa una gran oportunidad para acelerar el progreso de su meta. Si logra este ahorro semanal, podría acumular S/ 2,300.92 al mes, lo que es casi el 66% de su ingreso mensual. Esto sugiere que su gasto actual en alimentación es muy elevado y que hay un margen considerable para optimización. La viabilidad del reto dependerá de su disciplina para ajustar hábitos de consumo de alimentos.",
  "titulo_mision": "Sabor a Japón: Ahorro Inteligente",
  "diagnostico": "¡Hola, Cesar! Hemos notado que tu categoría de alimentación es un área con un gran potencial de ahorro. Al optimizar tus gastos aquí, no solo cuidarás tu bolsillo, sino que también acelerarás tu camino hacia ese increíble viaje a Japón. Cada sol que ahorres en comida te acerca más a probar el auténtico ramen en Tokio.",
  "estrategia": "Para lograrlo, te propongo dos reglas de oro: Primero, establece un presupuesto semanal fijo para alimentación y ¡no te salgas de él! Segundo, intenta preparar tus comidas en casa la mayor parte de la semana. Llevar tu almuerzo al trabajo o cocinar en casa puede hacer una gran diferencia.",
  "mensaje_motivacional": "¡Vamos, Cesar! Tienes el potencial para hacer de este reto una realidad. ¡Imagina esos ahorros transformándose en experiencias inolvidables en Japón! ¡Tú puedes con esto!"
}
```

### Respuesta de Gemini — REPORTE_COMPLETO

```json
{
  "pensamiento_interno_ia": "El usuario, Cesar, tiene un perfil de ingeniero de sistemas con un ingreso mensual de S/ 3,500.00. Su meta activa es 'viaje a japon' con un progreso del 15%. Los hallazgos muestran un Score de Salud de 100/100, un Balance Acumulado de S/ 14145.66 y la categoría más impactante es Alimentación (32.1% del gasto total). Mi análisis paso a paso es el siguiente: 1. El Score de Salud de 100/100 es excepcional, indicando una salud financiera 'Excelente'. 2. El Balance Acumulado de S/ 14145.66 is muy positivo y representa un ahorro significativo, lo cual es excelente para su meta. 3. La categoría de Alimentación es la más impactante, lo que sugiere un área potencial para optimización, a pesar del excelente desempeño general. 4. La meta de 'viaje a japon' se beneficia enormemente del balance acumulado, y cualquier ajuste en Alimentación podría acelerar su progreso. 5. El tono debe ser amigable y directo, como LUKA el Auditor Estratégico.",
  "analisis_score": "¡Cesar, tu Score de Salud de 100/100 es simplemente excelente! Esto significa que tus finanzas están en una posición muy sólida, demostrando una gestión impecable y un gran control sobre tus recursos.",
  "impacto_meta": "¡Qué buen balance, Cesar! Con S/ 14145.66 acumulados, estás construyendo una base fantástica para tu 'viaje a japon'. Este ahorro te acerca muchísimo a tu meta. Sin embargo, la categoría de Alimentación, siendo el 32.1% de tus gastos, es un punto clave. Si logras optimizar un poco ahí, podrías acelerar aún más ese viaje soñado. ¡Imagina cuánto más rápido podrías llegar a Japón con esos ahorros extra!",
  "veredicto_final": "Cesar, lo que va del año 2026 ha sido simplemente espectacular para tus finanzas. Tu Score de Salud perfecto y un balance acumulado de más de S/ 14,000 demuestran una gestión financiera ejemplar. Estás ahorrando de manera muy efectiva y construyendo un futuro sólido. Si bien la alimentación representa una parte importante de tus gastos, tu rendimiento general es de admirar y te posiciona de manera excelente para alcanzar tus objetivos.",
  "mensaje_motivacional": "¡Cesar, tu disciplina y esfuerzo están dando frutos increíbles! Sigue con esa energía y enfoque, porque cada decisión inteligente te acerca más a tus sueños. ¡El resto del año es tuyo para seguir brillando!"
}
```

### Respuesta de Gemini — COMPROBADOR_EVOLUCION

```json
{
  "pensamiento_interno_ia": "El usuario Cesar Paulo muestra un incremento notable en el IVG del periodo B debido a un alza del 221% en Transporte y 100% en Suscripciones. Debo proponer límites y acciones correctivas.",
  "veredicto_narrativo": "Cesar, tu Índice de Madurez Financiera muestra un retroceso debido a un incremento significativo de gastos en áreas no esenciales como Transporte y Suscripciones. Es prioritario estabilizar estos rubros para retomar tu senda de ahorro y garantizar la salud de tus finanzas.",
  "recetas_medicas": [
    {
      "categoria": "Transporte",
      "diagnostico": "Aumento del 221% en el uso de taxis y transporte privado.",
      "posologia": [
        "1. Planificar viajes semanales y priorizar el uso de transporte público o compartido.",
        "2. Establecer un presupuesto semanal estricto para transporte privado de máximo S/ 50.",
        "3. Caminar o usar bicicleta para trayectos menores a 10 cuadras."
      ],
      "pronostico": "Ahorro proyectado de S/ 1,211.48 en los próximos 3 meses."
    },
    {
      "categoria": "Suscripciones Streaming",
      "diagnostico": "Aumento del 100% en el gasto por duplicación de servicios activos.",
      "posologia": [
        "1. Listar todas las plataformas activas y evaluar el tiempo real de uso de cada una.",
        "2. Cancelar al menos una suscripción redundante o inactiva esta misma semana.",
        "3. Compartir cuentas familiares para dividir los costos de suscripción."
      ],
      "pronostico": "Ahorro proyectado de S/ 358.58 en los próximos 3 meses."
    }
  ]
}
```

### Respuesta de Gemini — ESPEJO_TEMPORAL

```json
{
  "pensamiento_interno_ia": "Proyección de futuro basada en un ahorro mensual de S/ 7,060.33 actual vs S/ 7,443.20 optimizado. El score actual es 42 y la tendencia indica una caída leve si no se recortan gastos no esenciales.",
  "cartaContinuidad": "Hola Cesar Paulo. Si decides mantener tus hábitos de gasto actuales durante los próximos 12 meses, tu score financiero descenderá a 21 puntos. Aunque logres un ahorro mensual acumulado de S/ 7,060.33, el peso de los gastos innecesarios limitará tu crecimiento. El camino se sentirá seguro, pero estarás dejando pasar oportunidades clave de inversión.",
  "cartaTransformacion": "Hola Cesar Paulo. Tomar la decisión de optimizar tus gastos no esenciales liberará un ahorro mensual de S/ 7,443.20. En 12 meses, esto representará un capital adicional acumulado de S/ 4,594.47 y tu score de salud financiera subirá a 39 puntos. Este esfuerzo transformará tu futuro, dándote la liquidez para concretar tus metas sin estrés."
}
```

### Respuesta de Gemini — ZONA_ENTRENAMIENTO

```json
{
  "pensamiento_interno_ia": "El usuario se encuentra en un estado financiero vulnerable ('UCI Financiera') debido a alta volatilidad. Propondré una rutina de entrenamiento intensivo de 3 pasos.",
  "estado_fisico": "UCI Financiera",
  "evaluacion_previa": "No se registran rutinas previas en el historial. Iniciamos periodo de adaptación.",
  "rutina": [
    {
      "nombre": "Cardio de Bolsillo",
      "descripcion": "Revisar detalladamente todos tus gastos menores a S/ 20 del mes pasado.",
      "duracion_dias": 30,
      "frecuencia": "1 vez por semana",
      "metrica_exito": "Identificar y registrar al menos 2 gastos totalmente innecesarios."
    },
    {
      "nombre": "Ayuno de Suscripciones",
      "descripcion": "Suspender temporalmente o cancelar un servicio de streaming que no hayas utilizado en los últimos 15 días.",
      "duracion_dias": 30,
      "frecuencia": "Única vez",
      "metrica_exito": "Una suscripción cancelada y confirmada en la app."
    },
    {
      "nombre": "Levantamiento de Ahorro",
      "descripcion": "Transferir de forma manual o automática S/ 10 a tu cuenta de ahorros al finalizar cada día.",
      "duracion_dias": 30,
      "frecuencia": "Diario",
      "metrica_exito": "Completar la transferencia diaria durante al menos 25 días del mes."
    }
  ]
}
```"
    },
    {
      "nombre": "Ayuno de Suscripciones",
      "descripcion": "Cancela 1 servicio de streaming que no hayas usado en 15 días.",
      "duracion_dias": 30,
      "frecuencia": "Única vez",
      "metrica_exito": "1 suscripción cancelada."
    },
    {
      "nombre": "Levantamiento de Ahorro",
      "descripcion": "Transfiere S/ 10 a tu cuenta de ahorros al final del día.",
      "duracion_dias": 30,
      "frecuencia": "Diario",
      "metrica_exito": "Transferencia completada."
    }
  ]
}
```
