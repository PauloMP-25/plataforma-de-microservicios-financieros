# 📋 Registro de Modificaciones Pendientes — LUKA APP

Este documento centraliza todas las modificaciones, refactorizaciones, pruebas, errores y bugs **pendientes** por resolver en el ecosistema de **LUKA APP** (Frontend y Backend).

> **Última actualización:** 15 de junio de 2026
> **Responsable del documento:** Paulo Moron (Cloud Architect)
> **Estado:** En Progreso — Solo se listan los ítems pendientes por solucionar.

---

## 🔧 Capa Backend

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Backend | **[B-05] Gastos de Suscripción:** El backend no conecta los gastos pagados y pendientes que vienen de la sección suscripción. | Alta | Pendiente | 12 de junio de 2026 | — |
| Backend | **[B-06] Dashboard - Sincronización por Eventos:** Verificar que los datos del dashboard se actualicen en tiempo real e inmediatamente después del ingreso de transacciones (gastos e ingresos) a través de eventos. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Integración API | **[F-46] Gastos - Sincronización de Suscripciones:** Integrar las tarjetas de "Pendiente por pagar" y "Próximo vencimiento" con el `microservicio-suscripciones`, extrayendo los datos reales de las suscripciones del usuario asociadas a la tarjeta (aún no integrado en frontend). | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-53] Gastos - Verificación End-to-End:** Realizar pruebas integrales en la vista de gastos para asegurar que todo el layout funcione en perfecta armonía con la integración final del backend. | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-29] Usuario Mock para Pruebas:** Agregar un usuario mock con correo `prueba@gmail.com` y contraseña `12345` para probar el flujo de ingreso, omitiendo la restricción de contraseña solo por esta vez. | Alta | Completado | 12 de junio de 2026 | 20 de junio de 2026 |
| Backend / API | **[B-07] API - Filtro de Ingresos:** Garantizar que los endpoints dedicados a la sección de Ingresos devuelvan exclusivamente datos y transacciones de tipo ingreso (excluyendo cualquier gasto). | Alta | Pendiente | 15 de junio de 2026 | — |
| Arquitectura | **[B-08] API - Streaming de Datos:** Implementar flujos reactivos (como Server-Sent Events - SSE o WebSockets) para el envío de volúmenes pesados de datos. En lugar de procesar peticiones masivas síncronas, el backend debe despachar la información mediante eventos y bloques progresivos al cliente. | Alta | Pendiente | 15 de junio de 2026 | — |

---

## 🎨 Capa Frontend (Angular)

### 🌐 Estándares Transversales (Aplica a todo el Frontend)

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Arquitectura | **[F-63] Modularización Global:** Promover de forma estricta la modularización de cada vista del sistema en componentes pequeños, altamente cohesivos y reutilizables (Standalone Components) para maximizar la escalabilidad y mantenibilidad. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-64] Consumo Reactivo (Eventos):** Adaptar los servicios y el frontend para recibir grandes volúmenes de información en tiempo real a través de un bus de eventos o SSE, renderizando la información progresivamente en la UI en lugar de bloquear la pantalla esperando un solo payload masivo. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-54] Formateo de Decimales:** En todas las secciones (Dashboard, Gastos, Ingresos, etc.), asegurar que cualquier cifra numérica que no sea entera se renderice estrictamente limitándose a dos decimales. | Alta | Pendiente | 15 de junio de 2026 | — |
| Rendimiento | **[F-55] Optimización General:** Auditar y aplicar lazy loading, minificación de componentes y reducción de carga inicial en todas las vistas, garantizando velocidad extrema y una experiencia de usuario fluida. | Alta | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-56] Mejora Continua de UI:** Realizar una revisión global de estilos en todas las pantallas para refinar el diseño, mejorar el contraste entre temas (claro/oscuro) y pulir micro-interacciones (hover, focus, transitions). | Media | Pendiente | 15 de junio de 2026 | — |

---

### 📊 Layout: Dashboard `[Responsable: Paulo Moron]`

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Interactividad | **[F-37] Dashboard - Filtros y Búsqueda:** Hacer funcional los filtros y la barra de búsqueda del dashboard para búsquedas y filtrados en tiempo real. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-40] Dashboard - Tendencia de Flujo de Caja:** Incorporar todos los meses al gráfico de flujo de caja y habilitar la capacidad de filtrarlos dinámicamente por año. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento | **[F-42] Dashboard - Carga Rápida:** Optimizar el BFF y el frontend para asegurar una carga ultra-rápida y renderizado inmediato de los datos consolidados en pantalla. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento | **[F-44] Dashboard - Renderizado y Lazy Loading:** Modularizar y optimizar el layout utilizando carga diferida (lazy loading) en Angular y estrategias de renderizado óptimas para mejorar los Core Web Vitals. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Interactividad | **[F-37] Dashboard - Filtros y Búsqueda:** Hacer funcional los filtros y la barra de búsqueda del dashboard para búsquedas y filtrados en tiempo real. | Alta | ✔ Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-40] Dashboard - Tendencia de Flujo de Caja:** Incorporar todos los meses al gráfico de flujo de caja y habilitar la capacidad de filtrarlos dinámicamente por año. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento | **[F-42] Dashboard - Carga Rápida:** Optimizar el BFF y el frontend para asegurar una carga ultra-rápida y renderizado inmediato de los datos consolidados en pantalla. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento | **[F-44] Dashboard - Renderizado y Lazy Loading:** Modularizar y optimizar el layout utilizando carga diferida (lazy loading) en Angular y estrategias de renderizado óptimas para mejorar los Core Web Vitals. | Alta | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Interactividad | **[F-21] Dashboard - Filtro de Fechas:** El filtro "Entre fechas" debe permitir desde la fecha que se elige hasta el día de hoy. | Media | Completado | 12 de junio de 2026 | 20 de junio de 2026 |
| Estructura | **[F-22] Dashboard - Historial:** Eliminar el historial de transacciones del Dashboard. | Media | Completado | 12 de junio de 2026 | 20 de junio de 2026 |
| Estilos | **[F-38] Dashboard - Coherencia Estética:** Verificar que los estilos de letras para cada componente del layout se encuentren coherentes y legibles en los modos claro y oscuro según el tema activo. | Media | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-39] Dashboard - Distribución de Gastos (Top 5):** Ajustar el gráfico de distribución de gastos para mostrar exclusivamente los 5 gastos de mayor importancia. | Media | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-41] Dashboard - Enriquecimiento Visual:** Añadir gráficos estadísticos complementarios (línea de tiempo de actividad, gráficos de barras comparativos, anillos/donas e histogramas de dispersión) que enriquezcan la UX. | Media | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento | **[F-43] Dashboard - Peso de Componentes:** Optimizar el CSS/SCSS del layout y componentes individuales para asegurar que los estilos y el script no excedan el peso de bundle óptimo. | Media | Completado | 15 de junio de 2026 | 20 de junio de 2026 |
| Estilos | **[F-25] Tema Oscuro - Header y Dashboard:** En modo oscuro las letras del header no se notan claro; definir un color visible para las letras sin afectar el tema claro. En el dashboard (ej. periodo) las letras deben ser blancas con hover más blanco, sin afectar el color principal. | Baja | ✔ Completado | 12 de junio de 2026 | 20 de junio de 2026 |
| Rendimiento/UX | **[F-102] Dashboard - Responsividad de Gráficos:** Ajuste dinámico del ancho de los gráficos (compactación y expansión a full-width) cuando hay pocos datos, con eliminación total del scroll horizontal. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Datos/Mocks | **[F-103] Dashboard - Corrección de Mock Data:** Ajuste en la generación de datos para reportar los 7 días en la vista semanal y nombres dinámicos de meses en el flujo de caja en lugar de estáticos. | Media | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-104] Dashboard - Prevención de Colisiones:** Lógica dinámica en las etiquetas de datos del gráfico de tendencia para evitar solapamientos y choques con las etiquetas de los ejes X e Y usando boundary checks. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| UX / Layout | **[F-105] Dashboard - Layout Adaptativo (Corto Plazo):** Reorganización dinámica inteligente del orden de los gráficos del grid (2 filas) cuando se detectan rangos de 1 a 3 meses usando CSS `order` y proporciones personalizadas. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| UI / Gráficos | **[F-106] Dashboard - Leyendas Inferiores en Donas:** Reubicación de las leyendas a la posición inferior (`bottom`) en gráficos circulares (Distribución y Pago) y eliminación del padding asimétrico para maximizar la visibilidad y el centrado del gráfico. | Media | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Bugfix / Gráficos | **[F-107] Dashboard - Corrección de Expansión (Chart.js):** Solución al desbordamiento y retención del tamaño del grid (bug de Chart.js al cambiar de filtros) incorporando `min-width: 0` y `overflow: hidden` a todos los contenedores principales. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |

