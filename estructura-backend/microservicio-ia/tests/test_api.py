import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch # Importamos el simulador
from main import app

client = TestClient(app)

def test_endpoint_analizar_gasto_hormiga():
    payload = {
        "id_usuario": "test-user",
        "tipo_solicitud": "CONSULTA_MODULO",
        "modulo_solicitado": "GASTO_HORMIGA",
        "historial_mensual": []
    }
    
    # Simulamos el método 'analizar' del CoachIA para que no llame a Google
    with patch("app.routers.analisis.coach.analizar") as mock_analizar:
        # Definimos qué debe devolver la simulación
        mock_analizar.return_value = {
            "id_usuario": "test-user",
            "tipo_modulo": "GASTO_HORMIGA",  # <-- Cambiado de modulo_IA a tipo_modulo
            "consejo_texto": "Este es un consejo simulado para evitar el error de cuota.",
            "metricas_analizadas": {}
        }
        
        response = client.post("/api/v1/ia/analizar", json=payload)
        
        assert response.status_code == 200
        assert "consejo_texto" in response.json()
        # Verificamos que se llamó a la función simulada
        mock_analizar.assert_called_once()