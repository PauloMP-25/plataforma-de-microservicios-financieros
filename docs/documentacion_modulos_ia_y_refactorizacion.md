# Documentación de Módulos IA y Refactorización

## 1. El Comprobador de Evolución (La Sala de Radiología)
**"El Comprobador de Evolución"** no es simplemente un comparador de gastos, es la **"Sala de Radiología"** financiera de LUKA. 

Este módulo nace para solucionar un problema crítico: los usuarios universitarios suelen vivir atrapados en su presente financiero inmediato. Rara vez miran hacia atrás con criterio técnico, y cuando lo hacen, la comparación es manual, subjetiva y frustrante (por ejemplo, concluir vagamente que "gastaron más" sin saber en qué o por qué). Esta falta de visibilidad objetiva provoca parálisis motivacional; los usuarios abandonan sus buenos hábitos financieros porque no saben si su esfuerzo de los últimos meses realmente tuvo impacto.

Este módulo convierte el historial pasado del usuario en **evidencia médica irrefutable de evolución personal**, entregando la validación objetiva que el usuario necesita para continuar o corregir su camino, usando la potencia conjunta de FastAPI, Pandas y Google Gemini.

### Beneficios para el Usuario
1. **Validación Objetiva del Esfuerzo**: El estudiante deja de depender de su intuición. El sistema le dice matemáticamente si su disciplina ha mejorado.
2. **Identificación Quirúrgica de Problemas**: No le dice "gastaste de más", le dice *exactamente en qué categoría específica* su control ha empeorado (reincidencias o nuevos excesos).
3. **Celebración de Pequeñas Victorias**: Reconoce automáticamente las "Categorías Conquistadas", brindando el refuerzo positivo necesario para mantener la motivación.
4. **Claridad Diagnóstica (Cero Lenguaje Técnico)**: Transforma cálculos estadísticos complejos (como el Coeficiente de Variación) en un "diagnóstico médico" fácil de entender: *Estancamiento*, *Regresión*, *Evolución Incipiente*, etc.
5. **Plan de Acción Personalizado**: No entrega consejos genéricos, sino una "Receta Médica" de tres pasos concretos para sanar exactamente lo que falló en ese periodo.

### Arquitectura y KPIs Principales
El módulo compara rigurosamente dos periodos de tiempo (Periodo A - Pasado vs. Periodo B - Reciente) que no se solapan y duran al menos 15 días. Utiliza Pandas para calcular 5 KPIs fundamentales:
- **KPI 1 — Delta de Tasa de Ahorro (ΔTS)**: Diferencia absoluta y relativa en la capacidad de ahorro del usuario. El indicador primario de crecimiento patrimonial.
- **KPI 2 — Índice de Volatilidad del Gasto (IVG)**: Mide qué tan predecible o caótico es el comportamiento del usuario (Desviación estándar de egresos normalizada).
- **KPI 3 — Categorías Conquistadas**: Detecta caídas mayores al 10% en el gasto diario promedio de categorías específicas, categorizándolas en *Victorias Parciales*, *Sólidas* o *Dominadas*.
- **KPI 4 — Categorías en Alerta (Reincidentes)**: Identifica áreas donde el gasto subió más de un 10%, y cruza la información con análisis previos para detectar si es una recaída conocida o un problema nuevo.
- **KPI 5 — Índice de Madurez Financiera (IMF)**: Un *score* ponderado final (0-100) que combina los 4 KPIs anteriores. Google Gemini toma este score y los datos asociados para emitir un Veredicto Narrativo de la situación.

### Experiencia de Interfaz (La Clínica Med-Noir)
La interfaz en Angular adopta una estética **"Med-Noir"** de alta tecnología forense. El elemento central es un **Esqueleto Radiográfico SVG**. Los huesos representan las distintas categorías financieras del usuario.
- Las categorías conquistadas "sanan" y emiten un resplandor verde.
- Las categorías reincidentes se muestran "fracturadas" con un resplandor rojo parpadeante. 
- Al hacer clic en un hueso fracturado, se despliega una **Receta Médica Digital**.

