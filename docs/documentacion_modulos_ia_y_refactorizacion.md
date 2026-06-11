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

## 3. Refactorización General del Microservicio IA: Filosofía "Divide y Vencerás"

Se ha llevado a cabo una reestructuración masiva del motor central `microservicio-ia` para garantizar su escalabilidad y limpieza:

1. **Factoría de Prompts Independiente**: Se extrajo la lógica de generación de prompts de todos los módulos (Gasto Hormiga, Comprobador Evolución, Hábitos Financieros, Simular Meta, Reto Ahorro Dinámico, Reporte Completo, Análisis Estilo de Vida) hacia archivos individuales dentro de `/app/servicios/ia/prompts/`.
2. **Modularización de Fallbacks**: El archivo monolítico heredado (`motor_reglas.py`) fue eliminado. Se consolidó la lógica en un `GestorFallbacks` que delega la creación del plan de contingencia a archivos individuales dentro de `/app/servicios/ia/fallbacks/`. Cada módulo tiene su propio archivo de fallback nativo.
3. **Desacoplamiento Absoluto del Motor Central**: El orquestador `servicio_analisis.py` fue liberado de los bloques monolíticos de decisión `if modulo == ...`. Ahora delega completamente la obtención de esquemas dinámicos llamando a `servicio.obtener_esquema_salida()` directamente en cada módulo y construyendo el objeto dinámicamente (`esquema_salida(**consejo)`). Esto respeta el Principio de Responsabilidad Única y facilita enormemente la integración de futuros módulos.
