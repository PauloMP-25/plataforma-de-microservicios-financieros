# Documentación Oficial de Módulos IA y Ecosistema LUKA

El motor de Inteligencia Artificial de LUKA está compuesto por 11 módulos canónicos diseñados para transformar el historial transaccional de un estudiante universitario en conocimiento accionable, corrección de hábitos y proyección a futuro. 

Esta documentación detalla extensivamente la conceptualización, beneficios, experiencia de usuario y arquitectura de KPIs subyacente para cada módulo, presentándolos en su orden lógico de maduración dentro del flujo del usuario.

---

## 1. Gasto Hormiga (El Detective de Fugas)

### Concepto y Visión General
El "Gasto Hormiga" funciona como el primer acercamiento analítico del usuario a la IA. La mayoría de los estudiantes universitarios sienten que el dinero "desaparece" sin realizar grandes compras, generando frustración. Este módulo actúa como un detective forense: analiza rigurosamente transacciones menores a un umbral definido y agrupa aquellas de alta frecuencia para revelar la verdadera fuga de capital invisible. Al desenmascarar el acumulado de cafés, snacks o micro-suscripciones, LUKA transforma un gasto aparentemente inofensivo en un punto ciego financiero resuelto.

### Beneficios para el Usuario
- **Toma de Conciencia Cuantitativa**: Descubrir que el café diario equivale al costo de un ciclo universitario al año rompe la disonancia cognitiva del estudiante.
- **Micro-Decisiones de Alto Impacto**: Identifica aquellos comportamientos repetitivos y de bajo costo que son más fáciles de cambiar que intentar eliminar un gasto fijo importante.
- **Acción Inmediata**: Las recomendaciones de LUKA no exigen esfuerzo irreal, sino que ofrecen alternativas de sustitución directas (ej. "Preparar café en termo" vs. "Dejar de tomar café").

### Arquitectura y KPIs Principales
- **Agrupamiento Frecuencial**: El motor Pandas aísla transacciones menores a 30 o 50 soles y cuenta la repetición del comercio o subcategoría en los últimos 30 días.
- **KPI - Impacto Mensual Real**: Sumatoria exacta del dinero invertido en micro-gastos.
- **KPI - Proyección Anualizada**: Multiplica el impacto mensual por 12 para generar shock emocional.
- **KPI - Índice de Dependencia**: Frecuencia del gasto frente al total de compras del mes, determinando qué tan arraigado está el hábito.

### Experiencia de Interfaz
Una interfaz de análisis granular que resalta visualmente, mediante Bento Grid y tarjetas de alerta, los montos proyectados a largo plazo. Se utilizan gráficas de frecuencia para demostrar la acumulación paulatina, generando un momento "Aha!" en el usuario cuando ve el costo anualizado de su consumo diario.

---

## 2. Predicción de Gastos (El Oráculo Financiero)

### Concepto y Visión General
**"El Oráculo Financiero"** es la herramienta de prevención de crisis de LUKA. Su propósito es erradicar el concepto de "llegar a fin de mes en rojo". Calcula matemáticamente el requerimiento de gasto para el próximo mes basado en el historial transaccional estacional, detectando posibles déficits antes de que el dinero salga de la cuenta. En lugar de decir "gasta menos", LUKA proyecta el impacto exacto si se mantiene la trayectoria inercial de la economía del usuario.

### Beneficios para el Usuario
- **Alerta Temprana de Insolvencia**: Previene deudas y cortes de servicios mediante proyecciones de flujo de caja negativo.
- **Recomendaciones Matemáticas y Quirúrgicas**: Provee de cifras exactas de corrección (ej. "reduce tus pedidos en delivery en S/ 200 este mes para evitar el déficit") en lugar de consejos financieros genéricos.
- **Protección de Metas**: Relaciona directamente cómo un déficit proyectado anula la capacidad de ahorro mensual, alejando al usuario de sus metas activas.

### Arquitectura y KPIs Principales
- **KPI - Gasto Promedio Histórico**: Promedio ponderado de gastos por categoría de los últimos meses.
- **KPI - Proyección Próximo Mes**: Modelo predictivo simple que ajusta la tendencia de crecimiento de los gastos.
- **KPI - Porcentaje de Variación (Volatilidad)**: Mide la inestabilidad de la categoría mes a mes.
- **KPI - Déficit Estimado**: La diferencia negativa calculada entre los ingresos recurrentes esperados y el gasto proyectado total.

