#!/bin/bash
# ============================================================================
# SCRIPT: configurar-espacio-docker.sh
# Descripción: Redirige el almacenamiento de Docker (data-root) a la partición
#              de alta velocidad y espacio "/media/paulo/Memoria Linux".
# Uso: sudo ./configurar-espacio-docker.sh
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

TARGET_DIR="/media/paulo/Memoria Linux/docker-data"
DAEMON_JSON="/etc/docker/daemon.json"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🐳 CONFIGURADOR DE ALMACENAMIENTO DOCKER${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Verificar que se ejecuta con sudo/root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}❌ Error: Este script requiere privilegios de root (sudo).${NC}"
    echo -e "Por favor, ejecútalo como: ${YELLOW}sudo ./configurar-espacio-docker.sh${NC}"
    exit 1
fi

# 1. Crear directorio en Memoria Linux si no existe
echo -e "${BLUE}[1/4] Creando directorio en Memoria Linux...${NC}"
if [ ! -d "/media/paulo/Memoria Linux" ]; then
    echo -e "${RED}❌ Error: No se encuentra montada la partición '/media/paulo/Memoria Linux'.${NC}"
    exit 1
fi

mkdir -p "$TARGET_DIR"
chmod 710 "$TARGET_DIR"
echo -e "${GREEN}✅ Carpeta creada en: $TARGET_DIR${NC}"
echo ""

# 2. Modificar /etc/docker/daemon.json
echo -e "${BLUE}[2/4] Configurando $DAEMON_JSON...${NC}"
python3 -c "
import json, os
data = {}
if os.path.exists('$DAEMON_JSON'):
    try:
        with open('$DAEMON_JSON', 'r') as f:
            data = json.load(f)
    except Exception:
        pass
data['data-root'] = '$TARGET_DIR'
with open('$DAEMON_JSON', 'w') as f:
    json.dump(data, f, indent=4)
"
echo -e "${GREEN}✅ Archivo de configuración actualizado con data-root: $TARGET_DIR${NC}"
echo ""

# 3. Detener Docker para aplicar cambios
echo -e "${BLUE}[3/4] Deteniendo servicio Docker...${NC}"
systemctl stop docker || echo "Docker ya estaba detenido."
systemctl stop docker.socket || true
echo -e "${GREEN}✅ Docker detenido.${NC}"
echo ""

# 4. Iniciar Docker con la nueva configuración
echo -e "${BLUE}[4/4] Iniciando servicio Docker...${NC}"
systemctl start docker.socket || true
systemctl start docker
echo -e "${GREEN}✅ Docker iniciado correctamente.${NC}"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✨ CONFIGURACIÓN COMPLETADA CON ÉXITO${NC}"
echo -e "Docker Root Dir actual: ${YELLOW}$(docker info 2>/dev/null | grep 'Docker Root Dir' | cut -d':' -f2- | xargs)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
