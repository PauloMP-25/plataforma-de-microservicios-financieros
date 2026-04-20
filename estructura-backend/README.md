# Backend - Plataforma de Microservicios Financieros

Esta carpeta contiene la lógica de negocio, servicios de infraestructura y módulos de inteligencia artificial de la plataforma.

## Módulos del Sistema

| Microservicio | Descripción | Tecnología |
| :--- | :--- | :--- |
| `microservicio-common` | Biblioteca compartida de seguridad (JWT), DTOs y excepciones. | Java / Spring |
| `microservicio-cliente` | Gestión de perfiles de usuario y entidades financieras. | Java / Spring Boot |
| `microservicio-usuario` | Gestiona el acceso, contraseñas, roles y la lógica de seguridad de bloqueo de IP tras 3 intentos fallidos | Java / Spring Boot |
| `microservicio-auditoria` | Registro centralizado de eventos y trazabilidad. | Java / Spring Boot |
| `microservicio-ia` | Análisis de transacciones, detección de anomalías y predicciones. | Python / Flask-FastAPI |
| `microservicio-mensajeria` | Gestión de notificaciones, OTP (Twilio) y correos (Gmail). | Java / Spring Boot |

## Requisitos previos

* **Java 21 (Amazon Corretto sugerido)**
* **Python 3.9+** (para el módulo de IA)
* **Maven 3.8+**
* **Docker** (para levantar bases de datos y brokers de mensajería)

## Flujo de Desarrollo

1. **Configuración de Red:** Los microservicios se comunican internamente a través de un API Gateway y usan Service Discovery (Eureka).
2. **Seguridad:** Todas las solicitudes deben pasar por la validación de JWT gestionada en el módulo `common`.
3. **Persistencia:** Cada microservicio gestiona su propia base de datos para garantizar el bajo acoplamiento.

## Ejecución de Tests
Para ejecutar las pruebas unitarias de todos los módulos de Java:
```bash
mvn test