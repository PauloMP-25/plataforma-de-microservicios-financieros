#!/bin/bash
# ============================================================================
# LUKA UTILS - Funciones útiles para desarrollo
# ============================================================================

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
DOCKER_DIR="$PROJECT_ROOT/estructura-backend/docker"

# ════════════════════════════════════════════════════════════════════════════
# DASHBOARD - Muestra estado completo del sistema
# ════════════════════════════════════════════════════════════════════════════
dashboard() {
    clear
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║           📊 LUKA ECOSYSTEM - DASHBOARD                         ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    cd "$DOCKER_DIR"
    
    # Estado general
    echo -e "${BLUE}🐳 CONTENEDORES EN EJECUCIÓN:${NC}"
    docker-compose ps | tail -n +2 | awk '{print "   " $0}'
    echo ""
    
    # Resumen por grupo
    echo -e "${BLUE}📊 RESUMEN POR GRUPO:${NC}"
    
    GROUP1=$(docker-compose ps --services --filter status=running | grep -E "^(api-gateway|ms-auditoria)$" | wc -l)
    GROUP2=$(docker-compose ps --services --filter status=running | grep -E "^(ms-usuario|ms-cliente|ms-mensajeria)$" | wc -l)
    GROUP3=$(docker-compose ps --services --filter status=running | grep -E "^(ms-nucleo-financiero|ms-ia|ms-suscripciones|ms-pago|ms-pagos)$" | wc -l)
    
    echo -e "   Grupo 1: ${GREEN}$GROUP1${NC}/2 servicios"
    echo -e "   Grupo 2: ${GREEN}$GROUP2${NC}/3 servicios"
    echo -e "   Grupo 3: ${GREEN}$GROUP3${NC}/4 servicios"
    echo ""
    
    # Uso de recursos
    echo -e "${BLUE}💾 VOLÚMENES DE DATOS:${NC}"
    docker volume ls | grep "luka" | awk '{print "   " $0}'
    echo ""
    
    # Puertos expuestos
    echo -e "${BLUE}🌐 PUERTOS EXPUESTOS:${NC}"
    echo "   API Gateway: http://localhost:8080"
    echo ""
}

# ════════════════════════════════════════════════════════════════════════════
# LOGS - Muestra logs filtrados por servicio
# ════════════════════════════════════════════════════════════════════════════
logs() {
    local SERVICE=$1
    
    if [ -z "$SERVICE" ]; then
        echo "Servicios disponibles:"
        echo "  api-gateway"
        echo "  ms-auditoria"
        echo "  ms-usuario"
        echo "  ms-cliente"
        echo "  ms-mensajeria"
        echo "  ms-nucleo-financiero"
        echo "  ms-ia"
        echo "  ms-suscripciones"
        echo "  ms-pago"
        echo "  rabbitmq"
        echo ""
        echo "Uso: logs <servicio>"
        return 1
    fi
    
    cd "$DOCKER_DIR"
    docker-compose logs -f "$SERVICE"
}

# ════════════════════════════════════════════════════════════════════════════
# HEALTH - Verifica salud de los servicios
# ════════════════════════════════════════════════════════════════════════════
health() {
    echo -e "${BLUE}🏥 VERIFICANDO SALUD DE SERVICIOS...${NC}"
    echo ""
    
    cd "$DOCKER_DIR"
    
    # Check API Gateway
    echo -n "API Gateway: "
    if curl -s http://localhost:8080/health >/dev/null 2>&1; then
        echo -e "${GREEN}✅ OK${NC}"
    else
        echo -e "${RED}❌ NO RESPONDE${NC}"
    fi
    
    # Check RabbitMQ
    echo -n "RabbitMQ: "
    if curl -s http://localhost:15672/ >/dev/null 2>&1; then
        echo -e "${GREEN}✅ OK${NC}"
    else
        echo -e "${RED}❌ NO RESPONDE${NC}"
    fi
    
    echo ""
}

# ════════════════════════════════════════════════════════════════════════════
# MAIN - Menú interactivo
# ════════════════════════════════════════════════════════════════════════════
main() {
    case "${1:-}" in
        dashboard)
            dashboard
            ;;
        logs)
            logs "${2:-}"
            ;;
        health)
            health
            ;;
        *)
            echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║           🛠️  LUKA UTILS - Funciones Disponibles              ║${NC}"
            echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            echo "Uso: ./luka-utils.sh <comando>"
            echo ""
            echo "Comandos:"
            echo "  dashboard       Muestra estado completo del sistema"
            echo "  logs <servicio> Ver logs de un servicio específico"
            echo "  health          Verifica salud de los servicios"
            echo ""
            ;;
    esac
}

main "$@"
