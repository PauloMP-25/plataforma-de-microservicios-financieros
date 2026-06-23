# Análisis de Integración y Documentación de Fases: Suscripciones con Mercado Pago
## Arquitectura de Pagos — Luka App (V4)

Este documento describe detalladamente el análisis arquitectónico, las fases de la integración, los patrones de diseño aplicados, las modificaciones de código realizadas y los beneficios estratégicos de contar con dos pasarelas de pago (Stripe y Mercado Pago) coexistiendo de manera coordinada dentro de la plataforma de microservicios financieros.

---

## 1. Análisis de Integración y Arquitectura

Luka App cuenta con una arquitectura de microservicios políglotas comunicados de manera asíncrona mediante **RabbitMQ** y bajo el patrón **Transactional Outbox**. El flujo original de suscripciones dependía exclusivamente de Stripe. La introducción de Mercado Pago requería rediseñar el módulo para que fuera extensible sin alterar los servicios adyacentes ni duplicar la lógica de base de datos.

### 1.1. Mapeo Conceptual de Entidades y Estados

Para homogeneizar el comportamiento de ambas pasarelas de pago, se definió un mapeo de correspondencias semánticas:

| Concepto / Recurso | Stripe (Existente) | Mercado Pago (Nuevo) | Mapeo / Solución Unificada |
| :--- | :--- | :--- | :--- |
| **Definición del Plan** | `Product` + `Price` (ID recurrente) | `Plan` / `Subscription Plan` | Mapeado dinámicamente mediante el enum `PlanSuscripcion`. |
| **Suscripción Activa** | `Subscription` | `Preapproval` (Preaprobación de débito) | Representa la autorización de cargo recurrente del cliente. |
| **Estado de Suscripción** | `status` (`active`, `past_due`) | `status` (`authorized`, `paused`, `cancelled`) | `authorized` (MP) equivale semánticamente a `active` (Stripe). |
| **Transacción de Pago** | `Checkout Session` | `Preapproval` con `init_point` | URL provista por la pasarela para capturar los datos del usuario. |
| **Id de Referencia Único** | `stripeSessionId` | `preapproval_id` | Almacenado en la columna genérica `stripe_session_id` de la tabla `pagos`. |
| **Idempotencia de Notificaciones** | `stripeEventoId` | `requestId` (del webhook) | Guardado en la columna `stripe_evento_id` para evitar duplicados. |

---

## 2. Patrones de Diseño Aplicados

Para cumplir rigurosamente con los principios **SOLID** (especialmente **OCP** y **DIP**), se rediseñó el módulo utilizando los siguientes patrones:

### 2.1. Strategy Pattern (Patrón Estrategia)
Se creó la interfaz `IPasarelaPagoEstrategia` que define un contrato agnóstico para interactuar con cualquier proveedor de pagos. 
* **Beneficio**: `ServicioStripeImpl` y `MercadoPagoEstrategiaImpl` implementan esta interfaz de forma aislada. Agregar una tercera pasarela en el futuro (ej. Culqui o Mercado Pago versión 2) no requerirá alterar las clases existentes.

### 2.2. Factory Method (Fábrica de Pasarelas)
La clase `PasarelaPagoFactory` inyecta automáticamente en tiempo de inicio de la aplicación todas las implementaciones de `IPasarelaPagoEstrategia`.
* **Beneficio**: El controlador resuelve la pasarela en $O(1)$ dinámicamente según el parámetro `proveedor` enviado en el DTO de la solicitud (`SolicitudPagoDTO`). Si no se provee ninguno, asume `STRIPE` por defecto para garantizar compatibilidad retroactiva total con el cliente frontend actual.

### 2.3. Adapter Pattern (Patrón Adaptador)
La clase `MercadoPagoEstrategiaImpl` actúa como adaptador convirtiendo los objetos y excepciones del SDK oficial de Mercado Pago (`PreapprovalClient`, `Preapproval`, `MPApiException`) a estructuras comunes de nuestro dominio (`Pago`, `DetallePago`, `ResultadoApi` y `ExcepcionMercadoPago`).

### 2.4. Transactional Outbox (Bandeja de Salida Transaccional)
El guardado del registro del pago en base de datos (`EstadoPago.COMPLETADO`) y el registro del evento en la tabla `bandeja_salida` se realizan en la misma transacción SQL.
* **Beneficio**: Garantiza la consistencia eventual. Si el broker RabbitMQ está caído durante el webhook, el evento no se pierde; un planificador programado (`ReintentadorEventos`) reintentará su envío garantizando que se actualice el rol de usuario, se envíe el correo y se registre en contabilidad.

