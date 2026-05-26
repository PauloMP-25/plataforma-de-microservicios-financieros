# Microservicio de Pagos y Suscripciones (ms-pagos)

Este microservicio gestiona la integración con Stripe para el procesamiento de pagos, gestión de suscripciones y procesamiento de webhooks financieros dentro del ecosistema LUKA APP.

## Arquitectura
El proyecto sigue los principios de **Clean Architecture**, dividiéndose en:
* **Dominio:** Entidades base (Suscripcion, Pago, Plan).
* **Aplicación:** Servicios de negocio para Stripe Checkout, suscripciones y DTOs.
* **Infraestructura:** Clientes de Stripe, configuración de seguridad y mensajería RabbitMQ para eventos de pago.
* **Presentación:** Controladores REST para pasarela de pago y webhooks.

## Características
* **Integración con Stripe:** Uso del SDK oficial de Stripe para Java.
* **Suscripciones Dinámicas:** Gestión de planes Free y Premium.
* **Webhooks:** Procesamiento asíncrono de eventos de Stripe (payment_intent.succeeded, customer.subscription.deleted).
* **Resiliencia:** Comunicación con `microservicio-usuario` vía Feign.

## API Endpoints (v1)
*Proximamente...*