---

### 💳 Layout: Gastos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Navegación | **[F-52] Gastos - Sub-sección de Registro:** Refactorizar el botón "Registrar gasto" para que, en lugar de abrir un modal, navegue hacia una sub-sección dedicada (`/gastos/registrar`), actualizando dinámicamente la ruta en el header. | Alta | Pendiente | 15 de junio de 2026 | — |
| Validación | **[F-28] Límite en Descripción:** En el formulario de gastos, agregar un límite de 200 caracteres en la descripción. | Media | Pendiente | 12 de junio de 2026 | — |
| Estilos | **[F-45] Gastos - Coherencia Estética:** Auditar y corregir el texto para asegurar una legibilidad perfecta y evitar problemas visuales o falta de contraste al alternar entre el tema oscuro y el claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-47] Gastos - Reemplazo de Tablas por Gráficos:** Eliminar la tabla de pendientes y recurrentes. Implementar en su lugar gráficos funcionales (ej. un gráfico de anillos/donut para mostrar la proporción de gastos recurrentes por servicio, o un gráfico de barras apiladas para visualizar los próximos pagos en el mes). | Media | Pendiente | 15 de junio de 2026 | — |
| Estructura | **[F-48] Gastos - Eliminación de Historial Local:** Remover la tabla de historial organizado de esta vista, ya que se consolidará de forma centralizada en la nueva sección global "Historial" con filtros dinámicos. | Media | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-49] Gastos - Top Categorías del Mes:** Modificar el bloque de "Gastos por categoría" para que filtre exclusivamente los datos del mes actual y renderice únicamente las 6 categorías más importantes (Top 6), dejando los totales al dashboard. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-50] Gastos - Mejora de Tabla Top Días:** Hacer funcional el filtro de la tabla "Top de días de gasto". Como idea de mejora visual, incorporar barras de progreso en línea (sparklines) o un mini mapa de calor (heatmap) para identificar rápidamente la intensidad del gasto diario. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-51] Gastos - Componentes Visuales Adicionales:** Incorporar componentes de apoyo visual (ej. tarjetas de insights, alertas de presupuesto excedido o micro-animaciones) que mejoren la experiencia del usuario sin saturar la vista. | Baja | Pendiente | 15 de junio de 2026 | — |

---

### 💵 Layout: Ingresos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Estructura | **[F-57] Ingresos - Botón de Historial:** Eliminar el botón de "Historial" de la parte inferior del layout, dejando exclusivamente el superior para mantener consistencia con la vista de gastos. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-58] Ingresos - Tarjetas KPI:** Añadir dos tarjetas KPI estratégicas junto a "Ingresos registrados" y "Categoría principal" (por ejemplo: "Comparación mes anterior (%)" e "Ingreso Promedio" o "Progreso de Meta"). | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-59] Ingresos - Gráfico de Categoría:** Limitar el gráfico de "Ingresos por Categoría" para que renderice exclusivamente el Top 5 de fuentes de ingreso más relevantes. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-60] Ingresos - Paginación de Recientes:** Ajustar la tabla de "Ingresos recientes" para mostrar únicamente los 6 primeros registros, incorporando controles de paginación funcionales para navegar por el resto de datos. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-61] Ingresos - Filtros Independientes:** Asegurar que cada gráfico dentro de este layout tenga sus propios selectores o filtros, de forma que cambiar el filtro de un gráfico no afecte inadvertidamente a los demás. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-62] Ingresos - Nuevos Gráficos:** Agregar dos gráficos funcionales adicionales que aporten valor a la experiencia de usuario (por ejemplo: "Evolución Mensual de Ingresos" y "Proporción de Ingreso Fijo vs Variable"). | Media | Pendiente | 15 de junio de 2026 | — |

---

### 📅 Layout: Presupuestos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-68] Presupuestos - Integración Completa:** Integrar de manera correcta y completa todo el layout de presupuestos con su respectivo endpoint en el backend. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-69] Presupuestos - Corrección de Mapeo (NaN):** Mapear correctamente los campos del "Historial de límites" recibidos del backend para corregir el bug actual donde se renderizan campos vacíos o valores `NaN`. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-70] Presupuestos - Desglose por Categorías:** Ajustar la sección de desglose para mostrar exclusivamente las 5 categorías principales (Top 5). Estos datos deben calcularse y filtrarse estrictamente según la fecha de inicio y fin del presupuesto. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-71] Presupuestos - Paginación de Historial:** Modificar la tabla de "Historial de límites" para que muestre únicamente los primeros 5 registros por defecto, implementando controles de paginación para visualizar los siguientes. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-72] Presupuestos - Coherencia Estética:** Auditar y garantizar la correcta visualización de colores, contraste y textos de todo el layout tanto en el tema oscuro como en el claro. | Media | Pendiente | 15 de junio de 2026 | — |

---

### 🔔 Layout: Suscripciones

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Estructura | **[F-73] Suscripciones - Creación de Layout Modular:** Diseñar y construir el nuevo layout de Suscripciones desde cero utilizando Standalone Components, asegurando que sea completamente modular e independiente. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-74] Suscripciones - Integración Completa:** Conectar y mapear correctamente todos los endpoints del backend para renderizar la lista de suscripciones activas, inactivas y vencidas. | Alta | Pendiente | 15 de junio de 2026 | — |
| Navegación | **[F-07] El sidebar se reinicia al navegar a "Suscripción":** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). *(Nota: Relacionado con Sidebar / Navegación)* | Alta | Pendiente | 12 de junio de 2026 | — |
| Lógica de Negocio | **[F-75] Suscripciones - Destacar Plan Luka:** Sincronizar el módulo para identificar si el usuario cuenta con el "Plan de Luka" (nuestra plataforma) y fijarlo dinámicamente como la suscripción principal y más destacada en la parte superior. | Media | Pendiente | 15 de junio de 2026 | — |
| UI / Estilos | **[F-76] Suscripciones - Tarjetas Temáticas (Brand Styles):** Estilizar las *cards* de suscripción aplicando paletas de colores representativas (Ej: Netflix, Spotify, YouTube, Twitch, Amazon Prime, Disney+, Max/HBO, Apple Music, Xbox Game Pass, PS Plus, Canva, ChatGPT, GitHub Copilot, Adobe CC). | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-77] Suscripciones - Coherencia de Temas:** Garantizar que los textos de las tarjetas (a pesar de tener colores temáticos de marca) mantengan un contraste perfecto y legibilidad tanto en el tema oscuro como en el tema claro de la app. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / Diseño | **[F-78] Suscripciones - Ideas de Diseño Visual:** Implementar un layout moderno estilo "Bento Grid" para las tarjetas. Añadir gráficos visuales como "Gasto mensual vs anual en suscripciones" y una barra de progreso global (timeline) que muestre visualmente qué suscripciones están más cerca de vencer. | Baja | Pendiente | 15 de junio de 2026 | — |

---

### 🧠 Layout: Inteligencia Artificial

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-79] Inteligencia Artificial - Integración de DTOs:** Verificar y mapear correctamente la recepción de los nuevos DTOs expuestos por el `microservicio-ia`. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-80] Inteligencia Artificial - Sincronización en Header:** Actualizar (aumentar/disminuir) dinámicamente el contador de "Consultas IA" (Autoclasificar) mostrado en el header principal cada vez que el usuario utilice un módulo de simulación. | Alta | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-81] Inteligencia Artificial - Filtros Dinámicos:** Hacer completamente dinámicos los filtros dentro de los módulos IA y solventar los bugs visuales menores generados al interactuar con ellos. | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-82] Inteligencia Artificial - Pruebas ('cesar_test'):** Validar funcionalmente cada módulo del simulador utilizando la cuenta de prueba `cesar_test` para aprovechar su data semilla de 700 registros y comprobar el rendimiento/visualización real. | Alta | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-83] Inteligencia Artificial - Tooltips de Ayuda:** Implementar botones informativos "¿Cómo usar este simulador?" en cada uno de los submódulos, tomando como referencia base el comportamiento actual de "Simular Meta". | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-84] Inteligencia Artificial - Mejora Visual Interna:** Refinar sustancialmente el diseño, márgenes, proporciones y los estilos visuales del interior de cada submódulo interactivo de la vista IA. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-85] Inteligencia Artificial - Coherencia de Temas:** Asegurar que los componentes, paneles de simulación y textos de IA mantengan una alta legibilidad sin problemas visuales tanto en el tema oscuro como en el tema claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos / Textos | **[F-23] Texto de Sugerencia IA:** Cambiar el texto a: "Tienes 2 intentos de sugerencia por IA. El contador también es visible en el header como 'Autoclasificar'." | Baja | Pendiente | 12 de junio de 2026 | — |

