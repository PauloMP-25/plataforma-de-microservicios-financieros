# Resumen de cambios realizados en autenticación, Gateway y Docker

## Contexto

El flujo de autenticación presentaba varias inconsistencias entre el frontend Angular, el microservicio de usuarios y el API Gateway. Además, en Docker solo estaban quedando disponibles el Gateway y el microservicio de usuario, mientras que los demás servicios esperados en los puertos 8082 a 8087 no se levantaban al iniciar el Gateway.

## Cambios en frontend de autenticación

### Contrato de autenticación

Archivo modificado: `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/core/models/auth/user.model.ts`

Se agregaron campos y tipos que el backend ya devolvía o esperaba:

- `refreshToken`
- `refreshExpiraEn`
- `SolicitudRefreshToken`
- `TipoVerificacionOtp`
- `SolicitudReenvioOtp`
- `detalles?: string[]` en `ResultadoApi<T>` para representar errores de validación del backend.

Motivo: el frontend no estaba sincronizado con la respuesta real de autenticación ni con el formato de errores de validación.

### Servicio de autenticación

Archivo modificado: `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/core/services/auth.service.ts`

Se confirmó y ajustó el consumo del Gateway con base:

```text
http://localhost:8080/api/v1/auth
```

Se corrigió activación de cuenta para usar:

```text
PUT /api/v1/auth/activar?correo=...&codigoOtp=...
```

También se agregó soporte para:

- Solicitar OTP por canal elegido.
- Refrescar token.
- Guardar `refreshToken` en sesión local.

Motivo: el frontend usaba un flujo distinto al backend; el backend no envía OTP automáticamente al registrar, sino que espera solicitud explícita por canal.

### Registro y validación de contraseña

Archivos modificados:

- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/autenticacion/crear-cuenta/crear-cuenta.ts`
- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/autenticacion/crear-cuenta/crear-cuenta.html`

Se sincronizó la validación de contraseña con el backend:

- Mínimo 8 caracteres.
- Al menos una minúscula.
- Al menos una mayúscula.
- Al menos un número.
- Al menos un carácter especial: `@$!%*?&#`.

Motivo: el backend usa `@ValidarPassword`; el frontend solo validaba mínimo 6 caracteres. Por eso el registro retornaba `400 Bad Request` cuando se usaba una contraseña como `123456`.

También se mejoró el mensaje de error para mostrar `detalles[]` devueltos por backend.

### Flujo de OTP por canal

Archivos modificados:

- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/autenticacion/contenedor-autenticacion/contenedor-autenticacion.ts`
- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/autenticacion/contenedor-autenticacion/contenedor-autenticacion.html`
- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/autenticacion/contenedor-autenticacion/contenedor-autenticacion.scss`
- `plataforma-de-microservicios-financieros/estructura-frontend/frontend/src/app/features/recuperar-contrasena/verificar-codigo/verificar-codigo.ts`

Se agregó vista intermedia para elegir canal:

- EMAIL
- SMS
- WHATSAPP

Motivo: el backend requiere que el usuario solicite el OTP explícitamente por un canal. El frontend antes asumía que el OTP ya había sido enviado al registrar.

## Cambios en API Gateway

Archivos modificados:

- `plataforma-de-microservicios-financieros/estructura-backend/infraestructura/api-gateway/src/main/resources/application-local.yml`
- `plataforma-de-microservicios-financieros/estructura-backend/infraestructura/api-gateway/src/main/resources/application-docker.yml`

### Rutas hacia microservicio usuario

Se cambió la ruta local del Gateway para que dentro de Docker no apunte a `localhost:8081`, sino al nombre del contenedor:

```yaml
uri: ${URL_PROD_USUARIO:http://localhost:8081}
```

En Docker Compose se inyecta:

```yaml
URL_PROD_USUARIO: http://ms-usuario:8081
```

Motivo: dentro de un contenedor, `localhost` apunta al mismo contenedor del Gateway, no al microservicio usuario.

### CORS duplicado

Se agregó filtro global:

```yaml
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin RETAIN_FIRST
```

Motivo: tanto Gateway como el microservicio usuario devolvían headers CORS, generando duplicación de `Access-Control-Allow-Origin` y bloqueo del navegador.

## Cambios en Docker Compose híbrido

Archivo modificado: `plataforma-de-microservicios-financieros/estructura-backend/docker/docker-compose-hibrido.yml`

### Servicios expuestos

El compose ya define los puertos esperados:

- Gateway: `8080`
- Usuario: `8081`
- Auditoría: `8082`
- Cliente: `8083`
- Mensajería: `8084`
- Núcleo financiero: `8085`
- IA: `8086`
- Pagos: `8087`

### Ajuste aplicado

Se agregaron dependencias explícitas del `api-gateway` hacia los servicios 8081-8087 y hacia `rabbitmq`.

Motivo: si se ejecutaba `docker compose up -d --build api-gateway`, Docker Compose solo levantaba las dependencias declaradas del Gateway. Antes el Gateway dependía solo de `redis`, por eso no se levantaban automáticamente los microservicios 8082-8087. Con este cambio, levantar el Gateway también arrastra todos los servicios necesarios.

## Comando recomendado para levantar todo

Desde la raíz del workspace:

```powershell
docker compose -f plataforma-de-microservicios-financieros/estructura-backend/docker/docker-compose-hibrido.yml up -d --build api-gateway
```

También se puede levantar todo explícitamente:

```powershell
docker compose -f plataforma-de-microservicios-financieros/estructura-backend/docker/docker-compose-hibrido.yml up -d --build rabbitmq redis ms-auditoria ms-cliente ms-mensajeria ms-nucleo-financiero ms-ia ms-pagos ms-usuario api-gateway
```

## Verificaciones realizadas

- Registro con contraseña débil reprodujo `400 Bad Request` y mostró detalles de validación.
- Registro con contraseña fuerte `Password1@` respondió `201` vía Gateway.
- Build de frontend en modo desarrollo finalizó correctamente.
- Docker Compose levantó contenedores para puertos 8080 a 8087.

## Pendiente recomendado

Si algún microservicio aparece como `unhealthy` después de iniciar, revisar sus logs individuales con:

```powershell
docker logs <nombre-del-contenedor> --tail 120
```

Los casos más probables son base de datos local inexistente, credenciales de PostgreSQL incorrectas, o variables externas vacías para mensajería/WhatsApp/Twilio/Gmail.
