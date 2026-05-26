<h1 align="center"> LUKA APP: Plataforma SaaS de Gestión Financiera Inteligente </h1>
<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-green?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Python-3.12-FFD43B?style=for-the-badge&logo=python&logoColor=3776AB" alt="Python">
  <img src="https://img.shields.io/badge/Angular-17-red?style=for-the-badge&logo=angular" alt="Angular">
  <img src="https://img.shields.io/badge/Docker-Enabled-blue?style=for-the-badge&logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/Google_Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Gemini">
  <img src="https://img.shields.io/badge/Eureka_Netflix-E50914?style=for-the-badge&logo=netflix&logoColor=white" alt="Eureka">
</p>

---

## Índice

* [Descripción del Proyecto](#descripción-del-proyecto)
* [Estado del Proyecto](#estado-del-proyecto)
* [Características Principales](#características-principales)
* [Tecnologías Utilizadas](#tecnologías-utilizadas)
* [Arquitectura y Estructura](#arquitectura-y-estructura)
* [Configuración Local](#configuración-local)
* [Personas Desarrolladoras](#personas-desarrolladoras)
* [Licencia](#licencia)

---

## Descripción del Proyecto

**LUKA APP** es una solución integral de gestión financiera diseñada bajo una arquitectura de microservicios escalable. La plataforma no solo gestiona transacciones, sino que utiliza **Inteligencia Artificial (Google Gemini)** para actuar como un coach financiero proactivo, detectando patrones de gasto y brindando proyecciones personalizadas para la salud económica del usuario.

---

## Estado del Proyecto

<p align="left">
  <img src="https://img.shields.io/badge/STATUS-EN%20DESARROLLO-brightgreen?style=for-the-badge" alt="Estado">
</p>

Actualmente, el sistema cuenta con la integración core de IA, Auditoría y Gestión Financiera. Próximamente se integrarán módulos de facturación, reportes avanzados e integración con WhatsApp.

---

## Características Principales
*   **Coach Financiero IA:** Consejos personalizados basados en historial y perfil de riesgo.
*   **Análisis de Gastos Hormiga:** Identificación automática de fugas de capital y categorías.
*   **Arquitectura Resiliente:** Comunicación asíncrona mediante **RabbitMQ** para auditoría inmutable.
*   **Gestión de Suscripciones (SaaS):** Diferenciación de servicios para roles `FREE`, `PREMIUM` y `ADMIN`.
*   **Service Discovery:** Gobernanza dinámica de servicios mediante **Eureka-Netflix**.

---

## Tecnologias Utilzadas

**Backend (Polyglot)**
*   Java 21 & Spring Boot 3.4.4: Núcleo transaccional, seguridad (JWT) y orquestación.
*   Python 3.12 & FastAPI: Procesamiento de IA, lógica de prompts y análisis predictivo.
*   RabbitMQ: Message Broker para el desacoplamiento de eventos y auditoría.
*   PostgreSQL: Almacenamiento relacional bajo el patrón *Database per Service*.
*   Google Gemini API: Motor de IA Generativa para coaching personalizado.


**Frontend:**

*   Angular 17: Framework para una interfaz de usuario SPA (Single Page Application).
*   PrimeNG & Bootstrap 5: Librerías de componentes UI y diseño responsivo.

**DevOps & Herramientas:**
*   Docker & Docker Compose: Contenerización y orquestación local del ecosistema.
*   Eureka Server: Registro y descubrimiento de microservicios.
*   Spring Cloud Gateway: Gateway centralizado para filtrado de seguridad y ruteo.
* GitHub Actions (CI/CD): Controlador de versiones


## Arquitectura

* **Arquitectura Polyglot:** Backend construido con Java (Spring Boot) y servicios especializados en Python (IA).

* El proyecto utiliza un patrón de microservicios con **Service Discovery (Eureka)** y **API Gateway**.

## Estructura del Proyecto
El proyecto se organiza como un **Monorepo** para facilitar la gestión del ciclo de vida del software:

* `/estructura-backend`: Código fuente de los microservicios (Spring Boot y Python).
* `/estructura-frontend`: Cliente web desarrollado en Angular.
* `/docker`: Archivos de despliegue para infraestructura (Postgres, Rabbit, Eureka).

## Configuración Local

Para facilitar el despliegue del ecosistema completo de **LUKA APP**, utilizamos una orquestación basada en Docker y un `Makefile` para simplificar los comandos.

### Requisitos Previos
* **Docker Desktop** (con soporte para Compose v2).
* **Make** (Instalado vía `winget install GnuWin32.Make` en Windows).
* **Git Bash** (Recomendado para ejecutar los comandos en Windows).

1. **Clonar el repositorio:**
   `git clone https://github.com/PauloMP-25/plataforma-de-microservicios-financieros.git`

2. **Preparar el entorno:**
   Entra a la carpeta de docker y genera tu archivo de secretos:
   Ingresa a: `cd estructura-backend/docker`
   Ejecuta ---> Con Make: `make setup` | Sin Make: `cp .env.example .env`

3. **Construir las imagenes:** Este paso descarga las dependencias de Maven y Python (solo la primera vez):
   Ejecuta ---> Con Make: `make build` | Sin Make: `docker compose build`

4. **Levantar el stack:** Puedes iniciar todo el sistema con un solo comando:
   Ejecuta ---> Con Make: `make up` | Sin Make: `docker compose up -d`

5. **Opcional:** Levanta por separado.
  `make infra` → Levanta solo la infraestructura (BD + RabbitMQ + Eureka)"
  `make service` → Levanta solo los microservicios (asume infra ya corriendo)"
  `make down` | `docker compose down` → Detiene y elimina contenedores (conserva volúmenes)"
  `make restart` → down + up"
* Nota: Todos los comandos funcionaran siempre y cuando te encuentres dentro de la carpeta "docker".

---
## Personas Desarrolladoras
*   **Paulo Moron** - Project Leader & Cloud Architect - [GitHub Profile](https://github.com/PauloMP-25)
*  **Cristina Astocaza** - Frontend Lead & QA. - [Github Profile](https://github.com/CristinaAstocaza)
*  **Gabriel Carazas** - Frontend Lead. - [Github Profile](https://github.com/gabriel0327)
*  **Paul Bendezu** - Frontend Lead. - [Github Profile](https://github.com/PaulBendezuTorres)
---


## Licencia
Este proyecto está bajo la Licencia **MIT**. Consulta el archivo `LICENSE` para más detalles.

© 2026 - LUKA APP Project