---

### 👤 Layout: Perfil

| Categoría       | Descripción                                                                                                                                                                                                                                                               | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :-------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------- | :-------- | :------------------ | :-------------- |
| Pruebas / QA    | **[F-86] Perfil - Pruebas Endpoint Contraseña:** Ejecutar pruebas End-to-End sobre el botón de "Actualizar contraseña" (el único endpoint pendiente de esta sección) y registrar la información/resultados en el documento `manual_endpoints`.                            | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Integración API | **[F-87] Perfil - Sincronización Avatar y Formulario:** Garantizar el guardado correcto del Avatar en la BD, así como el mapeo exacto de todos los campos del formulario con los datos provenientes del backend, actualizando la UI en tiempo real ante cualquier cambio. | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Validación      | **[F-88] Perfil - Validación Estricta Numérica:** Implementar reglas rígidas en los inputs numéricos para bloquear el ingreso de letras o símbolos, y limitar exactamente los caracteres permitidos según el tipo de dato (ej. máximo 8 dígitos). *(Reemplaza a F-33)*    | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Estructura      | **[F-89] Perfil - Eliminación de Membresía e Historial:** Remover por completo la sección interna de "Membresía" de esta vista y asegurar que el Historial sea movido de forma independiente al menú principal.                                                           | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos         | **[F-90] Perfil - Coherencia Estética:** Auditar la vista de perfil para garantizar que los *inputs*, *labels* y textos sean completamente legibles y mantengan una alta fidelidad tanto en el tema claro como en el tema oscuro.                                         | Media     | Pendiente | 15 de junio de 2026 | —               |
| Modularización  | **[F-31] Modularizar Perfil:** Refactorizar y modularizar la sección Perfil.                                                                                                                                                                                              | Media     | Pendiente | 12 de junio de 2026 | —               |
| Estilos         | **[F-34] Perfil - Etiqueta Sin Verificar:** Cambiar el color de letra de la etiqueta "Sin verificar".                                                                                                                                                                     | Baja      | Pendiente | 12 de junio de 2026 | —               |

---

### 🔑 Layout: Login / Registro

| Categoría | Descripción                                                                | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :-------- | :------------------------------------------------------------------------- | :-------- | :----- | :---------------- | :-------------- |
| —         | *No hay modificaciones pendientes estrictamente frontend en esta sección.* | —         | —      | —                 | —               |

---

### 🗺️ Layout: Sidebar / Navegación

| Categoría  | Descripción                                                                                                                                                                                                                                                                  | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :--------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------- | :-------- | :------------------ | :-------------- |
| Navegación | **[F-07] El sidebar se reinicia al navegar a "Suscripción":** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). | Alta      | Pendiente | 12 de junio de 2026 | —               |

---

### ⚙️ Layout: Configuración

| Categoría         | Descripción                                                                                                                                                                                                                                                   | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :---------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------- | :-------- | :------------------ | :-------------- |
| Lógica de Negocio | **[F-97] Configuración - Ocultamiento Plan Premium:** Añadir lógica condicional para eliminar/ocultar completamente la sección de "Mejorar a Premium" si se detecta que el usuario ya cuenta con un plan Premium o Pro.                                       | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Pruebas / QA      | **[F-98] Configuración - Flujo de Eliminación de Cuenta:** Probar funcionalmente el botón de desactivar/eliminar cuenta, garantizando que siempre se despliegue un modal de advertencia/confirmación respectivo antes de enviar la solicitud al backend.      | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Interactividad    | **[F-99] Configuración - Motor de Color Principal:** Implementar la lógica para que los botones selectores de "Color Principal" modifiquen dinámicamente las variables CSS globales, logrando que el color cambie instantáneamente en toda la aplicación web. | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Navegación        | **[F-100] Configuración - Redirección a Ayuda:** Ajustar el enlace o botón de la subsección de soporte para que redirija de manera correcta a la vista de "Ayuda" de la plataforma.                                                                           | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos           | **[F-101] Configuración - Legibilidad de Textos (Temas):** Verificar de forma exhaustiva que la transición entre el tema claro y oscuro no cause la desaparición de letras, *labels* o textos por falta de contraste con los fondos.                          | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos           | **[F-36] Tema Oscuro:** La parte de selección de tema en Configuración debería notarse claramente en color oscuro.                                                                                                                                            | Baja      | Pendiente | 12 de junio de 2026 | —               |

---

### 📜 Layout: Historial (Sección Principal)

| Categoría  | Descripción                                                                                                                                                  | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :--------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------- | :-------- | :------------------ | :-------------- |
| Estructura | **[F-24] Unificar Tablas en Historial:** Unificar las tablas de ingreso y gastos en la sección historial principal y quitar las tablas de las sub-secciones. | Media     | Pendiente | 12 de junio de 2026 | —               |
| Navegación | **[F-26] Reubicación de Historial:** Cambiar la sección historial a la sección principal.                                                                    | Media     | Pendiente | 12 de junio de 2026 | —               |

---


## ✅ Modificaciones Resueltas (Historial)

# Modificaciones Pendientes Resueltas

Este documento registra el historial de modificaciones y tareas completadas (anteriormente listadas como pendientes) de la Plataforma de Microservicios Financieros.

---
### 🎯 Layout: Metas [Responsable: Paul Bendezu]

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-65] Metas - Integración de Campos:** Enlazar y renderizar correctamente todos los campos y atributos de datos que envía el backend para la sección de metas. | Alta | ✔ Completado | 15 de junio de 2026 | 16 de junio de 2026 |
| Interactividad | **[F-66] Metas - Funcionalidad de Filtros:** Hacer completamente operativos los filtros de visualización de metas, procesando correctamente la selección en el frontend. | Media | ✔ Completado | 15 de junio de 2026 | 16 de junio de 2026 |
| Estilos | **[F-67] Metas - Coherencia Estética:** Revisar el diseño para garantizar la correcta visualización de colores, jerarquías y textos de los componentes tanto en el tema oscuro como en el tema claro. | Media | ✔ Completado | 15 de junio de 2026 | 16 de junio de 2026 |
| Modularización | **[F-30] Modularizar Metas:** Refactorizar la sección aislando su lógica y estructura en microcomponentes dedicados. | Media | ✔ Completado | 12 de junio de 2026 | 16 de junio de 2026 |
| Lógica de Negocio | **[F-30-A] Nuevas Categorías de Metas:** Añadir tres propósitos adicionales (Salud, Inversión, Negocio) con iconos específicos y soporte en el listado y formulario. | Media | ✔ Completado | 15 de junio de 2026 | 16 de junio de 2026 |
| Arquitectura | **[F-30-B] Servicio Utilitario & Mocks:** Centralizar lógica de categorías, parseo de prefijos y ciclo de vida de almacenamiento mock local en `MetasUtilityService`. | Media | ✔ Completado | 15 de junio de 2026 | 16 de junio de 2026 |
| Arquitectura | **[F-30-C] Centralización de Datos (`MetasDataService`):** Aislar llamadas HTTP y lógica de fallback a mocks locales en un servicio de datos inyectable. | Alta | ✔ Completado | 16 de junio de 2026 | 16 de junio de 2026 |
| Interactividad | **[F-66-A] Filtro de Año por Fecha de Creación:** Modificar la lógica de filtros y años disponibles para operar sobre la fecha de creación de la meta. | Media | ✔ Completado | 16 de junio de 2026 | 16 de junio de 2026 |
| Estilos / Bugfix | **[F-67-A] Corrección de Alineación en Detalles:** Corregir el layout flexbox de "Faltan ahorrar" en la barra lateral de detalles de meta. | Alta | ✔ Completado | 16 de junio de 2026 | 16 de junio de 2026 |

