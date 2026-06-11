"""
servicios/ia/fallbacks/fallback_generico.py
Fallback genérico y funciones auxiliares para fallbacks en texto plano.
"""
from typing import Dict, Any, Optional
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

def generar_encabezado_resiliencia(
    modulo, 
    metricas: Dict[str, Any], 
    nombres: str,
    contexto: Optional[ContextoEstrategicoIADTO] = None
) -> str:
    # 1. Información de Perfil (ms-cliente)
    perfil_info = ""
    meta_info = ""
    if contexto:
        perfil_info = f"👤 **Perfil:** {contexto.ocupacion} | **Ingreso declarado:** S/ {contexto.ingreso_mensual:.2f}\n"
        if contexto.nombre_meta_principal and contexto.nombre_meta_principal != "Ninguna":
            meta_info = f"🎯 **Meta Activa:** {contexto.nombre_meta_principal} (Progreso: {contexto.porcentaje_meta_principal:.1f}%)\n"

    # 2. Resumen Financiero del Mes (ms-financiero)
    resumen_fin = ""
    if "_total_ingresos" in metricas and "_total_gastos" in metricas:
        total_ing = metricas["_total_ingresos"]
        total_gas = metricas["_total_gastos"]
        balance = round(total_ing - total_gas, 2)
        resumen_fin = f"💵 **Resumen Financiero del Periodo:** Ingresos: S/ {total_ing:.2f} | Gastos: S/ {total_gas:.2f} | Balance: S/ {balance:.2f}\n"

    # 3. Encabezado común de resiliencia
    return (
        f"### 🕵️‍♂️ Análisis de {modulo.value.replace('_', ' ').title()} (Modo de Resiliencia)\n\n"
        f"¡Hola {nombres}! Aunque nuestro servicio de IA principal (Gemini) está temporalmente ocupado o experimenta latencia, "
        f"nuestro motor analítico local ha procesado tu información financiera con éxito:\n\n"
        f"{perfil_info}"
        f"{resumen_fin}"
        f"{meta_info}\n"
    )

def generar_fallback_generico(modulo, metricas: Dict[str, Any], nombres: str, contexto) -> str:
    # Fallback genérico para otros módulos
    header = generar_encabezado_resiliencia(modulo, metricas, nombres, contexto)
    return (
        f"{header}"
        f"Por ahora no puedo darte un consejo hiper-personalizado por un retraso con la IA de Gemini, "
        f"pero nuestro motor matemático sigue analizando tus datos con éxito. "
        f"¡Mantén la disciplina financiera!"
    )