### Cambios y Refactorización Realizada
- **Salida Estructurada**: Se implementó la salida `ConsejoEstructuradoEvolucion` para asegurar un formato JSON estricto proveniente de Gemini.
- **Prompt Desacoplado**: Se extrajo el prompt hacia `prompt_comprobador_evolucion.py`.
- **Fallback Independiente**: Se creó `fallback_comprobador_evolucion.py` que genera una receta médica de contingencia estadística pura usando Pandas si la IA falla.
- **Persistencia UUID**: Ahora guarda sus datos y métricas en la BD referenciando el `usuario_id` para seguimientos longitudinales.

---

## 2. Zona de Entrenamiento (Luka Gym)

### Concepto y Visión General
El **"Luka Gym"** es la Zona de Entrenamiento del usuario. El Score Luka existe como un número, pero el usuario no siempre sabe qué acciones concretas suben o bajan ese puntaje mes a mes. Sin un entrenador que traduzca el score en hábitos accionables, el número se vuelve estático. El estudiante necesita sentir que su salud financiera es como un "músculo" que puede ejercitar y fortalecer activamente. 
Este módulo analiza la "Presión Arterial" del presupuesto (velocidad de gasto) y la "Temperatura de Ahorro", generando una rutina estructurada de ejercicios a largo plazo (mensual) para garantizar resultados visibles.

### Beneficios para el Usuario
1. **Traducción del Score a Hábitos**: Convierte las métricas analíticas en misiones o ejercicios tangibles.
2. **Motivación y Gamificación**: Adopta un lenguaje de gimnasio (entrenador exigente pero motivador), ayudando al usuario a mantenerse comprometido.
3. **Planes Mensuales Accionables**: Las rutinas están diseñadas para un mes, dando el tiempo necesario para ver un cambio real en la salud financiera.
4. **Claridad sobre el Éxito**: Cada ejercicio incluye una "Métrica de Éxito" clara (ej. "0 pedidos de comida"), evitando metas ambiguas.

### Arquitectura y KPIs Principales
Utiliza Pandas para revisar el comportamiento del usuario en los últimos 30 días, analizando:
- Historial de ingresos y egresos con fechas y categorías.
- Presupuestos mensuales por categoría y porcentaje consumido.
- Se clasifican los datos en estado físico (ej. Sedentario) y se determinan rutinas que varían desde mantenimiento hasta quema intensiva de gastos.

### Cambios y Refactorización Realizada
- **Implementación Completa (Backend/Frontend)**: Se creó el módulo y se integró con la interfaz Angular.
- **Persistencia UUID Mensual**: Los datos y la rutina se guardan en la BD asociados al `usuario_id` del coach financiero con un enfoque de progreso mensual.
- **Salida Estructurada**: Se utilizó `ConsejoEstructuradoEntrenamiento` para obtener 3 rutinas estructuradas (nombre, descripción, duración, frecuencia, métrica de éxito).
- **Prompt y Fallback Aislados**: Se definieron `prompt_entrenamiento.py` y `fallback_zona_entrenamiento.py` para aislar la lógica y garantizar resiliencia.

---

## 3. El Espejo del Tiempo (Sala de Espejos Temporales)

### Concepto y Visión General
**"El Espejo del Tiempo"** es la herramienta de visualización de futuros posibles de LUKA. El universitario vive completamente en el **presente financiero**: no conecta emocionalmente su gasto de hoy con su situación en 3, 6 o 12 meses. La herramienta más poderosa para cambiar el comportamiento no es el dato frío, sino la **visualización vívida y personalizada de dos futuros alternativos**: el futuro si sigue igual (Línea de Continuidad) y el futuro si adopta el plan de LUKA (Línea de Transformación).

