# ═══════════════════════════════════════════════════════════════════════════════
#  LUKA APP — Makefile
#  Comandos de gestión del stack Docker.
#  Uso: make <comando>
# ═══════════════════════════════════════════════════════════════════════════════

.PHONY: help setup up down restart logs ps build rebuild clean nuke health \
        infra services tail-gateway tail-ia tail-financiero

# ── Variables ─────────────────────────────────────────────────────────────────
COMPOSE         = docker compose
COMPOSE_PROD    = docker compose -f docker-compose.yml -f docker-compose.prod.yml
PROJECT         = luka-app

# Colores para la terminal
CYAN  = \033[0;36m
GREEN = \033[0;32m
YELLOW= \033[1;33m
RED   = \033[0;31m
NC    = \033[0m

# ── AYUDA ─────────────────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "$(CYAN)╔══════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║         LUKA APP — Comandos Docker           ║$(NC)"
	@echo "$(CYAN)╚══════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)SETUP$(NC)"
	@echo "  make setup          → Copia .env.example → .env (primera vez)"
	@echo ""
	@echo "$(GREEN)CICLO DE VIDA$(NC)"
	@echo "  make up             → Levanta todo el stack (modo detached)"
	@echo "  make infra          → Levanta solo la infraestructura (BD + RabbitMQ + Eureka)"
	@echo "  make services       → Levanta solo los microservicios (asume infra ya corriendo)"
	@echo "  make down           → Detiene y elimina contenedores (conserva volúmenes)"
	@echo "  make restart        → down + up"
	@echo ""
	@echo "$(GREEN)BUILD$(NC)"
	@echo "  make build          → Construye todas las imágenes"
	@echo "  make rebuild        → Reconstruye sin caché (limpio total)"
	@echo ""
	@echo "$(GREEN)OBSERVABILIDAD$(NC)"
	@echo "  make ps             → Estado de todos los contenedores"
	@echo "  make health         → Healthcheck rápido de todos los servicios"
	@echo "  make logs           → Logs en tiempo real de todos los servicios"
	@echo "  make tail-gateway   → Logs del API Gateway"
	@echo "  make tail-ia        → Logs del Microservicio IA"
	@echo "  make tail-financiero→ Logs del Núcleo Financiero"
	@echo ""
	@echo "$(RED)LIMPIEZA$(NC)"
	@echo "  make clean          → Elimina contenedores + imágenes del proyecto"
	@echo "  make nuke           → Elimina TODO (contenedores + volúmenes + imágenes) ⚠️"
	@echo ""

# ── SETUP INICIAL ─────────────────────────────────────────────────────────────
setup:
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN)✔ .env creado desde .env.example$(NC)"; \
		echo "$(YELLOW)⚠ IMPORTANTE: Edita .env y agrega tu GEMINI_API_KEY y JWT_SECRET_KEY$(NC)"; \
	else \
		echo "$(YELLOW)→ .env ya existe, no se sobreescribió$(NC)"; \
	fi

# ── VERIFICAR .ENV ────────────────────────────────────────────────────────────
check-env:
	@if [ ! -f .env ]; then \
		echo "$(RED)✘ ERROR: No existe .env. Ejecuta: make setup$(NC)"; \
		exit 1; \
	fi
	@if grep -q "TU_GEMINI_API_KEY_AQUI" .env; then \
		echo "$(RED)✘ ERROR: Reemplaza GEMINI_API_KEY en .env con tu clave real$(NC)"; \
		exit 1; \
	fi
	@if grep -q "TU_JWT_SECRET_HEX_AQUI" .env; then \
		echo "$(RED)✘ ERROR: Reemplaza JWT_SECRET_KEY en .env$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✔ .env verificado correctamente$(NC)"

# ── CICLO DE VIDA ─────────────────────────────────────────────────────────────
up: check-env
	@echo "$(CYAN)▶ Levantando stack LUKA APP...$(NC)"
	$(COMPOSE) up -d
	@echo ""
	@echo "$(GREEN)✔ Stack iniciado$(NC)"
	@echo "  API Gateway     → http://localhost:8080"
	@echo "  Eureka Dashboard→ http://localhost:8761"
	@echo "  RabbitMQ UI     → http://localhost:15672"
	@echo ""

infra: check-env
	@echo "$(CYAN)▶ Levantando infraestructura base...$(NC)"
	$(COMPOSE) up -d \
		postgres-usuario postgres-cliente postgres-financiero \
		postgres-auditoria postgres-mensajeria \
		rabbitmq eureka-server
	@echo "$(GREEN)✔ Infraestructura lista$(NC)"

services: check-env
	@echo "$(CYAN)▶ Levantando microservicios...$(NC)"
	$(COMPOSE) up -d \
		ms-usuario ms-cliente ms-auditoria \
		ms-mensajeria ms-nucleo-financiero ms-ia api-gateway
	@echo "$(GREEN)✔ Microservicios iniciados$(NC)"

down:
	@echo "$(YELLOW)■ Deteniendo stack LUKA APP...$(NC)"
	$(COMPOSE) down
	@echo "$(GREEN)✔ Stack detenido$(NC)"

restart: down up

# ── BUILD ─────────────────────────────────────────────────────────────────────
build: check-env
	@echo "$(CYAN)▶ Construyendo imágenes...$(NC)"
	$(COMPOSE) build
	@echo "$(GREEN)✔ Build completado$(NC)"

rebuild: check-env
	@echo "$(CYAN)▶ Reconstruyendo imágenes sin caché...$(NC)"
	$(COMPOSE) build --no-cache
	@echo "$(GREEN)✔ Rebuild completado$(NC)"

# ── OBSERVABILIDAD ────────────────────────────────────────────────────────────
ps:
	@echo "$(CYAN)Estado del stack LUKA APP:$(NC)"
	$(COMPOSE) ps

health:
	@echo "$(CYAN)Verificando healthchecks...$(NC)"
	@for svc in api-gateway eureka-server ms-usuario ms-cliente ms-auditoria ms-mensajeria ms-nucleo-financiero ms-ia; do \
		status=$$(docker inspect --format='{{.State.Health.Status}}' luka-$$svc 2>/dev/null || echo "not found"); \
		if [ "$$status" = "healthy" ]; then \
			echo "$(GREEN)  ✔ $$svc → $$status$(NC)"; \
		elif [ "$$status" = "not found" ]; then \
			echo "$(YELLOW)  ? $$svc → no encontrado$(NC)"; \
		else \
			echo "$(RED)  ✘ $$svc → $$status$(NC)"; \
		fi; \
	done

logs:
	$(COMPOSE) logs -f --tail=50

tail-gateway:
	$(COMPOSE) logs -f --tail=100 api-gateway

tail-ia:
	$(COMPOSE) logs -f --tail=100 ms-ia

tail-financiero:
	$(COMPOSE) logs -f --tail=100 ms-nucleo-financiero

# ── LIMPIEZA ──────────────────────────────────────────────────────────────────
clean: down
	@echo "$(YELLOW)■ Eliminando imágenes del proyecto...$(NC)"
	docker rmi $$(docker images "luka/*" -q) 2>/dev/null || true
	@echo "$(GREEN)✔ Imágenes eliminadas$(NC)"

nuke:
	@echo "$(RED)⚠ ATENCIÓN: Esto elimina contenedores, volúmenes e imágenes$(NC)"
	@read -p "¿Continuar? (escribe 'si' para confirmar): " confirm; \
	if [ "$$confirm" = "si" ]; then \
		$(COMPOSE) down -v --rmi all --remove-orphans; \
		echo "$(GREEN)✔ Limpieza total completada$(NC)"; \
	else \
		echo "$(YELLOW)→ Operación cancelada$(NC)"; \
	fi