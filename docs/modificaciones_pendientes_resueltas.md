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
