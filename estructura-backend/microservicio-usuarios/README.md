# Microservicio de Identidad y Acceso (IAM)

Este microservicio es el núcleo de seguridad de la plataforma. Gestiona la autenticación, el registro de usuarios, la validación de cuentas mediante correo electrónico y la protección perimetral contra ataques de fuerza bruta.

## Arquitectura
El proyecto sigue los principios de **Clean Architecture**, dividiéndose en:
* **Dominio:** Entidades base (Usuario, Rol, IntentosLogin).
* **Presentacion:** 
* **Aplicación:** Servicios de negocio y DTOs.
* **Infraestructura:** Implementación de persistencia (JPA/PostgreSQL) y seguridad (Spring Security/JWT).

## Características de Seguridad
* **Autenticación JWT:** Generación de tokens HS384 con claims personalizados y roles.
* **IpRateLimitFilter:** Bloqueo automático por IP tras 3 intentos fallidos de login.
* **Confirmación de Cuenta:** Sistema de activación de perfil vía Token enviado por email.
* **Inicialización de Roles:** Carga automática de `ROLE_FREE` y `ROLE_ADMIN` mediante `DataLoader`.

## API Endpoints (v1)

### 1. Autenticación
`POST /api/v1/auth/login`

* **Request Body:**
```json
{
  "nombreUsuario": "pablo_dev",
  "contrasenia": "********"
}"

### 2. Registro Usuarios
`POST /api/v1/auth/registrar`
{
  "nombreUsuario": "paulo_25",
  "correo": "paulo@ejemplo.com",
  "contrasenia": "********"
}

### 3. Confirmacion Email
`GET /api/v1/auth/confirmar-email?token={tu_token}`

Parámetros: token (String) enviado al correo del usuario.
Respuestas:
- 200 OK: "Cuenta activada exitosamente".
- 400 Bad Request: Token expirado o inválido.