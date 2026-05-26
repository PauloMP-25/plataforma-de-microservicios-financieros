"""
test_habitos_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para el módulo HABITOS_FINANCIEROS con frecuencia dinámica.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime, timedelta
from app.servicios.modulos.habitos_financieros import HabitosFinancierosService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_habitos_semanal_exito():
    service = HabitosFinancierosService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Ingeniero",
        ingreso_mensual=5000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=50.0,
        porcentaje_alerta_gasto=80,
        nombre_meta_principal="Laptop"
    )
    
    # 25 transacciones en la última semana
    hoy = datetime.now()
    data = []
    for i in range(25):
        data.append({
            "fecha": hoy - timedelta(days=i % 7), 
            "tipo": "GASTO", 
            "monto": 20.0, 
            "categoria": "Comida"
        })
    
    df = pd.DataFrame(data)
    
    # Ejecutar con frecuencia SEMANAL
    metricas = service.ejecutar_calculos(df, contexto, frecuencia="SEMANAL")
    assert metricas["frecuencia_analizada"] == "SEMANAL"
    assert metricas["total_transacciones_periodo"] > 0
    print("[OK] Test Hábitos Semanal Exitoso")

def test_habitos_insuficiente():
    service = HabitosFinancierosService()
    # Solo 10 transacciones (el mínimo es 20)
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 10)
    
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, ContextoEstrategicoIADTO(
            nombres="X",
            ocupacion="Estudiante",
            ingreso_mensual=1000.0,
            tono_ia="AMIGABLE",
            porcentaje_meta_principal=10.0,
            porcentaje_alerta_gasto=90
        ))
    print("[OK] Test Hábitos Insuficiente validado")

if __name__ == "__main__":
    test_habitos_semanal_exito()
    test_habitos_insuficiente()
