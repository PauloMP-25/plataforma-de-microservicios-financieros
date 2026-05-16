# Arquitectura del Microservicio de Pagos (ms-pagos)
## Ecosistema LUKA APP V4

Este microservicio gestiona la monetización y suscripciones de la plataforma mediante la integración con **Stripe**. Está diseñado siguiendo una arquitectura limpia (Clean Architecture) y patrones de alta disponibilidad como **Transactional Outbox**.

---

### 1. Capa de Dominio (Domain)
*Ubicación: `com.pagos.dominio`*
Es el núcleo del microservicio, libre de dependencias externas.
*   **Entidades**: 
    *   `Pago`: Representa la cabecera de la transacción financiera.
    *   `DetallePago`: Contiene los ítems del pago (Plan, descripción, descuentos).
    *   `BandejaSalida`: Registro técnico para el patrón Outbox.
*   **Enums**: `EstadoPago`, `PlanSuscripcion`.
*   **Repositorios**: Interfaces para el acceso a datos (`RepositorioPago`, `RepositorioBandejaSalida`).

### 2. Capa de Aplicación (Application)
*Ubicación: `com.pagos.aplicacion`*
Contiene la lógica de negocio y orquestación.
*   **Servicios (Interfaces)**: `IServicioStripe`, `IServicioWebhook`.
*   **DTOs**: Contratos de entrada y salida (`SolicitudCheckoutDTO`, `RespuestaCheckoutDTO`).
*   **Tareas Programadas**: `ReintentadorEventos` que procesa la bandeja de salida cada 10 segundos para garantizar que los eventos lleguen a RabbitMQ.

### 3. Capa de Infraestructura (Infrastructure)
*Ubicación: `com.pagos.infraestructura`*
Implementaciones tecnológicas y adaptadores.
*   **Adaptadores Stripe**: `ServicioStripeImpl` y `ServicioWebhookImpl`. Gestionan la creación de sesiones y el procesamiento de eventos asíncronos de Stripe.
*   **Mensajería**: 
    *   `PublicadorPagosImpl`: Implementa el guardado en la `BandejaSalida`.
    *   `PublicadorAuditoriaPagosImpl`: Publica trazas de auditoría (Acceso, Transacción, Evento) hacia el bus de mensajes.
*   **Configuración**: `ConfiguracionStripe`, `ConfiguracionColasPagos`, `ConfiguracionSeguridad`.

### 4. Capa de Presentación (Web/API)
*Ubicación: `com.pagos.presentacion`*
Puntos de entrada al sistema.
*   **Controladores**: 
    *   `ControladorPago`: Checkout y consulta de suscripción del usuario.
    *   `ControladorWebhook`: Endpoint público para Stripe.
    *   `ControladorAdminPagos`: Gestión de reportes financieros.
*   **Manejador de Excepciones**: `ManejadorGlobalExcepciones`, que mapea errores de Stripe a respuestas `ResultadoApi<T>` estandarizadas.

---

### Flujo de Integración con el Ecosistema
El microservicio de pagos actúa como un **emisor de eventos dominantes**. Cuando un pago es exitoso:

1.  **ms-pagos** guarda el estado y publica un mensaje en `exchange.pagos`.
2.  **ms-usuario** escucha y actualiza el **Rol** y el **Plan** del usuario automáticamente.
3.  **ms-auditoria** captura el evento y genera un registro de trazabilidad financiera.
4.  **ms-nucleo-financiero** crea un registro de **Ingreso** en el balance del usuario.
5.  **ms-mensajeria** envía el **Email** de bienvenida y comprobante de pago.

---
**Autor:** Antigravity (AI Assistant)
**Versión:** 1.0.0
**Fecha:** Mayo 2026
