#!/bin/bash
# ============================================================================
# SCRIPT: finish-luka.sh
# Descripción: Detiene todos los microservicios de Luka y el servidor Angular
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
echo -e "${RED}🛑 DETENIENDO LA APLICACIÓN LUKA (BACKEND + FRONTEND)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 1. Detener contenedores backend
if [ -d "$DOCKER_DIR" ]; then
    cd "$DOCKER_DIR"
    echo -e "${YELLOW}■ Deteniendo contenedores del Backend...${NC}"
    docker-compose -f "$COMPOSE_FILE" stop
fi

# 2. Detener el frontend Angular en puerto 4200
echo -e "${YELLOW}■ Buscando y cerrando el servidor Angular en puerto 4200...${NC}"
FRONTEND_PID=$(lsof -t -i:4200 2>/dev/null || true)
if [ -n "$FRONTEND_PID" ]; then
    kill -9 $FRONTEND_PID 2>/dev/null || true
    echo -e "${GREEN}✔ Servidor Angular detenido.${NC}"
else
    echo -e "${GREEN}✔ Servidor Angular ya estaba detenido.${NC}"
fi

echo ""
echo -e "${GREEN}✅ PROCESOS DE LUKA APP FINALIZADOS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
