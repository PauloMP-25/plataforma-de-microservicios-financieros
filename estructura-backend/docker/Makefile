# ═══════════════════════════════════════════════════════════════════════════════
#  LUKA APP — Makefile (v3.1 - Hybrid & Modular Support)
# ═══════════════════════════════════════════════════════════════════════════════

.PHONY: help local-up local-down local-infra local-ms local-ia prod-up prod-down ps logs clean

# ── Variables ─────────────────────────────────────────────────────────────────
COMPOSE_LOCAL = docker compose -f docker-compose-hibrido.yml
COMPOSE_PROD  = docker compose -f docker-compose.yml

# Colores
CYAN  = \033[0;36m
GREEN = \033[0;32m
YELLOW= \033[1;33m
RED   = \033[0;31m
NC    = \033[0m

# ── AYUDA ─────────────────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "$(CYAN)╔══════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║         LUKA APP — Makefile Híbrido          ║$(NC)"
	@echo "$(CYAN)╚══════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)MODO LOCAL (Híbrido - Laptop Friendly)$(NC)"
	@echo "  make local-up      → Levanta el stack optimizado completo"
	@echo "  make local-infra   → Etapa 1: Eureka y Gateway"
	@echo "  make local-ms      → Etapa 2: Microservicios Core (Usuario, Cliente, Mensajería, Auditoría)"
	@echo "  make local-ia      → Etapa 3: Módulos Analíticos (IA, Núcleo Financiero)"
	@echo "  make local-down    → Detiene el stack local"
	@echo ""
	@echo "$(GREEN)MODO PRODUCCIÓN (Full Docker)$(NC)"
	@echo "  make prod-up       → Levanta TODO (Postgres locales + RabbitMQ + Services)"
	@echo "  make prod-down     → Detiene el stack de producción"
	@echo ""
	@echo "$(GREEN)OBSERVABILIDAD$(NC)"
	@echo "  make ps            → Estado de contenedores"
	@echo "  make logs          → Ver logs en tiempo real"
	@echo ""

# ── MODO LOCAL HÍBRIDO ────────────────────────────────────────────────────────

local-up:
	@echo "$(CYAN)▶ Iniciando LUKA en modo HÍBRIDO (Optimizado)...$(NC)"
	$(COMPOSE_LOCAL) up -d
	@echo "$(GREEN)✔ Stack local listo.$(NC)"

local-infra:
	@echo "$(CYAN)▶ Iniciando Etapa 1: Infraestructura (Eureka + Gateway)...$(NC)"
	$(COMPOSE_LOCAL) up -d eureka-server api-gateway
	@echo "$(GREEN)✔ Infraestructura lista.$(NC)"

local-ms:
	@echo "$(CYAN)▶ Iniciando Etapa 2: Microservicios Core...$(NC)"
	$(COMPOSE_LOCAL) up -d ms-usuario ms-cliente ms-mensajeria ms-auditoria
	@echo "$(GREEN)✔ Microservicios Core iniciados.$(NC)"

local-ia:
	@echo "$(CYAN)▶ Iniciando Etapa 3: Módulos Analíticos (IA + Financiero)...$(NC)"
	$(COMPOSE_LOCAL) up -d ms-ia ms-nucleo-financiero
	@echo "$(GREEN)✔ Módulos Analíticos iniciados.$(NC)"

local-down:
	@echo "$(YELLOW)■ Deteniendo stack local...$(NC)"
	$(COMPOSE_LOCAL) down

# ── MODO PRODUCCIÓN ──────────────────────────────────────────────────────────

prod-up:
	@echo "$(RED)▶ Iniciando LUKA en modo PRODUCCIÓN (Full Stack)...$(NC)"
	$(COMPOSE_PROD) up -d
	@echo "$(GREEN)✔ Stack completo listo.$(NC)"

prod-down:
	@echo "$(RED)■ Deteniendo stack de producción...$(NC)"
	$(COMPOSE_PROD) down

# ── UTILIDADES ────────────────────────────────────────────────────────────────

ps:
	@echo "$(CYAN)Estado de los servicios:$(NC)"
	docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

logs:
	$(COMPOSE_LOCAL) logs -f --tail=100

clean:
	@echo "$(YELLOW)■ Limpiando contenedores y redes huérfanas...$(NC)"
	docker system prune -f