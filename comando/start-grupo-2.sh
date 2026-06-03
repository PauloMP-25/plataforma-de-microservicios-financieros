#!/bin/bash
# ============================================================================
# GRUPO 2: Usuario + Cliente + Mensajería
# Dependencias: Grupo 1 (API Gateway + Auditoría)
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
BACKEND_DIR="$PROJECT_ROOT/estructura-backend"
DOCKER_DIR="$BACKEND_DIR/docker"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 GRUPO 2: Usuario + Cliente + Mensajería${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Validar que Grupo 1 esté corriendo
cd "$DOCKER_DIR"
GATEWAY_RUNNING=$(docker-compose ps | grep "api-gateway" | grep "Up" || echo "")

if [ -z "$GATEWAY_RUNNING" ]; then
    echo -e "${YELLOW}⚠️  GRUPO 1 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: ./start-grupo-1.sh${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Grupo 1 detectado${NC}"
echo ""

# Servicios del grupo 2
SERVICES="ms-usuario ms-cliente ms-mensajeria"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • ms-usuario"
echo "   • ms-cliente"
echo "   • ms-mensajeria"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando: docker-compose up -d $SERVICES${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 2 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado de contenedores activos:${NC}"
docker-compose ps | grep -E "ms-usuario|ms-cliente|ms-mensajeria|api-gateway|ms-auditoria" || true
echo ""
