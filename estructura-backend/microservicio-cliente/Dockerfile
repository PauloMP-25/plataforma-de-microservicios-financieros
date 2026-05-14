# ═══════════════════════════════════════════════════════════════════
#  LUKA APP — Dockerfile Multi-Stage para Microservicios Spring Boot
#  Java 21 · Spring Boot 3.4.4 · Maven
#  Estrategia: Build en JDK completo → Runtime en JRE Alpine (ligero)
# ═══════════════════════════════════════════════════════════════════

# ── ETAPA 1: BUILD (Usamos Maven directamente) ───────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Primero instalamos la librería común localmente en el contenedor
COPY estructura-backend/libreria-comun ./libreria-comun
RUN cd libreria-comun && mvn install -DskipTests -B

# 2. Ahora preparamos el microservicio-cliente
WORKDIR /build/microservicio-cliente
COPY estructura-backend/microservicio-cliente/pom.xml .

# Descargamos dependencias (esta capa se cachea)
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos
COPY estructura-backend/microservicio-cliente/src ./src

# Compilamos usando mvn directamente
RUN mvn package -DskipTests -B

# Extraemos las capas
RUN java -Djarmode=layertools -jar target/*.jar extract --destination /build/extracted


# ── ETAPA 2: RUNTIME ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="LUKA APP DevOps Team"
LABEL description="LUKA APP - Microservicio Spring Boot"

# Dependencias mínimas de seguridad
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata && \
    cp /usr/share/zoneinfo/America/Lima /etc/localtime && \
    echo "America/Lima" > /etc/timezone && \
    apk del tzdata

# Usuario no-root por seguridad (principio de mínimo privilegio)
RUN addgroup -S lukaapp && adduser -S lukaapp -G lukaapp

WORKDIR /app

# Copiamos las capas en orden de menor a mayor frecuencia de cambio.
# Esto maximiza el hit de caché en redeployments frecuentes.
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/dependencies/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/application/ ./

USER lukaapp

# Puerto expuesto (cada servicio puede sobreescribirlo con --build-arg)
ARG APP_PORT=8081
ENV APP_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${APP_PORT}/actuator/health || exit 1

# dumb-init maneja señales del SO correctamente (SIGTERM para graceful shutdown)
ENTRYPOINT ["dumb-init", "--"]

# JVM optimizada para contenedores:
#   -XX:+UseContainerSupport       → detecta límites de CPU/RAM del contenedor
#   -XX:MaxRAMPercentage=75.0      → usa máx. 75% de la RAM asignada al contenedor
#   -XX:+UseG1GC                   → recolector G1 equilibrado para microservicios
#   -Djava.security.egd            → inicio más rápido evitando bloqueos de /dev/random
CMD ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
    "org.springframework.boot.loader.launch.JarLauncher"]