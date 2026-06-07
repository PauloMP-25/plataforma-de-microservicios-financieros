#!/bin/bash
# ============================================================================
# SCRIPT: finish-infra.sh
# Descripción: Detiene la infraestructura local (Postgres, Redis, RabbitMQ)
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
echo -e "${RED}🛑 DETENIENDO CONTENEDORES DE INFRAESTRUCTURA LOCAL${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -d "$DOCKER_DIR" ]; then
    cd "$DOCKER_DIR"
    echo -e "${YELLOW}■ Deteniendo PostgreSQL, Redis y RabbitMQ...${NC}"
    docker-compose -f "$COMPOSE_FILE" stop
fi

echo ""
echo -e "${GREEN}✅ INFRAESTRUCTURA LOCAL DETENIDA${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
