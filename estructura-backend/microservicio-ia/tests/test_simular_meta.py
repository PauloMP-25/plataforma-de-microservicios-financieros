"""
test_simulacion_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para SIMULAR_META con regla híbrida (30 TXs o 1 Mes).
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
from datetime import datetime, timedelta
from app.servicios.modulos.simular_meta import SimularMetaService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def test_simulacion_hibrida_exito():
    service = SimularMetaService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Ingeniero",
        ingreso_mensual=5000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=50.0,
        porcentaje_alerta_gasto=80,
        nombre_meta_principal="Laptop"
    )
    
    # 35 transacciones en un periodo corto
    hoy = datetime.now()
    data = []
    for i in range(35):
        data.append({"fecha": hoy, "tipo": "INGRESO" if i == 0 else "GASTO", "monto": 1000 if i==0 else 20, "categoria": "Base"})
    
    df = pd.DataFrame(data)
    
    # Simular meta de S/ 3000 con ahorro previo de S/ 500
    metricas = service.ejecutar_calculos(
        df, contexto, 
        meta_monto=3000.0, 
        meta_ahorro_previo=500.0,
        fecha_objetivo=(hoy + timedelta(days=365)).strftime("%Y-%m-%d")
    )
    
    assert metricas["es_viable"] is True
    assert metricas["viabilidad_fecha_objetivo"] is True
    assert metricas["ahorro_faltante"] == 2500.0
    print("[OK] Test Simulación Híbrida Exitoso")

def test_simulacion_insuficiente():
    service = SimularMetaService()
    # Solo 5 transacciones y 1 día de historial
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 5)
    
    metricas = service.ejecutar_calculos(df, ContextoEstrategicoIADTO(
        nombres="X",
        ocupacion="Estudiante",
        ingreso_mensual=1000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=10.0,
        porcentaje_alerta_gasto=90
    ))
    assert metricas["es_viable"] is False
    assert "razon_insuficiencia" in metricas
    print("[OK] Test Simulación Insuficiente validado")

if __name__ == "__main__":
    test_simulacion_hibrida_exito()
    test_simulacion_insuficiente()
