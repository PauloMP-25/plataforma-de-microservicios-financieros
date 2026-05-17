"""
test_prediccion_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para PREDECIR_GASTOS con tendencia estadística.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime, timedelta
from app.servicios.modulos.predecir_gastos import PrediccionGastosService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_prediccion_exito():
    service = PrediccionGastosService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Ingeniero",
        ingreso_mensual=5000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=50.0,
        porcentaje_alerta_gasto=80,
        nombre_meta_principal="Laptop"
    )
    
    # 60 transacciones en los últimos 2 meses
    hoy = datetime.now()
    data = []
    for i in range(60):
        tipo = "INGRESO" if i % 15 == 0 else "GASTO"
        monto = 2500 if tipo == "INGRESO" else 60
        data.append({
            "fecha": hoy - timedelta(days=i), 
            "tipo": tipo, 
            "monto": monto, 
            "categoria": "Fijo"
        })
    
    df = pd.DataFrame(data)
    
    # El módulo PrediccionGastosService usa meses_minimos en su __init__ original, 
    # pero aquí validamos la lógica de cálculo.
    metricas = service.ejecutar_calculos(df, contexto)
    assert "gasto_proyectado" in metricas
    assert "balance_proyectado" in metricas
    assert metricas["balance_proyectado"] > 0
    print("[OK] Test Predicción Exitoso")

def test_prediccion_insuficiente():
    service = PrediccionGastosService()
    # Menos de 50 transacciones
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 20)
    
    # Nota: El servicio debe llamar a validar_historial en su ejecutar_calculos
    # si queremos que falle aquí.
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, ContextoEstrategicoIADTO(
            nombres="X",
            ocupacion="Estudiante",
            ingreso_mensual=1000.0,
            tono_ia="AMIGABLE",
            porcentaje_meta_principal=10.0,
            porcentaje_alerta_gasto=90
        ))
    print("[OK] Test Predicción Insuficiente validado")

if __name__ == "__main__":
    test_prediccion_exito()
    test_prediccion_insuficiente()
