#!/bin/bash
# ============================================================================
# GRUPO 1: API Gateway + Microservicio Auditoría
# Dependencias: PostgreSQL + RabbitMQ
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
echo -e "${BLUE}  🚀 GRUPO 1: API Gateway + Auditoría${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Validar directorio
if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta Docker no encontrada en $DOCKER_DIR${NC}"
    exit 1
fi

cd "$DOCKER_DIR"
echo -e "${GREEN}✅ Ubicación: $(pwd)${NC}"
echo ""

# Servicios del grupo 1
SERVICES="postgres-auditoria postgres-usuario postgres-cliente postgres-financiero postgres-mensajeria postgres-ia postgres-pagos rabbitmq api-gateway ms-auditoria"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • postgres-auditoria"
echo "   • postgres-usuario"
echo "   • postgres-cliente"
echo "   • postgres-financiero"
echo "   • postgres-mensajeria"
echo "   • postgres-ia"
echo "   • postgres-pagos"
echo "   • rabbitmq"
echo "   • api-gateway"
echo "   • ms-auditoria"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando: docker-compose up -d $SERVICES${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 1 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado de contenedores:${NC}"
docker-compose ps | grep -E "api-gateway|ms-auditoria"
echo ""
echo -e "${YELLOW}🌐 API Gateway disponible en:${NC} http://localhost:8080"
echo ""
