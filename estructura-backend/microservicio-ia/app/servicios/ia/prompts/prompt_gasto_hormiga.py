"""
servicios/ia/prompts/prompt_gasto_hormiga.py
Generador del prompt para el módulo GASTO_HORMIGA.
"""

from typing import Dict, Any
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.ia.prompts.constructor_historial import construir_seccion_historial

def generar_prompt_gasto_hormiga(
    metricas: Dict[str, Any],
    contexto: ContextoEstrategicoIADTO,
) -> str:
    """Construye el prompt para el módulo GASTO_HORMIGA."""
    
    if not metricas["hay_hormigas"]:
        return (
            "[SKIP_IA] ¡Felicidades "
            + contexto.nombres
            + "! No he detectado gastos hormiga este mes. Sigue así."
        )

    if metricas.get("comparacion_disponible", True):
        variacion_pct = metricas["variacion_vs_mes_anterior"]
        if variacion_pct > 0:
            variacion_str = f"Los gastos hormiga AUMENTARON un {variacion_pct:.1f}% vs el mes anterior."
        elif variacion_pct < 0:
            variacion_str = f"Los gastos hormiga DISMINUYERON un {abs(variacion_pct):.1f}% vs el mes anterior. ¡Buen progreso!"
        else:
            variacion_str = "Los gastos hormiga se mantuvieron estables respecto al mes anterior."
    else:
        variacion_str = "No hay datos históricos suficientes para comparar con el mes anterior."

    historial_previo = metricas.get("_historial_previo")
    historial_insight = metricas.get("_historial_insight") or {}

    seccion_historial = construir_seccion_historial(
        historial_previo=historial_previo,
        historial_insight=historial_insight,
        kpi_anterior_key="total_gastos_hormiga",
        kpi_anterior_label="Fuga en la sesión anterior (S/)",
        categoria_anterior_key="principal_gasto_hormiga",
        categoria_anterior_label="Categoría con mayor fuga",
    )

    prompt = f"""
Eres LUKA, el Detective Financiero de la app de finanzas personales "Luka App".
Tu personalidad: {contexto.tono_ia}. Dirígete siempre al usuario por su nombre.

════════════════════════════════════════
PERFIL DEL USUARIO
════════════════════════════════════════
{contexto.resumen_para_prompt}

════════════════════════════════════════
HALLAZGOS DEL MOTOR ANALÍTICO (este mes)
════════════════════════════════════════
- Fuga acumulada en gastos hormiga: S/ {metricas['total_gastos_hormiga']:.2f}
- Categoría con mayor fuga: {metricas['principal_gasto_hormiga']}
- {variacion_str}
- Proyección de fuga anual si no actúa: S/ {metricas['proyeccion_fuga_anual']:.2f}
- Meta de ahorro activa: {contexto.nombre_meta_principal} (progreso: {contexto.porcentaje_meta_principal}%)
{seccion_historial}
════════════════════════════════════════
INSTRUCCIONES DE ANÁLISIS
════════════════════════════════════════
1. Sé directo y concreto. Usa los datos numéricos reales del análisis.
2. Conecta la fuga de dinero con el impacto real en su meta "{contexto.nombre_meta_principal}".
3. Propón exactamente entre 2 y 4 pasos de acción concretos y accionables esta semana.
4. Si el historial previo indica que el usuario ya tuvo gastos hormiga antes,
   menciona sutilmente si mejoró o empeoró, sin ser repetitivo ni condescendiente.
5. El tono debe ser: {contexto.tono_ia}.
"""
    return prompt.strip()
