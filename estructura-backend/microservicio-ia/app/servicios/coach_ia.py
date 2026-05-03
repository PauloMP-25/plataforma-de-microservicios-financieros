"""
servicios/coach_ia.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Coach Financiero: recibe el InsightAnalitico del Motor Analítico
y usa Gemini para convertirlo en un consejo humano y accionable.
 
Responsabilidades (y SOLO estas):
  1. Construir el prompt a partir del InsightAnalitico (no del DataFrame crudo).
  2. Llamar a Gemini y obtener el texto del consejo.
  3. Construir el KpiWidget para el header del Dashboard.
  4. Ensamblar la RespuestaModulo final.
 
Principio fundamental:
  Gemini NUNCA ve el historial de transacciones directamente.
  Solo recibe el resumen técnico calculado por el Motor Analítico.
  Esto reduce tokens, mejora la coherencia y evita alucinaciones sobre datos.
 
Patrón de uso:
    coach = CoachIA()
    respuesta = coach.generar_respuesta(usuario_id, insight, grafico)
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
 
import json
import logging
from typing import Optional, Tuple
 
import google.generativeai as genai
from google.api_core import exceptions as google_exceptions
from google.generativeai.types import GenerationConfig
 
from app.configuracion import Configuracion, obtener_configuracion
from app.excepciones import (
    AnalisisFinancieroError,
    GeminiAutenticacionError,
    GeminiCuotaExcedidaError,
    GeminiFiltroSeguridadError,
)
from app.modelos.esquemas import (
    EstadoCoach,
    InsightAnalitico,
    KpiWidget,
    MetadataGrafico,
    NivelRiesgo,
    NombreModulo,
    RespuestaModulo,
    TipoMovimiento,
)
 
logger = logging.getLogger(__name__)


# ── Instrucciones de tono para universitarios peruanos ───────────────────────
_TONO_BASE = (
    "Eres el coach financiero personal de LUKA, una app diseñada para universitarios peruanos. "
    "Tu estilo: cercano, honesto y motivador. "
    "Hablas en español peruano neutro (sin jerga extrema). "
    "Nunca repites los números que ya están en los datos; los interpretas y les das contexto. "
    "Máximo 180 palabras. Sin markdown pesado: solo saltos de línea y guiones simples."
)



# ── System prompts específicos por módulo ────────────────────────────────────
_SYSTEM_PROMPTS: dict[NombreModulo, str] = {
    NombreModulo.CLASIFICAR: (
        "Eres un experto en taxonomía financiera personal. "
        "Tu tarea: evaluar si la categoría asignada es la más útil para el análisis futuro "
        "y explicar por qué categorizar bien importa para los universitarios."
    ),
    NombreModulo.PREDECIR_GASTOS: (
        "Eres un analista predictivo. Tienes el resumen de los últimos meses. "
        "Tu tarea: interpretar la proyección calculada, explicar si la tendencia es preocupante "
        "y sugerir UNA acción concreta para esta semana antes de que llegue el próximo mes."
    ),
    NombreModulo.DETECTAR_ANOMALIAS: (
        "Eres un auditor financiero personal. "
        "Tu tarea: explicar los gastos inusuales detectados, evaluar si son riesgosos o justificados, "
        "y dar UNA recomendación preventiva para el próximo mes."
    ),
    NombreModulo.OPTIMIZAR_SUSCRIPCIONES: (
        "Eres un cazador de gastos ocultos especializado en universitarios. "
        "Tu tarea: interpretar el impacto de los gastos hormiga detectados y sugerir "
        "cuál de ellos podría eliminarse sin afectar la calidad de vida."
    ),
    NombreModulo.CAPACIDAD_AHORRO: (
        "Eres un planificador financiero especializado en presupuestos para universitarios. "
        "Tu tarea: evaluar la capacidad de ahorro real vs. el objetivo 50/30/20, "
        "y proponer 2 ajustes concretos y alcanzables para mejorarla."
    ),
    NombreModulo.SIMULAR_META: (
        "Eres un motivador financiero especializado en metas de mediano plazo. "
        "Tu tarea: interpretar la proyección de la meta, celebrar el avance si lo hay, "
        "y sugerir si el ritmo actual es suficiente o si necesita acelerarse."
    ),
    NombreModulo.ESTACIONALIDAD: (
        "Eres un analista de comportamiento financiero estacional. "
        "Tu tarea: explicar los patrones cíclicos detectados y ayudar al universitario "
        "a planificar con anticipación los meses de mayor gasto."
    ),
    NombreModulo.PRESUPUESTO_DINAMICO: (
        "Eres un gestor de presupuesto adaptativo para universitarios. "
        "Tu tarea: presentar el presupuesto semanal de forma motivadora, "
        "explicar en qué categorías debe ser más cuidadoso y por qué el control semanal "
        "es más efectivo que el mensual."
    ),
    NombreModulo.SIMULAR_ESCENARIO: (
        "Eres un asesor financiero que ayuda a tomar decisiones informadas. "
        "Tu tarea: interpretar el impacto del escenario hipotético, explicar si el balance "
        "resultante es saludable y dar una recomendación clara: ¿proceder o no?"
    ),
    NombreModulo.REPORTE_COMPLETO: (
        "Eres el CFO personal del universitario. Tienes todos sus datos del período. "
        "Tu tarea: elaborar un resumen ejecutivo de 2 minutos de lectura: "
        "qué salió bien, qué salió mal, los 2 riesgos principales y las 2 oportunidades "
        "de mejora más rentables. Sé directo y usa los datos calculados."
    ),
}

class CoachIA:
    """
    Integración con Google Gemini para el módulo de consejo financiero.
 
    Instanciar UNA sola vez en el ciclo de vida del microservicio (singleton).
    El modelo de Gemini se inicializa una sola vez en __init__.
 
    Flujo:
        insight = motor_ia.analizar_X(df, config)
        respuesta = coach.generar_respuesta(usuario_id, insight, grafico)
    """
 
    def __init__(self) -> None:
        config = obtener_configuracion()
        genai.configure(api_key=config.gemini_api_key)
        self._modelo = genai.GenerativeModel(config.gemini_modelo)
        self._config_generacion = GenerationConfig(
            max_output_tokens=config.gemini_max_tokens,
            temperature=config.gemini_temperatura,
        )
        logger.info(
            "[COACH-IA] Inicializado | modelo=%s | temp=%.1f | max_tokens=%d",
            config.gemini_modelo,
            config.gemini_temperatura,
            config.gemini_max_tokens,
        )
 
    # ══════════════════════════════════════════════════════════════════════════
    # MÉTODO PRINCIPAL
    # ══════════════════════════════════════════════════════════════════════════
 
    def generar_respuesta(
        self,
        usuario_id: str,
        insight: InsightAnalitico,
        grafico: Optional[MetadataGrafico] = None,
    ) -> RespuestaModulo:
        """
        Orquesta la generación del consejo financiero con degradación elegante.
 
        Garantía: este método NUNCA lanza excepción por fallos de Gemini.
        Si Gemini falla por cualquier motivo, retorna la respuesta con
        consejo=None y estado_coach indicando el motivo exacto.
        El insight, gráfico y kpi siempre están presentes.
 
        Parámetros
        ----------
        usuario_id : ID del usuario (para trazabilidad).
        insight    : Resumen técnico producido por el Motor Analítico.
        grafico    : Metadata del gráfico (opcional, calculada por el Motor).
 
        Retorna
        -------
        RespuestaModulo siempre completa:
          - consejo=str + estado_coach=EXITOSO  → Gemini respondió.
          - consejo=None + estado_coach=MOTIVO  → Gemini falló; datos siguen presentes.
        """
        kpi = self._construir_kpi(insight)
        consejo, estado_coach = self._intentar_consejo(insight)
 
        respuesta = RespuestaModulo(
            usuario_id=usuario_id,
            modulo=insight.modulo,
            consejo=consejo,
            estado_coach=estado_coach,
            insight=insight,
            grafico=grafico,
            kpi=kpi,
        )
 
        logger.info(
            "[COACH-IA] Respuesta generada | usuario=%s | módulo=%s | coach=%s",
            usuario_id,
            insight.modulo.value,
            estado_coach.value,
        )
        return respuesta
 
    def _intentar_consejo(
        self,
        insight: InsightAnalitico,
    ) -> Tuple[Optional[str], EstadoCoach]:
        """
        Intenta obtener el consejo de Gemini y captura todos los fallos
        posibles, retornando siempre (consejo_o_None, EstadoCoach).
 
        Nunca propaga excepciones: el motor analítico no debe verse
        afectado por la disponibilidad de Gemini.
        """
        try:
            prompt = self._construir_prompt(insight)
            consejo = self._llamar_gemini(prompt, insight.modulo)
            return consejo, EstadoCoach.EXITOSO
 
        except GeminiCuotaExcedidaError:
            logger.warning(
                "[COACH-IA] Cuota agotada — consejo omitido | módulo=%s | usuario retiene datos analíticos.",
                insight.modulo.value,
            )
            return None, EstadoCoach.CUOTA_AGOTADA
 
        except GeminiAutenticacionError:
            logger.error(
                "[COACH-IA] API Key inválida — consejo omitido | módulo=%s",
                insight.modulo.value,
            )
            return None, EstadoCoach.AUTH_ERROR
 
        except GeminiFiltroSeguridadError:
            logger.warning(
                "[COACH-IA] Filtro de seguridad de Gemini — consejo omitido | módulo=%s",
                insight.modulo.value,
            )
            return None, EstadoCoach.NO_DISPONIBLE
 
        except AnalisisFinancieroError as exc:
            # Timeout u otros errores técnicos de la API
            es_timeout = "DeadlineExceeded" in str(exc.detalles or "")
            estado = EstadoCoach.TIMEOUT if es_timeout else EstadoCoach.NO_DISPONIBLE
            logger.error(
                "[COACH-IA] Error técnico (%s) — consejo omitido | módulo=%s",
                estado.value, insight.modulo.value,
            )
            return None, estado
 
        except Exception as exc:
            logger.error(
                "[COACH-IA] Error inesperado — consejo omitido | módulo=%s | %s",
                insight.modulo.value, str(exc), exc_info=True,
            )
            return None, EstadoCoach.NO_DISPONIBLE
 
    # ══════════════════════════════════════════════════════════════════════════
    # CONSTRUCCIÓN DEL PROMPT
    # ══════════════════════════════════════════════════════════════════════════
 
    def _construir_prompt(self, insight: InsightAnalitico) -> str:
        """
        Ensambla el prompt completo a partir del InsightAnalitico.
 
        Estructura:
          [ROL + TONO BASE]
          [SYSTEM PROMPT ESPECÍFICO DEL MÓDULO]
          [RESUMEN FINANCIERO CALCULADO]
          [HALLAZGOS DEL MÓDULO]
          [NIVEL DE ALERTA]
          [INSTRUCCIÓN DE TAREA]
        """
        system_prompt = _SYSTEM_PROMPTS.get(insight.modulo, _SYSTEM_PROMPTS[NombreModulo.REPORTE_COMPLETO])
 
        # ── Sección de métricas generales ─────────────────────────────────────
        seccion_metricas = self._formatear_metricas(insight)
 
        # ── Sección de hallazgos específicos ──────────────────────────────────
        seccion_hallazgos = self._formatear_hallazgos(insight)
 
        # ── Instrucción de alerta ─────────────────────────────────────────────
        seccion_alerta = self._formatear_alerta(insight.nivel_alerta)
 
        # ── Instrucción de tarea final ────────────────────────────────────────
        seccion_tarea = self._formatear_tarea(insight.modulo)
 
        secciones = [
            f"# ROL\n{_TONO_BASE}\n\n## Misión específica\n{system_prompt}",
            seccion_metricas,
            seccion_hallazgos,
            seccion_alerta,
            seccion_tarea,
        ]
 
        prompt = "\n\n".join(s for s in secciones if s and s.strip())
        logger.debug(
            "[COACH-IA] Prompt construido | módulo=%s | %d chars",
            insight.modulo.value,
            len(prompt),
        )
        return prompt
 
    def _formatear_metricas(self, insight: InsightAnalitico) -> str:
        """Formatea las métricas financieras generales para el prompt."""
        lineas = [
            "## Contexto financiero del período",
            f"- Período analizado: {insight.periodo_analizado}",
            f"- Total transacciones: {insight.total_transacciones_analizadas}",
            f"- Total ingresos: S/ {insight.total_ingresos:,.2f}",
            f"- Total gastos: S/ {insight.total_gastos:,.2f}",
            f"- Balance neto: S/ {insight.balance_neto:,.2f}",
        ]
        if insight.promedio_gasto_mensual > 0:
            lineas.append(f"- Gasto mensual promedio: S/ {insight.promedio_gasto_mensual:,.2f}")
        if insight.promedio_ingreso_mensual > 0:
            lineas.append(f"- Ingreso mensual promedio: S/ {insight.promedio_ingreso_mensual:,.2f}")
        return "\n".join(lineas)
 
    def _formatear_hallazgos(self, insight: InsightAnalitico) -> str:
        """
        Convierte los hallazgos del módulo en texto legible para Gemini.
        Los diccionarios anidados se serializan a JSON compacto para ahorrar tokens.
        """
        if not insight.hallazgos:
            return ""
 
        lineas = ["## Hallazgos del análisis"]
        for clave, valor in insight.hallazgos.items():
            clave_legible = clave.replace("_", " ").capitalize()
            if isinstance(valor, list):
                if valor:
                    if isinstance(valor[0], dict):
                        # Lista de dicts: serializar compacto
                        lineas.append(f"- {clave_legible}: {json.dumps(valor[:5], ensure_ascii=False)}")
                    else:
                        lineas.append(f"- {clave_legible}: {', '.join(str(v) for v in valor[:5])}")
                else:
                    lineas.append(f"- {clave_legible}: (ninguno)")
            elif isinstance(valor, dict):
                lineas.append(f"- {clave_legible}: {json.dumps(valor, ensure_ascii=False)}")
            elif isinstance(valor, float):
                lineas.append(f"- {clave_legible}: {valor:,.2f}")
            else:
                lineas.append(f"- {clave_legible}: {valor}")
 
        return "\n".join(lineas)
 
    def _formatear_alerta(self, nivel: NivelRiesgo) -> str:
        """Genera la sección de alerta según el nivel de riesgo calculado."""
        mensajes = {
            NivelRiesgo.BAJO: (
                "## Nivel de alerta: BAJO\n"
                "La situación financiera es estable. Enfoca el consejo en optimización y crecimiento."
            ),
            NivelRiesgo.MEDIO: (
                "## Nivel de alerta: MEDIO\n"
                "Hay señales de alerta moderadas. Menciona el riesgo con tacto y sugiere ajustes preventivos."
            ),
            NivelRiesgo.ALTO: (
                "## Nivel de alerta: ALTO\n"
                "Situación financiera preocupante. Sé directo sobre el problema sin alarmismo excesivo. "
                "Propón 2 acciones concretas para esta semana."
            ),
            NivelRiesgo.CRITICO: (
                "## Nivel de alerta: CRÍTICO\n"
                "Situación financiera crítica (balance negativo proyectado). "
                "Prioriza la estabilización sobre cualquier otra recomendación. Sé claro y urgente."
            ),
        }
        return mensajes.get(nivel, mensajes[NivelRiesgo.BAJO])
 
    def _formatear_tarea(self, modulo: NombreModulo) -> str:
        """Instrucción de cierre para que Gemini sepa exactamente qué producir."""
        tareas = {
            NombreModulo.CLASIFICAR: (
                "Genera un análisis breve: (1) si la categoría actual es correcta o sugieres otra, "
                "(2) por qué importa clasificar bien. Máx. 100 palabras."
            ),
            NombreModulo.PREDECIR_GASTOS: (
                "Genera la predicción en lenguaje natural: (1) cuánto gastarás este mes según la tendencia, "
                "(2) si es más o menos que el mes anterior y por qué importa, "
                "(3) UNA acción concreta para esta semana. Máx. 150 palabras."
            ),
            NombreModulo.DETECTAR_ANOMALIAS: (
                "Explica los gastos inusuales: (1) cuántos encontraste y en qué categorías, "
                "(2) si son preocupantes o justificables, "
                "(3) qué hacer al respecto. Máx. 150 palabras."
            ),
            NombreModulo.OPTIMIZAR_SUSCRIPCIONES: (
                "Presenta los gastos hormiga: (1) cuánto suman al mes y al año, "
                "(2) cuál cancelarías primero y por qué, "
                "(3) qué harías con ese dinero recuperado. Máx. 150 palabras."
            ),
            NombreModulo.CAPACIDAD_AHORRO: (
                "Evalúa la capacidad de ahorro: (1) cómo está vs. la regla 50/30/20, "
                "(2) dos ajustes concretos para mejorar el porcentaje este mes. Máx. 160 palabras."
            ),
            NombreModulo.SIMULAR_META: (
                "Interpreta la proyección de la meta: (1) si va a tiempo o retrasado, "
                "(2) qué tan difícil es el ritmo requerido, "
                "(3) un consejo motivador y realista. Máx. 150 palabras."
            ),
            NombreModulo.ESTACIONALIDAD: (
                "Explica los patrones estacionales: (1) en qué meses gastas más y por qué, "
                "(2) cómo prepararse para los próximos meses pico, "
                "(3) una estrategia de ahorro estacional. Máx. 160 palabras."
            ),
            NombreModulo.PRESUPUESTO_DINAMICO: (
                "Presenta el presupuesto semanal: (1) cuánto tienes para esta semana, "
                "(2) en qué categorías ser más cuidadoso, "
                "(3) por qué el control semanal funciona mejor para universitarios. Máx. 150 palabras."
            ),
            NombreModulo.SIMULAR_ESCENARIO: (
                "Evalúa el escenario: (1) cómo cambia tu balance si lo implementas, "
                "(2) si es viable financieramente, "
                "(3) recomendación clara: ¿proceder, ajustar o esperar? Máx. 150 palabras."
            ),
            NombreModulo.REPORTE_COMPLETO: (
                "Genera el reporte ejecutivo mensual: (1) resumen de 2 líneas del período, "
                "(2) lo que salió bien, (3) lo que salió mal, "
                "(4) los 2 riesgos principales, (5) las 2 oportunidades de mejora. "
                "Termina con un puntaje de salud financiera y una frase de acción. Máx. 200 palabras."
            ),
        }
        tarea = tareas.get(
            modulo,
            "Genera un consejo financiero claro, concreto y accionable basado en los datos. Máx. 180 palabras.",
        )
        return f"## Tarea\n{tarea}"
 
    # ══════════════════════════════════════════════════════════════════════════
    # LLAMADA A GEMINI
    # ══════════════════════════════════════════════════════════════════════════
 
    def _llamar_gemini(self, prompt: str, modulo: NombreModulo) -> str:
        """
        Envía el prompt a Gemini y retorna el texto del consejo.
 
        Propaga excepciones tipadas para que el router las maneje correctamente:
          - GeminiCuotaExcedidaError  → 503 (rate limit)
          - GeminiAutenticacionError  → 500 (config error)
          - AnalisisFinancieroError   → 503 (error técnico)
        """
        try:
            logger.debug(
                "[GEMINI] Enviando prompt | módulo=%s | %d chars",
                modulo.value,
                len(prompt),
            )
            respuesta = self._modelo.generate_content(
                prompt,
                generation_config=self._config_generacion,
            )
 
            # Verificamos que Gemini no haya bloqueado la respuesta por seguridad
            if not respuesta.candidates:
                raise GeminiFiltroSeguridadError(
                    "Gemini bloqueó la respuesta. Revisa el contenido del prompt.",
                    detalles={"modulo": modulo.value},
                )
 
            texto = respuesta.text.strip()
            logger.debug("[GEMINI] Respuesta recibida | %d chars", len(texto))
            return texto
 
        except google_exceptions.ResourceExhausted:
            logger.warning("[GEMINI] Cuota agotada (429) | módulo=%s", modulo.value)
            raise GeminiCuotaExcedidaError()
 
        except google_exceptions.Unauthenticated:
            logger.error("[GEMINI] API Key inválida | módulo=%s", modulo.value)
            raise GeminiAutenticacionError()
 
        except (google_exceptions.InvalidArgument, google_exceptions.DeadlineExceeded) as exc:
            logger.error("[GEMINI] Error técnico | módulo=%s | %s", modulo.value, str(exc))
            raise AnalisisFinancieroError(
                "Error técnico al conectar con el motor de IA.",
                detalles=str(exc),
            )
 
        except (GeminiFiltroSeguridadError, GeminiCuotaExcedidaError, GeminiAutenticacionError):
            raise  # Excepciones propias: las captura _intentar_consejo
 
        except Exception as exc:
            logger.error(
                "[GEMINI] Error inesperado | módulo=%s | %s",
                modulo.value, str(exc), exc_info=True,
            )
            raise AnalisisFinancieroError(
                "Error inesperado en el motor de IA.",
                detalles=str(exc),
            )
 
    # ══════════════════════════════════════════════════════════════════════════
    # CONSTRUCCIÓN DEL KPI PARA EL DASHBOARD
    # ══════════════════════════════════════════════════════════════════════════
 
    def _construir_kpi(self, insight: InsightAnalitico) -> Optional[KpiWidget]:
        """
        Extrae el KPI más relevante de los hallazgos para mostrar en el
        header del widget del Dashboard de LUKA.
 
        Cada módulo define su propio KPI principal.
        """
        hallazgos = insight.hallazgos
        modulo = insight.modulo
 
        try:
            if modulo == NombreModulo.PREDECIR_GASTOS:
                proyeccion = hallazgos.get("gasto_proyectado", 0)
                variacion = hallazgos.get("variacion_vs_mes_anterior_pct", 0)
                tendencia = "subida" if variacion > 0 else ("bajada" if variacion < 0 else "estable")
                return KpiWidget(
                    valor=proyeccion,
                    etiqueta="Gasto proyectado próximo mes",
                    unidad="S/",
                    tendencia=tendencia,
                )
 
            if modulo == NombreModulo.DETECTAR_ANOMALIAS:
                total = hallazgos.get("total_anomalias", 0)
                monto = hallazgos.get("monto_total_anomalo", 0.0)
                return KpiWidget(
                    valor=float(total),
                    etiqueta=f"Gastos inusuales — S/ {monto:,.2f} en total",
                    unidad="transacciones",
                    tendencia="subida" if total > 3 else "estable",
                )
 
            if modulo == NombreModulo.OPTIMIZAR_SUSCRIPCIONES:
                impacto_anual = hallazgos.get("impacto_anual_estimado", 0.0)
                return KpiWidget(
                    valor=impacto_anual,
                    etiqueta="Impacto anual estimado en gastos hormiga",
                    unidad="S/",
                    tendencia="subida",
                )
 
            if modulo == NombreModulo.CAPACIDAD_AHORRO:
                pct = hallazgos.get("porcentaje_ahorro_real", 0.0)
                cumple = hallazgos.get("cumple_regla_50_30_20", False)
                return KpiWidget(
                    valor=pct,
                    etiqueta="Tasa de ahorro actual" + (" ✓" if cumple else " (bajo objetivo)"),
                    unidad="%",
                    tendencia="bajada" if not cumple else "estable",
                )
 
            if modulo == NombreModulo.SIMULAR_META:
                meses = hallazgos.get("meses_para_alcanzar", 0)
                return KpiWidget(
                    valor=float(meses),
                    etiqueta=f"Meses para alcanzar: {hallazgos.get('nombre_meta', 'la meta')}",
                    unidad="meses",
                    tendencia="estable",
                )
 
            if modulo == NombreModulo.ESTACIONALIDAD:
                cv = hallazgos.get("coeficiente_variacion_pct", 0.0)
                return KpiWidget(
                    valor=cv,
                    etiqueta="Coeficiente de variación estacional",
                    unidad="%",
                    tendencia="subida" if cv > 25 else "estable",
                )
 
            if modulo == NombreModulo.PRESUPUESTO_DINAMICO:
                semanal = hallazgos.get("presupuesto_semanal_total", 0.0)
                return KpiWidget(
                    valor=semanal,
                    etiqueta="Presupuesto recomendado esta semana",
                    unidad="S/",
                    tendencia="estable",
                )
 
            if modulo == NombreModulo.SIMULAR_ESCENARIO:
                impacto = hallazgos.get("impacto_mensual", 0.0)
                tendencia = "bajada" if impacto < 0 else "subida"
                return KpiWidget(
                    valor=abs(impacto),
                    etiqueta="Impacto mensual del escenario",
                    unidad="S/",
                    tendencia=tendencia,
                )
 
            if modulo == NombreModulo.REPORTE_COMPLETO:
                score = hallazgos.get("salud_financiera_score", 0.0)
                clasificacion = hallazgos.get("clasificacion_salud", "")
                return KpiWidget(
                    valor=score,
                    etiqueta=f"Salud financiera: {clasificacion}",
                    unidad="/ 100",
                    tendencia="estable",
                )
 
        except Exception as exc:
            logger.debug(
                "[COACH-IA] No se pudo construir KPI para módulo %s: %s",
                modulo.value, str(exc),
            )
 
        # KPI genérico: balance neto del período
        return KpiWidget(
            valor=round(insight.balance_neto, 2),
            etiqueta="Balance neto del período",
            unidad="S/",
            tendencia="bajada" if insight.balance_neto < 0 else "subida",
        )