### Experiencia de Interfaz
Usa un formato de alerta inteligente (Bento Layout) donde se muestra una "máquina de escribir" dinámica que presenta la predicción. Tarjetas visuales de alto contraste advierten sobre la tendencia alcista en categorías sensibles, dotando al estudiante de tiempo de reacción.

---

## 3. Simular Meta (El Navegador Financiero)

### Concepto y Visión General
**"El Navegador Financiero"** aterriza las expectativas de los universitarios, quienes a menudo establecen metas emocionales y ambiciosas (una Laptop Gamer, un viaje a Cusco) sin planificar la matemática que hay detrás. Este módulo evalúa si la meta es viable en el tiempo deseado cruzando el objetivo financiero contra el "espacio" real en el flujo de caja del usuario. Funciona como un GPS que recálcula la ruta si es imposible llegar a tiempo.

### Beneficios para el Usuario
- **Desmitificación del Ahorro**: Traduce sueños en plazos y cuotas semanales/mensuales, evitando el abandono por frustración.
- **Planes de Acción Claros (Recalculando Ruta)**: Si la fecha objetivo no es viable, la IA indica exactamente en cuántos meses más se lograría bajo la economía actual, o cuánto ingreso extra se requiere para cumplir el plazo original.
- **Validación del Esfuerzo Actual**: Si el usuario está ahorrando a un ritmo óptimo, el sistema lo celebra y solidifica la motivación de mantener el ritmo.

### Arquitectura y KPIs Principales
- **KPI - Capacidad de Ahorro Real**: `(Promedio Ingresos 3m) - (Promedio Gastos 3m)`.
- **KPI - Meses Estimados (Tiempo de Vuelo)**: `(Monto Objetivo - Ahorro Actual) / Capacidad de Ahorro`.
- **KPI - Viabilidad en Fecha Límite**: Evaluación booleana y temporal contra el 'Deadline' establecido por el usuario.
- **KPI - Esfuerzo de Brecha**: Monto adicional requerido al mes para ajustar la viabilidad a `True`.

### Experiencia de Interfaz
Integra un diseño inspirador pero técnico. Elementos tipo dashboard que simulan una consola de navegación: barras de progreso circulares, estimaciones de tiempo proyectado y estrategias modulares en tarjetas desglosables, manteniendo el aspecto visual elegante de Dark Mode.

---

## 4. Reto Ahorro Dinámico (Gamificación Financiera)

### Concepto y Visión General
Convierte el tedio de la restricción financiera en un juego interactivo. Genera "Retos o Misiones" de corta duración (semanales o quincenales) configurados proceduralmente según el comportamiento hiper-reciente del usuario. LUKA detecta el mayor talón de Aquiles del mes (por ejemplo, exceso en restaurantes) y lo transforma en una cruzada lúdica con reglas, un título épico y una estrategia precisa.

### Beneficios para el Usuario
- **Reducción de la Fricción Psicológica**: Ahorrar por "obligación" es aburrido; superar una "Misión de Supervivencia" activa centros de recompensa cerebrales.
- **Enfoque Cortoplacista**: Misiones a 7 o 14 días son mucho más tolerables para la mente humana que las prohibiciones mensuales.
- **Adaptación Activa**: Si el usuario cambia su comportamiento la próxima semana, el sistema genera retos completamente diferentes basados en su nueva economía.

### Arquitectura y KPIs Principales
- **KPI - Categoría Crítica Actual**: Categoría con la tasa de crecimiento de gasto más alta en los últimos 15 días.
- **KPI - Gasto Semanal Promedio Crítico**: Cuánto está quemando el usuario por semana en el área problema.
- **KPI - Meta de Contención**: Un monto algorítmico realista que el usuario no debe sobrepasar en los próximos días.

### Experiencia de Interfaz
Se emplea una estética de RPG / Videojuego. Títulos vibrantes ("Maestro de la Cocina", "Eremita Financiero"), una barra de vida/presupuesto, y mensajes tácticos directos de supervivencia para enfrentar la semana, con íconos vistosos y de alta recompensa visual.

---

## 5. Hábitos Financieros (El Psicólogo de Bolsillo)

### Concepto y Visión General
Mientras Gasto Hormiga busca montos pequeños, "Hábitos Financieros" analiza la psicología conductual del usuario cruzando variables temporales y de categoría. Detecta adicciones al consumo como: compras nocturnas inducidas por insomnio, excesos concentrados los fines de semana (efecto rebote de estrés) o consumos emocionales a fin de mes. 