Ver en números reales la diferencia entre ambos futuros con sus propios datos es el mayor motivador de cambio de hábitos documentado en psicología del comportamiento. Gemini actúa como el "yo del futuro" que le escribe una **"Carta desde el Futuro"** al usuario del presente, en segunda persona y en tiempo presente, usando los montos reales calculados por el motor de Pandas.

### Beneficios para el Usuario
1. **Choque de Realidad Emocional**: Ver en números reales y concretos (con sus propios datos) la diferencia entre sus dos futuros posibles en 12 meses provoca un impacto emocional inmediato que ningún consejo genérico logra.
2. **Motivación por Contraste**: El usuario no ve una proyección abstracta, sino su propia vida narrada desde el futuro. Las "Cartas desde el Futuro" personalizan los datos con su nombre y sus metas reales.
3. **Metas Conectadas al Tiempo**: Por primera vez el usuario puede ver cuáles de sus metas activas se cumplirían o fracasarían en cada escenario a 3, 6 y 12 meses, conectando el hábito diario con el resultado concreto.
4. **Diferencia Monetaria como Driver**: El "Contador de Diferencia Neta" entre los dos futuros es el dato de mayor impacto. Un número grande en pantalla dice más que mil consejos textuales.
5. **Control sobre el Futuro**: Al permitir que el usuario alterne entre hitos (3M, 6M, 12M), el módulo le da la sensación de poder sobre su futuro financiero, reforzando la motivación de cambio.

### Arquitectura y KPIs Principales

El motor analítico (Pandas/NumPy) construye dos proyecciones a 3, 6 y 12 meses usando el promedio de los últimos 3 meses como base. Para cada proyección calcula:

- **Score Proyectado por hito**: Usando la tendencia actual del Score Luka (subiendo, bajando, estancado), proyecta el score a cada hito temporal.
- **Ahorro Acumulado por hito**: `ahorro_mensual * hito_meses`.
- **Estado de Metas por hito**: Cruza el ahorro acumulado proyectado con el `monto_restante` de cada meta activa para determinar cuáles se cumplirían y cuáles fracasarían.

**Línea de Continuidad** — Sin cambios de hábito:
- Proyecta usando la capacidad de ahorro actual: `promedio_ingresos_3m - promedio_gastos_3m`.

**Línea de Transformación** — Con optimización de LUKA:
- Identifica las categorías de gasto no esencial del historial.
- Aplica una reducción conservadora del **25%** sobre esos gastos para calcular un ahorro mensual optimizado.
- Proyecta los mismos hitos con el nuevo ahorro optimizado.

**Restricción de datos**: Requiere mínimo **30 transacciones** registradas. Con menos datos, el sistema lanza un error `HistorialInsuficienteError` y el frontend deshabilita el botón de análisis con un mensaje explicativo.

### Experiencia de Interfaz (Sala de Espejos Temporales)
La interfaz en Angular adopta una temática de **"Sala de Espejos Temporales"**:
- Un banner superior muestra el **Consejo de LUKA** como coach financiero.
- Una columna izquierda muestra el **Pasado** (score actual, saldo inicial, metas activas, fugas críticas detectadas).
- Una columna derecha muestra **El Futuro** proyectado (selector de hito 3M/6M/12M), con dos tarjetas: Futuro de Continuidad vs. Futuro de Transformación.
- Un footer central muestra la **Diferencia Neta Estimada** (en S/) entre ambos futuros al hito seleccionado.

### Cambios y Refactorización Realizada
- **Nuevo módulo `NombreModulo.ESPEJO_TEMPORAL`**: Registrado en el Enum del sistema para trazabilidad completa.
- **Salida Estructurada `ConsejoEstructuradoEspejo`**: Gemini recibe solo los KPIs resumidos (FASE 3) y devuelve exclusivamente dos cartas narrativas (`cartaContinuidad`, `cartaTransformacion`). Las proyecciones numéricas las calcula Pandas en FASE 2 y se devuelven en el campo `insight.hallazgos` de la respuesta.
- **Prompt Desacoplado**: Se creó `prompt_espejo_tiempo.py` con el rol de "Yo del Futuro" para Gemini, pasándole solo los datos esenciales calculados por Pandas.
- **Fallback Independiente**: Se creó `fallback_espejo_tiempo.py` que genera las cartas narrativas de forma local usando plantillas f-string con los montos ya calculados, sin necesidad de IA.
- **Persistencia UUID**: El historial se guarda en la tabla genérica `ia_historial_coaching` con `modulo = "ESPEJO_TEMPORAL"` y `usuario_id` para seguimiento longitudinal.
- **Integración en Router**: Endpoint `POST /api/v1/ia/espejo-tiempo`.

