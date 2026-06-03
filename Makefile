# ════════════════════════════════════════════════════════════════════════════
# MASTER MAKEFILE - LUKA ECOSYSTEM (agents/configurar-entorno-espanol)
# ════════════════════════════════════════════════════════════════════════════

.PHONY: help setup-docker setup-terminator backend backend-prod status logs down clean frontend

# Colores para output
CYAN  := \033[0;36m
GREEN := \033[0;32m
YELLOW:= \033[1;33m
RED   := \033[0;31m
NC    := \033[0m

help:
	@echo ""
	@echo "$(CYAN)╔══════════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║         🚀 LUKA ECOSYSTEM — MASTER AUTOMACIÓN DE DESARROLLO      ║$(NC)"
	@echo "$(CYAN)╚══════════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(YELLOW)⚙️ CONFIGURACIÓN INICIAL (Ejecución Única)$(NC)"
	@echo "  make setup-docker        → Cambia data-root de Docker a Memoria Linux (Recomendado - requiere sudo)"
	@echo "  make setup-terminator    → Configura la cuadrícula de logs 3x3 en Terminator"
	@echo ""
	@echo "$(YELLOW)🚀 CONTROL DEL BACKEND (Microservicios)$(NC)"
	@echo "  make backend             → Levanta microservicios en modo Híbrido (1 GB RAM c/u) y abre logs"
	@echo "  make backend-prod        → Levanta microservicios en modo Producción (1.5/2 GB RAM c/u) y abre logs"
	@echo ""
	@echo "$(YELLOW)🎨 CONTROL DEL FRONTEND (Angular)$(NC)"
	@echo "  make frontend            → Inicializa el servidor Angular en http://localhost:4200"
	@echo ""
	@echo "$(YELLOW)🛠️ UTILIDADES$(NC)"
	@echo "  make status              → Muestra el estado actual de los contenedores"
	@echo "  make logs                -> Ver logs generales consolidados de todos los servicios"
	@echo "  make down                → Detiene los contenedores del backend"
	@echo "  make clean               → Detiene contenedores y elimina volúmenes huérfanos"
	@echo ""

setup-docker:
	@echo "$(CYAN)▶ Configurando espacio de Docker...$(NC)"
	sudo ./configurar-espacio-docker.sh

setup-terminator:
	@echo "$(CYAN)▶ Configurando Terminator...$(NC)"
	./configurar-terminator.sh

backend:
	@echo "$(CYAN)▶ Iniciando backend en modo híbrido (1GB RAM)...$(NC)"
	./dev-backend.sh --hibrido

backend-prod:
	@echo "$(CYAN)▶ Iniciando backend en modo producción (1.5GB/2GB RAM)...$(NC)"
	./dev-backend.sh --produccion

frontend:
	@echo "$(CYAN)▶ Iniciando frontend Angular...$(NC)"
	./dev-frontend.sh

status:
	@cd estructura-backend/docker && docker-compose ps

logs:
	@cd estructura-backend/docker && docker-compose logs -f

down:
	@echo "$(YELLOW)■ Deteniendo contenedores backend...$(NC)"
	@cd estructura-backend/docker && docker-compose down
	@echo "$(GREEN)✔ Contenedores detenidos.$(NC)"

clean:
	@echo "$(RED)■ Limpiando contenedores y volúmenes...$(NC)"
	@cd estructura-backend/docker && docker-compose down -v
	@echo "$(GREEN)✔ Limpieza completada.$(NC)"
