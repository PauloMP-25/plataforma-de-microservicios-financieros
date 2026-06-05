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

# Validar que la infraestructura local (devinfra) esté ejecutándose
POSTGRES_RUNNING=$(docker ps -q --filter name=luka-postgres-infra --filter status=running)
REDIS_RUNNING=$(docker ps -q --filter name=luka-redis-infra --filter status=running)
RABBIT_RUNNING=$(docker ps -q --filter name=luka-rabbitmq-infra --filter status=running)

if [ -z "$POSTGRES_RUNNING" ] || [ -z "$REDIS_RUNNING" ] || [ -z "$RABBIT_RUNNING" ]; then
    echo -e "${RED}❌ Error: La infraestructura local (devinfra) no está ejecutándose.${NC}"
    echo -e "${YELLOW}Por favor, ejecuta primero: devinfra${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Infraestructura local detectada (Postgres, Redis, RabbitMQ)${NC}"
echo ""

SERVICES="ms-usuario ms-cliente ms-mensajeria"

echo -e "${YELLOW}📦 Levantando servicios:${NC}"
echo "   • ms-usuario      → puerto 8081"
echo "   • ms-cliente      → puerto 8083"
echo "   • ms-mensajeria   → puerto 8084"
echo ""

for service in $SERVICES; do
    echo -e "${YELLOW}🗑️ Deteniendo y removiendo contenedor anterior para: $service...${NC}"
    docker-compose -f "$COMPOSE_FILE" rm -f -s -v $service || true
    echo -e "${YELLOW}🔨 Compilando y construyendo imagen para: $service...${NC}"
    docker-compose --env-file ../.env -f "$COMPOSE_FILE" build $service
done

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🐳 Creando contenedores con: $COMPOSE_FILE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

docker-compose --env-file ../.env -f "$COMPOSE_FILE" up --no-start --no-build --force-recreate $SERVICES

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ GRUPO 2 levantado exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📊 Estado:${NC}"
docker-compose -f "$COMPOSE_FILE" ps ms-usuario ms-cliente ms-mensajeria
echo ""
