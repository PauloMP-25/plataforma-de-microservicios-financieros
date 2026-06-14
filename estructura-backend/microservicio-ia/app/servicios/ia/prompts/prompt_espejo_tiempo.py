"""
servicios/ia/prompts/prompt_espejo_tiempo.py  ·  v1.0 — ESPEJO DEL TIEMPO
══════════════════════════════════════════════════════════════════════════════
Genera el prompt reducido que se envía a Gemini en la FASE 3.

Regla de oro: NUNCA pasar el DataFrame completo ni tablas crudas.
Solo los KPIs resumidos que Gemini necesita para narrar creativamente.
Gemini NO calcula nada aquí; su única tarea es escribir dos «cartas
al futuro» (cartaContinuidad y cartaTransformacion).

Autor: microservicio-ia LUKA
"""

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO


def generar_prompt_espejo_tiempo(
    metricas: dict,
    contexto: ContextoEstrategicoIADTO,
) -> str:
    """
    Construye el prompt de Gemini para el módulo Espejo del Tiempo.

    Parámetros:
        metricas — Dict de KPIs calculados en FASE 2 (ejecutar_calculos).
        contexto — ContextoEstrategicoIADTO con nombre y tono del usuario.

    Retorna:
        str con el prompt completo listo para enviar a Gemini.
    """
    nombre = contexto.primer_nombre
    tono = contexto.tono_ia

    # Extraer KPIs clave del dict de métricas
    ahorro_actual = metricas.get("ahorro_mensual_actual", 0.0)
    ahorro_optimizado = metricas.get("ahorro_mensual_optimizado", 0.0)
    diferencia_12m = metricas.get("diferencia_neta_12m", 0.0)
    score_inicial = metricas.get("score_actual", 50)

    hitos_cont_12m = metricas.get("proyeccion_continuidad", {}).get("hitos12Meses", {})
    hitos_trans_12m = metricas.get("proyeccion_transformacion", {}).get("hitos12Meses", {})

    score_cont_12m = hitos_cont_12m.get("scoreProyectado", score_inicial)
    score_trans_12m = hitos_trans_12m.get("scoreProyectado", score_inicial)

    cumplidas_cont = metricas.get("metas_cumplidas_continuidad_12m", [])
    fracasadas_cont = metricas.get("metas_fracasadas_continuidad_12m", [])
    cumplidas_trans = metricas.get("metas_cumplidas_transformacion_12m", [])
    fracasadas_trans = metricas.get("metas_fracasadas_transformacion_12m", [])

    # Listas formateadas para el prompt
    def _lista(items: list) -> str:
        return ", ".join(f"'{i}'" for i in items) if items else "ninguna"

    return f"""Eres el Espejo del Tiempo de LUKA, un oráculo financiero que habla al usuario
sobre su propio futuro. Usa exclusivamente la información numérica que se te
proporciona — NO inventes montos ni porcentajes.

━━━ DATOS DEL PRESENTE ━━━
• Nombre del usuario       : {nombre}
• Tono preferido           : {tono}
• Score financiero actual  : {score_inicial} / 100
• Capacidad de ahorro/mes  : S/ {ahorro_actual:,.2f}
• Ahorro optimizado/mes    : S/ {ahorro_optimizado:,.2f}
• Diferencia neta en 12 m  : S/ {diferencia_12m:,.2f} adicionales

━━━ PROYECCIÓN «SIN CAMBIOS» — 12 MESES ━━━
• Score proyectado         : {score_cont_12m} / 100
• Metas que se cumplirán   : {_lista(cumplidas_cont)}
• Metas que fracasarán     : {_lista(fracasadas_cont)}

━━━ PROYECCIÓN «TRANSFORMACIÓN» — 12 MESES ━━━
• Score proyectado         : {score_trans_12m} / 100
• Metas que se cumplirán   : {_lista(cumplidas_trans)}
• Metas que fracasarán     : {_lista(fracasadas_trans)}

━━━ TU TAREA ━━━
Escribe DOS cartas breves (máximo 5 oraciones cada una), en segunda persona,
en tiempo presente, dirigidas directamente a {nombre}.

CARTA 1 — «cartaContinuidad»:
  - Describe el futuro de {nombre} si CONTINÚA con sus hábitos actuales.
  - Menciona su score proyectado ({score_cont_12m}) y su ahorro (S/ {ahorro_actual:,.2f}/mes).
  - Nombra explícitamente qué metas logrará y cuáles no.
  - El tono debe ser {tono}: honesto pero sin alarmismo.

CARTA 2 — «cartaTransformacion»:
  - Describe el futuro de {nombre} si REDUCE sus gastos no esenciales.
  - Menciona que su ahorro mejora a S/ {ahorro_optimizado:,.2f}/mes y que en 12 meses
    acumula S/ {diferencia_12m:,.2f} adicionales respecto a no cambiar nada.
  - Nombra las metas que ahora sí lograría.
  - El tono debe ser {tono}: esperanzador y concreto.

RESTRICCIONES ESTRICTAS:
1. Usa SOLO los números que se te dieron. Jamás inventes cifras.
2. Cada carta: máximo 5 oraciones. Ninguna carta debe ser mayor a 120 palabras.
3. Saluda al usuario con su nombre ({nombre}) al inicio de cada carta.
4. No uses Markdown, viñetas ni subtítulos dentro de las cartas. Solo prosa fluida.
5. Tu salida DEBE ajustarse al esquema JSON solicitado.
"""
