"""
servicios/gasto_hormiga.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Módulo GASTO_HORMIGA — Módulo de referencia del Pipeline LUKA-COACH V4.

Responsabilidad única:
  Identificar compras pequeñas y recurrentes que, sumadas, impactan
  significativamente el presupuesto del universitario peruano.

Regla de Negocio (Fase 2):
  "Gasto hormiga" = transacción de tipo GASTO con monto < 1% del ingreso mensual
  que aparece al menos [MIN_RECURRENCIAS] veces en el historial analizado.

Flujo completo:
  1. BaseAnalisisService.run_pipeline() valida que haya ≥ 3 meses de historial.
  2. ejecutar_calculos() identifica hormigas, agrupa por descripción y calcula
     impacto mensual estimado.
  3. orquestar_prompt() construye el prompt empático para Gemini usando el
     contexto del universitario (nombres, meta, tono), SIN enviar transacciones.

Cómo usar:
    service = GastoHormigaService()
    resultado = service.run_pipeline(transacciones, contexto)
    # resultado["metricas"] → datos calculados
    # resultado["prompt"]   → listo para enviar a Gemini
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from typing import Any, Dict, List

import pandas as pd

from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.core.base_analisis import BaseAnalisisService


logger = logging.getLogger(__name__)

# ── Constantes del módulo ─────────────────────────────────────────────────────
MIN_RECURRENCIAS: int = 2          # Aparece 2+ veces → es recurrente
TIPO_GASTO: str = "GASTO"          # Valor del campo 'tipo' en las transacciones Java
TOP_N_CATEGORIAS: int = 3          # Cuántas categorías mostrar en el consejo


class GastoHormigaService(BaseAnalisisService):
    """
    Implementación del Módulo GASTO_HORMIGA.

    Modelo de referencia para el Pipeline LUKA-COACH V4.
    Extiende BaseAnalisisService e implementa solo las 2 fases específicas.

    La Fase 1 (validación de 3 meses de historial) es heredada automáticamente.
    """

    def __init__(self) -> None:
        super().__init__(
            nombre_modulo="GASTO_HORMIGA",
            meses_minimos=3,  # Regla de negocio: mínimo 3 meses para detección fiable
        )

    # ══════════════════════════════════════════════════════════════════════════
    # FASE 2 — MOTOR DE CÁLCULO LOCAL
    # ══════════════════════════════════════════════════════════════════════════

    def ejecutar_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
    ) -> Dict[str, Any]:
        """
        Identifica gastos hormiga usando el umbral dinámico del ContextoDTO.

        Algoritmo:
          1. Filtra solo transacciones de tipo GASTO.
          2. Aplica el umbral: monto < 1% del ingreso mensual del usuario.
          3. Agrupa por descripción (nombre del comercio) para detectar recurrencia.
          4. Filtra los que tienen >= MIN_RECURRENCIAS apariciones.
          5. Calcula impacto mensual estimado promediando por el período analizado.
          6. Identifica la categoría con mayor gasto hormiga acumulado.

        Args:
            df:       DataFrame validado por Fase 1 (≥ 3 meses de datos).
            contexto: DTO con el ingreso mensual del usuario para calcular umbral.

        Returns:
            Dict con métricas duras listas para ser inyectadas en el prompt.
        """
        umbral = float(contexto.umbral_gasto_hormiga)

        logger.debug(
            "[GASTO_HORMIGA | F2] Umbral hormiga: S/ %.2f (1%% de S/ %.2f)",
            umbral,
            float(contexto.ingreso_mensual),
        )

        # ── 1. Filtrar solo GASTOS ────────────────────────────────────────────
        col_tipo = self._detectar_columna_tipo(df)
        if col_tipo:
            df_gastos = df[df[col_tipo].str.upper() == TIPO_GASTO].copy()
        else:
            # Si no hay columna de tipo, asumimos que todos son gastos
            df_gastos = df.copy()
            logger.warning(
                "[GASTO_HORMIGA | F2] Columna 'tipo' no encontrada. "
                "Procesando todas las transacciones como gastos."
            )

        if df_gastos.empty:
            return self._resultado_sin_hormigas(razon="sin_gastos_registrados")

        # ── 2. Filtrar por umbral de monto ────────────────────────────────────
        if umbral <= 0:
            # Sin ingreso declarado → usamos S/ 30 como umbral por defecto
            umbral = 30.0
            logger.warning(
                "[GASTO_HORMIGA | F2] Ingreso mensual = 0. "
                "Usando umbral por defecto: S/ %.2f", umbral,
            )

        df_hormigas = df_gastos[df_gastos["monto"] < umbral].copy()

        if df_hormigas.empty:
            return self._resultado_sin_hormigas(razon="sin_gastos_bajo_umbral")

        # ── 3. Detectar columna de descripción ───────────────────────────────
        col_descripcion = self._detectar_columna_descripcion(df_hormigas)

        # ── 4. Agrupar por descripción para encontrar recurrencia ─────────────
        if col_descripcion:
            resumen = (
                df_hormigas
                .groupby(col_descripcion)
                .agg(
                    total_acumulado=("monto", "sum"),
                    frecuencia=("monto", "count"),
                    promedio=("monto", "mean"),
                    categoria=("categoriaNombre" if "categoriaNombre" in df_hormigas.columns
                               else col_descripcion, "first"),
                )
                .reset_index()
                .rename(columns={col_descripcion: "descripcion"})
            )
        else:
            # Sin descripción: analizamos solo por monto
            resumen = pd.DataFrame({
                "descripcion": ["Gastos pequeños sin descripción"],
                "total_acumulado": [df_hormigas["monto"].sum()],
                "frecuencia": [len(df_hormigas)],
                "promedio": [df_hormigas["monto"].mean()],
                "categoria": ["Sin categoría"],
            })

        # ── 5. Filtrar recurrentes (>= MIN_RECURRENCIAS) ──────────────────────
        recurrentes = resumen[resumen["frecuencia"] >= MIN_RECURRENCIAS].copy()

        if recurrentes.empty:
            return self._resultado_sin_hormigas(razon="sin_recurrencia_suficiente")

        recurrentes = recurrentes.sort_values("total_acumulado", ascending=False)

        # ── 6. Calcular métricas consolidadas ─────────────────────────────────
        total_hormiga = float(recurrentes["total_acumulado"].sum())
        items_detectados = int(recurrentes["frecuencia"].sum())

        # Calcular meses cubiertos para estimar impacto mensual
        col_fecha = self._detectar_columna_fecha(df)
        meses_cubiertos = self._calcular_meses_cubiertos(df, col_fecha)
        impacto_mensual = total_hormiga / meses_cubiertos if meses_cubiertos > 0 else total_hormiga

        # Categoría con más gasto hormiga acumulado
        categoria_principal = self._detectar_categoria_principal(df_hormigas, recurrentes)

        # Top 3 para el prompt
        top_hormigas = self._extraer_top_hormigas(recurrentes, top_n=TOP_N_CATEGORIAS)

        logger.info(
            "[GASTO_HORMIGA | F2] Detectados %d items hormiga | "
            "Total: S/ %.2f | Mensual estimado: S/ %.2f | "
            "Categoría principal: %s",
            len(recurrentes),
            total_hormiga,
            impacto_mensual,
            categoria_principal,
        )

        return {
            "total_hormiga":             round(total_hormiga, 2),
            "items_detectados":          items_detectados,
            "comercios_unicos":          len(recurrentes),
            "categoria_principal":       categoria_principal,
            "impacto_mensual_estimado":  round(impacto_mensual, 2),
            "impacto_anual_estimado":    round(impacto_mensual * 12, 2),
            "umbral_aplicado":           round(umbral, 2),
            "meses_analizados":          round(meses_cubiertos, 1),
            "top_hormigas":              top_hormigas,
            "tiene_hormigas":            True,
        }

    # ══════════════════════════════════════════════════════════════════════════
    # FASE 3 — ORQUESTACIÓN DEL PROMPT
    # ══════════════════════════════════════════════════════════════════════════

    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """
        Construye el prompt empático para Gemini.

        RESTRICCIÓN CRÍTICA:
          No se envían transacciones individuales ni el DataFrame.
          Solo el resumen numérico de Fase 2 + los datos del ContextoDTO.

        El prompt se adapta según:
          - Si hay hormigas detectadas: consejo de reducción + motivación por meta.
          - Si no hay hormigas: mensaje de felicitación + refuerzo positivo.

        Args:
            metricas: Resultado de ejecutar_calculos() (métricas duras).
            contexto: DTO con perfil del universitario.

        Returns:
            String del prompt completo, listo para enviar a Gemini.
        """
        if not metricas.get("tiene_hormigas", False):
            return self._prompt_sin_hormigas(contexto)
        return self._prompt_con_hormigas(metricas, contexto)

    # ══════════════════════════════════════════════════════════════════════════
    # BUILDERS DE PROMPTS (privados)
    # ══════════════════════════════════════════════════════════════════════════

    def _prompt_con_hormigas(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """Prompt para cuando se detectaron gastos hormiga significativos."""

        top_lista = ""
        if metricas.get("top_hormigas"):
            top_lista = "\n".join(
                f"  • {item['descripcion']}: S/ {item['total']:.2f} "
                f"({item['frecuencia']} veces)"
                for item in metricas["top_hormigas"]
            )
            top_lista = f"\nPrincipales gastos hormiga detectados:\n{top_lista}"

        return f"""# Rol