---

### 📈 Layout: Perfil Financiero [Responsable: Paul Bendezu]

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-91] Perfil Financiero - Integración Completa:** Conectar eficientemente el frontend con el `microservicio-cliente` para el registro, lectura y actualización de los datos del perfil financiero. | Alta | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| Lógica de Negocio | **[F-92] Perfil Financiero - Ingresos Auto-Calculados:** Sincronizar el campo "Ingreso mensual" para que se calcule y autocomplete automáticamente a partir de los registros del cliente, bloqueando la edición manual. | Alta | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| Pruebas / QA | **[F-93] Perfil Financiero - Pruebas de Auto-Relleno:** Mapear y probar que campos como `tono-ia`, `ocupación`, `objetivo_principal`, etc., se precarguen correctamente en el formulario si el backend detecta que ya existen. | Alta | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| UX / Diseño | **[F-94] Perfil Financiero - Formulario de Registro (UI):** Diseñar una interfaz moderna para el registro de perfil. *Idea: Implementar un "Wizard" (formulario multi-pasos) que use sliders para "Tolerancia al riesgo" y tarjetas visuales (cards seleccionables) para escoger el "Tono IA", en lugar de inputs clásicos aburridos.* | Alta | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| Estructura | **[F-95] Perfil Financiero - Eliminación de Gráficos:** Eliminar definitivamente los gráficos circulares de la parte inferior de la vista ("Gastos por composición de ingresos" y "Gastos por categoría"). | Media | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| Estilos | **[F-96] Perfil Financiero - Coherencia Estética:** Garantizar que todos los nuevos componentes, sliders y tarjetas del formulario funcionen visualmente de forma perfecta tanto en el tema oscuro como en el tema claro. | Media | Realizado | 15 de junio de 2026 | 17 de junio de 2026 |
| Interactividad | **[F-35] Perfil Financiero - Filtros:** La parte de filtro de fechas debería separarse entre mes y año. | Baja | Realizado | 12 de junio de 2026 | 17 de junio de 2026 |
| Lógica de Negocio | **[F-97] Perfil Financiero - Catálogo Ampliado de Logros:** Expandir el número de logros a 16 con condiciones de progreso específicas conectadas con datos del resumen financiero. | Alta | Realizado | 16 de junio de 2026 | 17 de junio de 2026 |
| UX / Paginación | **[F-98] Perfil Financiero - Subruta y Paginación de Logros:** Registrar la subruta `financiero/logros`, y diseñar un grid responsivo con controles de paginación reactivos (8 elementos por página). | Alta | Realizado | 16 de junio de 2026 | 17 de junio de 2026 |
| Estructura | **[F-99] Perfil Financiero - Modularización de Servicios:** Dividir el service original gigante en 4 servicios específicos (`Financiero`, `Logros`, `Wizard`, `Reporte`) para mayor separación de responsabilidades y evitar dependencias circulares. | Alta | Realizado | 17 de junio de 2026 | 17 de junio de 2026 |
| Navegación | **[F-100] Perfil Financiero - Navegación Programática de Logros:** Implementar redirección programática mediante Angular `Router` para prevenir bloqueos de navegación en HMR. | Media | Realizado | 16 de junio de 2026 | 17 de junio de 2026 |
| Estilos | **[F-101] Perfil Financiero - Encapsulación de Estilos SCSS:** Corregir el anidamiento incorrecto de selectores bajo `:host` en los componentes mediante contenedores envolventes o directivas de host binding. | Media | Realizado | 16 de junio de 2026 | 17 de junio de 2026 |


## 🛡️ Auditoría Pre-Producción (Roadmap 4 Semanas)

Esta sección agrupa 20 requerimientos críticos de arquitectura, seguridad y rendimiento exigidos para el pase a producción final.

| Categoría | Descripción | Capa | Prioridad | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Seguridad | **[S-01] Guards de Rutas (Autenticación):** Implementar `AuthGuard` y `RoleGuard` en Angular para bloquear totalmente el acceso mediante URL directa a vistas privadas (Dashboard, Perfil, etc.) si no hay sesión activa. | Frontend | Alta | Pendiente |
| Seguridad | **[S-02] Validación Estricta JWT:** El API Gateway o microservicios deben validar rigurosamente la firma, expiración y emisor del Token JWT en cada petición HTTP, rechazando con HTTP 401 si es inválido. | Backend | Alta | Completado |
| Seguridad | **[S-03] Protección CSRF & XSS:** Desinfectar (*sanitize*) sistemáticamente todos los inputs de texto en Angular y habilitar cabeceras HTTP de seguridad (Helmet, CORS estrictos) en el backend. | Fullstack | Alta | Pendiente |
| Seguridad | **[S-04] Rate Limiting:** Implementar un límite de peticiones (Rate Limiting) en endpoints críticos (Login, IA, Registro) para frustrar ataques de fuerza bruta o de denegación de servicio (DDoS). | Backend | Alta | Completado |
| Seguridad | **[S-05] Cifrado de BD:** Asegurar que datos sensibles como contraseñas, documentos de identidad o tokens de integraciones estén debidamente encriptados o hasheados (BCrypt) en PostgreSQL. | Backend | Alta | Pendiente |
| Rendimiento | **[P-01] Caché Distribuido (Redis):** Implementar Redis en el backend para almacenar en caché consultas pesadas o catálogos estáticos (ej. listas de categorías) que no cambian constantemente, aliviando la BD. | Backend | Alta | Completado |
| Rendimiento | **[P-02] Paginación Real en Base de Datos:** Garantizar que los endpoints que retornan grandes volúmenes de datos utilicen paginación a nivel de SQL (`LIMIT`/`OFFSET`) y no carguen toda la tabla a memoria. | Backend | Alta | Completado |
| Rendimiento | **[P-03] Optimización de Assets (Imágenes):** Convertir avatares y recursos gráficos a formatos de nueva generación (WebP) y aplicar estrategias de *Lazy Loading* nativo en todas las etiquetas de imagen de la app. | Frontend | Media | Pendiente |
| Rendimiento | **[P-04] Tree Shaking & Bundle Size:** Auditar el bundle de Angular (ej. con *webpack-bundle-analyzer*), removiendo dependencias no utilizadas para reducir drásticamente el peso inicial de la aplicación. | Frontend | Alta | Pendiente |
| Rendimiento | **[P-05] Compresión HTTP:** Habilitar compresión GZIP o Brotli en el Ingress/BFF/Servidor para reducir el peso en la transferencia de los JSON de respuesta. | Backend | Media | Pendiente |
| Lógica | **[L-01] Transacciones ACID (Rollback):** Envolver los procesos críticos de guardado en transacciones de Spring Boot (`@Transactional`) para asegurar que si algo falla, ocurra un *rollback* y los datos no queden inconsistentes. | Backend | Alta | Completado |
| Lógica | **[L-02] Manejo Global de Excepciones:** Estandarizar las respuestas de error usando `@ControllerAdvice` para retornar siempre el modelo `ResultadoApi<T>`, evitando la fuga de trazas internas (stacktraces) de Java al cliente. | Backend | Alta | Completado |
| Lógica | **[L-03] Manejo de Estado (Signals/Store):** Aprovechar Signals (Angular 17+) o NgRx para centralizar el estado, evitando peticiones API redundantes al navegar entre vistas que consumen la misma data. | Frontend | Alta | Completado |
| Lógica | **[L-04] Flujo de Refresh Token:** Establecer un mecanismo robusto y silencioso de rotación de tokens (Refresh Token) para evitar que la sesión de los usuarios expire abruptamente mientras usan activamente el SaaS. | Fullstack | Alta | Pendiente |
| Lógica | **[L-05] Mockup System Integrado:** Crear una suite de "Mockups de Desarrollo" (similar a la cuenta `cesar_test`) accesible mediante un botón en desarrollo, para simular flujos sin consumir cuotas de API de IA o pasarelas de pago reales. | Fullstack | Media | Pendiente |
| UX / UI | **[U-01] Esqueletos de Carga (Skeleton Loaders):** Reemplazar los *spinners* clásicos por *Skeleton Loaders* en gráficos y tablas, mejorando psicológicamente la percepción de velocidad para el usuario. | Frontend | Media | Pendiente |
| Navegación | **[U-02] Breadcrumbs Dinámicos:** Construir un sistema de migas de pan automáticas en el Header que permita al usuario saber rápidamente en qué sub-módulo está y navegar hacia arriba fácilmente. | Frontend | Baja | Completado |
| UX / UI | **[U-03] Control de Cambios no Guardados:** Activar un `CanDeactivate Guard` que lance un modal de advertencia ("Tienes cambios sin guardar") si el usuario intenta cambiar de vista mientras rellena un formulario importante. | Frontend | Alta | Pendiente |
| UX / UI | **[U-04] Feedback de Acciones (Toasts Globales):** Asegurar que el 100% de las transacciones (guardar, eliminar, actualizar) disparen un mensaje flotante (*Toast* o *Snackbar*) informando éxito o error para mantener tranquilo al usuario. | Frontend | Alta | Pendiente |
| Estructura | **[U-05] Entorno de Pruebas "Staging":** Orquestar mediante Docker Compose un entorno espejo de Staging completo (BBDD + Backend empaquetado + Frontend servido con Nginx) idéntico a producción para el QA final. | DevOps | Alta | Pendiente |

