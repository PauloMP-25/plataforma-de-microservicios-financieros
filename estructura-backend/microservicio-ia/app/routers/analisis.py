from fastapi import APIRouter, HTTPException, Body
from app.modelos.evento_analisis import EventoAnalisisIA, ResultadoAnalisisIA
from app.servicios.coach_ia import CoachIA

router = APIRouter(
    prefix="/api/v1/ia",
    tags=["Módulos de IA"]
)

coach = CoachIA()

@router.post("/analizar", 
             response_model=ResultadoAnalisisIA,
             summary="Ejecutar análisis de IA",
             description="Recibe el historial financiero y activa el módulo de IA solicitado.")
async def analizar_finanzas(evento: EventoAnalisisIA):
    resultado = coach.analizar(evento)
    if not resultado:
        raise HTTPException(status_code=500, detail="Gemini no pudo generar el análisis.")
    return resultado

