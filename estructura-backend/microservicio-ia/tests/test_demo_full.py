import asyncio
import pandas as pd
import json
import os
from datetime import datetime
from unittest.mock import MagicMock, patch

# Asegurar que se carguen las variables de entorno
from dotenv import load_dotenv
load_dotenv(".env.book.env")
key = os.getenv("GEMINI_API_KEY")
print(f"--- API KEY CARGADA: {key[:5]}...{key[-5:] if key else 'None'} ---")

from app.servicios.modulos.analisis_estilo_de_vida import AnalisisEstiloVidaService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.ia.coach_ia import CoachIA
from app.modelos.esquemas import NombreModulo

async def demo():
    print("--- Iniciando Simulacion de Microservicio IA - Modulo Estilo de Vida ---\n")
    
    # 1. Preparar datos y contexto
    service = AnalisisEstiloVidaService()
    contexto = ContextoEstrategicoIADTO(
        nombres="Paulo",
        ocupacion="Ingeniero de Software",
        ingreso_mensual=5500.0,
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=45.5,
        porcentaje_alerta_gasto=85,
        nombre_meta_principal="Viaje a Japón"
    )
    
    # Simular historial: Mayoría en RESTAURANTE (Foodie) y algo de TECNOLOGIA
    data = []
    for i in range(15):
        data.append({"fecha": datetime.now(), "tipo": "GASTO", "monto": 45.0, "categoria": "RESTAURANTE"})
    for i in range(10):
        data.append({"fecha": datetime.now(), "tipo": "GASTO", "monto": 120.0, "categoria": "TECNOLOGIA"})
    
    df = pd.DataFrame(data)

    # 2. Ejecutar motor analítico (Pandas)
    print("--- Ejecutando Motor Analitico... ---")
    metricas = service.ejecutar_calculos(df, contexto)
    prompt = service.orquestar_prompt(metricas, contexto)

    # 3. Llamar a Gemini (Mocks de infraestructura, Llamada REAL a API)
    print("--- Generando respuesta con Google Gemini (Flash 1.5)... ---")
    coach = CoachIA()
    
    # Mockear dependencias externas para ejecución aislada
    with patch.object(coach, '_cache_redis'), \
         patch('app.servicios.ia.coach_ia.SessionLocal'), \
         patch('app.servicios.ia.coach_ia.publicador_auditoria'):
        
        coach._verificar_cuota_diaria = MagicMock()
        coach._obtener_de_cache = MagicMock(return_value=(None, None, False))
        coach._guardar_en_db = MagicMock()
        coach._auditar_consumo_tokens = MagicMock()

        consejo, estado, fallback = await coach.obtener_consejo_ia(
            usuario_id="demo_user_001",
            modulo=NombreModulo.ANALISIS_ESTILO_VIDA,
            prompt=prompt,
            datos_para_hash={"prompt": prompt},
            rol="PRO"
        )

        # 4. Consolidar Respuesta Final
        respuesta_final = {
            "id_transaccion": "ia-resp-999",
            "modulo": "ANALISIS_ESTILO_VIDA",
            "timestamp": datetime.now().isoformat(),
            "analisis_tecnico": metricas,
            "coach_ia": {
                "consejo": consejo,
                "estado": estado,
                "usando_fallback": fallback
            }
        }
        
        print("\n--- PROCESAMIENTO COMPLETADO ---\n")
        print(json.dumps(respuesta_final, indent=4, ensure_ascii=False))

if __name__ == "__main__":
    asyncio.run(demo())
