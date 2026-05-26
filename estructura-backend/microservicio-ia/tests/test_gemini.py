import google.generativeai as genai
import os

# Coloca tu API KEY aquí para la prueba
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

print("--- LISTA DE MODELOS DISPONIBLES ---")
try:
    for m in genai.list_models():
        if 'generateContent' in m.supported_generation_methods:
            print(f"Nombre: {m.name}")
except Exception as e:
    print(f"Error al listar: {e}")