Eres el coach financiero personal de LUKA, una app diseñada para universitarios peruanos.
Hablas en español peruano neutro, de forma {contexto.tono_ia.lower()}.
Nunca repites los números exactos del análisis; los interpretas y les das contexto humano.
Máximo 180 palabras. Sin markdown pesado: solo saltos de línea y guiones simples.

## Contexto del universitario
{contexto.resumen_para_prompt}

## Hallazgos del análisis GASTO_HORMIGA
- Gasto total en pequeñas compras: S/ {metricas['total_hormiga']:,.2f}
- Impacto mensual estimado: S/ {metricas['impacto_mensual_estimado']:,.2f}
- Impacto anual estimado: S/ {metricas['impacto_anual_estimado']:,.2f}
- Categoría principal: {metricas['categoria_principal']}
- Comercios recurrentes: {metricas['comercios_unicos']}
- Historial analizado: {metricas['meses_analizados']:.0f} meses{top_lista}

## Tarea
Genera un consejo financiero para {contexto.primer_nombre} con este estilo:

1. (1 oración) Muéstrale el impacto REAL de sus gastos hormiga en términos anuales.
   No repitas el número, INTERPRETA: ¿a qué equivale ese dinero? (ej: meses de internet, % de la meta).
2. (1-2 oraciones) Conecta el ahorro potencial con su meta '{contexto.nombre_meta_principal}' 
   (actualmente al {contexto.porcentaje_meta_principal}%). ¿Cuánto más cerca estaría si redujera estas compras?
