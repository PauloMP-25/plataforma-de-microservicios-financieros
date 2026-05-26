"""
servicios/ia/clasificador_ia.py  ·  v1.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Servicio de Auto-Clasificación On-the-Fly.
══════════════════════════════════════════════════════════════════════════════
"""

import logging
import json
import google.generativeai as genai
from typing import List
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import SolicitudClasificacionDTO, RespuestaClasificacionDTO

logger = logging.getLogger(__name__)

class ClasificadorIAService:
    def __init__(self) -> None:
        self.config = obtener_configuracion()
        genai.configure(api_key=self.config.gemini_api_key)
        self.model = genai.GenerativeModel(self.config.gemini_modelo)

    async def clasificar(self, solicitud: SolicitudClasificacionDTO) -> RespuestaClasificacionDTO:
        """
        Analiza el contexto de una transacción y devuelve 3 categorías sugeridas.
        """
        # VALIDACIÓN DE SEGURIDAD: Evitar contexto vacío
        contexto = f"{solicitud.notas or ''} {solicitud.etiquetas or ''}".strip()
        if not contexto:
            logger.warning(f"[CLASIFICADOR] Intento de clasificación sin contexto para ID: {solicitud.id_temporal}")
            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=["General", "Otros", "Varios"],
                usando_fallback=True
            )

        prompt = f"""
        Actúa como un clasificador contable preciso para la app financiera Luka.
        El usuario ha registrado un movimiento de tipo {solicitud.tipo_movimiento}.
        Detalles proporcionados: 
        - Notas: "{solicitud.notas}"
        - Etiquetas: "{solicitud.etiquetas}"

        Tu tarea:
        1. Analiza el texto para identificar la intención del gasto o ingreso.
        2. Basado en estándares financieros (Hogar, Salud, Alimentación, Transporte, Ocio, etc.), genera las 3 categorías más precisas del catálogo de Luka App.
        3. Devuelve ÚNICAMENTE un array JSON con 3 nombres de categorías en español.

        Ejemplo de salida: ["Alimentación", "Restaurantes", "Ocio"]
        """

        try:
            response = await self.model.generate_content_async(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=0.2, # Baja temperatura para mayor precisión
                    candidate_count=1,
                    max_output_tokens=100
                )
            )
            
            # Limpiar la respuesta para obtener solo el JSON
            texto = response.text.strip()
            if "```json" in texto:
                texto = texto.split("```json")[1].split("```")[0].strip()
            elif "```" in texto:
                texto = texto.split("```")[1].split("```")[0].strip()
            
            sugerencias = json.loads(texto)
            
            if not isinstance(sugerencias, list):
                raise ValueError("La IA no devolvió una lista")
                
            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=sugerencias[:3],
                usando_fallback=False
            )

        except Exception as e:
            logger.error(f"[CLASIFICADOR] Error con Gemini: {e}")
            # Fallback local
            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=["General", "Otros", "Varios"],
                usando_fallback=True
            )
