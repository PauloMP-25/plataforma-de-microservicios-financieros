import hashlib
import json
from typing import Any, Dict

def generar_hash_datos(datos: Dict[str, Any]) -> str:
    """
    Genera un hash SHA-256 a partir de un diccionario de datos.
    Se asegura de que el orden de las llaves sea consistente.
    """
    # Convertimos el diccionario a una cadena JSON con llaves ordenadas
    datos_serializados = json.dumps(datos, sort_keys=True, ensure_ascii=False)
    # Generamos el hash
    return hashlib.sha256(datos_serializados.encode("utf-8")).hexdigest()
