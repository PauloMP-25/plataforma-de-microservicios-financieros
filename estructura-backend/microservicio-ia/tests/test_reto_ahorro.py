"""
test_reto_ahorro_pro.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Pruebas para el motor de estados del Reto Gamificado.
══════════════════════════════════════════════════════════════════════════════
"""

import pandas as pd
import pytest
from datetime import datetime, timedelta
from app.servicios.modulos.reto_ahorro_dinamico import RetoAhorroDinamicoService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.utilidades.excepciones import HistorialInsuficienteError

def test_reto_flujo_completo():
    service = RetoAhorroDinamicoService()
    usuario_id = "test_user_77"
    contexto = ContextoEstrategicoIADTO(nombres="Paulo", meta_principal="Laptop")
    
    # 1. Simular 25 transacciones para pasar el mínimo de 20
    hoy = datetime.now()
    data = []
    for i in range(25):
        data.append({"fecha": hoy - timedelta(days=i), "tipo": "GASTO", "monto": 50.0, "categoria": "Restaurantes"})
    
    df = pd.DataFrame(data)
    
    # 2. Paso A: Crear Reto (Estado NUEVO)
    # Nota: Esto creará un registro en la BD real si está conectada
    print("\n[PASO 1] Creando nuevo reto...")
    res_nuevo = service.ejecutar_calculos(df, contexto, usuario_id=usuario_id, frecuencia="SEMANAL")
    assert res_nuevo["estado_reto"] in ["NUEVO", "ACTIVO"]
    
    # 3. Paso B: Consultar Reto (Estado ACTIVO)
    print("[PASO 2] Consultando progreso del reto...")
    res_activo = service.ejecutar_calculos(df, contexto, usuario_id=usuario_id)
    assert res_activo["estado_reto"] == "ACTIVO"
    assert "progreso_temporal" in res_activo
    print(f"[OK] Progreso detectado: {res_activo['progreso_temporal']}%")

def test_reto_insuficiente():
    service = RetoAhorroDinamicoService()
    # Solo 5 transacciones (mínimo 20)
    df = pd.DataFrame([{"fecha": datetime.now(), "tipo": "GASTO", "monto": 10.0, "categoria": "X"}] * 5)
    
    with pytest.raises(HistorialInsuficienteError):
        service.ejecutar_calculos(df, ContextoEstrategicoIADTO(nombres="X"))
    print("[OK] Test Reto Insuficiente validado")

if __name__ == "__main__":
    test_reto_flujo_completo()
    test_reto_insuficiente()
