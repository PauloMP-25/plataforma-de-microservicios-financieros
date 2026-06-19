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
from pydantic import BaseModel, Field
from app.configuracion import obtener_configuracion
from app.modelos.esquemas import SolicitudClasificacionDTO, RespuestaClasificacionDTO, CategoriaSugeridaDTO

logger = logging.getLogger(__name__)

class CategoriaSugeridaList(BaseModel):
    sugerencias: List[CategoriaSugeridaDTO] = Field(..., description="Lista de exactamente 5 sugerencias de categoría con su icono.")

class ClasificadorIAService:
    def __init__(self) -> None:
        self.config = obtener_configuracion()
        genai.configure(api_key=self.config.gemini_api_key)
        self.model = genai.GenerativeModel(self.config.gemini_modelo)

    async def clasificar(self, solicitud: SolicitudClasificacionDTO) -> RespuestaClasificacionDTO:
        """
        Analiza el contexto de una transacción y devuelve 5 categorías sugeridas con sus iconos respectivos.
        """
        # Definir fallbacks según tipo de movimiento
        es_ingreso = (solicitud.tipo_movimiento or "").upper() == "INGRESO"
        if es_ingreso:
            fallback_sugs = [
                CategoriaSugeridaDTO(categoria="Salario", icono="briefcase"),
                CategoriaSugeridaDTO(categoria="Freelance", icono="code"),
                CategoriaSugeridaDTO(categoria="Inversiones", icono="trending-up"),
                CategoriaSugeridaDTO(categoria="Ventas", icono="tag"),
                CategoriaSugeridaDTO(categoria="Otros Ingresos", icono="wallet")
            ]
        else:
            fallback_sugs = [
                CategoriaSugeridaDTO(categoria="Alimentos", icono="utensils"),
                CategoriaSugeridaDTO(categoria="Transporte", icono="bus"),
                CategoriaSugeridaDTO(categoria="Servicios", icono="wifi"),
                CategoriaSugeridaDTO(categoria="Hogar", icono="house"),
                CategoriaSugeridaDTO(categoria="Otros", icono="receipt")
            ]

        # VALIDACIÓN DE SEGURIDAD: Evitar contexto vacío
        contexto = f"{solicitud.descripcion or ''} {solicitud.etiquetas or ''}".strip()
        if not contexto:
            logger.warning(f"[CLASIFICADOR] Intento de clasificación sin contexto para ID: {solicitud.id_temporal}")
            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=fallback_sugs,
                usando_fallback=True
            )

        prompt = f"""<rol>Clasificador Contable de Precisión</rol>
<contexto>El usuario registró un movimiento de tipo: {solicitud.tipo_movimiento}.</contexto>
<transaccion>
- Descripcion: {solicitud.descripcion}
- Etiquetas: {solicitud.etiquetas or ""}
</transaccion>
<instrucciones>
Genera exactamente 5 sugerencias de categoría financieras en español y para cada una asigna un icono lógico de FontAwesome (ej. 'utensils', 'bus', 'wifi', 'house', 'briefcase-medical', 'film', 'briefcase', 'code', 'trending-up', 'tag', 'gift', 'wallet', 'receipt').
</instrucciones>"""

        try:
            response = await self.model.generate_content_async(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=0.1,
                    response_mime_type="application/json",
                    response_schema=CategoriaSugeridaList
                )
            )
            
            # Limpiar la respuesta para obtener solo el JSON en caso de que persistan comentarios o markdown
            texto = response.text.strip()
            if "```json" in texto:
                texto = texto.split("```json")[1].split("```")[0].strip()
            elif "```" in texto:
                texto = texto.split("```")[1].split("```")[0].strip()
            
            # Quitar cualquier texto antes o después del JSON
            if not (texto.startswith("{") or texto.startswith("[")):
                start_dict = texto.find("{")
                start_list = texto.find("[")
                if start_dict != -1 and (start_list == -1 or start_dict < start_list):
                    texto = texto[start_dict:texto.rfind("}")+1]
                elif start_list != -1:
                    texto = texto[start_list:texto.rfind("]")+1]

            sugerencias = []
            try:
                # Intentar cargar como dict o lista
                data = json.loads(texto)
                if isinstance(data, dict):
                    sugerencias_raw = data.get("sugerencias", [])
                elif isinstance(data, list):
                    sugerencias_raw = data
                else:
                    sugerencias_raw = []
                
                for item in sugerencias_raw:
                    if isinstance(item, dict):
                        sugerencias.append(
                            CategoriaSugeridaDTO(
                                categoria=item.get("categoria", "Otros"),
                                icono=item.get("icono", "receipt")
                            )
                        )
                    elif isinstance(item, str):
                        sugerencias.append(
                            CategoriaSugeridaDTO(
                                categoria=item,
                                icono="receipt"
                            )
                        )
            except Exception as e_json:
                logger.error(f"[CLASIFICADOR] Error al decodificar JSON manual: {e_json}. Texto: {texto}")
                raise ValueError("JSON inválido retornado por Gemini")

            # Completar o recortar para asegurar exactamente 5 elementos
            if len(sugerencias) > 5:
                sugerencias = sugerencias[:5]
            while len(sugerencias) < 5:
                sugerencias.append(CategoriaSugeridaDTO(categoria="Otros", icono="receipt"))

            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=sugerencias,
                usando_fallback=False
            )

        except Exception as e:
            logger.error(f"[CLASIFICADOR] Error con Gemini Structured Output: {e}")
            # Fallback local
            return RespuestaClasificacionDTO(
                id_temporal=solicitud.id_temporal,
                sugerencias=fallback_sugs,
                usando_fallback=True
            )
