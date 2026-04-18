# Microservicio-Mensajería
**Puerto:** `8084` | **Base de datos:** `db_microservicio_mensajeria`

## Estado del Proyecto — Abril 2026

### Primera versión completa
**Fecha:** 16 de Abril, 2026
**Responsable:** Paulo Cesar Moron Poma

---

## Responsabilidad

Actúa como receptor de órdenes del **Microservicio-Usuario**.
Gestiona la generación y validación de códigos OTP de 6 dígitos,
enviándolos por **Email (JavaMail)** o **SMS (Twilio)**.

---

## Flujo de Operación

```
Frontend / ms-usuario
       │
       ▼
POST /api/v1/mensajeria/otp/generar
       │  usuarioId + email + tipo
       │
       ├─► Persiste CodigoVerificacion (con usuarioId obligatorio)
       ├─► Envía por EmailService o SmsService
       └─► Reporta a ms-auditoría (async)

POST /api/v1/mensajeria/otp/validar
       │  usuarioId + codigo + tokenActivacion
       │
       ├─► Verifica bloqueo (máx 3 intentos → 10h bloqueado)
       ├─► Busca código pendiente más reciente
       ├─► Si correcto:
       │     ├─► Marca código como usado
       │     ├─► Resetea contador de intentos
       │     ├─► Llama ms-usuario vía Feign → confirmarEmail(tokenActivacion)
       │     └─► Reporta OTP_VALIDACION_EXITOSA a ms-auditoría
       └─► Si incorrecto:
             ├─► Incrementa intentos
             ├─► Si intentos ≥ 3: bloquea usuarioId por 10h
             └─► Reporta OTP_VALIDACION_FALLIDA a ms-auditoría
```

---

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/api/v1/mensajeria/otp/generar` | Genera y envía un OTP |
| `POST` | `/api/v1/mensajeria/otp/validar?tokenActivacion={token}` | Valida el OTP |

### POST /generar — Body
```json
{
  "usuarioId" : "550e8400-e29b-41d4-a716-446655440000",
  "email"     : "usuario@ejemplo.com",
  "telefono"  : "+51999888777",
  "tipo"      : "EMAIL"
}
```

### POST /validar — Body
```json
{
  "usuarioId" : "550e8400-e29b-41d4-a716-446655440000",
  "codigo"    : "482910"
}
```
El parámetro `tokenActivacion` va como query param:
`POST /validar?tokenActivacion=uuid-del-token-de-confirmacion`

---

## Configuración requerida (variables de entorno)

```properties
MAIL_USERNAME=tu_correo@gmail.com
MAIL_PASSWORD=app_password_gmail

TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_PHONE_NUMBER=+15005550006
```

---

## Dependencias de otros microservicios

| Servicio | Puerto | Uso |
|----------|--------|-----|
| microservicio-usuario | 8081 | Activar cuenta vía Feign (GET /api/v1/auth/confirmar-email) |
| microservicio-auditoría | 8082 | Reportar eventos OTP vía RestTemplate async |

---

## Tablas en PostgreSQL

```sql
-- Códigos OTP
CREATE TABLE codigos_verificacion (
  id              BIGSERIAL PRIMARY KEY,
  usuario_id      UUID NOT NULL,
  email           VARCHAR(150) NOT NULL,
  telefono        VARCHAR(20),
  codigo          VARCHAR(6) NOT NULL,
  tipo            VARCHAR(10) NOT NULL,
  fecha_creacion  TIMESTAMP NOT NULL,
  fecha_expiracion TIMESTAMP NOT NULL,
  usado           BOOLEAN NOT NULL DEFAULT false,
  fecha_uso       TIMESTAMP
);

-- Control de intentos de validación fallidos
CREATE TABLE intentos_validacion_otp (
  id                   BIGSERIAL PRIMARY KEY,
  usuario_id           UUID NOT NULL UNIQUE,
  intentos             INT NOT NULL DEFAULT 0,
  ultima_modificacion  TIMESTAMP NOT NULL,
  bloqueado            BOOLEAN NOT NULL DEFAULT false,
  bloqueado_hasta      TIMESTAMP
);
```
*(Hibernate genera las tablas automáticamente con `ddl-auto=update`)*

---

## Jobs Programados

| Job | Frecuencia | Acción |
|-----|-----------|--------|
| `eliminarCodigosExpirados` | Cada 24 horas | Limpia códigos OTP no usados y expirados |
| `desbloquearUsuariosExpirados` | Cada 15 minutos | Desbloquea usuarios cuyo período venció |

---

## Hitos implementados

- [x] Arquitectura Limpia (Dominio / Aplicación / Infraestructura / Presentación)
- [x] Entidad `CodigoVerificacion` con `usuarioId` (UUID) obligatorio
- [x] Entidad `IntentoValidacion` para bloqueo por usuarioId
- [x] `ServicioMensajeria` — generación, validación, bloqueo, activación
- [x] `EmailService` con HTML enriquecido (JavaMail)
- [x] `SmsService` con Twilio (inicialización lazy)
- [x] `ClienteUsuario` (Feign) → activa cuenta en ms-usuario
- [x] `ClienteAuditoria` (RestTemplate @Async) → reporta a ms-auditoría
- [x] `@Scheduled` de limpieza cada 24h + desbloqueo cada 15min
- [x] Manejador global de excepciones con respuestas estructuradas
- [x] Tests unitarios con Mockito (generación, validación, bloqueo)

## Próximos pasos

- [ ] Agregar endpoint para reenviar OTP (con cooldown de 60 segundos)
- [ ] Soporte para `tokenActivacion` almacenado en este microservicio
       para no depender de que el frontend lo pase como query param
- [ ] Agregar Actuator + Health Check
- [ ] Dockerizar junto a los otros dos microservicios
