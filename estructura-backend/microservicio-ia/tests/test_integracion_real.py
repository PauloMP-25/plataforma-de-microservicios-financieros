import os
import sys
from decimal import Decimal
from datetime import datetime, timedelta
import pandas as pd
import google.generativeai as genai

# Añadir el directorio raíz al path para poder importar la app
sys.path.append(os.getcwd())

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.gasto_hormiga import GastoHormigaService
from app.servicios.coach_ia import CoachIA
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import NombreModulo

def test_real_gemini_advice():
    print("\n--- INICIANDO PRUEBA DE INTEGRACIÓN REAL CON GEMINI ---")
    
    # 1. Cargar configuración (lee .env.book.env)
    try:
        config = obtener_configuracion()
        print(f"Modelo configurado: {config.gemini_modelo}")
        if not config.gemini_api_key or "AIza" not in config.gemini_api_key:
            print("⚠️ ADVERTENCIA: La API Key parece no estar configurada correctamente.")
    except Exception as e:
        print(f"❌ Error al cargar configuración: {e}")
        return
    
    # 2. Crear contexto de usuario (Paulo)
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo César Moron",
        ocupacion="Estudiante de Ingeniería de Sistemas",
        ingreso_mensual=Decimal("1500.00"),
        tono_ia="MOTIVADOR",
        porcentaje_meta_principal=Decimal("35.50"),
        nombre_meta_principal="Laptop para mi tesis",
        porcentaje_alerta_gasto=80
    )
    
    # 3. Generar transacciones ficticias (4 meses de historial con hormigas)
    hoy = datetime.now()
    transacciones = []
    # 120 días de historial
    for i in range(120):
        fecha = hoy - timedelta(days=i)
        # Gasto hormiga cada 2 días (ej: café de S/ 8.50)
        if i % 2 == 0:
            transacciones.append({
                "id": f"tx-{i}",
                "monto": 8.50,
                "tipo": "GASTO",
                "nombreCliente": "Starbucks Campus",
                "categoriaNombre": "Alimentación",
                "fechaTransaccion": fecha.isoformat()
            })
        # Sueldo mensual
        if fecha.day == 1:
            transacciones.append({
                "id": f"sueldo-{i}",
                "monto": 1500.00,
                "tipo": "INGRESO",
                "nombreCliente": "Empresa SAC",
                "categoriaNombre": "Sueldo",
                "fechaTransaccion": fecha.isoformat()
            })

    print(f"Generadas {len(transacciones)} transacciones para el análisis.")

    # 4. Ejecutar Pipeline Gasto Hormiga (Fase 1, 2, 3)
    service = GastoHormigaService()
    try:
        resultado_pipeline = service.run_pipeline(transacciones, contexto)
        
        prompt = resultado_pipeline["prompt"]
        metricas = resultado_pipeline["metricas"]
        
        print("\n[MÉTRICAS CALCULADAS]")
        print(f"- Total hormiga: S/ {metricas['total_hormiga']}")
        print(f"- Impacto mensual estimado: S/ {metricas['impacto_mensual_estimado']}")
        print(f"- Items detectados: {metricas['items_detectados']}")
        print(f"- Comercios recurrentes: {metricas['comercios_unicos']}")
        
        print("\n[PROMPT GENERADO (Previsualización)]")
        print("-" * 50)
        print(prompt[:400] + "...")
        print("-" * 50)

        # 5. Llamar a Gemini de verdad usando CoachIA
        print("\nEnviando prompt a Google Gemini...")
        coach = CoachIA()
        
        # El nombre del módulo en el enum para CoachIA
        nombre_mod = NombreModulo.OPTIMIZAR_SUSCRIPCIONES 
        
        # Forzar un modelo conocido para la prueba
        coach._modelo = genai.GenerativeModel("gemini-flash-latest")
        print(f"Probando con modelo: gemini-flash-latest")
        
        # Llamada directa al método privado para testear el prompt crudo
        consejo = coach._llamar_gemini(prompt, nombre_mod)
        
        print("\n✅ [CONSEJO GENERADO POR GEMINI]")
        print("=" * 60)
        print(consejo)
        print("=" * 60)
        print("\n¡Prueba de integración exitosa!")
        
    except Exception as e:
        print(f"\n[ERROR] Error durante el pipeline o conexión con Gemini: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_real_gemini_advice()
