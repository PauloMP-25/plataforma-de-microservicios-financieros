# Manual de Docker para Entorno Local (Windows)

Este manual está diseñado para el equipo de desarrollo y QA que utiliza Windows. Describe los pasos necesarios para levantar el entorno de desarrollo del backend de la plataforma LUKA utilizando Docker Desktop.

## Prerrequisitos
1. Tener **Docker Desktop** instalado, abierto y corriendo en Windows.
2. Contar con el archivo `.env.local` configurado en la carpeta `estructura-backend`.

---

## FASE 1: Levantar la Infraestructura Local

El archivo híbrido asume que los servicios base (Bases de datos, Mensajería, Caché) se encuentran corriendo en la "máquina host" (apuntados mediante `host.docker.internal`). Dado que estás en Windows usando Docker Desktop, la manera más rápida de tener esta infraestructura es lanzar contenedores individuales que expongan sus puertos a tu máquina.

Ejecuta los siguientes comandos en tu terminal (PowerShell o CMD) uno por uno. Estos contenedores correrán en segundo plano:

### 1.1 Iniciar PostgreSQL
```bash
docker run -d --name luka-postgres -p 5432:5432 -e POSTGRES_PASSWORD=adminUTP postgres:16.14
```
*(Esto descargará y arrancará PostgreSQL exponiendo el puerto 5432).*

### 1.2 Iniciar RabbitMQ
```bash
docker run -d --name luka-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```
*(Esto arrancará RabbitMQ. Podrás ver el panel de control ingresando a `http://localhost:15672` con usuario `guest` y contraseña `guest`).*

### 1.3 Iniciar Redis
```bash
docker run -d --name luka-redis -p 6379:6379 redis:7.4
```
*(Esto arrancará Redis exponiendo el puerto 6379).*

---

## FASE 2: Levantar los Microservicios (Backend Híbrido)

Una vez que la infraestructura de la Fase 1 esté corriendo exitosamente, procederemos a levantar todos los microservicios de LUKA. Estos microservicios están optimizados en memoria para que no saturen tu computadora.

1. Abre tu terminal y navega hasta la carpeta `estructura-backend/docker`:
   ```bash
   cd estructura-backend/docker
   ```

2. Ejecuta el comando de Docker Compose para construir las imágenes de los contenedores (si no existen o si hubo cambios en el código) y levantarlos en segundo plano:
   ```bash
   docker-compose -f docker-compose-hibrido.yml up -d --build
   ```
   *(Nota: El flag `--build` fuerza a Docker a compilar tu código Java/Python y crear las imágenes frescas antes de iniciar).*

3. **Verificar el estado:**
   Puedes abrir **Docker Desktop** y observar cómo todos los contenedores con el prefijo `luka-` se van poniendo en verde. Dado que los servicios de Java (Spring Boot) toman cerca de 1 minuto en arrancar por completo, espera un momento.

---

## FASE 3: Verificación (Opcional)

Para cerciorarte de que los servicios principales han encendido correctamente, puedes verificar los "Health Checks":

* **API Gateway:** Ingresa en tu navegador a `http://localhost:8080/actuator/health` (Deberías ver `{"status":"UP"}`).
* **Microservicio IA:** Ingresa en tu navegador a `http://localhost:8086/actuator/health` (Deberías ver `{"status":"UP"}`).

Los demás microservicios requerirán un token JWT válido para acceder a `/health-internal`, lo cual comprueba que Spring Security está protegiéndolos correctamente.

---

## FASE 4: Detener el Entorno

Cuando termines tu jornada de trabajo o desees apagar todo para liberar recursos, sigue estos pasos:

1. **Detener los microservicios:**
   Navega a `estructura-backend/docker` y ejecuta:
   ```bash
   docker-compose -f docker-compose-hibrido.yml down
   ```

2. **Detener la infraestructura (Postgres, RabbitMQ, Redis):**
   ```bash
   docker stop luka-postgres luka-rabbitmq luka-redis
   ```
   *(Nota: Puedes encenderlos nuevamente otro día usando `docker start luka-postgres luka-rabbitmq luka-redis` en lugar de crear unos nuevos).*
