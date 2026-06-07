#!/bin/bash
# ============================================================================
# SCRIPT: start-luka.sh
# Descripción: Levanta todos los microservicios de Luka y el Frontend
# ============================================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
COMANDO_DIR="$PROJECT_ROOT/comando"
DOCKER_DIR="$PROJECT_ROOT/estructura-backend/docker"
COMPOSE_FILE="docker-compose-hibrido.yml"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 INICIANDO LA PLATAFORMA LUKA ECOSYSTEM${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 1. Asegurar que la infraestructura esté corriendo
POSTGRES_RUNNING=$(docker ps -q --filter name=luka-postgres-infra --filter status=running)
REDIS_RUNNING=$(docker ps -q --filter name=luka-redis-infra --filter status=running)
RABBIT_RUNNING=$(docker ps -q --filter name=luka-rabbitmq-infra --filter status=running)

if [ -z "$POSTGRES_RUNNING" ] || [ -z "$REDIS_RUNNING" ] || [ -z "$RABBIT_RUNNING" ]; then
    echo -e "${YELLOW}⚠️  La infraestructura local no está corriendo. Iniciándola...${NC}"
    bash "$COMANDO_DIR/dev-infra.sh"
    sleep 5
fi

# 2. Levantar el Backend por Grupos
echo -e "${YELLOW}▶ Preparando Grupo 1 (Gateway + Auditoría)...${NC}"
bash "$COMANDO_DIR/start-grupo-1.sh"
cd "$DOCKER_DIR"
docker-compose -f "$COMPOSE_FILE" start api-gateway ms-auditoria
sleep 5

echo -e "${YELLOW}▶ Preparando Grupo 2 (Usuario + Cliente + Mensajería)...${NC}"
bash "$COMANDO_DIR/start-grupo-2.sh"
cd "$DOCKER_DIR"
docker-compose -f "$COMPOSE_FILE" start ms-usuario ms-cliente ms-mensajeria
sleep 5

echo -e "${YELLOW}▶ Preparando Grupo 3 (Financiero + IA + Pagos + Suscripciones)...${NC}"
bash "$COMANDO_DIR/start-grupo-3.sh"
cd "$DOCKER_DIR"
docker-compose -f "$COMPOSE_FILE" start ms-nucleo-financiero ms-ia ms-suscripciones ms-pagos

echo ""
echo -e "${GREEN}✅ BACKEND COMPLETAMENTE OPERATIVO${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 3. Lanzar Frontend en segundo plano
echo -e "${YELLOW}▶ Iniciando Frontend Angular...${NC}"
cd "$COMANDO_DIR"
bash ./dev-frontend.sh &

echo -e "${GREEN}✨ ¡LUKA APP INICIADA CON ÉXITO!${NC}"
echo ""
