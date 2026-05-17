"""
test_gasto_hormiga_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para el módulo GASTO_HORMIGA con regla 20/20.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime
from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_gasto_hormiga_exito():
    service = DeteccionGastosHormigaService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Ingeniero",
        ingreso_mensual=5000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=50.0,
        porcentaje_alerta_gasto=80,
        nombre_meta_principal="Laptop"
    )
    
    # 1. Simular 20 txs mes actual + 20 txs mes anterior
    data = []
    hoy = datetime.now()
    mes_ant = hoy.replace(day=1) - pd.Timedelta(days=1)
    
    for _ in range(20):
        data.append({"fecha": hoy, "tipo": "GASTO", "monto": 10.0, "categoria": "Café"})
        data.append({"fecha": mes_ant, "tipo": "GASTO", "monto": 10.0, "categoria": "Café"})
    
    df = pd.DataFrame(data)
    
    # Ejecutar
    metricas = service.ejecutar_calculos(df, contexto)
    assert metricas["total_gastos_hormiga"] > 0
    assert metricas["hay_hormigas"] is True
    print("[OK] Test Gasto Hormiga Exitoso (20/20 txs)")

def test_gasto_hormiga_insuficiente():
    service = DeteccionGastosHormigaService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Estudiante",
        ingreso_mensual=1000.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=10.0,
        porcentaje_alerta_gasto=90
    )
    
    # Solo 5 transacciones
    data = [{"fecha": datetime.now(), "tipo": "GASTO", "monto": 5.0, "categoria": "Dulces"}] * 5
    df = pd.DataFrame(data)
    
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, contexto)
    print("[OK] Test Gasto Hormiga Insuficiente validado correctamente")

if __name__ == "__main__":
    test_gasto_hormiga_exito()
    test_gasto_hormiga_insuficiente()
