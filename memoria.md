# Memoria de Desarrollo - Plataforma de Microservicios Financieros

Última actualización: 2026-05-21 17:17 (PET)

## 🚀 Logros Recientes
- Creación y reubicación de la carpeta local de habilidades a `.skills/`.
- Agregada habilidad local para Angular.
- Corrección de la animación de deslizamiento en el contenedor de autenticación usando `Location.go()` en vez de la destrucción de componentes por rutas.
- Configuración de reglas personalizadas en `.cursorrules`.
- Implementada la conexión HTTP en `SolicitarCorreo` con `AuthService.solicitarRecuperacion()`.
- Implementada la lógica de reenvío de código OTP en `VerificarCodigo` con `AuthService.solicitarRecuperacion()`.
- Implementada la lógica de restablecimiento de contraseña en `NuevaContrasena` con `AuthService.resetPassword()`.

## 📌 Estado Actual de los Componentes
- **Backend (Spring Boot)**: Microservicio de usuarios activo en puerto 8081 con endpoints de autenticación y recuperación de contraseña.
- **Frontend (Angular)**: Páginas de login, registro y recuperación de contraseña totalmente conectadas a los servicios HTTP del backend.
- **Base de Datos**: PostgreSQL / MySQL en microservicios backend correspondientes.

## 🛠️ Próximos Pasos (Pendientes)
- [ ] Realizar pruebas manuales de extremo a extremo del flujo de recuperación de contraseña.
- [ ] Implementar la validación y gestión de sesiones persistentes en el frontend.

## 🧠 Decisiones Clave y Notas
- Utilizar `sessionStorage` temporalmente para pasar el correo, el `registroId` de verificación, y el código OTP entre los pasos del flujo de recuperación de contraseña sin comprometer datos globales.
