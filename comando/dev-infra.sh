#!/bin/bash
# ============================================================================
# SCRIPT: dev-infra.sh
# Descripción: Levanta la infraestructura local de desarrollo (PostgreSQL, Redis, RabbitMQ)
# ============================================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
DOCKER_DIR="$PROJECT_ROOT/estructura-backend/docker"
COMPOSE_FILE="docker-compose-infra.yml"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 INICIANDO INFRAESTRUCTURA DE DESARROLLO${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta Docker no encontrada en $DOCKER_DIR${NC}"
    exit 1
fi

cd "$DOCKER_DIR"

echo -e "${YELLOW}🐳 Levantando contenedores de infraestructura...${NC}"
docker-compose -f "$COMPOSE_FILE" up -d

echo ""
echo -e "${GREEN}✅ INFRAESTRUCTURA INICIADA CORRECTAMENTE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado de la infraestructura:${NC}"
docker-compose -f "$COMPOSE_FILE" ps
echo ""
