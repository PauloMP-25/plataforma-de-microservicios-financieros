# Plataforma de Microservicios Financieros (SaaS)

Esta es una solución integral de gestión financiera diseñada bajo una arquitectura de microservicios escalable. La plataforma no solo gestiona transacciones, sino que utiliza Inteligencia Artificial para el análisis predictivo y cuenta con un robusto sistema de auditoría y seguridad.

## Características Principales

* **Arquitectura Polyglot:** Backend construido con Java (Spring Boot) y servicios especializados en Python (IA).
* **Análisis Predictivo:** Microservicio dedicado a la detección de anomalías y proyecciones financieras.
* **Seguridad Avanzada:** Implementación de seguridad perimetral con bloqueo de IP y gestión de identidades.
* **Auditoría Centralizada:** Registro detallado de operaciones para cumplimiento y trazabilidad.

## Estructura del Proyecto (Monorepo)

El proyecto se organiza de la siguiente manera:

* `/estructura-backend`: Contiene los microservicios (Spring Boot, Python).
* `/estructura-frontend`: Interfaz de usuario desarrollada en Angular.
* `/docker`: Archivos de configuración para el despliegue en contenedores.

## Stack Tecnológico

**Backend:**
* Java 21+ / Spring Boot
* Spring Cloud (Gateway, Config Server, Eureka)
* Python (para el Microservicio de IA)
* PostgreSQL (Persistencia)

**Frontend:**
* Angular
* Boostrap / PrimeNG

**DevOps & Herramientas:**
* Docker & Docker Compose
* GitHub Actions (CI/CD)

## Configuración Local

1. Clona el repositorio:
   `git clone https://github.com/PauloMP-25/plataforma-de-microservicios-financieros.git`
2. Configura las variables de entorno en cada microservicio.
3. Ejecuta los contenedores de base de datos usando Docker Compose.
4. Levanta los servicios de Spring Boot.

---
© 2026 - Desarrollado por PauloMP-25