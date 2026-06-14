# 🐛 Registro de Bugs y Correcciones Pendientes — LUKA APP

> **Última actualización:** 12 de junio de 2026
> **Responsable del documento:** Paulo Moron (Cloud Architect)
> **Estado:** En progreso — los ítems marcados con ✅ ya fueron corregidos.

Este documento centraliza todos los bugs detectados y mejoras de UX pendientes en el ecosistema LUKA APP. Está organizado por capa (Backend / Frontend) y por sección funcional para facilitar la asignación de tareas al equipo.

---

## 🔧 Backend

### Presupuesto

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| B-01 | ✅ **No existía un endpoint para actualizar presupuestos.** El microservicio-cliente no exponía una ruta PUT que permitiera modificar el monto o la fecha de un presupuesto existente. Se creó el endpoint correspondiente y se integró con el frontend. | **RESUELTO** | Paulo |

### Metas

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| B-02 | ✅ **La entidad `Meta` no almacenaba el propósito/icono.** El frontend envía un icono al crear una meta, pero el backend no tenía un campo para persistirlo. Se añadió el campo `proposito` a la entidad para que el icono se guarde y se mapee correctamente al consultarlo. | **RESUELTO** | Paulo |

### Transacciones (Racha)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| B-03 | ✅ **La racha se incrementaba con transacciones de fechas pasadas.** Al registrar un gasto con fecha anterior, el sistema contabilizaba ese día como actividad del usuario, inflando la racha. Se corrigió para que la racha solo se actualice con base en la **fecha de registro** (hoy), no la fecha de la transacción. | **RESUELTO** | Paulo |

### Premium / Pagos

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| B-04 | ✅ **La boleta del microservicio-pago no se persistía en la BD.** Tras la confirmación del webhook de Stripe, la boleta quedaba sin guardar. Se implementó la creación y persistencia de `Boleta` en `ServicioWebhookImpl` con `RepositorioBoleta`. | **RESUELTO** | Paulo |

### Suscripciones

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| B-05 | **Gastos de Suscripción:** El backend no conecta los gastos pagados y pendientes que vienen de la sección suscripción. | Pendiente | Paulo |

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
| F-07 | **El sidebar se reinicia al navegar a "Suscripción".** Al entrar a la pantalla de suscripción, el sidebar cambia a las secciones principales (Dashboard, Gastos, etc.) en lugar de mantenerse en la subsección de usuario (Perfil, Perfil Financiero, Configuración). | Pendiente | Paulo |
| F-08 | ✅ **Los banners de "LUKA Premium" no desaparecían tras suscribirse.** Al cambiar de rol a Premium o Pro, los banners promocionales del sidebar y de Perfil Financiero seguían visibles. Se añadieron condiciones `*ngIf` con `auth.esPremium()` y `auth.esPro()`. | **RESUELTO** | Paulo |

### Metas (Modal)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-09 | ✅ **El campo "Ingreso Inicial" en el modal de metas debía ser "Monto Disponible".** El campo ahora muestra el saldo actual del usuario (sincronizado), es de solo lectura y su etiqueta fue actualizada. | **RESUELTO** | Paulo |

### Premium / Dashboard

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-10 | ✅ **Tras pasar a Premium, el header no reflejaba el nuevo gasto ni se actualizaban las secciones.** Las KPI cards, el dashboard y la sección de gastos no se sincronizaban después del evento de pago. Se implementó la escucha del evento `TRANSACTION_MODIFIED` para invalidar cachés y refrescar datos. | **RESUELTO** | Paulo |

### Perfil

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-11 | ✅ **El selector de fecha del perfil no ajusta los días según el mes.** Al seleccionar febrero, el selector sigue mostrando 31 días en lugar de 28/29. Debe validarse dinámicamente según el mes y año seleccionado. | **RESUELTO** | Cristina |

### Login / Registro

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-12 | ✅ **Simplificar el flujo de registro.** Se eliminó la opción de "Registrarse con celular" del formulario inicial. El número de teléfono (WhatsApp o SMS) ahora se solicita **únicamente en el paso de verificación de identidad**, donde se envía el código OTP. | **RESUELTO** | Cristina |

### Perfil Financiero

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-13 | ✅ **Eliminar la sección de "Patrimonio".** La información mostrada no era clara para el usuario y generaba confusión. Se retiró de la vista de Perfil Financiero. | **RESUELTO** | Cristina |

