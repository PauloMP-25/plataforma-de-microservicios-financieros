#!/bin/bash

# ============================================================================
# SCRIPT: dev-frontend.sh
# Descripción: Inicia el servidor Angular del frontend
# Uso: ./dev-frontend.sh
# ============================================================================

set -e  # Salir si hay error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Rutas
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/estructura-frontend/frontend"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🚀 Iniciando Frontend - Luka Ecosystem${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Verificar que la carpeta existe
if [ ! -d "$FRONTEND_DIR" ]; then
    echo -e "${RED}❌ Error: Carpeta frontend no encontrada en $FRONTEND_DIR${NC}"
    exit 1
fi

echo -e "${YELLOW}📁 Carpeta del proyecto:${NC} $FRONTEND_DIR"

# Cambiar a directorio
cd "$FRONTEND_DIR"
echo -e "${GREEN}✅ Ubicación: $(pwd)${NC}"

# Verificar si node_modules existe
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}📦 Instalando dependencias (primera ejecución)...${NC}"
    npm install
    echo -e "${GREEN}✅ Dependencias instaladas${NC}"
else
    echo -e "${GREEN}✅ Dependencias ya instaladas${NC}"
fi

# Verificar si Angular CLI está disponible
if ! command -v ng &> /dev/null; then
    echo -e "${YELLOW}⚠️  Angular CLI no encontrado globalmente, usando local...${NC}"
    NG_CMD="npx ng"
else
    NG_CMD="ng"
fi

# Iniciar servidor
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎯 Iniciando ng serve...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

$NG_CMD serve --open --host 0.0.0.0
