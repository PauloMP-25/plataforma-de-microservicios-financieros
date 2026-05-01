import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_endpoint_analizar_gasto_hormiga():
    payload = {
        "id_usuario": "test-user",
        "tipo_solicitud": "CONSULTA_MODULO",
        "modulo_solicitado": "GASTO_HORMIGA",
        "historial_mensual": []
    }
    response = client.post("/api/v1/ia/analizar", json=payload)
    assert response.status_code == 200
    assert "consejo_texto" in response.json()