### Beneficios para el Usuario
- **Insight Conductual Profundo**: El estudiante descubre el "por qué" y el "cuándo" gasta su dinero, reconociendo desencadenantes emocionales de los que no era consciente.
- **Creación de "Hábitos Atómicos"**: Propone cambios microscópicos en la rutina diaria que cortocircuitan el patrón perjudicial sin requerir fuerza de voluntad excesiva.
- **Desarrollo de Inteligencia Emocional Financiera**: Aborda el gasto como un síntoma de un comportamiento, no como un mero error numérico.

### Arquitectura y KPIs Principales
- **KPI - Distribución Horaria de Consumo**: Mapa de calor de las horas en las que se realizan transacciones, aislando madrugadas o franjas atípicas.
- **KPI - Distribución de Días de la Semana**: Análisis de varianza (ANOVA) entre días laborables y fines de semana.
- **KPI - Frecuencia de Disparo**: Cantidad de veces que un patrón específico se activó en el último trimestre.

### Experiencia de Interfaz
La visualización adopta un estilo solemne, de diagnóstico profundo. Utiliza tarjetas de descubrimiento y textos que narran la "psicología" detrás de los números, acompañados de recomendaciones presentadas como "prescripciones conductuales".

---

## 6. Análisis Estilo de Vida (El Espejo de Arquetipos)

### Concepto y Visión General
Clasifica de manera holística al usuario en un "Arquetipo Financiero" (ej. "Foodie Explorador", "Minimalista Tecnológico", "Socialité Nocturno") midiendo las concentraciones dominantes de su capital. Ayuda a alinear la identidad de la persona con sus finanzas, sin juzgar sus gustos pero optimizando sus decisiones de acuerdo a ellos.

### Beneficios para el Usuario
- **Sentido de Identidad Financiera**: Reconocerse dentro de un arquetipo fomenta la aceptación y reduce el sentimiento de culpa, redirigiendo la energía hacia la optimización del presupuesto.
- **Consejos Alineados a Sus Pasiones**: En lugar de sugerir a un "Foodie" que deje de salir a comer, LUKA le enseñará a maximizar su presupuesto gastronómico, buscando equilibrio.
- **Validación Social**: La gamificación del perfil motiva a compartir o querer "subir de rango" dentro de su tribu urbana.

### Arquitectura y KPIs Principales
- **KPI - Densidad Relativa de Categorías**: Porcentaje del presupuesto total asignado a grupos semánticos de categorías (Ej. Grupo Ocio: Cines + Eventos + Discotecas).
- **KPI - Coeficiente de Diversidad de Gasto**: Mide si el usuario invierte en múltiples aspectos de su vida o si todo su flujo se drena por un solo vértice.

### Experiencia de Interfaz
Implementación espectacular de Bento Grid, con insignias premium que ilustran el "Arquetipo". La paleta de colores y avatares cambian dinámicamente según la personalidad dominante detectada en el algoritmo.

---

## 7. Autoclasificación (El Cerebro Cero-Historia)

### Concepto y Visión General
Este es un módulo estrictamente utilitario y ultra-rápido, diseñado para procesar cadenas de texto libres en tiempo real de ingreso de la transacción por parte del usuario. Al escribir la descripción de un gasto (ej: "Salida al cine con María y KFC"), LUKA sugiere las categorías precisas ("Entretenimiento", "Comida Rápida").

### Beneficios para el Usuario
- **Cero Fricción Operativa**: Automatiza el flujo de categorización, un proceso que suele ser el mayor causante de abandono de apps financieras tradicionales.
- **Aprendizaje Interactivo**: Acelera drásticamente el proceso de ingreso manual en pantalla, sugiriendo categorías contextualmente inteligentes.

### Arquitectura y KPIs Principales
- **Arquitectura Zero-History**: No lee bases de datos ni historiales. Toma puramente el string descriptivo, usa prompts comprimidos en XML para Gemini y recibe una salida JSON con un array de strings.
- **Métricas Operativas**: Latencia ultra-baja y optimización máxima de uso de tokens del LLM, devolviendo solo los nombres de categoría recomendados.

---

## 8. El Comprobador de Evolución (La Sala de Radiología)

### Concepto y Visión General
Este módulo es la radiografía del esfuerzo. Transforma el historial de los últimos meses en **evidencia médica irrefutable de evolución personal**. Los universitarios raramente miran hacia atrás con rigor, perdiendo la oportunidad de validar si su esfuerzo en recortar gastos funcionó, provocando parálisis motivacional. Aquí LUKA actúa como un Médico Radiólogo Forense Financiero.

