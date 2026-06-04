# ════════════════════════════════════════════════════════════════════════════
# MAKEFILE - Luka Ecosystem Dev Commands
# ════════════════════════════════════════════════════════════════════════════

.PHONY: help up-group-1 up-group-2 up-group-3 down logs clean status frontend

# Variables
DOCKER_DIR := ../estructura-backend/docker
FRONTEND_DIR := .

# ════════════════════════════════════════════════════════════════════════════
# 🎯 AYUDA
# ════════════════════════════════════════════════════════════════════════════
help:
@echo "╔══════════════════════════════════════════════════════════════════╗"
@echo "║           🚀 LUKA ECOSYSTEM - COMANDOS DISPONIBLES 🚀           ║"
@echo "╚══════════════════════════════════════════════════════════════════╝"
@echo ""
@echo "📋 BACKEND - Microservicios (3 grupos)"
@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
@echo "  make up-group-1        Levanta: API Gateway + Auditoría"
@echo "  make up-group-2        Levanta: Usuario + Cliente + Mensajería"
@echo "  make up-group-3        Levanta: Financiero + IA + Suscripciones + Pagos"
@echo ""
@echo "🛠️  GESTIÓN GENERAL"
@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
@echo "  make logs              Ver logs en tiempo real (todos los servicios)"
@echo "  make status            Ver estado de todos los contenedores"
@echo "  make down              Detener todos los contenedores"
@echo "  make clean             Limpiar volúmenes y datos (⚠️  destructivo)"
@echo ""
@echo "🎨 FRONTEND"
@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
@echo "  make frontend          Inicia Angular (ng serve --open)"
@echo ""
@echo "📊 INFO"
@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
@echo "  make help              Muestra esta ayuda"
@echo ""
@echo "💾 Partición: /media/paulo/datos/ (932 GB)"
@echo ""

# ════════════════════════════════════════════════════════════════════════════
# 🚀 MICROSERVICIOS
# ════════════════════════════════════════════════════════════════════════════

up-group-1:
@./start-grupo-1.sh

up-group-2:
@./start-grupo-2.sh

up-group-3:
@./start-grupo-3.sh

# ════════════════════════════════════════════════════════════════════════════
# 🛠️  GESTIÓN GENERAL
# ════════════════════════════════════════════════════════════════════════════

logs:
@cd $(DOCKER_DIR) && docker-compose logs -f

status:
@cd $(DOCKER_DIR) && docker-compose ps

down:
@echo "🛑 Deteniendo todos los contenedores..."
@cd $(DOCKER_DIR) && docker-compose down
@echo "✅ Contenedores detenidos"

clean:
	@echo "🗑️  ADVERTENCIA: Esto eliminará todos los volúmenes y datos"
	@read -p "¿Continuar? (s/n) " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Ss]$$ ]]; then \
	cd $(DOCKER_DIR) && docker-compose down -v; \
	echo "✅ Limpieza completada"; \
	else \
	echo "❌ Operación cancelada"; \
	fi

# ════════════════════════════════════════════════════════════════════════════
# 🎨 FRONTEND
# ════════════════════════════════════════════════════════════════════════════

frontend:
	@bash $(FRONTEND_DIR)/comando/dev-frontend.sh

# ════════════════════════════════════════════════════════════════════════════
# 📊 COMANDOS ADICIONALES (NO MOSTRADOS EN HELP)
# ════════════════════════════════════════════════════════════════════════════

build:
@echo "🔨 Compilando imágenes Docker..."
@cd $(DOCKER_DIR) && docker-compose build

pull:
@echo "📥 Descargando últimas imágenes..."
@cd $(DOCKER_DIR) && docker-compose pull

restart:
@echo "🔄 Reiniciando todos los servicios..."
@cd $(DOCKER_DIR) && docker-compose restart

prune:
@echo "🗑️  Limpiando recursos Docker no utilizados..."
@docker system prune -f
@echo "✅ Prune completado"

