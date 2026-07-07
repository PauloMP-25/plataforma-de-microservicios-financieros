# 📋 Reporte de Observaciones y Mejoras Pendientes — Luka App

Este documento detalla de manera estructurada y clara los requerimientos de corrección y mejora para las secciones de Frontend (Angular) y Backend de la aplicación.

---

## 🎨 Capa Frontend (Angular)

### 📊 Dashboard & Filtros (LISTO)
1. **Filtro de "Última Semana" a "Últimos 6 Meses":**
   * Cambiar la opción de filtro "Última semana" por **"Últimos 6 meses"** (tanto en el selector como en la lógica del filtro correspondiente).
   * Al hacer clic en la **"X"** de limpieza de filtros rápidos, el sistema debe activar por defecto el filtro **"Este año"** y sincronizar/rellenar de forma automática los campos de fecha correspondientes (evitando que queden vacíos o mostrando un placeholder genérico del tipo `dd/mm/yyyy`).
2. **Alineación de KPI de Presupuesto:**
   * El indicador (card) de **"Uso de Presupuesto"** ubicado en el Dashboard principal debe sincronizarse y mostrar exactamente la misma información y alineación visual que el card del mismo nombre en la sección de **Límites**.
3. **Filtros por Año en Gráficos de Barras:**
   * Agregar botones rápidos para poder filtrar los datos por año en las gráficas de barra, habilitando los filtros para los años **2024, 2025 y 2026**.

---

### 💳 Suscripciones (LISTO)
1. **Flujo de Vencimiento de Suscripciones:**
   * Cuando una suscripción de plan Premium o Pro expire (se venza), no debe simplemente eliminarse de la vista o base de datos. Debe cambiar su estado visible a **Vencido/Inactivo** y la cuenta del usuario debe degradarse (actualizarse) automáticamente al rol **Free**.
   * Quitar el botón grande de **"Limpiar filtros"** en la sección de Suscripciones para mejorar el diseño.
2. **Listado de Plataformas Soportadas:**
   * Agregar una lista exhaustiva en el frontend (Typescript) de todas las plataformas donde se pueden pagar suscripciones con sus respectivos iconos personalizados (por ejemplo: WhatsApp, UTP, Netflix, Spotify, etc.).
   * **Nota (Iconos faltantes):** Es necesario descargar e incluir manualmente en formato SVG o PNG (dentro de `assets/logos/`) los logos de las siguientes plataformas, ya que **FontAwesome no los tiene en su catálogo**: Netflix, Disney+, HBO Max, Crunchyroll, ChatGPT Plus, Gemini Advanced, Claro, Movistar, WOW, Bitel, Canva Pro, Notion, Platzi, Coursera, Duolingo, Domestika, Udemy, Adobe Creative Cloud, Zoom Pro, Nintendo Switch Online, SmartFit, Bodytech, Strava y UTP.
3. **Integración Total con Backend:**
   * Conectar de manera definitiva todas las operaciones CRUD (Agregar, eliminar, editar) de la sección de Suscripciones con los endpoints correspondientes del Backend.

---

### 📜 Historial de Transacciones
1. **Corrección de Filtro de Categorías:**
   * Ajustar el comportamiento del filtro global de "Todas las categorías". Actualmente mezcla ingresos y gastos; debe filtrarlas dinámicamente según el tipo de movimiento seleccionado.
2. **Claridad en Filtros y Ordenamiento:**
   * El filtro "Más recientes" y la visualización de datos ordenados resultan confusos. Se requiere optimizar este ordenamiento visual en la UI.
   * Agregar un **botón de aplicar filtros** para evitar que se envíen múltiples peticiones repetitivas al servidor con cada cambio individual en los selectores.

---

### 📉 Sección de Gastos (Listo)
1. **Cálculo de Promedio Mensual:**
   * Verificar la lógica del card de **"Promedio de gastos mensual"**. Actualmente parece estar promediando sobre la base de todo el año, lo cual distorsiona la métrica real.
2. **Nuevo KPI de Gasto Mensual:**
   * Incorporar una tarjeta KPI adicional que muestre el **"Gasto total del mes actual"**.

---

