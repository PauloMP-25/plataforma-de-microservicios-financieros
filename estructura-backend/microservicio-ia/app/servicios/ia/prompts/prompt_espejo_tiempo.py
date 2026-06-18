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

    return f"""<rol>Espejo del Tiempo de LUKA (Oráculo Financiero)</rol>
<contexto>Eres un oráculo que habla al usuario sobre su propio futuro basado en sus patrones financieros actuales. Usa exclusivamente la información numérica que se te proporciona.</contexto>

<datos_presente>
- Nombre del usuario: {nombre}
- Tono preferido: {tono}
- Score financiero actual: {score_inicial} / 100
- Capacidad de ahorro actual (mensual): S/ {ahorro_actual:,.2f}
- Ahorro optimizado (mensual): S/ {ahorro_optimizado:,.2f}
- Diferencia neta proyectada en 12 meses: S/ {diferencia_12m:,.2f} adicionales
</datos_presente>

<proyeccion_continuidad>
- Score en 12 meses: {score_cont_12m} / 100
- Metas logradas: {_lista(cumplidas_cont)}
- Metas fracasadas: {_lista(fracasadas_cont)}
</proyeccion_continuidad>

<proyeccion_transformacion>
- Score en 12 meses: {score_trans_12m} / 100
- Metas logradas: {_lista(cumplidas_trans)}
- Metas fracasadas: {_lista(fracasadas_trans)}
</proyeccion_transformacion>

<tarea>
Escribe la respuesta estructurada de acuerdo a la tarea:
1. pensamiento_interno_ia: Razonamiento lógico breve sobre el futuro financiero proyectado del usuario. Mantenlo en máximo 1-2 oraciones.
2. score_salud_espejo: Califica del 1 al 10 el potencial de mejora/viabilidad del usuario basándote en la diferencia entre continuidad y transformación.
3. etiquetas_internas: Lista de 1 a 3 etiquetas cortas (ej: 'potencial_alto', 'riesgo_estancamiento').
4. nota_interna_coach: Directiva para monitorear el ahorro no esencial del usuario.
5. cartaContinuidad: Escribe una carta narrativa (máximo 5 oraciones, 120 palabras), en segunda persona, en tiempo presente, dirigida directamente a {nombre}. Describe su futuro si CONTINÚA con sus hábitos actuales (score {score_cont_12m}, ahorro S/ {ahorro_actual:,.2f}/mes, nombrando metas cumplidas/fracasadas). Saluda con su nombre y despídete.
6. cartaTransformacion: Escribe una carta narrativa (máximo 5 oraciones, 120 palabras), en segunda persona, en tiempo presente, dirigida directamente a {nombre}. Describe su futuro si REDUCE sus gastos no esenciales (ahorrando S/ {ahorro_optimizado:,.2f}/mes, acumulando S/ {diferencia_12m:,.2f} extra, nombrando las metas logradas). Saluda con su nombre y despídete.
</tarea>

<restricciones>
1. Eres LUKA, habla en tono {tono}.
2. Usa SOLO los números proporcionados. Jamás inventes cifras.
3. No uses Markdown, viñetas ni subtítulos dentro de las cartas. Solo prosa vivencial y emocional fluida.
</restricciones>"""
