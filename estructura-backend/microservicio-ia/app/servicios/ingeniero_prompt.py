"""
servicios/ingeniero_prompt.py
══════════════════════════════════════════════════════════════════════════════
Módulo de Ingeniería de Prompt para el Coach Financiero IA.

Responsabilidad ÚNICA: construir el prompt contextualizado que se enviará
a Google Generative AI (Gemini). NO conoce RabbitMQ ni la API de Gemini
directamente; solo transforma un EventoAnalisisIA en un string de prompt.

Principio de diseño:
  - Cada sección del prompt (sistema, contexto, tarea) es un método privado.
  - El método público `construir` orquesta el ensamblado final.
  - Si el contexto es nulo/incompleto, se degrada graciosamente con
    secciones genéricas, nunca lanza excepción.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from app.modelos.evento_analisis import (
    EventoAnalisisIA,
    ContextoUsuario,
    TonoIA,
    TipoMovimiento,
)

logger = logging.getLogger(__name__)


# ── Mapeo de tono → instrucciones de estilo ───────────────────────────────────

_INSTRUCCIONES_TONO: dict[TonoIA, str] = {
    TonoIA.MOTIVADOR: (
        "Usa un tono energético y alentador. Celebra cada pequeño avance, "
        "transforma los obstáculos en retos superables y termina siempre "
        "con una frase de impulso positivo."
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


class Ingeniero_Prompt:
    """
    Construye prompts contextualizados para Gemini actuando como
    coach financiero personalizado.

    Uso:
        ingeniero = IngenierioPrompt()
        prompt = ingeniero.construir(evento)
        respuesta = modelo_gemini.generate_content(prompt)
    """

    # ── Punto de entrada público ──────────────────────────────────────────────

    def construir(self, evento: EventoAnalisisIA) -> str:
        """
        Ensambla el prompt completo desde las secciones.

        Args:
            evento: EventoAnalisisIA deserializado desde RabbitMQ.

        Returns:
            String listo para enviar a Gemini.
        """
        secciones = [
            self._seccion_rol_sistema(evento.contexto),
            self._seccion_transaccion(evento),
            self._seccion_contexto_financiero(evento.contexto),
            self._seccion_metas(evento.contexto),
            self._seccion_limite_presupuesto(evento),
            self._seccion_tarea_final(evento.contexto),
        ]

        prompt = "\n\n".join(s for s in secciones if s)  # omite secciones vacías

        logger.debug("[PROMPT] Prompt construido (%d chars)", len(prompt))
        return prompt

    # ── Secciones privadas ────────────────────────────────────────────────────

    def _seccion_rol_sistema(self, contexto: ContextoUsuario) -> str:
        """Define el rol del modelo y el estilo de comunicación."""
        tono_instruccion = _TONO_DEFAULT

        if contexto.tiene_perfil and contexto.perfil_financiero:
            tono_instruccion = _INSTRUCCIONES_TONO.get(
                contexto.perfil_financiero.tono_ia, _TONO_DEFAULT
            )

        return (
            "# ROL\n"
            "Eres un coach financiero personal experto en finanzas personales "
            "para personas en Latinoamérica. Tu misión es analizar cada gasto "
            "del usuario y entregar un consejo accionable, breve y personalizado "
            "que lo ayude a mejorar su salud financiera.\n\n"
            f"## Estilo de comunicación\n{tono_instruccion}\n\n"
            "## Restricciones de formato\n"
            "- Responde SIEMPRE en español.\n"
            "- Máximo 120 palabras en total.\n"
            "- Sin markdown ni asteriscos: el mensaje se enviará por WhatsApp.\n"
            "- No repitas los datos del gasto textualmente; interprètalos."
        )

    def _seccion_transaccion(self, evento: EventoAnalisisIA) -> str:
        """Describe la transacción que dispara el análisis."""
        t = evento.transaccion
        tipo_label = "registraste un gasto" if t.tipo == TipoMovimiento.GASTO else "recibiste un ingreso"

        return (
            "# TRANSACCIÓN ANALIZADA\n"
            f"El usuario acaba de registrar la siguiente operación:\n"
            f"- Tipo: {t.tipo.value}\n"
            f"- Descripción: {t.descripcion}\n"
            f"- Categoría: {t.categoria}\n"
            f"- Monto: S/ {t.monto:,.2f}"
        )

    def _seccion_contexto_financiero(self, contexto: ContextoUsuario) -> str:
        """Agrega el perfil económico del usuario si está disponible."""
        if not contexto.tiene_perfil or not contexto.perfil_financiero:
            return (
                "# PERFIL DEL USUARIO\n"
                "No se dispone de información de perfil. "
                "Ofrece un consejo general aplicable a cualquier usuario."
            )

        perfil = contexto.perfil_financiero
        ingreso_fmt = f"S/ {perfil.ingreso_mensual:,.2f}" if perfil.ingreso_mensual > 0 else "no especificado"

        return (
            "# PERFIL DEL USUARIO\n"
            f"- Ocupación: {perfil.ocupacion}\n"
            f"- Ingreso mensual estimado: {ingreso_fmt}\n"
            "Ten en cuenta esta realidad económica al formular tus recomendaciones. "
            "Un consejo viable para un estudiante es diferente al de un profesional senior."
        )

    def _seccion_metas(self, contexto: ContextoUsuario) -> str:
        """Lista las metas de ahorro y su progreso actual."""
        if not contexto.tiene_metas:
            return (
                "# METAS DE AHORRO\n"
                "El usuario no tiene metas configuradas. "
                "Sugiere que establezca al menos una meta concreta para motivar su ahorro."
            )

        lineas = ["# METAS DE AHORRO ACTIVAS"]
        for meta in contexto.metas:
            barra = self._barra_progreso(meta.progreso_porcentaje)
            lineas.append(
                f"▸ {meta.nombre}: "
                f"S/ {meta.monto_actual:,.2f} de S/ {meta.monto_objetivo:,.2f} "
                f"({meta.progreso_porcentaje}%) {barra} "
                f"— faltan S/ {meta.monto_restante:,.2f}"
            )

        lineas.append(
            "\nRelaciona el gasto analizado con el impacto en estas metas. "
            "Si el gasto aleja al usuario de una meta, menciónalo con tacto."
        )
        return "\n".join(lineas)

    def _seccion_limite_presupuesto(self, evento: EventoAnalisisIA) -> str:
        """Evalúa el gasto contra el límite global configurado."""
        contexto = evento.contexto

        if not contexto.tiene_limite_activo:
            return ""  # Sección omitida si no hay límite activo

        limite = contexto.limite_global
        monto_gasto = evento.transaccion.monto

        # Calculamos qué % del límite representa ESTE gasto individual
        porcentaje_uso = (monto_gasto / limite.monto_limite) * 100 if limite.monto_limite > 0 else 0
        umbral_alerta = limite.porcentaje_alerta

        if porcentaje_uso >= 100:
            estado = "LÍMITE SUPERADO"
            instruccion = (
                f"Este gasto de S/ {monto_gasto:,.2f} supera el límite mensual de "
                f"S/ {limite.monto_limite:,.2f}. ALERTA CRÍTICA: informa al usuario "
                "que ha excedido su presupuesto y sugiere medidas inmediatas."
            )
        elif porcentaje_uso >= umbral_alerta:
            estado = "ZONA DE ALERTA"
            instruccion = (
                f"Con este gasto de S/ {monto_gasto:,.2f}, el usuario está en zona "
                f"de alerta ({porcentaje_uso:.1f}% del límite de S/ {limite.monto_limite:,.2f}). "
                "Advierte con suavidad que se está acercando al tope mensual."
            )
        else:
            estado = "DENTRO DEL LÍMITE"
            instruccion = (
                f"El gasto representa el {porcentaje_uso:.1f}% del límite mensual "
                f"(S/ {limite.monto_limite:,.2f}). El usuario va bien, refuerza positivamente."
            )

        return (
            f"# CONTROL DE PRESUPUESTO GLOBAL\n"
            f"Estado: {estado}\n"
            f"Límite configurado: S/ {limite.monto_limite:,.2f}/mes\n"
            f"Gasto actual analizado: S/ {monto_gasto:,.2f} ({porcentaje_uso:.1f}% del límite)\n\n"
            f"Instrucción: {instruccion}"
        )

    def _seccion_tarea_final(self, contexto: ContextoUsuario) -> str:
        """Instrucción final que cierra el prompt con la tarea concreta."""
        tono_str = "amigable"
        if contexto.tiene_perfil and contexto.perfil_financiero:
            tono_str = contexto.perfil_financiero.tono_ia.value.lower()

        return (
            "# TAREA\n"
            f"Con toda la información anterior, genera UN SOLO mensaje de WhatsApp "
            f"con tono {tono_str} que:\n"
            "1. Reconozca brevemente el gasto registrado.\n"
            "2. Lo relacione con el estado del presupuesto y/o el progreso de las metas.\n"
            "3. Dé UNA recomendación concreta y accionable adaptada al perfil del usuario.\n"
            "Recuerda: máximo 120 palabras, sin markdown, en español."
        )

    # ── Utilidades internas ───────────────────────────────────────────────────

    @staticmethod
    def _barra_progreso(porcentaje: float, ancho: int = 10) -> str:
        """Genera una barra de progreso ASCII para el prompt."""
        llenos  = int((porcentaje / 100) * ancho)
        vacios  = ancho - llenos
        return f"[{'█' * llenos}{'░' * vacios}]"