#!/bin/bash
# ============================================================================
# GRUPO 1: API Gateway + Microservicio Auditoría
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
echo -e "${BLUE}  🚀 GRUPO 1: API Gateway + Auditoría${NC}"
echo -e "${YELLOW}  📌 BD/RabbitMQ/Redis: locales (host)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta Docker no encontrada en $DOCKER_DIR${NC}"
    exit 1
fi

cd "$DOCKER_DIR"
echo -e "${GREEN}✅ Ubicación: $(pwd)${NC}"
echo ""

SERVICES="api-gateway ms-auditoria"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • api-gateway     → puerto 8080"
echo "   • ms-auditoria    → puerto 8082"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando con: $COMPOSE_FILE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose --env-file ../.env -f "$COMPOSE_FILE" up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 1 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado:${NC}"
docker-compose -f "$COMPOSE_FILE" ps api-gateway ms-auditoria
echo ""
echo -e "${YELLOW}🌐 API Gateway:${NC} http://localhost:8080"
echo ""
