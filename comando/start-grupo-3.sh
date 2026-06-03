#!/bin/bash
# ============================================================================
# GRUPO 3: Financiero + IA + Suscripciones + Pagos
# Dependencias: Grupo 1 + Grupo 2
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
echo -e "${BLUE}  🚀 GRUPO 3: Financiero + IA + Suscripciones + Pagos${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Validar que Grupo 1 y 2 estén corriendo
cd "$DOCKER_DIR"
GATEWAY_RUNNING=$(docker-compose ps | grep "api-gateway" | grep "Up" || echo "")
USUARIO_RUNNING=$(docker-compose ps | grep "ms-usuario" | grep "Up" || echo "")

if [ -z "$GATEWAY_RUNNING" ]; then
    echo -e "${RED}❌ GRUPO 1 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: ./start-grupo-1.sh${NC}"
    exit 1
fi

if [ -z "$USUARIO_RUNNING" ]; then
    echo -e "${RED}❌ GRUPO 2 no está ejecutándose${NC}"
    echo -e "${YELLOW}Ejecuta primero: ./start-grupo-2.sh${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Grupo 1 y Grupo 2 detectados${NC}"
echo ""

# Servicios del grupo 3
SERVICES="ms-nucleo-financiero ms-ia ms-suscripciones ms-pago"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • ms-nucleo-financiero"
echo "   • ms-ia"
echo "   • ms-suscripciones"
echo "   • ms-pago"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Ejecutando: docker-compose up -d $SERVICES${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose up -d $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 3 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Stack completo de Luka:${NC}"
docker-compose ps
echo ""
echo -e "${GREEN}✨ ¡LUKA ECOSYSTEM 100% OPERACIONAL! ✨${NC}"
echo ""
