# ═══════════════════════════════════════════════════════════════════
#  LUKA APP — Dockerfile Multi-Stage para Microservicios Spring Boot
#  Java 21 · Spring Boot 3.4.4 · Maven
#  Estrategia: Build en JDK completo → Runtime en JRE Alpine (ligero)
# ═══════════════════════════════════════════════════════════════════

# ── ETAPA 1: BUILD (Instalamos libreria-comun y luego el MS) ─────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Primero instalamos la librería común localmente en el contenedor
COPY estructura-backend/libreria-comun ./libreria-comun
RUN cd libreria-comun && mvn install -DskipTests -B

# 2. Ahora preparamos el microservicio-pago
WORKDIR /build/microservicio-pago
COPY estructura-backend/microservicio-pago/pom.xml .

# Descargamos dependencias (esta capa se cachea)
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos
COPY estructura-backend/microservicio-pago/src ./src
RUN mvn package -DskipTests -B

# Extraemos las capas
RUN java -Djarmode=layertools -jar target/*.jar extract --destination /build/extracted


# ── ETAPA 2: RUNTIME ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="LUKA APP DevOps Team"
LABEL description="LUKA APP - Microservicio de Pagos (Stripe)"

# Dependencias mínimas de seguridad
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata && \
    cp /usr/share/zoneinfo/America/Lima /etc/localtime && \
    echo "America/Lima" > /etc/timezone && \
    apk del tzdata

# Usuario no-root por seguridad
RUN addgroup -S lukaapp && adduser -S lukaapp -G lukaapp

WORKDIR /app

# Copiamos las capas
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/dependencies/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=lukaapp:lukaapp /build/extracted/application/ ./

USER lukaapp

# Puerto expuesto
ARG APP_PORT=8087
EXPOSE ${APP_PORT}

# Health check interno
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${APP_PORT}/actuator/health || exit 1

# dumb-init maneja señales del SO correctamente
ENTRYPOINT ["dumb-init", "--"]

# JVM optimizada
CMD ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
    "org.springframework.boot.loader.launch.JarLauncher"]