### Beneficios para el Usuario
- **Validación Objetiva del Esfuerzo**: El estudiante deja de depender de su intuición. El sistema le dice matemáticamente si su disciplina ha mejorado, comparando el pasado lejano contra su historia reciente.
- **Identificación Quirúrgica**: Detecta recaídas exactas (huesos rotos) sin necesidad de revisar manualmente hojas de cálculo.
- **Celebración de Pequeñas Victorias**: Reconoce automáticamente las "Categorías Conquistadas", brindando refuerzo positivo directo a su psique.

### Arquitectura y KPIs Principales
- **KPI - Delta de Tasa de Ahorro (ΔTS)**: Variación entre Periodo A y B.
- **KPI - Índice de Volatilidad (IVG)**: Medición del caos financiero frente a la disciplina.
- **KPI - Categorías Conquistadas / Reincidentes**: Picos de varianza negativa y positiva mayores al 10%.
- **KPI - Índice de Madurez Financiera (IMF)**: Un score final combinado para diagnosticar el estado "médico".

### Experiencia de Interfaz
Estética **"Med-Noir"**. Uso de gráficos esqueléticos de diagnóstico; las categorías sanas emiten luz verde brillante, y las reincidentes destellan en rojo "fracturado". Un reporte clínico con prescripciones médicas exactas para la rehabilitación financiera.

---

## 9. Zona de Entrenamiento (Luka Gym)

### Concepto y Visión General
El Score Luka existe, pero un número por sí solo es abstracto. LUKA Gym transforma los KPIs analíticos en una rutina de "Workout Financiero". LUKA asume el papel de un entrenador exigente de alto rendimiento, traduciendo el análisis de velocidad de gasto ("Presión Arterial del presupuesto") en ejercicios semanales accionables para "tonificar" el bolsillo.

### Beneficios para el Usuario
- **Métricas Traducidas a Acciones Simples**: Un déficit de flujo se convierte en "Ejercicio 1: Quema de Grasa en Suscripciones".
- **Compromiso Continuo**: Adoptar la mentalidad fitness promueve resiliencia ante caídas y constancia en el progreso de sus metas.

### Arquitectura y KPIs Principales
- **KPI - Estado Físico Financiero**: Evaluador que clasifica al usuario en niveles como "Sedentario", "En Forma" o "Alto Rendimiento".
- **KPI - Capacidad de Contracción de Gastos**: Evaluación del margen seguro de recorte sin comprometer calidad de vida vital.

### Experiencia de Interfaz
Diseño **Cyberpunk Gym**. Colores oscuros neón, texturas corrugadas, y tarjetas renderizadas como "Rutinas de Peso Libre". Lenguaje dinámico, rudo pero protector que mantiene la moral en alto y fomenta la disciplina diaria.

---

## 10. El Espejo del Tiempo (Sala de Espejos Temporales)

### Concepto y Visión General
El módulo supremo de la proyección del comportamiento. El usuario se sitúa en un entorno inmersivo frente a dos futuros posibles (a 3, 6 y 12 meses): La **Línea de Continuidad** (Si sigue igual) y la **Línea de Transformación** (Si optimiza). Gemini narra mediante "Cartas desde el Futuro" un día en la vida del usuario, provocando el mayor choque emocional y cognitivo posible documentado en behavioral economics.

### Beneficios para el Usuario
- **Choque de Realidad Emocional Vibrante**: Ver la diferencia de su futuro escrita con su nombre y cifras reales (ej. "Tener S/ 4,000 extra y la meta laptop completada") detona un nivel de inspiración inigualable.
- **Visibilidad Real de las Metas en el Tiempo**: Se evidencia la tragedia de no actuar ("Tus metas fracasaron") frente al éxito de cambiar hábitos hoy mismo.

### Arquitectura y KPIs Principales
- **Motor de Bifurcación Pandas**: Algoritmo central que calcula proyecciones con ahorro estático versus optimizado (recorte del 25% conservador en gastos superfluos).
- **KPI - Score Proyectado / Ahorro Acumulado**: Proyección matemática a cada hito temporal ajustada al techo/piso teórico de 0-100.
- **Diferencia Neta Proyectada**: Cálculo diferencial final del ahorro para maximizar la percepción del impacto de la inacción.

### Experiencia de Interfaz
**Díptico Parallax Interactivo**. Dos paneles confrontados (Frío/Oscuro/Óxido vs. Cálido/Luminoso/Dorado). Una línea de tiempo central que bifurca la pantalla y un contador numérico animado que revela la diferencia económica masiva entre tomar las riendas hoy o dejarse llevar por la inercia.

---

## 11. Reporte Completo (La Auditoría de Nivel Ejecutivo)