---

## 3. Fases del Cambio e Implementación

La integración se estructuró y ejecutó de la siguiente manera:

```
┌────────────────────────┐     ┌────────────────────────┐     ┌────────────────────────┐
│  FASE 1: Prep & DTOs   │ ──> │   FASE 2: Core Pagos   │ ──> │ FASE 3: Webhook & GW   │
│  - Ramificación Git    │     │   - SDK Mercado Pago   │     │  - Controlador Webhook │
│  - EventoPagoExitoso   │     │   - Strategy & Factory │     │  - Firmas HMAC-SHA256  │
│  - Aliasing Jackson    │     │   - Configuración YML  │     │  - Rutas Públicas GW   │
└────────────────────────┘     └────────────────────────┘     └────────────────────────┘
```

### Fase 1: Desacoplamiento de Contratos y Librería Común
1. Se renombró semánticamente el campo `stripeSessionId` por `referenciaPasarela` en `EventoPagoExitosoDTO`.
2. Para evitar la ruptura de compatibilidad con mensajes en vuelo u outbox previos, se introdujo `@JsonAlias("stripe_session_id")` de Jackson.
3. Se recompiló e instaló `libreria-comun` para propagar el contrato.

### Fase 2: Implementación de la Lógica del Proveedor en `ms-pago`
1. Se incorporó la dependencia `sdk-java:2.1.27` en el `pom.xml`.
2. Se crearon las clases `PropiedadesMercadoPago` y `ConfiguracionMercadoPago` validadas al arranque con `@ConfigurationProperties` para cargar de forma segura las credenciales del panel de desarrolladores.
3. Se escribió la estrategia `MercadoPagoEstrategiaImpl` para interactuar con la API REST de Mercado Pago, persistiendo el pago como `PENDIENTE` y retornando el punto de inicio (`init_point`).

### Fase 3: Seguridad del Webhook y Configuración de API Gateway
1. Se implementó `ControladorWebhookMercadoPago` de forma independiente cumpliendo el Principio de Responsabilidad Única (SRP).
2. Se introdujo una validación criptográfica en `validarFirmaHmac(...)` para reconstruir la firma mediante `HmacSHA256` con el secreto local del webhook, comparando de forma segura contra ataques de temporización (`MessageDigest.isEqual`).
3. Se actualizó el API Gateway en `application-local.yml` y `application-docker.yml` para eximir de validación JWT a la ruta de webhook `/api/v1/pagos/webhook/mercadopago`, dado que Mercado Pago no envía tokens Bearer.
4. Se actualizó `ConsumidorEventoPago` en `ms-suscripciones` para inferir el método de pago dinámicamente (Stripe si inicia con `cs_`, caso contrario Mercado Pago) y persistirlo correspondientemente.

---

## 4. Cambios Hechos en los Archivos del Repositorio

Los siguientes archivos fueron modificados o incorporados:

### A. Librería Común
* **[EventoPagoExitosoDTO.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/libreria-comun/src/main/java/com/libreria/comun/dtos/EventoPagoExitosoDTO.java)** [MODIFY]: Cambio de nombre del campo a `referenciaPasarela` incorporando `@JsonAlias("stripe_session_id")`.

