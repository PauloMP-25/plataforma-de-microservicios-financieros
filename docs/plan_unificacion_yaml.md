# Plan de Implementación: Refactorización y Parametrización de Configuraciones (base, local, docker)

Este plan detalla el proceso para unificar, limpiar y estructurar todos los archivos de configuración de Spring Boot (`application.yml`, `application-local.yml` y `application-docker.yml`) en todos los microservicios del ecosistema Luka, garantizando que:

1. **Cabeceras Descriptivas**: Todas las propiedades principales y secciones de configuración estarán organizadas bajo cabeceras de 119 caracteres de ancho con su descripción respectiva, centradas.
2. **Parametrización Estricta**: No existan contraseñas, tokens de seguridad, URLs o credenciales harcodeadas (cableadas) directamente en el cuerpo de las configuraciones de YAML. En su lugar, se referenciarán variables de entorno correspondientes con defaults de respaldo, resolviéndose desde:
   - Las variables inyectadas por la plataforma (Render/Docker) en producción.
   - El archivo `.env.local` cargado dinámicamente en desarrollo local.
3. **Importación Dinámica**:
   - En `application-local.yml` se cargará la configuración local vía `optional:file:../.env.local[.properties]` (o `../../.env.local[...]` para el API Gateway).
   - En `application-docker.yml` **NO** se cargará ningún archivo de entorno, ya que Render y Docker Compose inyectarán las variables directamente al entorno de ejecución del contenedor.

---

## Cambios Realizados

### 1. API Gateway

- **`application-local.yml`**: Importación redirigida a `.env.local`. Cabeceras de 119 caracteres alineadas y secretos parametrizados.
- **`application-docker.yml`**: Sin importación manual de archivos de entorno. Cabeceras de 119 caracteres alineadas.

### 2. Microservicio Auditoría

- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ y Redis parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 3. Microservicio Cliente

- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ y Redis parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 4. Microservicio Mensajería

- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ, Redis, Twilio y SMTP parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 5. Microservicio Núcleo Financiero

- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB y RabbitMQ parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 6. Microservicio Pago

- **`application.yml`**: Cabeceras de 119 caracteres alineadas.
- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ, Redis y Stripe parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 7. Microservicio Suscripciones

- **`application.yml`**: Creado nuevo archivo base.
- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ, Redis y Stripe parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

### 8. Microservicio Usuario

- **`application-local.yml`**: Importación redirigida a `.env.local`. Conexiones DB, RabbitMQ y Redis parametrizadas.
- **`application-docker.yml`**: Cabeceras de 119 caracteres alineadas. Sin importación.

---

## Plan de Verificación

1. **Compilación Maven**:
   - Compilación completa de cada uno de los microservicios modificados para comprobar compatibilidad de dependencias y sintaxis YAML:
     ```bash
     mvn clean compile
     ```
2. **Revisión de Git**:
   - Ejecutar `git diff` para verificar que ninguna variable secreta o cableada haya quedado en texto plano en los archivos YAML de configuración.
