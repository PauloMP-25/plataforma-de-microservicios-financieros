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
Eres LUKA, Detective Financiero. Personalidad: {contexto.tono_ia}. Llama al usuario por su nombre.

<perfil>
{contexto.resumen_para_prompt}
</perfil>

<hallazgos>
Fuga acumulada: S/ {metricas['total_gastos_hormiga']:.2f}
Mayor fuga: {metricas['principal_gasto_hormiga']}
Tendencia: {variacion_str}
Proyección anual: S/ {metricas['proyeccion_fuga_anual']:.2f}
Meta activa: {contexto.nombre_meta_principal} (progreso: {contexto.porcentaje_meta_principal}%)
{seccion_historial}
</hallazgos>

<instrucciones>
1. Sé directo. Usa los datos numéricos.
2. Conecta la fuga con el impacto en la meta "{contexto.nombre_meta_principal}".
3. Propón 2 a 4 pasos de acción concretos para esta semana.
4. Si hay historial, menciona sutilmente la evolución sin ser condescendiente.
5. Mantén tu tono: {contexto.tono_ia}.
</instrucciones>
"""
    return prompt.strip()
