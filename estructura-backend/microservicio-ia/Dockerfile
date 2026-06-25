# ═══════════════════════════════════════════════════════════════════
#  LUKA APP — Dockerfile Multi-Stage para Microservicio IA (Python)
#  Python 3.12 · FastAPI · Uvicorn
#  Estrategia: Build con deps completos → Runtime con imagen slim
# ═══════════════════════════════════════════════════════════════════

# ── ETAPA 1: BUILD DE DEPENDENCIAS ───────────────────────────────────────────
FROM python:3.12-slim AS builder

LABEL stage="builder"

WORKDIR /build

# Dependencias del sistema necesarias para compilar paquetes científicos
# (numpy, scipy, scikit-learn requieren compiladores C)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    gcc \
    g++ \
    libffi-dev \
    && rm -rf /var/lib/apt/lists/*

# Copiamos solo requirements.txt para aprovechar la caché de Docker.
# Si el archivo no cambia, pip no reinstala nada en rebuilds.
COPY estructura-backend/microservicio-ia/requirements.txt .

# Instalamos en un directorio aislado para copiarlo limpiamente al runtime
RUN pip install --upgrade pip --no-cache-dir && \
    pip install --no-cache-dir \
        --prefix=/install \
        -r requirements.txt


# ── ETAPA 2: RUNTIME ─────────────────────────────────────────────────────────
FROM python:3.12-slim AS runtime

LABEL maintainer="LUKA APP DevOps Team"
LABEL description="LUKA APP - Microservicio IA Financiera (FastAPI + Gemini)"

# Dependencias mínimas de runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Zona horaria de Perú
ENV TZ=America/Lima
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Usuario no-root
RUN groupadd -r lukaapp && useradd -r -g lukaapp lukaapp

WORKDIR /app

# Copiamos las dependencias pre-compiladas desde la etapa builder
COPY --from=builder /install /usr/local

# Copiamos el código fuente de la aplicación
COPY --chown=lukaapp:lukaapp estructura-backend/microservicio-ia .

# Variables de entorno de Python para producción:
#   PYTHONUNBUFFERED=1  → logs en tiempo real sin buffering
#   PYTHONDONTWRITEBYTECODE=1 → no genera archivos .pyc innecesarios
#   PYTHONPATH=/app     → importaciones desde la raíz del proyecto
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app

USER lukaapp

# Puerto por defecto. Railway/Render sobreescriben esta variable con la suya propia.
ENV PORT=8086
EXPOSE ${PORT}

# Health check: verifica el endpoint de salud de FastAPI
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT}/health || exit 1

# dumb-init para manejo correcto de señales en PID 1
ENTRYPOINT ["dumb-init", "--"]

# Uvicorn en modo producción:
#   --host 0.0.0.0     → acepta conexiones externas al contenedor
#   --port $PORT       → Railway/Render inyectan el puerto dinámicamente
#   --workers 1        → 1 proceso worker (free tier: 512MB; 2 workers provoca OOM)
#   --log-level info   → nivel de log adecuado para producción
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT} --workers 1 --log-level info"]