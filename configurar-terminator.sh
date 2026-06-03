#!/bin/bash
# ============================================================================
# SCRIPT: configurar-terminator.sh
# Descripción: Configura un layout de Terminator llamado "luka" con una
#              cuadrícula de 3x3 para monitorear logs de microservicios.
# Uso: ./configurar-terminator.sh
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  📟 CONFIGURADOR DE LAYOUT PARA TERMINATOR${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Ejecutar el script de actualización en Python
python3 -c "
import os

config_path = os.path.expanduser('~/.config/terminator/config')
os.makedirs(os.path.dirname(config_path), exist_ok=True)

# Contenido del layout luka
luka_layout = '''  [[luka]]
    [[[window0]]]
      type = Window
      parent = \"\"
    [[[vpaned0]]]
      type = VPaned
      parent = window0
      order = 0
      position = 300
    [[[vpaned1]]]
      type = VPaned
      parent = vpaned0
      order = 0
      position = 300
    [[[row1]]]
      type = HPaned
      parent = vpaned1
      order = 0
      position = 400
    [[[terminal1_1]]]
      type = Terminal
      parent = row1
      order = 0
      command = \"docker logs -f luka-gateway 2>/dev/null || docker logs -f luka-api-gateway; bash\"
      profile = default
    [[[hpaned1_2]]]
      type = HPaned
      parent = row1
      order = 1
      position = 400
    [[[terminal1_2]]]
      type = Terminal
      parent = hpaned1_2
      order = 0
      command = \"docker logs -f luka-ms-usuario; bash\"
      profile = default
    [[[terminal1_3]]]
      type = Terminal
      parent = hpaned1_2
      order = 1
      command = \"docker logs -f luka-ms-cliente; bash\"
      profile = default
    [[[row2]]]
      type = HPaned
      parent = vpaned1
      order = 1
      position = 400
    [[[terminal2_1]]]
      type = Terminal
      parent = row2
      order = 0
      command = \"docker logs -f luka-ms-mensajeria; bash\"
      profile = default
    [[[hpaned2_2]]]
      type = HPaned
      parent = row2
      order = 1
      position = 400
    [[[terminal2_2]]]
      type = Terminal
      parent = hpaned2_2
      order = 0
      command = \"docker logs -f luka-ms-nucleo-financiero; bash\"
      profile = default
    [[[terminal2_3]]]
      type = Terminal
      parent = hpaned2_2
      order = 1
      command = \"docker logs -f luka-ms-pago; bash\"
      profile = default
    [[[row3]]]
      type = HPaned
      parent = vpaned0
      order = 1
      position = 400
    [[[terminal3_1]]]
      type = Terminal
      parent = row3
      order = 0
      command = \"docker logs -f luka-ms-ia; bash\"
      profile = default
    [[[hpaned3_2]]]
      type = HPaned
      parent = row3
      order = 1
      position = 400
    [[[terminal3_2]]]
      type = Terminal
      parent = hpaned3_2
      order = 0
      command = \"docker logs -f luka-ms-auditoria; bash\"
      profile = default
    [[[terminal3_3]]]
      type = Terminal
      parent = hpaned3_2
      order = 1
      command = \"watch -n 2 'docker ps | grep luka'; bash\"
      profile = default
'''

if not os.path.exists(config_path):
    default_config = f'''[global_config]
[keybindings]
[profiles]
  [[default]]
    use_system_font = False
    font = DejaVu Sans Mono 9
[layouts]
  [[default]]
    [[[window0]]]
      type = Window
      parent = \"\"
    [[[child1]]]
      type = Terminal
      parent = window0
      profile = default
{luka_layout}'''
    with open(config_path, 'w') as f:
        f.write(default_config)
    print('OK_CREATE')
else:
    with open(config_path, 'r') as f:
        content = f.read()
    
    # Separar en secciones principales
    sections = {}
    current_section = None
    lines = content.splitlines()
    
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('[') and stripped.endswith(']') and not stripped.startswith('[['):
            current_section = stripped
            sections[current_section] = []
        elif current_section:
            sections[current_section].append(line)
            
    # Si no hay layouts en el archivo, forzar creación
    if '[layouts]' not in sections:
        sections['[layouts]'] = [
            '  [[default]]',
            '    [[[window0]]]',
            '      type = Window',
            '      parent = \"\"',
            '    [[[child1]]]',
            '      type = Terminal',
            '      parent = window0',
            '      profile = default'
        ]
        
    # Limpiar layouts viejos de luka
    layout_lines = sections['[layouts]']
    cleaned_layout_lines = []
    skip = False
    
    for line in layout_lines:
        stripped = line.strip()
        if stripped.startswith('[[') and stripped.endswith(']]'):
            if stripped == '[[luka]]':
                skip = True
            else:
                skip = False
        if not skip:
            cleaned_layout_lines.append(line)
            
    # Agregar el nuevo luka layout
    cleaned_layout_lines.extend(luka_layout.splitlines())
    sections['[layouts]'] = cleaned_layout_lines
    
    # Reconstruir archivo
    with open(config_path, 'w') as f:
        # Escribir primero global_config, keybindings, profiles
        order = ['[global_config]', '[keybindings]', '[profiles]', '[layouts]']
        for sec in order:
            if sec in sections:
                f.write(sec + '\\n')
                for line in sections[sec]:
                    f.write(line + '\\n')
        # Escribir otras secciones si existieran
        for sec, lines in sections.items():
            if sec not in order:
                f.write(sec + '\\n')
                for line in lines:
                    f.write(line + '\\n')
    print('OK_UPDATE')
" > /tmp/terminator_res.txt

RES=$(cat /tmp/terminator_res.txt)
if [ "$RES" = "OK_CREATE" ]; then
    echo -e "${GREEN}✅ Archivo de configuración de Terminator creado con el layout 'luka'.${NC}"
elif [ "$RES" = "OK_UPDATE" ]; then
    echo -e "${GREEN}✅ Layout 'luka' insertado/actualizado exitosamente en la configuración de Terminator.${NC}"
else
    echo -e "${RED}❌ Ocurrió un error configurando Terminator: $RES${NC}"
    exit 1
fi

echo -e ""
echo -e "Puedes abrir la cuadrícula de monitoreo ejecutando: ${YELLOW}terminator -l luka${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
