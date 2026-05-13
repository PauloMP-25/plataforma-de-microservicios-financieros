# 📘 Guía de Integración: Microservicio de Usuario (MS-USUARIO)

Esta guía detalla los endpoints, DTOs y flujos de error para el equipo de Frontend.

---

## 🔐 1. Autenticación y Registro

### 1.1 Registro de Usuario
**Endpoint:** `POST /api/v1/auth/registrar`  
**Descripción:** Crea una cuenta nueva. El usuario se crea en estado inactivo.  
**Requiere Token:** No.

**Cuerpo de la Petición (Request Body):**
```json
{
  "nombreUsuario": "paulo_dev",
  "correo": "paulo@ejemplo.com",
  "password": "Luka1234!",
  "confirmarPassword": "Luka1234!"
}
```

**Respuestas:**
*   **201 Created:** Registro exitoso. Retorna el UUID del usuario.
    ```json
    {
      "exito": true,
      "mensaje": "Registro exitoso. Revise su correo para activar la cuenta.",
      "datos": "550e8400-e29b-41d4-a716-446655440000",
      "codigo": "OK"
    }
    ```
*   **400 Bad Request:** Error de validación (ej: contraseña débil).
    ```json
    {
      "exito": false,
      "mensaje": "La contraseña debe contener al menos una mayúscula",
      "datos": null,
      "codigo": "ERROR_VALIDACION"
    }
    ```

---

### 1.2 Inicio de Sesión (Login)
**Endpoint:** `POST /api/v1/auth/login`  
**Descripción:** Autentica al usuario y retorna tokens JWT.  
**Requiere Token:** No.

**Cuerpo de la Petición:**
```json
{
  "correo": "paulo@ejemplo.com",
  "password": "Luka1234!"
}
```

**Respuestas:**
*   **200 OK:** Login exitoso.
    ```json
    {
      "exito": true,
      "mensaje": "Autenticación exitosa.",
      "datos": {
        "token": "eyJhbG...",
        "refreshToken": "eyJhbG...",
        "expiraEn": 86400000,
        "nombreUsuario": "paulo_dev",
        "roles": ["ROLE_FREE", "ROLE_PRO", "ROLE_PREMIUM", "ROLE_ADMIN", "ROLE_ADMINISTRADOR"]
      },
      "codigo": "OK"
    }
    ```
*   **Nota sobre Roles**: Por defecto, toda cuenta nueva recibe el rol `ROLE_FREE`. Los roles son:
    - `ROLE_FREE`: Universitario estándar.
    - `ROLE_PRO`: Usuario con funciones avanzadas de análisis.
    - `ROLE_PREMIUM`: Acceso total e ilimitado al coach.
    - `ROLE_ADMIN` / `ROLE_ADMINISTRADOR`: Gestión de plataforma.
*   **401 Unauthorized:** Credenciales incorrectas.
*   **429 Too Many Requests:** IP bloqueada tras 3 intentos fallidos.

---

## 🛡️ 2. Seguridad: Protección contra Fuerza Bruta (IP Blocking)

**Regla Crítica:** El sistema bloquea automáticamente cualquier IP que registre **3 intentos fallidos** de login en una ventana de 10 minutos.

*   **Comportamiento:** El API Gateway interceptará la petición antes de llegar al microservicio.
*   **Respuesta de Bloqueo:**
    ```text
    HTTP Status: 429 Too Many Requests
    Cuerpo: (Vacio o mensaje del Gateway)
    ```

---

### 1.3 Activación de Cuenta (OTP)
**Endpoint:** `PUT /api/v1/auth/activar/{usuarioId}`  
**Descripción:** Valida el código OTP enviado al correo para habilitar la cuenta.  
**Parámetros URL:** `usuarioId` (UUID).  
**Parámetros Query:** `codigoOtp` (String), `telefono` (Opcional).  
**Requiere Token:** No.

**Respuesta Exitosa (200 OK):**
```json
{
  "exito": true,
  "mensaje": "Cuenta activada correctamente. Ya puede iniciar sesión.",
  "datos": "OK",
  "codigo": "OK"
}
```

---

## 🔑 3. Gestión de Contraseña y Sesión

### 3.1 Recuperar Contraseña (Solicitud)
**Endpoint:** `POST /api/v1/auth/recuperar-solicitar`  
**Cuerpo:** `{"correo": "paulo@ejemplo.com"}`  
**Respuesta:** Retorna un mensaje indicando que se envió el código si el correo existe.

### 3.2 Confirmar Recuperación
**Endpoint:** `POST /api/v1/auth/recuperar-confirmar`  
**Parámetros Query:** `registroId` (UUID), `codigoOtp` (String).  
**Cuerpo:** `{"nuevoPassword": "...", "confirmarPassword": "..."}`

---

### 3.3 Logout
**Endpoint:** `POST /api/v1/auth/logout`  
**Requiere Token:** Sí (Bearer Token).  
**Descripción:** Invalida el token actual (lo añade a una blacklist en Redis).

---

## 🛠️ 4. Formato Estándar de Errores
Todos los errores siguen esta estructura:
```json
{
  "exito": false,
  "mensaje": "Descripción legible para el usuario",
  "datos": null,
  "codigo": "CÓDIGO_INTERNO_ERROR"
}
```
*   `ERROR_VALIDACION`: Campos del DTO inválidos.
*   `ACCESO_NO_AUTORIZADO`: Token ausente o expirado.
*   `CREDENCIALES_INVALIDAS`: Correo o password incorrectos.

---

## 🏗️ 5. Arquitectura y Patrones de Diseño (Ecosistema LUKA)

El sistema ha sido modernizado bajo estándares de **Clean Architecture** y patrones de diseño avanzados:

### 5.1 Patrón Strategy (Microservicio de Mensajería)
El despacho de notificaciones utiliza el **Patrón Strategy** puro. Esto permite que el sistema sea agnóstico al canal de envío:
- **Email**: Implementado vía SMTP (JavaMail).
- **SMS**: Implementado vía Twilio SDK.
- **WhatsApp**: Implementado vía Meta Cloud API.
- **Extensibilidad**: Se pueden añadir nuevos canales (Telegram, Push, etc.) sin modificar la lógica de negocio central.

### 5.2 Specification Pattern (MS-USUARIO, MS-AUDITORIA, MS-MENSAJERIA)
Implementado para desacoplar la lógica de consulta del motor de persistencia. Permite búsquedas dinámicas modulares:
- **MS-USUARIO**: Filtrado por `habilitado`, `rol`, `fechaCreacion` y `texto`.
- **MS-AUDITORIA**: Filtrado por `modulo`, `rangoFechas`, `servicioOrigen`.
- **MS-MENSAJERIA**: Auditoría de OTPs por `usuarioId`, `proposito` y `estadoUsado`.

---

## 🚀 Próximos Pasos para Frontend
1.  **Activación de Cuenta**: Al registrarse, capturar el `usuarioId` y enviarlo al endpoint de activación junto al código recibido por el usuario.
2.  **Manejo de Throttling**: Si el API retorna un error `429`, mostrar un temporizador al usuario indicando el tiempo de bloqueo.
3.  **Roles**: Asegurarse de que el panel administrativo use los filtros dinámicos (Specification Pattern) para la gestión masiva de usuarios.