### Dashboard / Secciones Principales (Anteriores)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-14 | ✅ **Reubicar las tablas de ingresos y gastos fuera de la sección principal del dashboard.** Se movieron a subsecciones dedicadas dentro de Ingresos y Gastos respectivamente, con más espacio para una mejor visualización. | **RESUELTO** | Cristina |
| F-15 | ✅ **Separar Membresía Luka de Suscripciones de pagos mensuales.** Se añadió la sección principal Suscripciones con icono de recibo para pagos recurrentes de plataformas, y se mantuvo Membresía en el sidebar del usuario con icono de corona y mensaje propio. | **RESUELTO** | Cristina |
| F-16 | ✅ **Evitar que Soporte abra el modal de planes.** El botón de soporte del sidebar premium ahora solo navega a Ayuda; el modal de planes queda limitado al botón Ver planes. | **RESUELTO** | Cristina |
| F-17 | ✅ **Agregar confirmación segura para eliminar cuenta.** El botón Eliminar cuenta abre un modal de advertencia y exige escribir la frase de confirmación antes de procesar la solicitud. | **RESUELTO** | Cristina |
| F-18 | ✅ **Crear páginas legales de Política de Privacidad y Términos y Condiciones.** Se añadieron páginas standalone enlazadas desde Configuración, con contenido legal y estilos SCSS coherentes con Luka App. | **RESUELTO** | Cristina |
| F-19 | ✅ **Mejorar tarjetas KPI de Gastos.** Se retiraron emojis, se reemplazaron por iconos FontAwesome y se agregaron fondos coloridos para hacer los indicadores más llamativos. | **RESUELTO** | Cristina |
| F-20 | ✅ **Corregir formato de templates y estilos con saltos de línea literales.** Se limpiaron caracteres `\n` en HTML/SCSS/TS afectados para evitar errores NG5002 y fallos de compilación. | **RESUELTO** | Cristina |

### Dashboard e Historial (Nuevos)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-21 | **Dashboard - Filtro de Fechas:** Filtro "Entre fechas" debe permitir desde la fecha que se elige hasta el día de hoy. | Pendiente | Paulo |
| F-22 | **Dashboard - Historial:** Eliminar el historial de transacciones del Dashboard. | Pendiente | Paulo |
| F-23 | **Texto de Sugerencia IA:** Cambiar el texto a: "Tienes 2 intentos de sugerencia por IA. El contador también es visible en el header como 'Autoclasificar'." | Pendiente | Paulo |
| F-24 | **Unificar Tablas en Historial:** Unificar las tablas de ingreso y gastos en la sección historial principal y quitar las tablas de las sub-secciones. | Pendiente | Paulo |
| F-25 | **Tema Oscuro - Header y Dashboard:** En modo oscuro las etiquetas del header no se notan claro; definir un color visible para las letras sin afectar el tema claro. En el dashboard (ej. periodo) las letras deben ser blancas con hover más blanco, sin afectar el color principal. | Pendiente | Paulo |
| F-26 | **Reubicación de Historial:** Cambiar la sección historial a la sección principal. | Pendiente | Paulo |

### Gastos

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-27 | **Reorganización de Modal y Pendientes:** El modal de gastos debería aparecer en la sección principal, y la sección de pendientes ir al lado derecho. | Pendiente | Paulo |
| F-28 | **Límite en Descripción:** En el formulario de gastos, agregar un límite de 200 caracteres en la descripción. | Pendiente | Paulo |

### Login y Extras

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-29 | **Usuario Mock para Pruebas:** Agregar un usuario mock con correo `prueba@gmail.com` y contraseña `12345` para probar el flujo de ingreso, omitiendo la restricción de contraseña solo por esta vez. | Pendiente | Paulo |

### Modularización

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-30 | **Modularizar Metas:** Refactorizar y modularizar la sección Metas. | Pendiente | Paulo |
| F-31 | **Modularizar Perfil:** Refactorizar y modularizar la sección Perfil. | Pendiente | Paulo |

### Metas (Nuevos)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-32 | ✅ **Cards Adicionales:** Agregar 3 cards (ej. DIVERSIÓN) en la sección de Metas. | **RESUELTO** | Paul |

### Perfil y Perfil Financiero (Nuevos)

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-33 | **Perfil - Validación de Teléfono:** Validar la sección de teléfono con el número establecido en base al código de país. | Pendiente | Paulo |
| F-34 | **Perfil - Etiqueta Sin Verificar:** Cambiar el color de letra de la etiqueta "Sin verificar". | Pendiente | Paulo |
| F-35 | **Perfil Financiero - Filtros:** La parte de filtro de fechas debería separarse entre mes y año. | Pendiente | Paulo |

### Configuración

| # | Descripción | Estado | Asignado |
|---|-------------|--------|----------|
| F-36 | **Tema Oscuro:** La parte de selección de tema en Configuración debería notarse claramente en color oscuro. | Pendiente | Paulo |

---

## 📊 Resumen

| Capa | Total | Resueltos | Pendientes |
|------|-------|-----------|------------|
| Backend | 5 | 4 | 1 |
| Frontend | 36 | 20 | 16 |
| **Total** | **41** | **24** | **17** |

---

> [!TIP]
> Cada miembro del equipo puede asignarse bugs pendientes escribiendo su nombre en la columna **Asignado**. Coordinar con Cristina Astocaza (Frontend Lead & QA) para priorización de los ítems de frontend.
