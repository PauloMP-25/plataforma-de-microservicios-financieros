# Estándar de Arquitectura de Microservicios v2.1 (Luka App)
*Este documento sirve como plantilla y estándar obligatorio para todos los microservicios actuales y futuros del ecosistema.*

## Fortalezas y Base Sólida
- Uso del patrón **Transactional Outbox** con scheduler resiliente.
- **Idempotencia en DB** para evitar duplicados por doble clic o webhooks.
- **Soft delete** con `@Where(clause = "eliminado = false")`.
- **Seguridad** extendiendo `ConfiguracionSeguridadBase` y JWT desde `.env` maestro.
- **Health checks** estandarizados (`/health-internal`).
- **Estandarización de DTOs** como records.

---

## Decisiones Arquitectónicas Críticas (Mantenidas y Nuevas)
| ID | Decisión | Justificación |
|---|---|---|
| **D1** | Transactional Outbox + ShedLock | Consistencia eventual con resiliencia. ShedLock evita duplicados en entornos con múltiples réplicas. |
| **D2** | Notificaciones controladas RabbitMQ | `prefetch=1`, cola dedicada. |
| **D3** | Idempotencia DB + Webhook | Prevenir duplicados por doble clic o reintentos de webhook (usando `Idempotency-Key`). |
| **D4** | Soft Delete con `@Where` | Historial íntegro y consultas limpias. |
| **D5** | Circuit Breaker en FeignClient | Fallback ante fallos de dependencias + Dead Letter Table (Resilience4j). |
| **D6** | Respuestas API estandarizadas | Uso obligatorio de `RespuestaGenerica<T>` (wrapper común) desde `libreria-comun`. |
| **D7** | Strategy Pattern para Reglas de Negocio | Desacoplar reglas de negocio complejas (ej. días hábiles vs calendario). |

---

## Estándares Obligatorios del Ecosistema (Robustecidos)

1. **Seguridad JWT:** Extender `ConfiguracionSeguridadBase` desde `libreria-comun`. Leer `luka.jwt.clave-secreta` del `.env` maestro.
2. **Configuración compartida:** `spring.config.import=optional:file:../.env[.properties]`. Reutilizar credenciales de PostgreSQL, RabbitMQ, etc.
3. **Eliminación Absoluta de Eureka:** Prohibido el uso de Eureka. Se debe eliminar cualquier dependencia (`spring-cloud-starter-netflix-eureka-client`), configuración en `.yml` / `.properties`, variables de entorno y endpoints de salud de Eureka en **todos** los microservicios. Enrutamiento estrictamente mediante API Gateway y orquestación externa.
4. **Health checks:** Endpoint `/actuator/health` redirigido a `/health-internal` con `ControladorSaludCustom`.
5. **Logs correlacionados:** Interceptor Feign y filtro HTTP que inyectan `X-Correlation-ID` en MDC.
6. **Métricas:** Micrometer con contadores de outbox, fallos y eventos de dominio.
7. **OpenAPI 3:** Documentación automática en `/swagger-ui.html`.

---

## Gaps Detectados y Acciones Obligatorias a Integrar en Cada MS

1. **Estandarización de respuestas API:** Todos los controladores retornarán `ResponseEntity<RespuestaGenerica<...>>`.
2. **Reutilización estricta:** Uso del `ManejadorGlobalExcepciones`, `InterceptorCorrelacion` y `AuditoriaListener` (`@Auditable`) de `libreria-comun`.
3. **Fallbacks (Resilience4j):** 
   - `@CircuitBreaker(name = "dependencia", fallbackMethod = "metodoFallback")`.
   - `waitDuration = 5s`, `failureRateThreshold = 50`, `ringBufferSizeInHalfOpenState = 2`.
4. **Optimización de código y calidad:**
   - Entidades con `@Builder(toBuilder = true)`.
   - Validaciones con `@Valid` y grupos en DTOs.
   - Prevención de N+1 Queries usando `@EntityGraph`.
   - **Índices críticos** aplicados a nivel de base de datos.
5. **Resiliencia extrema de Schedulers:**
   - Bloqueo distribuido vía `@SchedulerLock` (ShedLock) con tabla en PostgreSQL.
   - Mecanismo Dead-letter (alerta RabbitMQ tras fallos consecutivos con backoffs exponenciales).
6. **Idempotencia en Webhooks/APIs Críticas:** Exigir header `Idempotency-Key` a guardar de forma única en BD.

---

## Pruebas Obligatorias (Tests)
- **Unitarias:** JUnit 5 + Mockito (> 80% cobertura mínima en lógica de negocio).
- **Integración:** `@SpringBootTest` + Testcontainers (PostgreSQL, RabbitMQ).
- **Contract Tests:** Spring Cloud Contract para los FeignClients.
- **Carga:** Simulación de carga extrema (ej. Gatling) para procesos críticos y batch.

---

## Documentación Técnica Obligatoria
- **OpenAPI 3:** Disponible y actualizada en `/swagger-ui.html`.
- **ADRs (Architecture Decision Records):** Para documentar decisiones clave (ej. D1 a D7), guardados en `docs/adr/`.
- **Diagramas:** Diagrama de secuencia completo de flujos críticos (creación → procesamiento → outbox → núcleo → notificación).

---

## Fases de Desarrollo Estándar (Recomendación)
- **Fase 0:** Preparación de `libreria-comun` (asegurar DTOs, excepciones, utilidades y seguridad JWT).
- **Fase 1:** Scaffolding, dominios optimizados y repositorios con índices y `@EntityGraph`.
- **Fase 2:** DTOs (como records) y Contratos de integración (Feign con Circuit Breaker).
- **Fase 3:** Lógica de negocio (patrones Strategy/Factory) y flujos críticos con Idempotencia.
- **Fase 4:** Schedulers con resiliencia (Outbox, ShedLock, Dead Letter).
- **Fase 5:** Controladores estandarizados, seguridad (JWT) y configuración del Gateway.

---

## Checklist de Aprobación para Implementación de Microservicios
- [ ] ¿`libreria-comun` tiene todos los componentes necesarios instalados y actualizados?
- [ ] ¿Se incluyeron ShedLock y Circuit Breaker en las dependencias?
- [ ] ¿Los índices de base de datos están definidos y optimizados?
- [ ] ¿Se implementó el patrón Strategy para la lógica de reglas de negocio compleja?
- [ ] ¿Hay idempotencia en webhooks/endpoints críticos mediante `Idempotency-Key`?
- [ ] ¿El scheduler de outbox tiene control de reintentos, dead-letter y backoff?
- [ ] ¿Todas las respuestas API usan `RespuestaGenerica<T>`?
- [ ] ¿Se configuró el interceptor de correlación (`X-Correlation-ID`)?
- [ ] ¿Las pruebas unitarias y de integración están implementadas (cobertura > 80%)?
