#!/bin/bash
# ============================================================================
# SCRIPT: dev-backend.sh
# Descripción: Levanta los microservicios del backend en orden por grupos,
#              y abre Terminator en cuadrícula para ver los logs.
# Uso: ./dev-backend.sh [--hibrido | --produccion]
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/estructura-backend/docker"

# Definir modo por defecto (Híbrido - sin BDs en contenedores, 1GB RAM)
COMPOSE_FILE="docker-compose-hibrido.yml"
MODE_NAME="HÍBRIDO (1 GB límite por contenedor)"

# Procesar argumentos
for arg in "$@"; do
    case $arg in
        --produccion)
            COMPOSE_FILE="docker-compose.yml"
            MODE_NAME="PRODUCCIÓN (1.5 GB / 2 GB límite por contenedor)"
            shift
            ;;
        --hibrido)
            COMPOSE_FILE="docker-compose-hibrido.yml"
            MODE_NAME="HÍBRIDO (1 GB límite por contenedor)"
            shift
            ;;
    esac
done

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 INICIANDO BACKEND LUKA ECOSYSTEM${NC}"
echo -e "${BLUE}  Modo: ${YELLOW}$MODE_NAME${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 1. Verificar directorio de Docker
if [ ! -d "$DOCKER_DIR" ]; then
    echo -e "${RED}❌ Error: No se encontró la carpeta Docker en: $DOCKER_DIR${NC}"
    exit 1
fi

# 2. Advertencia de almacenamiento Docker
echo -e "${BLUE}🔍 Verificando almacenamiento Docker...${NC}"
DOCKER_ROOT=$(docker info 2>/dev/null | grep "Docker Root Dir" | cut -d':' -f2- | xargs || echo "")
if [[ "$DOCKER_ROOT" == *"/media/paulo/Memoria Linux"* ]]; then
    echo -e "${GREEN}✅ Docker está configurado en Memoria Linux ($DOCKER_ROOT).${NC}"
else
    echo -e "${YELLOW}⚠️  Aviso: Docker NO está configurado para usar 'Memoria Linux'.${NC}"
    echo -e "Actualmente usa: ${RED}$DOCKER_ROOT${NC}"
    echo -e "Puedes cambiarlo ejecutando: ${GREEN}sudo ./configurar-espacio-docker.sh${NC}"
    echo ""
    read -p "¿Deseas continuar de todas formas con el arranque? (s/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo -e "${RED}Arranque cancelado por el usuario.${NC}"
        exit 1
    fi
fi
echo ""

# Cambiar al directorio de docker compose
cd "$DOCKER_DIR"

# 3. Definición de Grupos de Servicios (Solo microservicios activos)
GRUPO_1="api-gateway ms-auditoria"
GRUPO_2="ms-usuario ms-cliente ms-mensajeria"
GRUPO_3="ms-nucleo-financiero ms-ia ms-pagos"

# Iniciar Grupo 1
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📦 Levantar Grupo 1: API Gateway + Auditoría...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
docker-compose -f "$COMPOSE_FILE" up -d $GRUPO_1
echo ""

echo -e "${YELLOW}⏳ Esperando 10 segundos para inicializar infraestructura...${NC}"
sleep 10
echo ""

# Iniciar Grupo 2
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📦 Levantar Grupo 2: Usuario + Cliente + Mensajería...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
docker-compose -f "$COMPOSE_FILE" up -d $GRUPO_2
echo ""

echo -e "${YELLOW}⏳ Esperando 5 segundos para estabilización...${NC}"
sleep 5
echo ""

# Iniciar Grupo 3
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📦 Levantar Grupo 3: Financiero + IA + Pagos...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
docker-compose -f "$COMPOSE_FILE" up -d $GRUPO_3
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✨ ¡TODOS LOS MICROSERVICIOS HAN SIDO LANZADOS!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 4. Abrir Terminator
echo -e "${BLUE}📟 Abriendo Terminator con logs en cuadrícula (layout: luka)...${NC}"
if command -v terminator &> /dev/null; then
    terminator -l luka &
    echo -e "${GREEN}✅ Terminator abierto en segundo plano.${NC}"
else
    echo -e "${YELLOW}⚠️  Terminator no está disponible en la terminal actual.${NC}"
    echo -e "Puedes abrirlo manualmente con: ${GREEN}terminator -l luka${NC}"
fi
echo ""