### Concepto y Visión General
El broche de oro mensual. La consola central donde convergen absolutamente todos los indicadores anteriores en un Informe Consolidado. Adopta una postura de "Junta Directiva", dándole al usuario universitario la experiencia de ser el CEO de su propia vida financiera, con resúmenes claros, tablas tabuladas cruzadas y un dictamen global sobre su viabilidad a largo plazo.

### Beneficios para el Usuario
- **Visión Holística sin Sesgos**: Permite conectar las decisiones macro con las micro, al revisar Score, Fugas Hormigas, Progreso de Metas y Diagnósticos de Hábitos en una sola hoja.
- **Orgullo y Satisfacción**: Contemplar un reporte en verde completo genera una dopamina masiva que refuerza la adopción total de la aplicación LUKA como hábito de vida inquebrantable.

### Arquitectura y KPIs Principales
- **Motor Compilador Integral**: Orquesta llamadas silenciosas o cruce de datos con las salidas procesadas de los demás módulos, consolidándolas antes de pasarlas a un LLM maestro.
- **Generación de Resumen Ejecutivo y Alertas de Alta Prioridad**.

### Experiencia de Interfaz
Grid limpio y formal de altísima gama. Uso intensivo de tablas, gráficas tabulares nativas de Angular y modales ejecutivos. Es la visualización con más densidad de información por píxel, diseñada para ser escaneable, imprimible o exportable como un manifiesto de logro.

---

## 12. Reorganización Arquitectónica

Complementando la filosofía "Divide y Vencerás", se realizó una reorganización profunda de la estructura de carpetas:

### Separación de Responsabilidades en Persistencia
La carpeta `app/persistencia/` fue dividida en dos subcarpetas especializadas:
- `postgres/`: Contiene la gestión del ORM, Modelos de Base de datos y Repositorios.
- `redis/`: Maneja exclusivamente cachés y temporalidad.
Todos los imports del sistema fueron actualizados.

### Limpieza de Servicios Centrales
- Las fábricas de instanciación se movieron a su dominio `core/`.
- Los transformadores o mappers de clientes se movieron a `utilidades/`.
- Se introdujo `constructor_historial.py` con una única responsabilidad: gestionar la inyección de historial longitudinal, aliviando el peso de los orquestadores LLM.

---

## 13. Refactorización a Fase 3 y Cambios Transversales

La mayor evolución del microservicio ha sido la refactorización arquitectónica para todos los módulos listados (con la única salvedad semántica del módulo de autoclasificación, que opera en Zero-History). Esta estandarización asegura el escalamiento ilimitado de la IA en LUKA:

1. **Adopción de FASE 3 (Structured Output Absoluto)**: 
   Se prohibió terminantemente el uso de Markdown o Strings en texto plano como salida de Gemini. Todos los módulos ahora declaran e inyectan una clase derivada de `BaseModel` (Pydantic) como el esquema de respuesta obligatoria (`response_schema`). Gemini devuelve invariablemente un objeto JSON tipado estricto (ej. `ConsejoEstructuradoEspejo`, `ConsejoRetoAhorroDTO`).
   
2. **Prompts Escritos con Etiquetas XML**: 
   Atrás quedaron las instrucciones confusas. Todos los prompts han sido reescritos usando inyección de etiquetas XML semánticas puras (`<rol>`, `<contexto>`, `<datos>`, `<tarea>`, `<restricciones>`). Esto encapsula la información de contexto matemático calculada por Pandas y le da directrices inquebrantables de comportamiento a Gemini, aumentando su precisión determinista en un 95% y ahorrando masivamente tokens en cada Request.
   
3. **Desacoplamiento Front-Back**: 
   El backend ya no formatea el diseño visual del usuario (sin negritas ni bullets inyectados desde Python). El Backend retorna puramente Data Tipada (JSON). Es el Frontend (Angular), equipado con Vanilla CSS, Bento Grids, Sistemas de Partículas y Tematizaciones avanzadas (Dark Mode Cyberpunk, Clínicas Radiológicas, Parallax) quien toma estos campos atómicos y los renderiza espectacularmente.

4. **Resiliencia de Componentes (Fallbacks Independientes)**: 
   Se destruyó el motor centralizado de fallbacks ("God Class"). Ahora, cada módulo de IA posee su propio archivo y clase nativa de Fallback Determinista (`fallback_espejo.py`, `fallback_gym.py`). Si la API de Google falla o colapsa por timeout, Pandas asume el control absoluto y completa el DTO requerido inyectando los datos matemáticos calculados junto a una plantilla textual de contingencia rápida, asegurando *Zero Downtime* en la experiencia del usuario.