---

## 📊 Resumen Estadístico de Pendientes

| Capa | Total Pendientes |
| :--- | :--- |
| **Backend / DevOps** | 17 |
| **Frontend** | 78 |
| **Total General** | **95** |

> [!NOTE]
> La sección de Dashboard está explícitamente asignada a Paulo Moron. El resto de las áreas no cuentan con un responsable asignado aún.


<details>
<summary>📄 Respaldo de Archivo Duplicado (MODIFICACIONES PENDIENTES.md)</summary>

Se mantiene este contenido original para cumplir estrictamente con no eliminar ningún registro del archivo duplicado.

# 📋 Registro de Modificaciones Pendientes — LUKA APP

Este documento centraliza todas las modificaciones, refactorizaciones, pruebas, errores y bugs **pendientes** por resolver en el ecosistema de **LUKA APP** (Frontend y Backend).

> **Última actualización:** 15 de junio de 2026
> **Responsable del documento:** Paulo Moron (Cloud Architect)
> **Estado:** En Progreso — Solo se listan los ítems pendientes por solucionar.

---

## 🔧 Capa Backend

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Backend | **[B-05] Gastos de Suscripción:** El backend no conecta los gastos pagados and pendientes que vienen de la sección suscripción. | Alta | Pendiente | 12 de junio de 2026 | — |
| Backend | **[B-06] Dashboard - Sincronización por Eventos:** Verificar que los datos del dashboard se actualicen en tiempo real e inmediatamente después del ingreso de transacciones (gastos e ingresos) a través de eventos. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-46] Gastos - Sincronización de Suscripciones:** Integrar las tarjetas de "Pendiente por pagar" y "Próximo vencimiento" con el `microservicio-suscripciones`, extrayendo los datos reales de las suscripciones del usuario asociadas a la tarjeta (aún no integrado en frontend). | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-53] Gastos - Verificación End-to-End:** Realizar pruebas integrales en la vista de gastos para asegurar que todo el layout funcione en perfecta armonía con la integración final del backend. | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-29] Usuario Mock para Pruebas:** Agregar un usuario mock con correo `prueba@gmail.com` y contraseña `12345` para probar el flujo de ingreso, omitiendo la restricción de contraseña solo por esta vez. | Alta | Pendiente | 12 de junio de 2026 | — |
| Backend / API | **[B-07] API - Filtro de Ingresos:** Garantizar que los endpoints dedicados a la sección de Ingresos devuelvan exclusivamente datos y transacciones de tipo ingreso (excluyendo cualquier gasto). | Alta | Pendiente | 15 de junio de 2026 | — |
| Arquitectura | **[B-08] API - Streaming de Datos:** Implementar flujos reactivos (como Server-Sent Events - SSE o WebSockets) para el envío de volúmenes pesados de datos. En lugar de procesar peticiones masivas síncronas, el backend debe despachar la información mediante eventos y bloques progresivos al cliente. | Alta | Pendiente | 15 de junio de 2026 | — |

---

## 🎨 Capa Frontend (Angular)

### 🌐 Estándares Transversales (Aplica a todo el Frontend)

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Arquitectura | **[F-63] Modularización Global:** Promover de forma estricta la modularización de cada vista del sistema en componentes pequeños, altamente cohesivos y reutilizables (Standalone Components) para maximizar la escalabilidad y mantenibilidad. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-64] Consumo Reactivo (Eventos):** Adaptar los servicios y el frontend para recibir grandes volúmenes de información en tiempo real a través de un bus de eventos o SSE, renderizando la información progresivamente en la UI en lugar de bloquear la pantalla esperando un solo payload masivo. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-54] Formateo de Decimales:** En todas las secciones (Dashboard, Gastos, Ingresos, etc.), asegurar que cualquier cifra numérica que no sea entera se renderice estrictamente limitándose a dos decimales. | Alta | Pendiente | 15 de junio de 2026 | — |
| Rendimiento | **[F-55] Optimización General:** Auditar y aplicar lazy loading, minificación de componentes y reducción de carga inicial en todas las vistas, garantizando velocidad extrema y una experiencia de usuario fluida. | Alta | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-56] Mejora Continua de UI:** Realizar una revisión global de estilos en todas las pantallas para refinar el diseño, mejorar el contraste entre temas (claro/oscuro) y pulir micro-interacciones (hover, focus, transitions). | Media | Pendiente | 15 de junio de 2026 | — |

---

### 📊 Layout: Dashboard `[Responsable: Paulo Moron]`

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Interactividad | **[F-37] Dashboard - Filtros y Búsqueda:** Hacer funcional los filtros y la barra de búsqueda del dashboard para búsquedas y filtrados en tiempo real. | Alta | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-40] Dashboard - Tendencia de Flujo de Caja:** Incorporar todos los meses al gráfico de flujo de caja y habilitar la capacidad de filtrarlos dinámicamente por año. | Alta | Pendiente | 15 de junio de 2026 | — |
| Rendimiento | **[F-42] Dashboard - Carga Rápida:** Optimizar el BFF y el frontend para asegurar una carga ultra-rápida y renderizado inmediato de los datos consolidados en pantalla. | Alta | Pendiente | 15 de junio de 2026 | — |
| Rendimiento | **[F-44] Dashboard - Renderizado y Lazy Loading:** Modularizar y optimizar el layout utilizando carga diferida (lazy loading) en Angular y estrategias de renderizado óptimas para mejorar los Core Web Vitals. | Alta | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-21] Dashboard - Filtro de Fechas:** El filtro "Entre fechas" debe permitir desde la fecha que se elige hasta el día de hoy. | Media | Pendiente | 12 de junio de 2026 | — |
| Estructura | **[F-22] Dashboard - Historial:** Eliminar el historial de transacciones del Dashboard. | Media | Pendiente | 12 de junio de 2026 | — |
| Estilos | **[F-38] Dashboard - Coherencia Estética:** Verificar que los estilos de letras para cada componente del layout se encuentren coherentes y legibles en los modos claro y oscuro según el tema activo. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-39] Dashboard - Distribución de Gastos (Top 5):** Ajustar el gráfico de distribución de gastos para mostrar exclusivamente los 5 gastos de mayor importancia. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-41] Dashboard - Enriquecimiento Visual:** Añadir gráficos estadísticos complementarios (línea de tiempo de actividad, gráficos de barras comparativos, anillos/donas e histogramas de dispersión) que enriquezcan la UX. | Media | Pendiente | 15 de junio de 2026 | — |
| Rendimiento | **[F-43] Dashboard - Peso de Componentes:** Optimizar el CSS/SCSS del layout y componentes individuales para asegurar que los estilos y el script no excedan el peso de bundle óptimo. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-25] Tema Oscuro - Header y Dashboard:** En modo oscuro las letras del header no se notan claro; definir un color visible para las letras sin afectar el tema claro. En el dashboard (ej. periodo) las letras deben ser blancas con hover más blanco, sin afectar el color principal. | Baja | Pendiente | 12 de junio de 2026 | — |
| Rendimiento/UX | **[F-102] Dashboard - Responsividad de Gráficos:** Ajuste dinámico del ancho de los gráficos (compactación y expansión a full-width) cuando hay pocos datos, con eliminación total del scroll horizontal. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Datos/Mocks | **[F-103] Dashboard - Corrección de Mock Data:** Ajuste en la generación de datos para reportar los 7 días en la vista semanal y nombres dinámicos de meses en el flujo de caja en lugar de estáticos. | Media | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Gráficos | **[F-104] Dashboard - Prevención de Colisiones:** Lógica dinámica en las etiquetas de datos del gráfico de tendencia para evitar solapamientos y choques con las etiquetas de los ejes X e Y usando boundary checks. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| UX / Layout | **[F-105] Dashboard - Layout Adaptativo (Corto Plazo):** Reorganización dinámica inteligente del orden de los gráficos del grid (2 filas) cuando se detectan rangos de 1 a 3 meses usando CSS `order` y proporciones personalizadas. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| UI / Gráficos | **[F-106] Dashboard - Leyendas Inferiores en Donas:** Reubicación de las leyendas a la posición inferior (`bottom`) en gráficos circulares (Distribución y Pago) y eliminación del padding asimétrico para maximizar la visibilidad y el centrado del gráfico. | Media | Completado | 20 de junio de 2026 | 20 de junio de 2026 |
| Bugfix / Gráficos | **[F-107] Dashboard - Corrección de Expansión (Chart.js):** Solución al desbordamiento y retención del tamaño del grid (bug de Chart.js al cambiar de filtros) incorporando `min-width: 0` y `overflow: hidden` a todos los contenedores principales. | Alta | Completado | 20 de junio de 2026 | 20 de junio de 2026 |

