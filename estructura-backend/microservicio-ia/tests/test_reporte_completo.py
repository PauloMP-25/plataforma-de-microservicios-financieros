"""
test_reporte_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para REPORTE_COMPLETO (Auditoría Anual + Score LUKA).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime
from app.servicios.modulos.reporte_completo import ReporteCompletoService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_reporte_anual_exito():
    service = ReporteCompletoService()
    contexto = ContextoEstrategicoIADTO(nombres="Paulo", meta_principal="Laptop")
    
    # 70 transacciones repartidas en el año actual
    anio_actual = datetime.now().year
    data = []
    for i in range(70):
        # Asegurar balance positivo para un buen score
        tipo = "INGRESO" if i % 10 == 0 else "GASTO"
        monto = 1000 if tipo == "INGRESO" else 50
        data.append({
            "fecha": datetime(anio_actual, (i % 12) + 1, 1), 
            "tipo": tipo, 
            "monto": monto, 
            "categoria": "General"
        })
    
    df = pd.DataFrame(data)
    
    metricas = service.ejecutar_calculos(df, contexto)
    assert metricas["anio"] == anio_actual
    assert 0 <= metricas["score_salud"] <= 100
    assert metricas["balance_anual"] > 0
    print(f"[OK] Test Reporte Anual Exitoso (Score: {metricas['score_salud']})")

def test_reporte_insuficiente():
    service = ReporteCompletoService()
    # Solo 10 transacciones (el mínimo es 60)
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 10)
    
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, ContextoEstrategicoIADTO(nombres="X"))
    print("[OK] Test Reporte Insuficiente validado")

if __name__ == "__main__":
    test_reporte_anual_exito()
    test_reporte_insuficiente()
