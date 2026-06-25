# 🗺️ DISTRIBUCIÓN DE MICROSERVICIOS POR PLATAFORMA

> Esta distribución define dónde vive cada microservicio en producción.
> Los dominios exactos se confirman al crear cada servicio en el panel de la plataforma.

## Railway — api-gateway · ms-usuario · ms-nucleo-financiero · ms-ia

| Microservicio | Puerto | Patrón de dominio Railway |
|---|---|---|
| `api-gateway` | 8080 | `https://luka-gateway-production-<hash>.up.railway.app` |
| `ms-usuario` | 8081 | `https://luka-usuario-production-<hash>.up.railway.app` |
| `ms-nucleo-financiero` | 8085 | `https://luka-financiero-production-<hash>.up.railway.app` |
| `ms-ia` | 8086 | `https://luka-ia-production-<hash>.up.railway.app` |

- Variable `PORT` inyectada dinámicamente por Railway ✅
- RAM free tier: ~512 MB por servicio
- `JAVA_TOOL_OPTIONS` con `MaxRAMPercentage=75.0` aplica directamente

---

## Koyeb — ms-mensajeria · ms-pagos · ms-suscripciones · ms-cliente

| Microservicio | Puerto | Patrón de dominio Koyeb |
|---|---|---|
| `ms-mensajeria` | 8084 | `https://luka-mensajeria-<org>.koyeb.app` |
| `ms-pagos` | 8087 | `https://luka-pagos-<org>.koyeb.app` |
| `ms-suscripciones` | 8088 | `https://luka-suscripciones-<org>.koyeb.app` |
| `ms-cliente` | 8083 | `https://luka-cliente-<org>.koyeb.app` |

- Variable `PORT` inyectada dinámicamente por Koyeb ✅
- RAM free tier: 512 MB por servicio
- Health check configurado en Koyeb apuntando a `/actuator/health`

---

## Render — ms-auditoria

| Microservicio | Puerto | Patrón de dominio Render |
|---|---|---|
| `ms-auditoria` | 8082 | `https://luka-auditoria.onrender.com` |

- Variable `PORT` inyectada dinámicamente por Render ✅
- RAM free tier: 512 MB
- Cold start: instancias se apagan tras 15 min de inactividad (tier gratuito)

---

> **⚠️ TODO al desplegar:** Reemplazar los dominios placeholder en `.env` (`URL_PROD_*` y `CORS_ALLOWED_ORIGINS`) con las URLs reales asignadas por cada plataforma. No olvidar añadir el dominio del frontend Angular.

---

# 📋 FASE 0 — INVENTARIO Y PREREQUISITOS

Antes de tocar código, debes tener en mano las siguientes credenciales de tus servicios cloud.

> 0.1 Datos que necesitas recopilar

NeonDB (una instancia por microservicio que tenga BD):
>Host:     ep-xxxx.us-east-1.aws.neon.tech
>Port:     5432
>Database: db_microservicio_usuario  (una por servicio)
>User:     luka_user
>Password: xxxxxxxx
>SSL:      require

La URL final tendrá esta forma:
>jdbc:postgresql://ep-xxxx.us-east-1.aws.neon.tech:5432/db_nombre?sslmode=require
---
Upstash Redis (una sola instancia compartida, o una por servicio según tu plan):
>Host:     caring-xxx.upstash.io
>Port:     6380  (puerto TLS de Upstash, NO el 6379)
>Password: xxxxxxxx
>SSL:      true
---
CLOUDAMQP
>Host:     shark.rmq.cloudamqp.com
>Port:     5671  (AMQPS con SSL, NO el 5672)
>Username: tu_usuario
>Password: tu_password
>Vhost:    tu_vhost
---

## 🔧 FASE 1 — CORRECCIONES EN libreria-comun

Estos cambios afectan a todos los microservicios Java porque heredan de esta librería.

1.1 Corregir CORS para aceptar dominios de producción  
Archivo: `libreria-comun/src/main/java/com/libreria/comun/seguridad/ConfiguracionSeguridadBase.java`  
Problema: La lista de orígenes CORS solo tiene localhost. En producción los microservicios recibirán peticiones desde dominios de Railway, Koyeb, Vercel, etc.  
Qué cambiar: Reemplaza la lista hardcodeada de `allowedOrigins` por una lectura desde una variable de entorno. La lógica debe quedar así:

> Si existe la variable de entorno `CORS_ALLOWED_ORIGINS`, parsearla como lista separada por comas.  
> Si no existe, usar la lista actual de localhost como fallback para desarrollo local.

Variable de entorno a agregar en todos los paneles cloud:

> `CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app,https://tu-app.railway.app`

1.2 Corregir el bug de routing key en `PublicadorEventosBase`  
Archivo: `libreria-comun/src/main/java/com/libreria/comun/mensajeria/PublicadorEventosBase.java`  
Línea exacta a corregir:

```java
// ACTUAL (incorrecto):
String rk = "auditoria.evento" + accion.toLowerCase();

// CORRECTO:
String rk = "auditoria.evento." + accion.toLowerCase();

1.3 Agregar soporte SSL para RabbitMQ CloudAMQP en LukaConfiguracion
Archivo: libreria-comun/src/main/java/com/libreria/comun/autoconfiguracion/LukaConfiguracion.java
Problema: El ConnectionFactory de RabbitMQ se construye sin configuración SSL. CloudAMQP requiere conexión por AMQPS (puerto 5671 con TLS).
Qué cambiar: Añadir dos propiedades nuevas inyectadas con @Value:

spring.rabbitmq.ssl.enabled    (boolean)
spring.rabbitmq.ssl.algorithm  (String, valor: "TLSv1.2")

En el método connectionFactory(), si ssl.enabled es true, habilitar SSL en la factory usando factory.getRabbitConnectionFactory().useSslProtocol().

Variables de entorno a añadir:

SPRING_RABBITMQ_SSL_ENABLED=true

FASE 2 — CORRECCIONES EN ARCHIVOS DE CONFIGURACIÓN YAML
Estos cambios se hacen en los application-docker.yml de cada microservicio Java. El objetivo es que absolutamente nada esté hardcodeado.

2.1 Microservicio Usuario
Archivo: microservicio-usuario/src/main/resources/application-docker.yml

Cambios necesarios:

yaml


# ACTUAL (incorrecto, el application.yml base lo pone en true):
data:
  redis:
    ssl:
      enabled: true   # hardcodeado
# CORRECTO en application-docker.yml:
data:
  redis:
    host: ${SPRING_DATA_REDIS_HOST}
    port: ${SPRING_DATA_REDIS_PORT:6380}
    password: ${SPRING_REDIS_PASSWORD}
    ssl:
      enabled: ${SPRING_DATA_REDIS_SSL_ENABLED:true}
En application.yml base cambiar:

yaml


ssl:
  enabled: ${REDIS_SSL:true}
2.2 Microservicio Auditoria
Archivo: microservicio-auditoria/src/main/resources/application-docker.yml

Mismo problema de Redis SSL que el usuario. Aplicar exactamente los mismos cambios del punto 2.1a.

En application.yml base de auditoría:

yaml


data:
  redis:
    ssl:
      enabled: ${REDIS_SSL:true}
Aumentar el connection-timeout del datasource para tolerar el cold start de Render:

yaml


hikari:
  connection-timeout: 60000   # era 30000
  initialization-fail-timeout: 120000
2.3 Microservicio Núcleo Financiero
Archivo: microservicio-nucleo-financiero/src/main/resources/application.yml (el base)

Problema crítico: Credenciales hardcodeadas. Reemplazar todo el bloque datasource:

yaml


# ACTUAL (crítico, eliminar):
datasource:
  url: jdbc:postgresql://127.0.0.1:5432/db_luka_financiero
  username: postgres
  password: adminUTP
# CORRECTO:
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/db_luka_financiero}
  username: ${SPRING_DATASOURCE_USERNAME:postgres}
  password: ${SPRING_DATASOURCE_PASSWORD:adminUTP}
2.4 Microservicio Pagos
Archivo: microservicio-pago/src/main/resources/application.yml (el base)

Problema: Usa nombres de variables distintos al perfil docker. Unificar todo a la convención SPRING_DATASOURCE_*:

yaml


# ACTUAL (incorrecto):
datasource:
  url: ${DATABASE_URL:...}
  username: ${DATABASE_USER:postgres}
  password: ${DATABASE_PASSWORD:adminUTP}
# CORRECTO (unificar con el perfil docker):
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/db_luka_pagos}
  username: ${SPRING_DATASOURCE_USERNAME:postgres}
  password: ${SPRING_DATASOURCE_PASSWORD:adminUTP}
2.5 Microservicio Suscripciones
Archivo: microservicio-suscripciones/src/main/resources/application-docker.yml

Problema: ddl-auto: validate con una BD nueva en NeonDB causará fallo en primer arranque porque Hibernate no encontrará las tablas.

Cambiar:

yaml


# ACTUAL:
jpa:
  hibernate:
    ddl-auto: validate
# CORRECTO para primer despliegue en Render:
jpa:
  hibernate:
    ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
También aumentar timeouts igual que auditoría:

yaml


hikari:
  connection-timeout: 60000
  initialization-fail-timeout: 120000
2.6 Todos los application-docker.yml — Añadir soporte AMQPS puerto 5671
En cada application-docker.yml de todos los microservicios Java, agregar o completar el bloque RabbitMQ con:

yaml


rabbitmq:
  host: ${SPRING_RABBITMQ_HOST}
  port: ${SPRING_RABBITMQ_PORT:5671}
  username: ${SPRING_RABBITMQ_USERNAME}
  password: ${SPRING_RABBITMQ_PASSWORD}
  virtual-host: ${SPRING_RABBITMQ_VIRTUAL_HOST}
  ssl:
    enabled: ${SPRING_RABBITMQ_SSL_ENABLED:true}
    algorithm: TLSv1.2
El cambio clave es: el default del puerto pasa de 5672 a 5671, y se añade el bloque ssl.

FASE 3 — CORRECCIONES EN DOCKERFILES
3.1 Reducir workers de Uvicorn en ms-ia
Archivo: microservicio-ia/Dockerfile

Cambiar la línea CMD:

dockerfile


# ACTUAL:
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8086", "--workers", "2", "--log-level", "info"]
# CORRECTO para plan gratuito:
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8086", "--workers", "1", "--log-level", "info"]
3.2 Corregir -Xss en ms-mensajeria
Archivo: microservicio-mensajeria/Dockerfile

Eliminar la flag -Xss512k y usar el CMD corregido:

dockerfile


CMD ["sh", "-c", "exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:MaxMetaspaceSize=128m \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker} \
    -Dserver.port=${PORT} \
    org.springframework.boot.loader.launch.JarLauncher"]
3.3 Verificar health check paths en todos los Dockerfiles
Cambiar en cada Dockerfile afectado:

dockerfile


# ACTUAL:
HEALTHCHECK CMD curl -f http://localhost:${PORT}/actuator/health || exit 1
# CORRECTO:
HEALTHCHECK CMD curl -f http://localhost:${PORT}/health-internal || exit 1
FASE 4 — CORRECCIONES EN microservicio-ia (Python)
4.1 Externalizar el dominio CORS hardcodeado
Archivo: microservicio-ia/main.py

Reemplazar:

python


if config.es_produccion:
    frontend_url = os.getenv("FRONTEND_URL", "")
    cors_extra = os.getenv("CORS_ALLOWED_ORIGINS", "")
    origins_list = [o.strip() for o in cors_extra.split(",") if o.strip()]
    if frontend_url:
        origins_list.append(frontend_url)
    allow_origins = origins_list if origins_list else ["*"]
4.2 Soporte SSL para Redis Upstash
Archivo: microservicio-ia/main.py — función _iniciar_escuchadores_adicionales()

python


redis_ssl = os.getenv("REDIS_SSL", "true").lower() == "true"
redis_client = redis.Redis(
    host=config.redis_host,
    port=config.redis_port,
    db=config.redis_db,
    password=config.redis_password or None,
    decode_responses=True,
    ssl=redis_ssl,
    ssl_cert_reqs=None,   # Upstash no requiere certificado del cliente
)
Variable de entorno a agregar en Railway:

REDIS_SSL=true

4.3 Soporte SSL para RabbitMQ CloudAMQP
Añadir en la configuración de pika (o aio-pika):

python


import ssl as ssl_lib
ssl_options = None
if os.getenv("RABBITMQ_SSL_ENABLED", "true").lower() == "true":
    ssl_context = ssl_lib.create_default_context()
    ssl_context.check_hostname = False
    ssl_context.verify_mode = ssl_lib.CERT_NONE
    ssl_options = pika.SSLOptions(ssl_context)
connection_params = pika.ConnectionParameters(
    host=config.rabbitmq_host,
    port=int(os.getenv("RABBITMQ_PUERTO", "5671")),
    virtual_host=config.rabbitmq_vhost,
    credentials=pika.PlainCredentials(config.rabbitmq_usuario, config.rabbitmq_password),
    ssl_options=ssl_options,
    heartbeat=60,
)
Variables de entorno a agregar:

RABBITMQ_SSL_ENABLED=true
RABBITMQ_PUERTO=5671

FASE 5 — ACTUALIZAR .env.local.example Y .env.local
5.1 Archivo de ejemplo
estructura-backend/.env.local.example

Se añaden o corrigen secciones para que el equipo conozca las variables necesarias (ver contenido en el archivo original, ya está actualizado).

5.2 docker-compose-hibrido.yml
Se cambian los valores null a referencias a variables del .env.local y se añaden variables SSL para ms-ia:

yaml


ms-ia:
  environment:
    REDIS_SSL: "true"
    RABBITMQ_SSL_ENABLED: "true"
    RABBITMQ_PUERTO: "5671"
    SPRING_RABBITMQ_SSL_ENABLED: "true"
FASE 6 — CREAR application-docker.yml PARA ms-ia (Python)
Se crea un archivo microservicio-ia/application-docker.yml con las variables de entorno listadas en la fase 4 (Redis, RabbitMQ, CORS, etc.) para que el contenedor pueda leerlas de forma consistente.

FASE 7 — CHECKLIST DE VERIFICACIÓN FINAL
Verificar localmente con .env.local actualizado.
Ejecutar make local-up dentro de estructura-backend/docker.
Confirmar que todos los microservicios pasan sus health checks (/health-internal).
Revisar checklist por microservicio (Redis SSL, RabbitMQ SSL, datasource con sslmode=require, sin credenciales hardcodeadas, health‑check correcto, CORS con variable, ddl-auto correcto).
Orden de ejecución recomendado: FASE 1 → FASE 2 → FASE 3 → FASE 4 → FASE 5 → FASE 6 → FASE 7.