---

### 💳 Layout: Gastos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Navegación | **[F-52] Gastos - Sub-sección de Registro:** Refactorizar el botón "Registrar gasto" para que, en lugar de abrir un modal, navegue hacia una sub-sección dedicada (`/gastos/registrar`), actualizando dinámicamente la ruta en el header. | Alta | Pendiente | 15 de junio de 2026 | — |
| Validación | **[F-28] Límite en Descripción:** En el formulario de gastos, agregar un límite de 200 caracteres en la descripción. | Media | Pendiente | 12 de junio de 2026 | — |
| Estilos | **[F-45] Gastos - Coherencia Estética:** Auditar y corregir el texto para asegurar una legibilidad perfecta y evitar problemas visuales o falta de contraste al alternar entre el tema oscuro y el claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-47] Gastos - Reemplazo de Tablas por Gráficos:** Eliminar la tabla de pendientes y recurrentes. Implementar en su lugar gráficos funcionales (ej. un gráfico de anillos/donut para mostrar la proporción de gastos recurrentes por servicio, o un gráfico de barras apiladas para visualizar los próximos pagos en el mes). | Media | Pendiente | 15 de junio de 2026 | — |
| Estructura | **[F-48] Gastos - Eliminación de Historial Local:** Remover la tabla de historial organizado de esta vista, ya que se consolidará de forma centralizada en la nueva sección global "Historial" con filtros dinámicos. | Media | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-49] Gastos - Top Categorías del Mes:** Modificar el bloque de "Gastos por categoría" para que filtre exclusivamente los datos del mes actual y renderice únicamente las 6 categorías más importantes (Top 6), dejando los totales al dashboard. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-50] Gastos - Mejora de Tabla Top Días:** Hacer funcional el filtro de la tabla "Top de días de gasto". Como idea de mejora visual, incorporar barras de progreso en línea (sparklines) o un mini mapa de calor (heatmap) para identificar rápidamente la intensidad del gasto diario. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-51] Gastos - Componentes Visuales Adicionales:** Incorporar componentes de apoyo visual (ej. tarjetas de insights, alertas de presupuesto excedido o micro-animaciones) que mejoren la experiencia del usuario sin saturar la vista. | Baja | Pendiente | 15 de junio de 2026 | — |

---

### 💵 Layout: Ingresos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Estructura | **[F-57] Ingresos - Botón de Historial:** Eliminar el botón de "Historial" de la parte inferior del layout, dejando exclusivamente el superior para mantener consistencia con la vista de gastos. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-58] Ingresos - Tarjetas KPI:** Añadir dos tarjetas KPI estratégicas junto a "Ingresos registrados" y "Categoría principal" (por ejemplo: "Comparación mes anterior (%)" e "Ingreso Promedio" o "Progreso de Meta"). | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-59] Ingresos - Gráfico de Categoría:** Limitar el gráfico de "Ingresos por Categoría" para que renderice exclusivamente el Top 5 de fuentes de ingreso más relevantes. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-60] Ingresos - Paginación de Recientes:** Ajustar la tabla de "Ingresos recientes" para mostrar únicamente los 6 primeros registros, incorporando controles de paginación funcionales para navegar por el resto de datos. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-61] Ingresos - Filtros Independientes:** Asegurar que cada gráfico dentro de este layout tenga sus propios selectores o filtros, de forma que cambiar el filtro de un gráfico no afecte inadvertidamente a los demás. | Media | Pendiente | 15 de junio de 2026 | — |
| Gráficos | **[F-62] Ingresos - Nuevos Gráficos:** Agregar dos gráficos funcionales adicionales que aporten valor a la experiencia de usuario (por ejemplo: "Evolución Mensual de Ingresos" y "Proporción de Ingreso Fijo vs Variable"). | Media | Pendiente | 15 de junio de 2026 | — |

---

### 🎯 Layout: Metas

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-65] Metas - Integración de Campos:** Enlazar y renderizar correctamente todos los campos y atributos de datos que envía el backend para la sección de metas. | Alta | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-66] Metas - Funcionalidad de Filtros:** Hacer completamente operativos los filtros de visualización de metas, procesando correctamente la selección en el frontend. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-67] Metas - Coherencia Estética:** Revisar el diseño para garantizar la correcta visualización de colores, jerarquías y textos de los componentes tanto en el tema oscuro como en el tema claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Modularización | **[F-30] Modularizar Metas:** Refactorizar la sección aislando su lógica y estructura en microcomponentes dedicados. | Media | Pendiente | 12 de junio de 2026 | — |

---

### 📅 Layout: Presupuestos

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-68] Presupuestos - Integración Completa:** Integrar de manera correcta y completa todo el layout de presupuestos con su respectivo endpoint en el backend. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-69] Presupuestos - Corrección de Mapeo (NaN):** Mapear correctamente los campos del "Historial de límites" recibidos del backend para corregir el bug actual donde se renderizan campos vacíos o valores `NaN`. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-70] Presupuestos - Desglose por Categorías:** Ajustar la sección de desglose para mostrar exclusivamente las 5 categorías principales (Top 5). Estos datos deben calcularse y filtrarse estrictamente según la fecha de inicio y fin del presupuesto. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-71] Presupuestos - Paginación de Historial:** Modificar la tabla de "Historial de límites" para que muestre únicamente los primeros 5 registros por defecto, implementando controles de paginación para visualizar los siguientes. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-72] Presupuestos - Coherencia Estética:** Auditar y garantizar la correcta visualización de colores, contraste y textos de todo el layout tanto en el tema oscuro como en el claro. | Media | Pendiente | 15 de junio de 2026 | — |

---

### 🔔 Layout: Suscripciones

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Estructura | **[F-73] Suscripciones - Creación de Layout Modular:** Diseñar y construir el nuevo layout de Suscripciones desde cero utilizando Standalone Components, asegurando que sea completamente modular e independiente. | Alta | Pendiente | 15 de junio de 2026 | — |
| Integración API | **[F-74] Suscripciones - Integración Completa:** Conectar y mapear correctamente todos los endpoints del backend para renderizar la lista de suscripciones activas, inactivas y vencidas. | Alta | Pendiente | 15 de junio de 2026 | — |
| Navegación | **[F-07] El sidebar se reinicia al navegar a "Suscripción":** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). *(Nota: Relacionado con Sidebar / Navegación)* | Alta | Pendiente | 12 de junio de 2026 | — |
| Lógica de Negocio | **[F-75] Suscripciones - Destacar Plan Luka:** Sincronizar el módulo para identificar si el usuario cuenta con el "Plan de Luka" (nuestra plataforma) y fijarlo dinámicamente como la suscripción principal y más destacada en la parte superior. | Media | Pendiente | 15 de junio de 2026 | — |
| UI / Estilos | **[F-76] Suscripciones - Tarjetas Temáticas (Brand Styles):** Estilizar las *cards* de suscripción aplicando paletas de colores representativas (Ej: Netflix, Spotify, YouTube, Twitch, Amazon Prime, Disney+, Max/HBO, Apple Music, Xbox Game Pass, PS Plus, Canva, ChatGPT, GitHub Copilot, Adobe CC). | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-77] Suscripciones - Coherencia de Temas:** Garantizar que los textos de las tarjetas (a pesar de tener colores temáticos de marca) mantengan un contraste perfecto y legibilidad tanto en el tema oscuro como en el tema claro de la app. | Media | Pendiente | 15 de junio de 2026 | — |
| UX / Diseño | **[F-78] Suscripciones - Ideas de Diseño Visual:** Implementar un layout moderno estilo "Bento Grid" para las tarjetas. Añadir gráficos visuales como "Gasto mensual vs anual en suscripciones" y una barra de progreso global (timeline) que muestre visualmente qué suscripciones están más cerca de vencer. | Baja | Pendiente | 15 de junio de 2026 | — |

