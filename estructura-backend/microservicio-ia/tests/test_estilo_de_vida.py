"""
test_estilo_vida_pro.py  ·  v1.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para el módulo ANALISIS_ESTILO_VIDA (Persona Finder).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime
from app.servicios.modulos.analisis_estilo_de_vida import AnalisisEstiloVidaService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_estilo_vida_foodie_exito():
    service = AnalisisEstiloVidaService()
    contexto = ContextoEstrategicoIADTO(nombres="Paulo", meta_principal="Laptop")
    
    # Simular 25 transacciones dominadas por Restaurantes/Café
    data = []
    for i in range(25):
        categoria = "RESTAURANTE" if i < 15 else "SUPERMERCADO"
        data.append({"fecha": datetime.now(), "tipo": "GASTO", "monto": 50.0, "categoria": categoria})
    
    df = pd.DataFrame(data)
    
    metricas = service.ejecutar_calculos(df, contexto)
    assert metricas["cluster_dominante"] == "FOODIE"
    assert metricas["distribucion"]["FOODIE"]["porcentaje"] > 50
    print(f"[OK] Test Estilo de Vida Exitoso - Cluster Detectado: {metricas['cluster_dominante']}")

def test_estilo_vida_insuficiente():
    service = AnalisisEstiloVidaService()
    # Solo 10 transacciones (mínimo 20)
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 10)
    
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, ContextoEstrategicoIADTO(nombres="X"))
    print("[OK] Test Estilo de Vida Insuficiente validado")

if __name__ == "__main__":
    test_estilo_vida_foodie_exito()
    test_estilo_vida_insuficiente()
