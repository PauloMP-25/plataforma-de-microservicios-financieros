"""
servicios/ingeniero_prompt.py  ·  v3 — Multi-Módulos + Historial Comparativo
══════════════════════════════════════════════════════════════════════════════
Arquitectura de Estrategia de Prompts:
 
  IngenierioPrompt.construir(evento, modulo) → str
      │
      ├── _system_prompt_para(modulo)    → instrucción de rol específica
      ├── _seccion_tono(contexto)        → estilo de comunicación
      ├── _seccion_perfil(contexto)      → quién es el usuario
      ├── _seccion_historial(evento)     ← NUEVO: comparativa 6 meses
      │     ├── _comparar_meses()        → delta%, categoría con exceso
      │     └── _tendencia_gastos()      → proyección lineal textual
      ├── _seccion_transaccion(evento)   → operación actual (si aplica)
      ├── _seccion_metas(contexto)       → progreso hacia objetivos
      ├── _seccion_limite(evento)        → alerta de presupuesto
      └── _seccion_tarea(modulo)        → instrucción final por módulo
 
Cada TipoModulo tiene su propio System Prompt especializado que le dice
a Gemini exactamente qué rol asumir y qué análisis realizar.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from typing import List, Optional, Dict

from app.modelos.evento_analisis import (
    ContextoUsuario,
    EventoAnalisisIA,
    ResumenMes,
    TipoModulo,
    TipoMovimiento,
    TonoIA,
)

logger = logging.getLogger(__name__)


# ── Mapeo de tono → instrucciones de estilo ───────────────────────────────────

_SYSTEM_PROMPTS: dict[TipoModulo, str] = {
 
    TipoModulo.TRANSACCION_AUTOMATICA: (
        "Eres un coach financiero personal que recibe una alerta en tiempo real "
        "sobre una transacción que acaba de registrar el usuario. Tu trabajo es "
        "analizarla en el contexto de sus finanzas actuales y entregar un consejo "
        "inmediato, breve y accionable. Actúa como el mejor amigo financiero del usuario."
    ),

    TipoModulo.PREDICCION_GASTOS: (
        "Eres un analista financiero predictivo experto en series temporales y "
        "comportamiento del consumidor latinoamericano. Tienes acceso al historial "
        "de los últimos 6 meses del usuario. Tu tarea es proyectar cómo cerrará "
        "el mes actual en términos de gastos, identificar si la tendencia es "
        "alcista o bajista, y recomendar ajustes concretos ANTES de que termine el mes. "
        "Habla de números reales, no de generalidades."
    ),
 
    TipoModulo.GASTO_HORMIGA: (
        "Eres un cazador de gastos ocultos especializado en micro-transacciones. "
        "Tu misión es identificar los 'gastos hormiga': pagos pequeños y recurrentes "
        "que individualmente parecen insignificantes pero que sumados representan "
        "un drenaje significativo del presupuesto mensual. "
        "Ejemplos típicos: café diario, suscripciones olvidadas, snacks, apps de delivery. "
        "Presenta el impacto TOTAL en soles y el equivalente anualizado. "
        "Sé específico con los nombres de los comercios o categorías detectadas."
    ),

    TipoModulo.AUTOCLASIFICACION: (
        "Eres un sistema experto en taxonomía financiera personal. "
        "El usuario registró una transacción con una categoría asignada por el sistema. "
        "Tu trabajo es: (1) evaluar si esa categoría es la más precisa y útil, "
        "(2) si no lo es, sugerir una categoría alternativa mejor con justificación, "
        "(3) explicar por qué una categorización correcta mejora la precisión "
        "de los análisis financieros futuros. "
        "Sé didáctico pero conciso."
    ),
 
    TipoModulo.COMPARACION_MENSUAL: (
        "Eres un analista de tendencias financieras personales. "
        "Compara el mes actual con el mes anterior del usuario. "
        "Si los gastos bajaron: celebra el logro, identifica qué categorías mejoraron "
        "y refuerza el comportamiento positivo. "
        "Si los gastos subieron: identifica con precisión en qué categoría ocurrió "
        "el exceso, cuánto representa ese exceso, y sugiere una acción correctiva "
        "específica y realista para la semana siguiente."
    ),
 
    TipoModulo.CAPACIDAD_AHORRO: (
        "Eres un planificador financiero personal especializado en optimización "
        "del ahorro para economías emergentes. Calcula la capacidad de ahorro "
        "real del usuario basándote en su historial de 6 meses, no en un solo mes. "
        "Aplica la regla 50/30/20 como referencia y muestra qué tan lejos o cerca "
        "está el usuario. Proporciona un plan de 3 pasos concretos para mejorar."
    ),

    TipoModulo.METAS_FINANCIERAS: (
        "Eres un motivador financiero especializado en metas de ahorro a mediano plazo. "
        "Analiza el progreso del usuario hacia sus metas, cruza esa información con "
        "su ritmo de ahorro actual del historial y proyecta si alcanzará las metas "
        "a tiempo. Si va retrasado, sugiere ajustes realistas al presupuesto mensual."
    ),
 
    TipoModulo.ANOMALIAS: (
        "Eres un auditor financiero personal con experiencia en detección de fraudes "
        "y gastos irregulares. Analiza el historial del usuario buscando transacciones "
        "o patrones que se desvíen significativamente de su comportamiento normal. "
        "Clasifica las anomalías por nivel de riesgo (alto/medio/bajo) y sugiere "
        "acciones concretas para cada una."
    ),
 
    TipoModulo.ESTACIONALIDAD: (
        "Eres un analista de comportamiento financiero estacional. "
        "Identifica patrones cíclicos en el historial del usuario: ¿en qué meses "
        "gasta más? ¿Hay picos en fechas especiales? ¿Cuál es su 'mes más barato'? "
        "Usa estos patrones para ayudarle a planificar con anticipación los próximos meses."
    ),

    TipoModulo.PRESUPUESTO_DINAMICO: (
        "Eres un gestor de presupuesto adaptativo. Basándote en el historial de "
        "los últimos 6 meses y el comportamiento de la semana pasada, calcula un "
        "presupuesto semanal y diario realista para el usuario. "
        "El presupuesto debe ser ambicioso pero alcanzable, con límites por categoría "
        "basados en los patrones históricos reales del usuario."
    ),
 
    TipoModulo.REPORTE_COMPLETO: (
        "Eres el CFO (Director Financiero) personal del usuario. "
        "Tienes acceso a todo su historial financiero de los últimos 6 meses. "
        "Tu tarea es elaborar un resumen ejecutivo mensual que un CEO leería en "
        "2 minutos: qué salió bien, qué salió mal, cuáles son los 3 riesgos "
        "principales y cuáles son las 3 oportunidades de mejora más rentables. "
        "Sé directo, usa datos concretos y termina con un plan de acción prioritizado."
    ),
}

# ── Mapeo tono → instrucción de estilo ───────────────────────────────────────
_INSTRUCCIONES_TONO: dict[TonoIA, str] = {

    TonoIA.MOTIVADOR: (
        "Usa un tono energético y alentador. Celebra cada avance, transforma "
        "los obstáculos en retos superables y termina con una frase de impulso positivo."
    ),

    TonoIA.FORMAL: (
        "Usa un tono profesional y preciso. Presenta los datos con objetividad, "
        "evita coloquialismos y estructura las recomendaciones con lenguaje técnico "
        "pero comprensible."
    ),
    TonoIA.AMIGABLE: (
        "Usa un tono cercano y conversacional, como un amigo que conoce de finanzas. "
        "Puedes usar emojis con moderación (máx. 2) y un lenguaje natural y cálido."
    ),
    TonoIA.DIRECTO: (
        "Sé conciso y al punto. Sin rodeos: datos primero, recomendación después. "
        "Máximo 3 oraciones por sección. Elimina cualquier relleno innecesario."
    ),
    TonoIA.EMPÁTICO: (
        "Reconoce primero cómo puede sentirse el usuario ante este gasto. "
        "Valida sus emociones antes de pasar al análisis. El bienestar emocional "
        "es tan importante como la salud financiera."
    ),
}

_TONO_DEFAULT = _INSTRUCCIONES_TONO[TonoIA.AMIGABLE]


# ══════════════════════════════════════════════════════════════════════════════
# CLASE PRINCIPAL
# ══════════════════════════════════════════════════════════════════════════════
class Ingeniero_Prompt:
    """
    Constructor modular de prompts. 
    Diseñado para transformar datos históricos en consejos accionables.
    Uso:
        ingeniero = IngenierioPrompt()
        prompt = ingeniero.construir(evento, TipoModulo.PREDICCION_GASTOS)
        respuesta = gemini.generate_content(prompt)
    """

    # ── Punto de entrada público ──────────────────────────────────────────────

    def construir(
            self, 
            evento: EventoAnalisisIA,
            modulo: TipoModulo = TipoModulo.TRANSACCION_AUTOMATICA,
        ) -> str:
        """
        Ensambla el prompt completo para el módulo solicitado.
        Omite secciones vacías automáticamente.
        """
        secciones = [
            self._system_prompt_para(modulo),
            self._seccion_tono(evento.contexto),
            self._seccion_perfil(evento.contexto),
            self._seccion_historial(evento),
            self._seccion_transaccion(evento, modulo),
            self._seccion_metas(evento.contexto),
            self._seccion_limite(evento),
            self._seccion_tarea(modulo, evento.contexto),
        ]
        # Filtramos secciones vacías y unimos con doble salto de línea
        prompt = "\n\n".join(s for s in secciones if s and s.strip())
        
        logger.info(f"[INGENIERIO-PROMPT] Módulo generado: {modulo.name}")
        return prompt
    
    # ── System Prompt por módulo ──────────────────────────────────────────────
    def _system_prompt_para(self, modulo: TipoModulo) -> str:
        sistema = _SYSTEM_PROMPTS.get(modulo, _SYSTEM_PROMPTS[TipoModulo.TRANSACCION_AUTOMATICA])
        return (
            f"# ROL Y MISIÓN\n"
            f"{sistema}\n\n"
            "## Restricciones absolutas\n"
            "- Responde SIEMPRE en español.\n"
            "- Máximo 200 palabras en total (el texto se mostrará en un widget del Dashboard).\n"
            "- Sin markdown pesado: usa solo saltos de línea y guiones para listas.\n"
            "- No repitas los datos de entrada textualmente; interprètalos y añade valor.\n"
            "- Si no tienes suficientes datos para una conclusión sólida, dilo honestamente."
        )
    
    # ── Estilo de comunicación ────────────────────────────────────────────────
    def _seccion_tono(self, contexto: ContextoUsuario) -> str:
        instruccion = _TONO_DEFAULT
        if contexto.tiene_perfil and contexto.perfil_financiero:
            instruccion = _INSTRUCCIONES_TONO.get(
                contexto.perfil_financiero.tono_ia, _TONO_DEFAULT
            )
        return f"## Estilo de comunicación\n{instruccion}"

    # ── Perfil del usuario ────────────────────────────────────────────────────
    def _seccion_perfil(self, contexto: ContextoUsuario) -> str:
        if not contexto.tiene_perfil or not contexto.perfil_financiero:
            return (
                "## Perfil del usuario\n"
                "Sin datos de perfil disponibles. Usa un enfoque general."
            )
        p = contexto.perfil_financiero
        ingreso = f"S/ {p.ingreso_mensual:,.2f}" if p.ingreso_mensual > 0 else "no registrado"
        return (
            f"## Perfil del usuario\n"
            f"- Ocupación: {p.ocupacion}\n"
            f"- Ingreso mensual: {ingreso}\n"
            "Adapta cada recomendación a esta realidad económica concreta."
        )
    
    # ── Historial comparativo (NUEVO v3) ──────────────────────────────────────
    def _seccion_historial(self, evento: EventoAnalisisIA) -> str:
        """
        Sección central de la v3: construye el análisis histórico comparativo.
        Incluye tabla de 6 meses, comparación mes actual vs anterior,
        y tendencia detectada.
        """
        if not evento.tiene_historial:
            return "## Historial\nSin datos históricos. Realiza un análisis basado solo en la transacción."
 
        # Ordenamos por fecha para que la IA vea la línea de tiempo correcta
        historial = sorted(evento.historial_mensual, key=lambda m: (m.anio, m.mes))
 
        lineas = ["## Historial financiero (últimos meses)"]
        lineas.append("Período       | Ingresos    | Gastos      | Balance")
        lineas.append("------------- | ----------- | ----------- | -----------")
 
        for m in historial:
            lineas.append(f"{m.periodo_label} | S/ {m.total_ingresos:>8,.2f} | S/ {m.total_gastos:>8,.2f} | S/ {m.balance:>8,.2f}")

        # Añadimos análisis delta si hay al menos 2 meses
        if len(historial) >= 2:
            comparacion = self._comparar_meses(evento)
            if comparacion:
                lineas.append(f"\n### Análisis Comparativo\n{comparacion}")
 
        # Tendencia de 6 meses
        tendencia = self._tendencia_gastos(historial)
        if tendencia:
            lineas.append("")
            lineas.append(f"## Tendencia detectada\n{tendencia}")
 
        return "\n".join(lineas)
    
    #─ Comparación mes actual vs anterior ─────────────────────────────────────
    def _comparar_meses(self, evento: EventoAnalisisIA) -> Optional[str]:
        """
        Genera el texto de comparación entre el mes actual y el anterior.
        Si los gastos bajaron → celebración.
        Si subieron → identifica la categoría con mayor exceso.
        """
        actual   = evento.mes_actual
        anterior = evento.mes_anterior
 
        if not actual or not anterior:
            return None
 
        variacion = actual.variacion_gastos_vs(anterior)
        delta_abs = abs(actual.total_gastos - anterior.total_gastos)
 
        if variacion <= -5:
            return (
                f"Los gastos bajaron un {abs(variacion):.1f}% respecto al mes anterior "
                f"(S/ {delta_abs:,.2f} menos). INSTRUCCIÓN: Felicita al usuario por esta "
                f"mejora y refuerza el comportamiento positivo. Identifica qué categoría "
                f"contribuyó más a la reducción."
            )
        elif variacion >= 5:
            categoria_exceso = actual.categoria_con_mayor_exceso_vs(anterior)
            cat_texto = f" La categoría con mayor exceso es '{categoria_exceso}'." if categoria_exceso else ""
            return (
                f"Los gastos subieron un {variacion:.1f}% respecto al mes anterior "
                f"(S/ {delta_abs:,.2f} más).{cat_texto} "
                f"INSTRUCCIÓN: Señala este exceso con tacto, identifica la causa probable "
                f"y sugiere una acción correctiva concreta para los próximos 7 días."
            )
        else:
            return (
                f"Los gastos se mantuvieron estables ({variacion:+.1f}% vs mes anterior). "
                f"INSTRUCCIÓN: Menciona la estabilidad como algo positivo y sugiere una "
                f"oportunidad de mejora incremental."
            )

    #── Tendencia de gastos ────────────────────────────────────────────────
    def _tendencia_gastos(self, historial: List[ResumenMes]) -> Optional[str]:
        """
        Calcula si la tendencia de gastos de los últimos meses es
        alcista, bajista o estable usando diferencias simples.
        """
        if len(historial) < 3:
            return None
 
        gastos = [m.total_gastos for m in historial[-3:]]  # últimos 3 meses
        diff1 = gastos[1] - gastos[0]
        diff2 = gastos[2] - gastos[1]
 
        if diff1 > 0 and diff2 > 0:
            promedio_incremento = (diff1 + diff2) / 2
            return (
                f"Tendencia ALCISTA: los gastos han aumentado consistentemente "
                f"un promedio de S/ {promedio_incremento:,.2f} por mes en los últimos 3 meses. "
                f"INSTRUCCIÓN: Este patrón debe mencionarse como una señal de alerta."
            )
        elif diff1 < 0 and diff2 < 0:
            promedio_reduccion = abs(diff1 + diff2) / 2
            return (
                f"Tendencia BAJISTA: los gastos han disminuido consistentemente "
                f"un promedio de S/ {promedio_reduccion:,.2f} por mes. "
                f"INSTRUCCIÓN: Destaca este patrón positivo."
            )
        else:
            return "Tendencia VARIABLE: no hay un patrón claro de alza o baja sostenida."
 
    #─ Descripción de la transacción actual ─────────────────────────────────
    def _seccion_transaccion(
        self, evento: EventoAnalisisIA, modulo: TipoModulo
    ) -> str:
        """Incluye la transacción solo si es relevante para el módulo."""
        if not evento.transaccion:
            return ""
 
        # Módulos de historial puro no necesitan el detalle de la transacción
        modulos_sin_transaccion = {
            TipoModulo.PREDICCION_GASTOS,
            TipoModulo.ESTACIONALIDAD,
            TipoModulo.REPORTE_COMPLETO,
            TipoModulo.PRESUPUESTO_DINAMICO,
        }
        if modulo in modulos_sin_transaccion:
            return ""
 
        t = evento.transaccion
        return (
            f"## Transacción analizada\n"
            f"- Tipo: {t.tipo.value}\n"
            f"- Descripción: {t.descripcion}\n"
            f"- Categoría registrada: {t.categoria}\n"
            f"- Monto: S/ {t.monto:,.2f}"
        )
    
    # ── Metas financieras ────────────────────────────────────────────────────
    def _seccion_metas(self, contexto: ContextoUsuario) -> str:
        if not contexto.tiene_metas:
            return (
                "## Metas de ahorro\n"
                "El usuario no tiene metas configuradas. "
                "Si el análisis lo permite, sugiere establecer al menos una meta concreta."
            )
        lineas = ["## Metas de ahorro activas"]
        for meta in contexto.metas:
            barra = self._barra_progreso(meta.progreso_porcentaje)
            lineas.append(
                f"- {meta.nombre}: S/ {meta.monto_actual:,.2f} / S/ {meta.monto_objetivo:,.2f} "
                f"({meta.progreso_porcentaje}%) {barra} "
                f"[faltan S/ {meta.monto_restante:,.2f}]"
            )
        lineas.append(
            "\nRelaciona el análisis con el impacto en estas metas. "
            "Si el gasto detectado aleja al usuario de una meta, menciónalo con tacto."
        )
        return "\n".join(lineas)
    
    # ── Límite global ─────────────────────────────────────────────────────────
 
    def _seccion_limite(self, evento: EventoAnalisisIA) -> str:
        contexto = evento.contexto
        if not contexto.tiene_limite_activo or not evento.transaccion:
            return ""
 
        limite = contexto.limite_global
        monto  = evento.transaccion.monto
        uso_pct = (monto / limite.monto_limite) * 100 if limite.monto_limite > 0 else 0
 
        if uso_pct >= 100:
            estado = "LÍMITE SUPERADO"
            instruccion = "ALERTA CRÍTICA: el usuario ha excedido su presupuesto mensual."
        elif uso_pct >= limite.porcentaje_alerta:
            estado = f"ZONA DE ALERTA ({uso_pct:.1f}%)"
            instruccion = "Advierte suavemente que se acerca al tope mensual."
        else:
            estado = f"Dentro del límite ({uso_pct:.1f}%)"
            instruccion = "Refuerza positivamente que el gasto está bajo control."
 
        return (
            f"## Control de presupuesto global\n"
            f"- Estado: {estado}\n"
            f"- Límite mensual configurado: S/ {limite.monto_limite:,.2f}\n"
            f"- Este gasto representa: {uso_pct:.1f}% del límite\n"
            f"- Instrucción: {instruccion}"
        )
    
    # ── Tarea final por módulo ────────────────────────────────────────────────
    def _seccion_tarea(
        self, modulo: TipoModulo, contexto: ContextoUsuario
    ) -> str:
        """Cierra el prompt con la instrucción de salida específica del módulo."""
        tono = "amigable"
        if contexto.tiene_perfil and contexto.perfil_financiero:
            tono = contexto.perfil_financiero.tono_ia.value.lower()
 
        tareas: dict[TipoModulo, str] = {
            TipoModulo.TRANSACCION_AUTOMATICA: (
                f"Con toda la información anterior, genera UN consejo inmediato "
                f"para el Dashboard con tono {tono}. Estructura: "
                f"(1) Reconoce el gasto, (2) relaciona con historial/metas/límite, "
                f"(3) una recomendación concreta. Máx. 150 palabras."
            ),
            TipoModulo.PREDICCION_GASTOS: (
                f"Genera una predicción de cierre del mes actual con tono {tono}. "
                f"Incluye: (1) gasto proyectado al cierre, (2) si superará o no el mes anterior, "
                f"(3) qué categorías vigilar esta semana. Sé específico con los montos."
            ),
            TipoModulo.GASTO_HORMIGA: (
                f"Genera un reporte de gastos hormiga con tono {tono}. "
                f"Incluye: (1) total mensual estimado de gastos hormiga, "
                f"(2) top 3 categorías hormiga, (3) impacto anualizado, "
                f"(4) una sugerencia de reducción realista."
            ),
            TipoModulo.AUTOCLASIFICACION: (
                f"Genera un análisis de clasificación con tono {tono}. "
                f"Incluye: (1) si la categoría actual es correcta o no, "
                f"(2) categoría sugerida (si aplica) con justificación, "
                f"(3) por qué importa clasificar bien."
            ),
            TipoModulo.COMPARACION_MENSUAL: (
                f"Genera el análisis comparativo con tono {tono}. "
                f"Sigue la instrucción de comparación: felicitar si bajó, "
                f"alertar con acción correctiva si subió."
            ),
        }
 
        tarea = tareas.get(
            modulo,
            f"Genera un análisis financiero claro y accionable para el Dashboard "
            f"con tono {tono}. Máximo 200 palabras, sin markdown."
        )
        return f"## Tarea\n{tarea}"

    # ── Utilidades internas ───────────────────────────────────────────────────

    @staticmethod
    def _barra_progreso(porcentaje: float, ancho: int = 8) -> str:
        llenos = int((porcentaje / 100) * ancho)
        return f"[{'█' * llenos}{'░' * (ancho - llenos)}]"