#!/bin/bash
# ============================================================================
# GRUPO 2: Usuario + Cliente + Mensajería
# Requiere: Grupo 1 corriendo (api-gateway + ms-auditoria)
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
echo -e "${BLUE}  🚀 GRUPO 2: Usuario + Cliente + Mensajería${NC}"
echo -e "${YELLOW}  📌 BD/RabbitMQ/Redis: locales (host)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta Docker no encontrada en $DOCKER_DIR${NC}"
    exit 1
fi

cd "$DOCKER_DIR"

# Validar que Grupo 1 esté corriendo
GATEWAY_RUNNING=$(docker-compose -f "$COMPOSE_FILE" ps | grep "api-gateway" | grep "Up" || echo "")
if [ -z "$GATEWAY_RUNNING" ]; then
    echo -e "${YELLOW}⚠️  GRUPO 1 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: devbackend-g1${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Grupo 1 detectado${NC}"
echo ""

SERVICES="ms-usuario ms-cliente ms-mensajeria"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • ms-usuario      → puerto 8081"
echo "   • ms-cliente      → puerto 8083"
echo "   • ms-mensajeria   → puerto 8084"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando con: $COMPOSE_FILE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose -f "$COMPOSE_FILE" up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 2 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado:${NC}"
docker-compose -f "$COMPOSE_FILE" ps | grep -E "ms-usuario|ms-cliente|ms-mensajeria" || true
echo ""
