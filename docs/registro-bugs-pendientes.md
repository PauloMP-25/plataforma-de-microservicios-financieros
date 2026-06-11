# 🐛 Registro de Bugs y Correcciones Pendientes — LUKA APP

> **Última actualización:** 10 de junio de 2026
> **Responsable del documento:** Paulo Moron (Cloud Architect)
> **Estado:** En progreso — los ítems marcados con ✅ ya fueron corregidos.

Este documento centraliza todos los bugs detectados y mejoras de UX pendientes en el ecosistema LUKA APP. Está organizado por capa (Backend / Frontend) y por sección funcional para facilitar la asignación de tareas al equipo.

---

## 🔧 Backend

### Presupuesto

| # | Descripción | Estado |
|---|-------------|--------|
| B-01 | ✅ **No existía un endpoint para actualizar presupuestos.** El microservicio-cliente no exponía una ruta PUT que permitiera modificar el monto o la fecha de un presupuesto existente. Se creó el endpoint correspondiente y se integró con el frontend. | **RESUELTO** |

### Metas

| # | Descripción | Estado |
|---|-------------|--------|
| B-02 | ✅ **La entidad `Meta` no almacenaba el propósito/icono.** El frontend envía un icono al crear una meta, pero el backend no tenía un campo para persistirlo. Se añadió el campo `proposito` a la entidad para que el icono se guarde y se mapee correctamente al consultarlo. | **RESUELTO** |

### Transacciones (Racha)

| # | Descripción | Estado |
|---|-------------|--------|
| B-03 | ✅ **La racha se incrementaba con transacciones de fechas pasadas.** Al registrar un gasto con fecha anterior, el sistema contabilizaba ese día como actividad del usuario, inflando la racha. Se corrigió para que la racha solo se actualice con base en la **fecha de registro** (hoy), no la fecha de la transacción. | **RESUELTO** |

### Premium / Pagos

| # | Descripción | Estado |
|---|-------------|--------|
| B-04 | ✅ **La boleta del microservicio-pago no se persistía en la BD.** Tras la confirmación del webhook de Stripe, la boleta quedaba sin guardar. Se implementó la creación y persistencia de `Boleta` en `ServicioWebhookImpl` con `RepositorioBoleta`. | **RESUELTO** |

---

## 🎨 Frontend

### Sección de Ingresos

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-01 | ✅ **Eliminar la columna de "Etiquetado" de la tabla de listado de ingresos.** Actualmente se muestra una columna que no aporta valor al usuario en esa vista. | **RESUELTO** | Cristina |
| F-02 | ✅ **Simplificar el filtro de ingresos.** Quitar el selector de rango de fechas y el botón "Filtrar". El filtrado debe ejecutarse de forma **automática** al seleccionar un mes o un tipo de ingreso (sin necesidad de confirmar). | **RESUELTO** | Cristina |
| F-03 | ✅ **Quitar la opción "Diario / Recurrente" del formulario de gastos.** Esta distinción no se utiliza actualmente y genera confusión. | **RESUELTO** | Cristina |
| F-04 | ✅ **La tarjeta de vista previa de un ingreso muestra el UID de la categoría en lugar de su nombre.** Debe resolverse el nombre legible a partir del UID antes de renderizarlo. | **RESUELTO** | Cristina |
| F-05 | ✅ **Mejorar UX del botón "Sugerir Categoría" en el formulario de gastos.** Actualmente queda aislado sin contexto. Debe informar al usuario que dispone de **2 intentos** de sugerencia por IA, que el contador es visible en el header, y que al agotarse quedará deshabilitado. | **RESUELTO** | Cristina |
| F-06 | ✅ **Eliminar el card de "Racha" e "Ingresos Totales" de la sección de ingresos.** Estos indicadores no corresponden a esa vista. | **RESUELTO** | Cristina |

### Sidebar / Navegación

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-07 | **El sidebar se reinicia al navegar a "Suscripción".** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). | Pendiente | — |
| F-08 | ✅ **Los banners de "LUKA Premium" no desaparecían tras suscribirse.** Al cambiar de rol a Premium o Pro, los banners promocionales del sidebar y de Perfil Financiero seguían visibles. Se añadieron condiciones `*ngIf` con `auth.esPremium()` y `auth.esPro()`. | **RESUELTO** |

### Metas (Modal)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-09 | ✅ **El campo "Ingreso Inicial" en el modal de metas debía ser "Monto Disponible".** El campo ahora muestra el saldo actual del usuario (sincronizado), es de solo lectura y su etiqueta fue actualizada. | **RESUELTO** |

### Premium / Dashboard

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-10 | ✅ **Tras pasar a Premium, el header no reflejaba el nuevo gasto ni se actualizaban las secciones.** Las KPI cards, el dashboard y la sección de gastos no se sincronizaban después del evento de pago. Se implementó la escucha del evento `TRANSACTION_MODIFIED` para invalidar cachés y refrescar datos. | **RESUELTO** |

### Perfil

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-11 | ✅ **El selector de fecha del perfil no ajusta los días según el mes.** Al seleccionar febrero, el selector sigue mostrando 31 días en lugar de 28/29. Debe validarse dinámicamente según el mes y año seleccionado. | **RESUELTO** | Cristina |

### Login / Registro

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-12 | **Simplificar el flujo de registro.** Eliminar la opción de "Registrarse con celular" del formulario inicial. El número de teléfono (WhatsApp o SMS) debe solicitarse **únicamente en el paso de verificación de identidad**, donde se envía el código OTP. | Pendiente | — |

### Perfil Financiero

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-13 | **Eliminar la sección de "Patrimonio".** La información mostrada no es clara para el usuario y genera confusión. Retirarla de la vista de Perfil Financiero. | Pendiente | — |

### Dashboard / Secciones Principales

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-14 | **Reubicar las tablas de ingresos y gastos fuera de la sección principal del dashboard.** Generan sobrecarga visual y no son suficientemente visibles. Deben moverse a subsecciones dedicadas dentro de Ingresos y Gastos respectivamente, donde dispongan de más espacio para una mejor visualización. | Pendiente | — |

---

## 📊 Resumen

| Capa | Total | Resueltos | Pendientes |
|------|-------|-----------|------------|
| Backend | 4 | 4 | 0 |
| Frontend | 14 | 11 | 3 |
| **Total** | **18** | **15** | **3** |

---

> [!TIP]
> Cada miembro del equipo puede asignarse bugs pendientes escribiendo su nombre en la columna **Asignado**. Coordinar con Cristina Astocaza (Frontend Lead & QA) para priorización de los ítems de frontend.