---

### 🧠 Layout: Inteligencia Artificial

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-79] Inteligencia Artificial - Integración de DTOs:** Verificar y mapear correctamente la recepción de los nuevos DTOs expuestos por el `microservicio-ia`. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-80] Inteligencia Artificial - Sincronización en Header:** Actualizar (aumentar/disminuir) dinámicamente el contador de "Consultas IA" (Autoclasificar) mostrado en el header principal cada vez que el usuario utilice un módulo de simulación. | Alta | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-81] Inteligencia Artificial - Filtros Dinámicos:** Hacer completamente dinámicos los filtros dentro de los módulos IA y solventar los bugs visuales menores generados al interactuar con ellos. | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-82] Inteligencia Artificial - Pruebas ('cesar_test'):** Validar funcionalmente cada módulo del simulador utilizando la cuenta de prueba `cesar_test` para aprovechar su data semilla de 700 registros y comprobar el rendimiento/visualización real. | Alta | Pendiente | 15 de junio de 2026 | — |
| UX / UI | **[F-83] Inteligencia Artificial - Tooltips de Ayuda:** Implementar botones informativos "¿Cómo usar este simulador?" en cada uno de los submódulos, tomando como referencia base el comportamiento actual de "Simular Meta". | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-84] Inteligencia Artificial - Mejora Visual Interna:** Refinar sustancialmente el diseño, márgenes, proporciones y los estilos visuales del interior de cada submódulo interactivo de la vista IA. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-85] Inteligencia Artificial - Coherencia de Temas:** Asegurar que los componentes, paneles de simulación y textos de IA mantengan una alta legibilidad sin problemas visuales tanto en el tema oscuro como en el tema claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos / Textos | **[F-23] Texto de Sugerencia IA:** Cambiar el texto a: "Tienes 2 intentos de sugerencia por IA. El contador también es visible en el header como 'Autoclasificar'." | Baja | Pendiente | 12 de junio de 2026 | — |

---

### 📈 Layout: Perfil Financiero

| Categoría | Descripción | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Integración API | **[F-91] Perfil Financiero - Integración Completa:** Conectar eficientemente el frontend con el `microservicio-cliente` para el registro, lectura y actualización de los datos del perfil financiero. | Alta | Pendiente | 15 de junio de 2026 | — |
| Lógica de Negocio | **[F-92] Perfil Financiero - Ingresos Auto-Calculados:** Sincronizar el campo "Ingreso mensual" para que se calcule y autocomplete automáticamente a partir de los registros del cliente, bloqueando la edición manual. | Alta | Pendiente | 15 de junio de 2026 | — |
| Pruebas / QA | **[F-93] Perfil Financiero - Pruebas de Auto-Relleno:** Mapear y probar que campos como `tono-ia`, `ocupación`, `objetivo_principal`, etc., se precarguen correctamente en el formulario si el backend detecta que ya existen. | Alta | Pendiente | 15 de junio de 2026 | — |
| UX / Diseño | **[F-94] Perfil Financiero - Formulario de Registro (UI):** Diseñar una interfaz moderna para el registro de perfil. *Idea: Implementar un "Wizard" (formulario multi-pasos) que use sliders para "Tolerancia al riesgo" y tarjetas visuales (cards seleccionables) para escoger el "Tono IA", en lugar de inputs clásicos aburridos.* | Alta | Pendiente | 15 de junio de 2026 | — |
| Estructura | **[F-95] Perfil Financiero - Eliminación de Gráficos:** Eliminar definitivamente los gráficos circulares de la parte inferior de la vista ("Gastos por composición de ingresos" y "Gastos por categoría"). | Media | Pendiente | 15 de junio de 2026 | — |
| Estilos | **[F-96] Perfil Financiero - Coherencia Estética:** Garantizar que todos los nuevos componentes, sliders y tarjetas del formulario funcionen visualmente de forma perfecta tanto en el tema oscuro como en el tema claro. | Media | Pendiente | 15 de junio de 2026 | — |
| Interactividad | **[F-35] Perfil Financiero - Filtros:** La parte de filtro de fechas debería separarse entre mes y año. | Baja | Pendiente | 12 de junio de 2026 | — |

---

### 👤 Layout: Perfil

| Categoría       | Descripción                                                                                                                                                                                                                                                               | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :-------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------- | :-------- | :------------------ | :-------------- |
| Pruebas / QA    | **[F-86] Perfil - Pruebas Endpoint Contraseña:** Ejecutar pruebas End-to-End sobre el botón de "Actualizar contraseña" (el único endpoint pendiente de esta sección) y registrar la información/resultados en el documento `manual_endpoints`.                            | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Integración API | **[F-87] Perfil - Sincronización Avatar y Formulario:** Garantizar el guardado correcto del Avatar en la BD, así como el mapeo exacto de todos los campos del formulario con los datos provenientes del backend, actualizando la UI en tiempo real ante cualquier cambio. | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Validación      | **[F-88] Perfil - Validación Estricta Numérica:** Implementar reglas rígidas en los inputs numéricos para bloquear el ingreso de letras o símbolos, y limitar exactamente los caracteres permitidos según el tipo de dato (ej. máximo 8 dígitos). *(Reemplaza a F-33)*    | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Estructura      | **[F-89] Perfil - Eliminación de Membresía e Historial:** Remover por completo la sección interna de "Membresía" de esta vista y asegurar que el Historial sea movido de forma independiente al menú principal.                                                           | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos         | **[F-90] Perfil - Coherencia Estética:** Auditar la vista de perfil para garantizar que los *inputs*, *labels* y textos sean completamente legibles y mantengan una alta fidelidad tanto en el tema claro como en el tema oscuro.                                         | Media     | Pendiente | 15 de junio de 2026 | —               |
| Modularización  | **[F-31] Modularizar Perfil:** Refactorizar y modularizar la sección Perfil.                                                                                                                                                                                              | Media     | Pendiente | 12 de junio de 2026 | —               |
| Estilos         | **[F-34] Perfil - Etiqueta Sin Verificar:** Cambiar el color de letra de la etiqueta "Sin verificar".                                                                                                                                                                     | Baja      | Pendiente | 12 de junio de 2026 | —               |

---

### 🔑 Layout: Login / Registro

| Categoría | Descripción                                                                | Prioridad | Estado | Fecha de registro | Fecha de cambio |
| :-------- | :------------------------------------------------------------------------- | :-------- | :----- | :---------------- | :-------------- |
| —         | *No hay modificaciones pendientes estrictamente frontend en esta sección.* | —         | —      | —                 | —               |

---

### 🗺️ Layout: Sidebar / Navegación

| Categoría  | Descripción                                                                                                                                                                                                                                                                  | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :--------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------- | :-------- | :------------------ | :-------------- |
| Navegación | **[F-07] El sidebar se reinicia al navegar a "Suscripción":** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). | Alta      | Pendiente | 12 de junio de 2026 | —               |

---

### ⚙️ Layout: Configuración