---

## 4. Reorganización Arquitectónica del Microservicio IA

Complementando la filosofía "Divide y Vencerás", se realizó una reorganización profunda de la estructura de carpetas:

### Separación de Responsabilidades en Persistencia
La carpeta `app/persistencia/` fue dividida en dos subcarpetas especializadas:
- `postgres/`: Contiene `database.py`, `modelos_db.py`, `repositorio_historial.py`, `repositorio_rutinas.py`.
- `redis/`: Contiene `cache_redis.py`.
Todos los imports del sistema fueron actualizados automáticamente.

### Limpieza de Servicios
- `fabrica_modulos.py` fue movido a `app/servicios/core/` (donde pertenece como parte del núcleo de instanciación).
- `mappers.py` fue movido a `app/utilidades/` (su única responsabilidad es transformar el perfil del usuario desde el cliente externo).
- La carpeta vacía `app/servicios/analitica/` fue eliminada.
- Se renombró `base_prompt.py` a `constructor_historial.py` para reflejar con precisión su única responsabilidad: inyectar la memoria de sesiones anteriores en los prompts.

### Optimización de Costos (Tokens de Gemini)
- Se eliminó la función `construir_instrucciones_esquema_estandar()` del antiguo `base_prompt.py`. Esta función añadía instrucciones de formato redundantes al prompt cuando ya el esquema Pydantic (`response_schema`) le indica a Gemini exactamente cómo responder.
- Esta eliminación reduce el consumo de **tokens de entrada** en cada consulta, maximizando el margen de rentabilidad de cada plan de suscripción (plan S/15).

---

## 5. Refactorización General del Microservicio IA: Filosofía "Divide y Vencerás"

Se ha llevado a cabo una reestructuración masiva del motor central `microservicio-ia` para garantizar su escalabilidad y limpieza:

1. **Factoría de Prompts Independiente**: Se extrajo la lógica de generación de prompts de todos los módulos (Gasto Hormiga, Comprobador Evolución, Hábitos Financieros, Simular Meta, Reto Ahorro Dinámico, Reporte Completo, Análisis Estilo de Vida) hacia archivos individuales dentro de `/app/servicios/ia/prompts/`.
2. **Modularización de Fallbacks**: El archivo monolítico heredado (`motor_reglas.py`) fue eliminado. Se consolidó la lógica en un `GestorFallbacks` que delega la creación del plan de contingencia a archivos individuales dentro de `/app/servicios/ia/fallbacks/`. Cada módulo tiene su propio archivo de fallback nativo.
3. **Desacoplamiento Absoluto del Motor Central**: El orquestador `servicio_analisis.py` fue liberado de los bloques monolíticos de decisión `if modulo == ...`. Ahora delega completamente la obtención de esquemas dinámicos llamando a `servicio.obtener_esquema_salida()` directamente en cada módulo y construyendo el objeto dinámicamente (`esquema_salida(**consejo)`). Esto respeta el Principio de Responsabilidad Única y facilita enormemente la integración de futuros módulos.

4. **Migración a FASE 3 (Structured Output) del módulo PREDECIR_GASTOS**: El módulo `predecir_gastos` ahora utiliza su propio esquema `ConsejoEstructuradoPredecir` (definido en `esquemas.py`), incluye un fallback nativo y un prompt simplificado que pide directamente el JSON, cumpliendo 100% con la arquitectura canónica.
