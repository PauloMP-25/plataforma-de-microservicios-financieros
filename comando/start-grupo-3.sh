#!/bin/bash
# ============================================================================
# GRUPO 3: Financiero + IA + Pagos
# Requiere: Grupo 1 + Grupo 2 corriendo
# BD, RabbitMQ y Redis corren LOCALMENTE (no en Docker)
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
DOCKER_DIR="$PROJECT_ROOT/estructura-backend/docker"
COMPOSE_FILE="docker-compose-hibrido.yml"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 GRUPO 3: Financiero + IA + Pagos${NC}"
echo -e "${YELLOW}  📌 BD/RabbitMQ/Redis: locales (host)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta Docker no encontrada en $DOCKER_DIR${NC}"
    exit 1
fi

cd "$DOCKER_DIR"

# Validar Grupo 1
GATEWAY_RUNNING=$(docker-compose -f "$COMPOSE_FILE" ps | grep "api-gateway" | grep "Up" || echo "")
if [ -z "$GATEWAY_RUNNING" ]; then
    echo -e "${RED}❌ GRUPO 1 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: devbackend-g1${NC}"
    exit 1
fi

# Validar Grupo 2
USUARIO_RUNNING=$(docker-compose -f "$COMPOSE_FILE" ps | grep "ms-usuario" | grep "Up" || echo "")
if [ -z "$USUARIO_RUNNING" ]; then
    echo -e "${RED}❌ GRUPO 2 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: devbackend-g2${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Grupo 1 y Grupo 2 detectados${NC}"
echo ""

SERVICES="ms-nucleo-financiero ms-ia ms-pagos"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • ms-nucleo-financiero → puerto 8085"
echo "   • ms-ia               → puerto 8086"
echo "   • ms-pagos            → puerto 8087"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando con: $COMPOSE_FILE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose -f "$COMPOSE_FILE" up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 3 levantado — ✨ LUKA ECOSYSTEM OPERACIONAL ✨${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Stack completo:${NC}"
docker-compose -f "$COMPOSE_FILE" ps
echo ""