3. (1 oración) UNA acción concreta y realista para esta semana.

Cierra con una frase motivadora corta."""

    def _prompt_sin_hormigas(self, contexto: ContextoEstrategicoIADTO) -> str:
        """Prompt para cuando el universitario no tiene gastos hormiga detectables."""
        return f"""# Rol
Eres el coach financiero personal de LUKA para universitarios peruanos.
Hablas en tono {contexto.tono_ia.lower()}, máximo 120 palabras.

## Contexto del universitario
{contexto.resumen_para_prompt}

## Hallazgos del análisis GASTO_HORMIGA
- No se detectaron gastos hormiga recurrentes en los últimos {self.meses_minimos} meses.
- El usuario tiene control sobre sus pequeñas compras cotidianas.

## Tarea
Genera un mensaje de felicitación para {contexto.primer_nombre}:
1. (1 oración) Reconoce su excelente disciplina en el control de gastos pequeños.
2. (1 oración) Refuerza el impacto positivo para su meta '{contexto.nombre_meta_principal}' 
   (actualmente al {contexto.porcentaje_meta_principal}%).
3. (1 oración) Sugiere un siguiente nivel: ¿qué podría optimizar ahora que ya domina este aspecto?

Tono celebratorio pero realista."""

    # ══════════════════════════════════════════════════════════════════════════
    # UTILIDADES INTERNAS DEL MÓDULO
    # ══════════════════════════════════════════════════════════════════════════

    @staticmethod
    def _resultado_sin_hormigas(razon: str) -> Dict[str, Any]:
        """Retorna el resultado estándar cuando no se detectan hormigas."""
        logger.info(
            "[GASTO_HORMIGA | F2] Sin hormigas detectadas | razón=%s", razon
        )
        return {
            "total_hormiga": 0.0,
            "items_detectados": 0,
            "comercios_unicos": 0,
            "categoria_principal": "Ninguna",
            "impacto_mensual_estimado": 0.0,
            "impacto_anual_estimado": 0.0,
            "umbral_aplicado": 0.0,
            "meses_analizados": 0.0,
            "top_hormigas": [],
            "tiene_hormigas": False,
            "razon": razon,
        }

    @staticmethod
    def _detectar_columna_tipo(df: pd.DataFrame) -> str | None:
        """Detecta la columna que contiene el tipo de movimiento (GASTO/INGRESO)."""
        for col in ["tipo", "type", "tipoMovimiento"]:
            if col in df.columns:
                return col
        return None

    @staticmethod
    def _detectar_columna_descripcion(df: pd.DataFrame) -> str | None:
        """Detecta la columna de descripción del comercio."""
        for col in ["nombreCliente", "nombre_cliente", "descripcion", "description", "comercio"]:
            if col in df.columns:
                return col
        return None

    @staticmethod
    def _calcular_meses_cubiertos(df: pd.DataFrame, col_fecha: str | None) -> float:
        """Calcula los meses cubiertos por el DataFrame, con conversión de tipo."""
        if col_fecha is None or col_fecha not in df.columns:
            return 3.0  # Fallback: asumimos 3 meses (ya validado en F1)
        # Convertimos explícitamente a datetime (puede llegar como string ISO)
        fechas = pd.to_datetime(df[col_fecha], errors="coerce").dropna()
        if len(fechas) < 2:
            return 1.0
        delta_dias = (fechas.max() - fechas.min()).days
        return max(delta_dias / 30.0, 1.0)

    @staticmethod
    def _detectar_categoria_principal(
        df_hormigas: pd.DataFrame,
        recurrentes: pd.DataFrame,
    ) -> str:
        """Identifica la categoría con mayor gasto hormiga acumulado."""
        col_categoria = None
        for col in ["categoriaNombre", "categoria_nombre", "categoria", "category"]:
            if col in df_hormigas.columns:
                col_categoria = col
                break

        if col_categoria is None:
            return "Sin categoría"

        if "categoria" in recurrentes.columns:
            try:
                return str(recurrentes.groupby("categoria")["total_acumulado"].sum().idxmax())
            except (ValueError, KeyError):
                pass

        try:
            return str(
                df_hormigas.groupby(col_categoria)["monto"].sum().idxmax()
            )
        except (ValueError, KeyError):
            return "Sin categoría"

    @staticmethod
    def _extraer_top_hormigas(
        recurrentes: pd.DataFrame,
        top_n: int = 3,
    ) -> List[Dict[str, Any]]:
        """Extrae los top N gastos hormiga para incluir en el prompt."""
        resultado = []
        for _, fila in recurrentes.head(top_n).iterrows():
            resultado.append({
                "descripcion": str(fila.get("descripcion", "Sin nombre")),
                "total":       round(float(fila.get("total_acumulado", 0)), 2),
                "frecuencia":  int(fila.get("frecuencia", 0)),
                "promedio":    round(float(fila.get("promedio", 0)), 2),
            })
        return resultado