### 📈 Sección de Ingresos
1. **Rediseño del Layout (Responsive):**
   * Adaptar de forma armónica la distribución de las tarjetas eliminando el card de **"Progreso de meta"** en su posición actual.
   * Quitar el texto secundario debajo de "Ingresos mes de julio".
   * Eliminar el gráfico de "Tipo de ingresos por método de ingresos de pago".
   * Intercambiar las ubicaciones de **"Por fuente de ingreso"** y la visualización de **"Progreso meta"**.
   * Asegurar la adaptabilidad móvil (responsiveness) en toda la sección.

---

### 🎯 Sección de Metas (LISTO)
1. **Actualización de Saldo Disponible:**
   * Al completar (cumplir) una meta, se debe recalcular y actualizar en tiempo real el **saldo disponible** tanto en el Header global de la aplicación como en la tarjeta interna de la sección de metas.
2. **Flujo de Creación (Monto Disponible):**
   * En el modal o sección para agregar una nueva meta, el campo "Monto disponible" debe reflejar el **saldo total acumulado (completo)** del usuario y no únicamente su saldo mensual.
3. **Visualización Previa y Detalles de la Meta:**
   * En el dropdown de opciones (tres puntos), corregir el botón **"Eliminar meta"** para que el color de fondo sea siempre visible (actualmente solo se ve al pasar el cursor).
   * Asegurar que la **fecha de inicio** se cargue correctamente en el detalle.
   * Para las metas cumplidas, corregir la carga de datos en los campos: "Completada el", "Fecha de inicio", "Tiempo empleado" (mejorar el cálculo/validación de duración) y corregir la visualización de la categoría asignada (actualmente muestra "Otros" por defecto).
   * Cambiar la etiqueta del filtro temporal a **"Mes Vencimiento"**.
   * *Requerimiento de datos:* El frontend enviará 3 campos clave: `fecha_inicio`, `fecha_objetivo` y `fecha_completada` (si no existen en el backend, se deben añadir a la entidad correspondientemente).

---

### 🛡️ Sección de Límites (Presupuestos)
1. **Cálculo de Límites Activos:**
   * Si se actualiza el límite del presupuesto, la aplicación debe procesar y tomar en cuenta todos los gastos realizados en el mes en curso, independientemente de los días restantes para que termine dicho mes.

---

### 🧠 Módulo de Inteligencia Artificial (IA)
1. **Restablecimiento de Filtros:**
   * Restaurar correctamente los controles de los filtros visuales dentro del módulo de IA a su estado limpio/por defecto cuando sea requerido.

---

### 👤 Perfil de Usuario & Header
1. **Fecha de Nacimiento:**
   * Sincronizar el campo "Fecha de nacimiento" en el backend para que el formulario del frontend pueda guardar, consultar y plasmar este dato directamente en la UI.
2. **Coherencia en el Header:**
   * Corregir el bug que impide mostrar el avatar/logo de perfil en el header principal.
   * El nombre del usuario mostrado en el header debe coincidir con el del menú lateral.
3. **Perfil Financiero (Diseño de Pasos):**
   * En el formulario guiado (Wizard), alinear correctamente los 3 pasos mediante conectores de línea (la transición visual actual del paso 1 al 2 se visualiza desconectada).

---

### 🔔 Notificaciones Globales
1. **Flotantes y Estilizadas:**
   * Reemplazar todas las alertas nativas (modales invasivos y diálogos nativos del navegador) por mensajes flotantes no bloqueantes y con un estilo moderno acorde al diseño general de Luka App, consumiendo de forma transversal el servicio de notificaciones del frontend.

---

### 🏷️ Iconografía de Categorías
1. **Iconografía Unificada:**
   * Asegurar que la selección de una categoría muestre siempre su icono asociado en todas las vistas de Luka App de forma consistente (a excepción de la sección de Metas).

---
---

## 🔧 Capa Backend

1. **Alineación de Nombres de Bases de Datos:**
   * Se debe revisar y unificar el esquema de nombres de base de datos utilizado por los microservicios en sus archivos de configuración (`application.yml` / `application-docker.yml`). Actualmente existe discrepancia entre la convención individual de microservicios (`db_microservicio_usuario`, `db_microservicio_cliente`, etc.) y el prefijo general `db_luka_suscripciones`. El objetivo es mantener una estructura limpia y homogénea en toda la arquitectura.
2. **Proceso de Detección de Suscripciones Vencidas:**
   * Implementar una tarea programada (cron job / background worker) que se ejecute periódicamente para identificar a los usuarios cuya suscripción ha vencido y no ha sido pagada. De detectarse, el servicio debe actualizar automáticamente su rol al plan básico (Free/Luka básico).
