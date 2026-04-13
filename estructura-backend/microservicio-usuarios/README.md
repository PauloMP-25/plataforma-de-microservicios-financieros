## Estado del Proyecto - Abril 2026
## Primera Actualizacion
Fecha: 12 de Abril, 2026
Responsable: Paulo Cesar Moron Poma

Arquitectura: Implementación de estructura de paquetes bajo el patrón de Arquitectura Limpia.

Dominio: * Entidades creadas: Usuario, Rol, IntentosLogin, TokenConfirmacionEmail.

Lógica inicial de persistencia con Repositorios JPA.

Seguridad: Definición de paquetes para gestión de JWT y Bloqueo de IP (en progreso).

## Estado del Proyecto - Abril 2026
## Segunda Actualizacion
Fecha: 12 de Abril, 2026
Responsable: Paulo Cesar Moron Poma

### Microservicio IAM (Finalizado Core)
- [x] **Seguridad Avanzada:** Implementación de `IpRateLimitFilter` para bloqueo por IP tras 3 intentos.
- [x] **Autenticación:** Integración de JWT (JSON Web Tokens) para sesiones seguras.
- [x] **Persistencia:** Configuración de entidades JPA para Usuarios, Roles e Intentos de Login en PostgreSQL.
- [x] **Controladores:** Endpoints de `/auth/login` y registro base listos para pruebas.

### Próximos Pasos:
1. Pruebas unitarias de la lógica de bloqueo.
2. Integración con el Microservicio de Auditoría (vía eventos).
3. Configuración de variables de entorno en `application.yml`.


## Estado del Proyecto - Abril 2026
## Tercera Actualizacion
Fecha: 13 de Abril, 2026 | Hora: 02:40 AM
Responsable: Paulo Cesar Moron Poma

## Microservicio IAM (Core Operativo)
Se ha finalizado la estabilización del núcleo de identidad y acceso, superando los bloqueos de configuración de Spring Security y persistencia.

Hitos Alcanzados:
- [x] Arquitectura Limpia: Estructura de paquetes organizada en Dominio, Aplicación e Infraestructura.
- [x] Seguridad Robusta: - Implementación de FiltroRateLimitIp (Bloqueo tras 3 intentos fallidos).

Generación y validación de JWT (HS384) con corrección de decodificación Base64.
Manejo de referencias circulares mediante inyección @Lazy y métodos estáticos.

- [x] Persistencia y Datos: - Mapeo completo en PostgreSQL (Usuarios, Roles, Tokens de Confirmación, Intentos de Login).
Creación de DataLoader para la inicialización automática de roles (ROLE_FREE, ROLE_ADMIN).

- [x] Flujo de Usuario:
Registro de usuario con asignación de roles.
Sistema de Confirmación de Correo funcional mediante tokens (activación de cuenta).
Login exitoso con retorno de Claims y Roles.

## Pendientes Críticos (Próxima Sesión):
Refactor de Login: Cambiar la lógica de autenticación para que el identificador principal sea el Correo Electrónico en lugar del nombre de usuario.
Envío de Correo Real: Integrar spring-boot-starter-mail con el servidor SMTP (Gmail/SendGrid) para que el token llegue físicamente al usuario.
Auditoría y Eventos: - Implementar un publicador de eventos para enviar registros a la base de datos de auditoría.
El token JWT ahora debe ser enviado en el Authorization header para que otros microservicios (Eventos/Pagos) validen al usuario.
Prueba de Estrés (Bloqueo IP): Provocar 3 fallos intencionales desde Postman para verificar el bloqueo en la tabla intentos_login.