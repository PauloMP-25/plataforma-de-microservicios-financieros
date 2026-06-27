import os
import glob

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    target = "enabled: ${SPRING_DATA_REDIS_SSL_ENABLED:false}"
    if target in content:
        content = content.replace(target, "enabled: true")
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Fixed {filepath}")

for filepath in glob.glob('/media/paulo/datos/proyecto-desarrollo-web-integrado/estructura-backend/**/src/main/resources/*.yml', recursive=True):
    process_file(filepath)