| Categoría         | Descripción                                                                                                                                                                                                                                                   | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :---------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------- | :-------- | :------------------ | :-------------- |
| Lógica de Negocio | **[F-97] Configuración - Ocultamiento Plan Premium:** Añadir lógica condicional para eliminar/ocultar completamente la sección de "Mejorar a Premium" si se detecta que el usuario ya cuenta con un plan Premium o Pro.                                       | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Pruebas / QA      | **[F-98] Configuración - Flujo de Eliminación de Cuenta:** Probar funcionalmente el botón de desactivar/eliminar cuenta, garantizando que siempre se despliegue un modal de advertencia/confirmación respectivo antes de enviar la solicitud al backend.      | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Interactividad    | **[F-99] Configuración - Motor de Color Principal:** Implementar la lógica para que los botones selectores de "Color Principal" modifiquen dinámicamente las variables CSS globales, logrando que el color cambie instantáneamente en toda la aplicación web. | Alta      | Pendiente | 15 de junio de 2026 | —               |
| Navegación        | **[F-100] Configuración - Redirección a Ayuda:** Ajustar el enlace o botón de la subsección de soporte para que redirija de manera correcta a la vista de "Ayuda" de la plataforma.                                                                           | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos           | **[F-101] Configuración - Legibilidad de Textos (Temas):** Verificar de forma exhaustiva que la transición entre el tema claro y oscuro no cause la desaparición de letras, *labels* o textos por falta de contraste con los fondos.                          | Media     | Pendiente | 15 de junio de 2026 | —               |
| Estilos           | **[F-36] Tema Oscuro:** La parte de selección de tema en Configuración debería notarse claramente en color oscuro.                                                                                                                                            | Baja      | Pendiente | 12 de junio de 2026 | —               |

---

### 📜 Layout: Historial (Sección Principal)

| Categoría  | Descripción                                                                                                                                                  | Prioridad | Estado    | Fecha de registro   | Fecha de cambio |
| :--------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------- | :-------- | :------------------ | :-------------- |
| Estructura | **[F-24] Unificar Tablas en Historial:** Unificar las tablas de ingreso y gastos en la sección historial principal y quitar las tablas de las sub-secciones. | Media     | Pendiente | 12 de junio de 2026 | —               |
| Navegación | **[F-26] Reubicación de Historial:** Cambiar la sección historial a la sección principal.                                                                    | Media     | Pendiente | 12 de junio de 2026 | —               |

---

## 🛡️ Auditoría Pre-Producción (Roadmap 4 Semanas)

Esta sección agrupa 20 requerimientos críticos de arquitectura, seguridad y rendimiento exigidos para el pase a producción final.

| Categoría | Descripción | Capa | Prioridad | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Seguridad | **[S-01] Guards de Rutas (Autenticación):** Implementar `AuthGuard` y `RoleGuard` en Angular para bloquear totalmente el acceso mediante URL directa a vistas privadas (Dashboard, Perfil, etc.) si no hay sesión activa. | Frontend | Alta | Pendiente |
| Seguridad | **[S-02] Validación Estricta JWT:** El API Gateway o microservicios deben validar rigurosamente la firma, expiración y emisor del Token JWT en cada petición HTTP, rechazando con HTTP 401 si es inválido. | Backend | Alta | Completado |
| Seguridad | **[S-03] Protección CSRF & XSS:** Desinfectar (*sanitize*) sistemáticamente todos los inputs de texto en Angular y habilitar cabeceras HTTP de seguridad (Helmet, CORS estrictos) en el backend. | Fullstack | Alta | Pendiente |
| Seguridad | **[S-04] Rate Limiting:** Implementar un límite de peticiones (Rate Limiting) en endpoints críticos (Login, IA, Registro) para frustrar ataques de fuerza bruta o de denegación de servicio (DDoS). | Backend | Alta | Completado |
| Seguridad | **[S-05] Cifrado de BD:** Asegurar que datos sensibles como contraseñas, documentos de identidad o tokens de integraciones estén debidamente encriptados o hasheados (BCrypt) en PostgreSQL. | Backend | Alta | Pendiente |
| Rendimiento | **[P-01] Caché Distribuido (Redis):** Implementar Redis en el backend para almacenar en caché consultas pesadas o catálogos estáticos (ej. listas de categorías) que no cambian constantemente, aliviando la BD. | Backend | Alta | Completado |
| Rendimiento | **[P-02] Paginación Real en Base de Datos:** Garantizar que los endpoints que retornan grandes volúmenes de datos utilicen paginación a nivel de SQL (`LIMIT`/`OFFSET`) y no carguen toda la tabla a memoria. | Backend | Alta | Completado |
| Rendimiento | **[P-03] Optimización de Assets (Imágenes):** Convertir avatares y recursos gráficos a formatos de nueva generación (WebP) y aplicar estrategias de *Lazy Loading* nativo en todas las etiquetas de imagen de la app. | Frontend | Media | Pendiente |
| Rendimiento | **[P-04] Tree Shaking & Bundle Size:** Auditar el bundle de Angular (ej. con *webpack-bundle-analyzer*), removiendo dependencias no utilizadas para reducir drásticamente el peso inicial de la aplicación. | Frontend | Alta | Pendiente |
| Rendimiento | **[P-05] Compresión HTTP:** Habilitar compresión GZIP o Brotli en el Ingress/BFF/Servidor para reducir el peso en la transferencia de los JSON de respuesta. | Backend | Media | Pendiente |
| Lógica | **[L-01] Transacciones ACID (Rollback):** Envolver los procesos críticos de guardado en transacciones de Spring Boot (`@Transactional`) para asegurar que si algo falla, ocurra un *rollback* y los datos no queden inconsistentes. | Backend | Alta | Completado |
| Lógica | **[L-02] Manejo Global de Excepciones:** Estandarizar las respuestas de error usando `@ControllerAdvice` para retornar siempre el modelo `ResultadoApi<T>`, evitando la fuga de trazas internas (stacktraces) de Java al cliente. | Backend | Alta | Completado |
| Lógica | **[L-03] Manejo de Estado (Signals/Store):** Aprovechar Signals (Angular 17+) o NgRx para centralizar el estado, evitando peticiones API redundantes al navegar entre vistas que consumen la misma data. | Frontend | Alta | Completado |
| Lógica | **[L-04] Flujo de Refresh Token:** Establecer un mecanismo robusto y silencioso de rotación de tokens (Refresh Token) para evitar que la sesión de los usuarios expire abruptamente mientras usan activamente el SaaS. | Fullstack | Alta | Pendiente |
| Lógica | **[L-05] Mockup System Integrado:** Crear una suite de "Mockups de Desarrollo" (similar a la cuenta `cesar_test`) accesible mediante un botón en desarrollo, para simular flujos sin consumir cuotas de API de IA o pasarelas de pago reales. | Fullstack | Media | Pendiente |
| UX / UI | **[U-01] Esqueletos de Carga (Skeleton Loaders):** Reemplazar los *spinners* clásicos por *Skeleton Loaders* en gráficos y tablas, mejorando psicológicamente la percepción de velocidad para el usuario. | Frontend | Media | Pendiente |
| Navegación | **[U-02] Breadcrumbs Dinámicos:** Construir un sistema de migas de pan automáticas en el Header que permita al usuario saber rápidamente en qué sub-módulo está y navegar hacia arriba fácilmente. | Frontend | Baja | Completado |
| UX / UI | **[U-03] Control de Cambios no Guardados:** Activar un `CanDeactivate Guard` que lance un modal de advertencia ("Tienes cambios sin guardar") si el usuario intenta cambiar de vista mientras rellena un formulario importante. | Frontend | Alta | Pendiente |
| UX / UI | **[U-04] Feedback de Acciones (Toasts Globales):** Asegurar que el 100% de las transacciones (guardar, eliminar, actualizar) disparen un mensaje flotante (*Toast* o *Snackbar*) informando éxito o error para mantener tranquilo al usuario. | Frontend | Alta | Pendiente |
| Estructura | **[U-05] Entorno de Pruebas "Staging":** Orquestar mediante Docker Compose un entorno espejo de Staging completo (BBDD + Backend empaquetado + Frontend servido con Nginx) idéntico a producción para el QA final. | DevOps | Alta | Pendiente |

---

## 📊 Resumen Estadístico de Pendientes

| Capa | Total Pendientes |
| :--- | :--- |
| **Backend / DevOps** | 17 |
| **Frontend** | 78 |
| **Total General** | **95** |

> [!NOTE]
> La sección de Dashboard está explícitamente asignada a Paulo Moron. El resto de las áreas no cuentan con un responsable asignado aún.

</details>
