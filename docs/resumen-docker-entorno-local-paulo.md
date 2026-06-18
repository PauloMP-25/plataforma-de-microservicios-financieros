# Resumen de cambios realizados en Docker, entorno local y repositorio

## Contexto

El proyecto contaba con un worktree Git activo (`agents/configurar-entorno-espanol`) que apuntaba a una ruta que en ocasiones no estaba disponible, causando que los comandos `devfrontend` y `devbackend` fallaran al abrir terminales nuevos. Adicionalmente, los scripts de inicio de los microservicios levantaban contenedores de PostgreSQL, RabbitMQ y Redis de forma innecesaria, ya que todos esos servicios se encuentran instalados localmente en el host.

Este documento describe los cambios realizados para consolidar el entorno, corregir los comandos de desarrollo y optimizar el consumo de RAM de los contenedores Docker.

---

## 1. Limpieza del repositorio (Git)

### Problema
- Existía un worktree Git en `.worktrees/agents-configurar-entorno-espanol` con la rama `agents/configurar-entorno-espanol` que divergía de `develop`.
- Las ramas locales `feature/frontend-ia`, `feature/integracion-backend-frontend`, `feature/integracion-dashboard` y `main` no habían sido mergeadas formalmente.
- Los aliases `devfrontend` y `devbackend` apuntaban a la ruta del worktree y fallaban en terminales no-interactivos.

### Acciones realizadas
- Se copió el archivo `docker-compose-hibrido.yml` del worktree a `develop` antes de cualquier merge para preservar la configuración.
- Se mergearon todas las ramas locales a `develop` sin conflictos (ya estaban al día).
- Se mergeó `agents/configurar-entorno-espanol` a `develop`, resolviendo el conflicto en `docker-compose-hibrido.yml` conservando la versión de `develop`.
- Se eliminó el worktree y la carpeta `.worktrees/`.
- Se eliminaron las ramas locales ya integradas: `agents/configurar-entorno-espanol`, `feature/frontend-ia`, `feature/integracion-backend-frontend`, `feature/integracion-dashboard` y `main` (local).
- **Rama de trabajo activa:** `testing/correccion-errores`, creada desde `develop` para la etapa de testing y corrección de errores.

---

## 3. Optimización de Docker — archivo `docker-compose-hibrido.yml`

Archivo modificado: `estructura-backend/docker/docker-compose-hibrido.yml`

### 3.1 Eliminación de contenedores de infraestructura local

**PostgreSQL, RabbitMQ y Redis corren instalados directamente en el host** (no en Docker). El archivo anterior los levantaba como contenedores, lo cual era redundante y consumía RAM adicional.

Se eliminaron los servicios `rabbitmq` y `redis` del `docker-compose-hibrido.yml`.

Las variables de entorno de todos los microservicios se actualizaron para apuntar a `host.docker.internal` en lugar de los nombres de contenedor:

```yaml
# Antes
SPRING_RABBITMQ_HOST: rabbitmq
REDIS_HOST: redis

# Ahora
SPRING_RABBITMQ_HOST: host.docker.internal
REDIS_HOST: host.docker.internal
```

Se eliminaron también todos los bloques `depends_on` que referenciaban `rabbitmq` y `redis`, ya que esos servicios ahora son externos al stack Docker.

### 3.2 Reducción de memoria de los contenedores

**Motivo:** Los límites anteriores (450M–600M por servicio) eran demasiado altos para un entorno de desarrollo local. La JVM con los ajustes correctos de heap opera bien dentro de 200–250 MB reales.

| Servicio | Límite anterior | Límite nuevo |
|---|---|---|
| ms-usuario | 450M | **256M** |
| ms-cliente | 450M | **256M** |
| ms-mensajeria | 450M | **256M** |
| ms-auditoria | 450M | **256M** |
| ms-nucleo-financiero | 450M | **256M** |
| ms-pagos | 450M | **256M** |
| ms-ia (Python) | 600M | **350M** |
| api-gateway | 450M | **256M** |

**RAM total máxima de los 8 contenedores: ~2.14 GB** (antes ~3.75 GB).

### 3.3 Reducción del heap de la JVM

```yaml
# Antes
JAVA_OPTS: "-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Ahora
JAVA_OPTS: "-Xms64m -Xmx200m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
```

- `-Xms64m`: la JVM arranca con solo 64 MB de heap (antes 128 MB).
- `-Xmx200m`: el heap no crece más de 200 MB (antes 384 MB).
- `-XX:+TieredStopAtLevel=1`: desactiva la compilación JIT completa en dev, reduciendo el tiempo de arranque y el uso de CPU.

---

### Cambios aplicados
- Todos los scripts ahora usan `-f docker-compose-hibrido.yml` explícitamente.
- Se corrigió la lista de `SERVICES` en cada grupo para incluir **solo microservicios** (sin BDs, RabbitMQ ni Redis).
- Se actualizaron los comentarios de cabecera indicando que la infraestructura corre localmente.

---

## 5. Consideraciones para el agente de Cristina

> Si Cristina o su agente vuelven a modificar `docker-compose-hibrido.yml`, tener en cuenta que **RabbitMQ y Redis ya no deben incluirse como servicios** en ese archivo. Los microservicios se conectan a ellos via `host.docker.internal` en los puertos estándar (5672 y 6379 respectivamente).
>
> El archivo `docker-compose.yml` (sin `-hibrido`) sigue siendo el archivo de producción/staging con todos los contenedores completos. No modificarlo para el entorno local.

---