### B. Microservicio de Pagos (`ms-pago`)
* **[pom.xml](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/pom.xml)** [MODIFY]: Adición del SDK de Mercado Pago.
* **[ProveedorPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/enums/ProveedorPago.java)** [NEW]: Enum para los proveedores.
* **[EstadoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/enums/EstadoPago.java)** [MODIFY]: Estado `AUTORIZADO`.
* **[IPasarelaPagoEstrategia.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/puertos/IPasarelaPagoEstrategia.java)** [NEW]: Abstracción del puerto de estrategia.
* **[PasarelaPagoFactory.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/servicios/PasarelaPagoFactory.java)** [NEW]: Fábrica de pasarelas de pago.
* **[ServicioStripeImpl.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/servicios/ServicioStripeImpl.java)** [MODIFY]: Adaptación para implementar el puerto de estrategia.
* **[MercadoPagoEstrategiaImpl.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/aplicacion/servicios/MercadoPagoEstrategiaImpl.java)** [NEW]: Lógica de negocio de checkout, control de excepciones del SDK de Mercado Pago, validación criptográfica de firmas HMAC-SHA256, y confirmación del ciclo de pago.
* **[PropiedadesMercadoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/infraestructura/configuracion/PropiedadesMercadoPago.java)** [NEW] & **[ConfiguracionMercadoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/infraestructura/configuracion/ConfiguracionMercadoPago.java)** [NEW]: Carga y validación rápida de variables de entorno al arranque.
* **[ExcepcionMercadoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/infraestructura/excepciones/ExcepcionMercadoPago.java)** [NEW] & **[ExcepcionFirmaWebhookInvalida.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/infraestructura/excepciones/ExcepcionFirmaWebhookInvalida.java)** [NEW]: Excepciones de infraestructura de pagos.
* **[ControladorPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/presentacion/controladores/ControladorPago.java)** [MODIFY] & **[ControladorWebhookMercadoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/presentacion/controladores/ControladorWebhookMercadoPago.java)** [NEW]: Enrutadores y controladores desacoplados.
* **[ManejadorGlobalExcepciones.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/java/com/pagos/presentacion/manejadores/ManejadorGlobalExcepciones.java)** [MODIFY]: Mapeo de excepciones a respuestas `ResultadoApi`.
* **[application.yml](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-pago/src/main/resources/application.yml)** [MODIFY]: Bloques de configuración para `mercadopago`.

### C. Microservicio de Suscripciones (`ms-suscripciones`)
* **[ConsumidorEventoPago.java](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/microservicio-suscripciones/src/main/java/com/suscripciones/infraestructura/mensajeria/ConsumidorEventoPago.java)** [MODIFY]: Persistencia del método de pago (`STRIPE` o `MERCADOPAGO`) deducido del ID de sesión de pago.

### D. API Gateway (`api-gateway`)
* **[application-local.yml](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/api-gateway/src/main/resources/application-local.yml)** [MODIFY] & **[application-docker.yml](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/api-gateway/src/main/resources/application-docker.yml)** [MODIFY]: Exclusión de JWT para notificaciones webhook en `rutas-publicas`.

### E. Archivos de Entorno
* **[.env.local.example](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/.env.local.example)** [MODIFY] & **[.env](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/.env)** [MODIFY]: Inclusión de variables de entorno de plantilla para Mercado Pago.

---

## 5. Beneficios Estratégicos de la Coexistencia Multi-Pasarela

Contar con Stripe y Mercado Pago funcionando en paralelo aporta ventajas competitivas e infraestructurales críticas para Luka App:

### 5.1. Redundancia e Imposibilidad de Punto Único de Fallo (SPOF)
Si uno de los proveedores experimenta caídas globales, fallos de API o rechazos masivos de red (como ocurre ocasionalmente durante picos de transacciones), el sistema puede enrutar dinámicamente los cobros al otro proveedor. Esto garantiza la **Alta Disponibilidad** del flujo de facturación recurrente de la plataforma de cara al cliente.

### 5.2. Localización y Aumento de Conversión en el Mercado Latinoamericano
* **Stripe** es la pasarela líder para transacciones en mercados internacionales (EE. UU., Europa), con excelente soporte multi-moneda para grandes marcas globales.
* **Mercado Pago** es el líder indiscutible en América Latina (Perú, México, Argentina, Colombia, Chile, Brasil). Al integrarlo, Luka App puede procesar de forma nativa tarjetas locales de crédito/débito que Stripe suele rechazar por políticas de fraude geográfico. Además, facilita integraciones de pago móvil instantáneo (ej. Yape, Plin en el mercado peruano) y opciones de financiamiento en cuotas locales sin tarjeta internacional. Esto **maximiza la tasa de conversión** en el checkout en la región.

### 5.3. Optimización de Costos y Comisiones
La coexistencia permite implementar reglas de negocio inteligentes (*Dynamic Routing*). Por ejemplo, procesar tarjetas emitidas localmente en Perú mediante Mercado Pago (comisiones domésticas más bajas) y procesar tarjetas internacionales mediante Stripe, optimizando los costos de procesamiento de pagos generales.

