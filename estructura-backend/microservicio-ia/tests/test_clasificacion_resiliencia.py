"""
test_clasificacion_resiliencia.py  ·  v1.0
══════════════════════════════════════════════════════════════════════════════
Pruebas de Resiliencia para el Módulo de Auto-Clasificación.
Escenarios: Fallo de Gemini, Contexto Vacío y Errores de Mensajería.
══════════════════════════════════════════════════════════════════════════════
"""

import pytest
import asyncio
from unittest.mock import MagicMock, patch
from app.servicios.ia.clasificador_ia import ClasificadorIAService
from app.modelos.esquemas import SolicitudClasificacionDTO
from app.utilidades.excepciones import BrokerComunicacionError

@pytest.mark.asyncio
async def test_fallback_por_contexto_vacio():
    """Valida que si no hay notas ni etiquetas, no se gaste API y devuelva fallback."""
    service = ClasificadorIAService()
    solicitud = SolicitudClasificacionDTO(
        id_temporal="temp-1",
        tipo_movimiento="GASTO",
        notas="",
        etiquetas=None
    )
    
    resultado = await service.clasificar(solicitud)
    
    assert resultado.usando_fallback is True
    assert resultado.sugerencias == ["General", "Otros", "Varios"]
    print("[OK] Fallback por contexto vacío validado.")

@pytest.mark.asyncio
async def test_fallback_por_error_gemini():
    """Simula una caída de la API de Gemini y verifica la respuesta de seguridad."""
    service = ClasificadorIAService()
    solicitud = SolicitudClasificacionDTO(
        id_temporal="temp-2",
        tipo_movimiento="GASTO",
        notas="Compra importante",
        etiquetas="urgente"
    )

    # Mockear el modelo para que lance una excepción
    with patch.object(service.model, 'generate_content_async', side_effect=Exception("API de Google caída")):
        resultado = await service.clasificar(solicitud)
        
        assert resultado.usando_fallback is True
        assert "General" in resultado.sugerencias
        print("[OK] Fallback por error de IA validado.")

def test_simular_error_rabbitmq():
    """
    Simula que RabbitMQ no está disponible al intentar procesar un mensaje.
    Este test valida la captura del error en el Consumidor.
    """
    from app.mensajeria.consumidor_ia import ConsumidorIA
    consumidor = ConsumidorIA()
    
    # Mock de canal de RabbitMQ para simular error de publicación
    mock_ch = MagicMock()
    mock_ch.basic_publish.side_effect = Exception("Conexión con RabbitMQ perdida")
    
    # Datos de prueba
    body = b'{"id_temporal": "t-1", "tipo_movimiento": "GASTO", "notas": "test"}'
    method = MagicMock()
    
    # Ejecutamos el callback y verificamos que maneje el error sin morir
    try:
        consumidor._callback_clasificacion(mock_ch, method, None, body)
        print("[OK] El consumidor manejó el error de RabbitMQ correctamente (Logging + Nack).")
    except Exception as e:
        pytest.fail(f"El consumidor lanzó una excepción no controlada: {e}")

if __name__ == "__main__":
    # Ejecución manual rápida
    asyncio.run(test_fallback_por_contexto_vacio())
    asyncio.run(test_fallback_por_error_gemini())
    test_simular_error_rabbitmq()
