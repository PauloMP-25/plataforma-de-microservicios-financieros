Fecha: 12 de Abril, 2026
Responsable: Paulo Cesar Moron Poma

Arquitectura: Implementación de estructura de paquetes bajo el patrón de Arquitectura Limpia.

Dominio: * Entidades creadas: Usuario, Rol, IntentosLogin, TokenConfirmacionEmail.

Lógica inicial de persistencia con Repositorios JPA.

Seguridad: Definición de paquetes para gestión de JWT y Bloqueo de IP (en progreso).

## 🚀 Estado del Proyecto - Abril 2026
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