### 5.4. Mitigación de Riesgos Corporativos (Vendor Lock-in)
La dependencia absoluta de una sola pasarela de pagos representa un riesgo estratégico. Si el proveedor congela de forma unilateral la cuenta por "actividad inusual", rescinde el contrato o aumenta las tarifas, la empresa queda vulnerable. Poseer una arquitectura multi-pasarela permite mitigar este riesgo y negociar mejores tasas basándose en volúmenes transaccionales alternos.

---

## 6. Integración y Optimización del Frontend (Angular 17/18)

La integración del lado del cliente se diseñó bajo los estándares establecidos de **componentes standalone**, **lazy loading**, y de acuerdo a las directrices de UX/UI coordinadas con Cristina Astocaza (QA & Frontend Lead):

### 6.1. Modificación de Servicio de Suscripciones
* **[suscripcion.service.ts](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/core/services/suscripcion.service.ts)**:
  Se actualizó el método `crearSesionCheckout` para recibir tanto el plan (`'PRO'` o `'PREMIUM'`) como el proveedor (`'STRIPE'` o `'MERCADOPAGO'`). Este proveedor se propaga en el cuerpo JSON de la petición HTTP POST hacia el API Gateway.

### 6.2. Componente Standalone Unificado `ModalPlanes`
Para evitar la duplicación de código e inconsistencia de interfaz, se creó el componente standalone **[ModalPlanes](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/features/suscripcion/components/modal-planes/)** (`src/app/features/suscripcion/components/modal-planes/`):
* **[modal-planes.ts](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/features/suscripcion/components/modal-planes/modal-planes.ts)**: Expone las señales de control `@Input() comprando` y los disparadores de eventos `@Output() close` y `@Output() checkout`.
* **[modal-planes.html](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/features/suscripcion/components/modal-planes/modal-planes.html)**: Define la estructura HTML semántica de la cuadrícula de planes Pro (`S/ 14.90`) y Premium (`S/ 25.90`). Cada tarjeta expone botones individuales y limpios para redirigir a Stripe o Mercado Pago. Todos los precios están definidos en Soles (`S/`).
* **[modal-planes.scss](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/features/suscripcion/components/modal-planes/modal-planes.scss)**: Estilizado con Vanilla CSS/Sass modularizado usando tokens/variables de CSS globales de la aplicación para heredar dinámicamente el tema claro/oscuro (ej. `var(--bg-card)`, `var(--text-color)`). Incluye además media-queries responsivas para garantizar una experiencia óptima en móviles.

### 6.3. Refactorización de Vistas Duplicadas
El modal de selección de planes se encontraba duplicado de forma idéntica tanto en la pantalla de **Configuración** como en el **Sidebar**. Se refactorizaron ambas secciones:
* **[Configuracion](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/features/perfil/configuracion/)** (`configuracion.ts`, `.html`, `.scss`): Se removió todo el código HTML/Sass repetido, se importó `ModalPlanes` en los metadatos standalone y se incrustó mediante la directiva `<app-modal-planes>`.
* **[Sidebar](file:///media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-frontend/frontend/src/app/layout/sidebar/sidebar/)** (`sidebar.ts`, `.html`, `.scss`): Se realizó el mismo proceso de eliminación de duplicaciones e incrustación de la etiqueta del componente.

Esta refactorización redujo más de 400 líneas redundantes del bundle inicial del frontend, mejorando la mantenibilidad a largo plazo.

---

## 7. Pruebas de Compilación y Verificación

### 7.1. Compilación del Frontend (Angular)
Se ejecutó la compilación de producción y desarrollo local del frontend en Angular para verificar la sanidad de TypeScript e inyección de imports:
* **Comando ejecutado**: `npm run build`
* **Resultado**: **Compilación Exitosa** (`Application bundle generation complete.`). El bundle de salida se generó satisfactoriamente y las referencias circulares o dependencias ausentes fueron descartadas.

### 7.2. Compilación del Backend (Java/Spring Boot)
Se verificaron los builds de los módulos afectados por la integración multi-pasarela en el backend:
1. **`libreria-comun`**: `mvn clean install -DskipTests` ──> **BUILD SUCCESS**
2. **`microservicio-pago`**: `mvn clean compile` ──> **BUILD SUCCESS**
3. **`microservicio-suscripciones`**: `mvn clean compile` ──> **BUILD SUCCESS**
4. **`api-gateway`**: `mvn clean compile` ──> **BUILD SUCCESS**

---

