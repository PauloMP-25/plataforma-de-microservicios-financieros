# 📋 Tareas de la Mañana - LUKA Ecosistema IA & Suscripciones Premium

Este documento detalla los objetivos para la próxima sesión de desarrollo.

---

## 📊 1. Tarjetas de Cuotas de Inteligencia Artificial en el Header
Implementar dos tarjetas métricas adicionales en el header de la aplicación para mostrar las cuotas de uso de IA del usuario en base a su plan/rol actual.

- [ ] **Mapeo de Roles de Plan:**
  - Leer el plan desde el token del usuario en `AuthService` (roles: `PREMIUM`, `PRO` y `FREE`).
  - Definir las cuotas correspondientes:
    - **PREMIUM:** 50 consultas analíticas de IA, 20 clasificaciones automáticas (según plan Premium).
    - **PRO:** 20 consultas analíticas de IA, 10 clasificaciones automáticas (según plan Pro).
    - **FREE:** 5 consultas analíticas de IA, 2 clasificaciones automáticas (cuota básica gratuita).
- [ ] **Agregar Cards al Header:**
  - Diseñar dos pills adicionales en el header (`metric-pill--ia-analitica` y `metric-pill--ia-clasificar`) utilizando el sistema de diseño de LUKA.
  - Mostrar la cuota restante (ej. `Consultas IA: 35/50` y `Autoclasificar: 12/20`).
- [ ] **Descuento Dinámico y Reactividad:**
  - Descontar una unidad de la cuota restante cada vez que el usuario realice una consulta a un módulo de IA (Reto, Proyecciones, etc.) o solicite una autoclasificación.
  - Sincronizar y actualizar las señales reactivas del header al completarse estas acciones.

---

## 💳 2. Flujo de Suscripción Premium (Simulación Stripe)
Implementar y verificar el flujo completo de compra de plan PREMIUM utilizando la integración de Stripe y desencadenando eventos concurrentes en el ecosistema.

- [ ] **Checkout Stripe:**
  - Al dar clic en "Elegir Plan Premium" desde el modal de planes de suscripción, redirigir e iniciar una sesión de Stripe Checkout utilizando las llaves del archivo `.env` del backend.
  - Completar y simular el pago exitoso en el entorno de desarrollo local.
- [ ] **Desencadenadores tras Pago Exitoso (Eventos):**
  - **Evento 1 (ms-suscripciones):** Crear el registro de la suscripción mensual en `ms-suscripciones` para que aparezca listada correctamente en la sección correspondiente de gastos/suscripciones recurrentes.
  - **Evento 2 (ms-nucleo-financiero):** Registrar automáticamente una transacción de tipo `GASTO` por el monto del plan (S/ 25.90) con los detalles del pago.
  - **Evento 3 (ms-usuario / Sesión):** Actualizar la sesión del usuario para asignar el rol `PREMIUM`, lo que a su vez actualizará las tarjetas de cuotas de IA en el header y desbloqueará las capacidades Premium de analítica.
