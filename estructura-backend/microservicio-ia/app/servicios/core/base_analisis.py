"""
servicios/base_analisis.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Clase base abstracta que define el Pipeline LUKA-COACH V4.

Arquitectura de 3 Fases (El Contrato que todos los módulos deben cumplir):

  ┌─ FASE 1: INGESTIÓN Y VALIDACIÓN (Capa de Datos) ──────────────────────┐
  │  - Recibe lista de transacciones crudas (JSON del ms-financiero).      │
  │  - Convierte a DataFrame de Pandas.                                    │
  │  - Valida que el rango histórico cumpla el mínimo de meses requerido.  │
  │  - Lanza ValidacionError con código HISTORIAL_INSUFICIENTE si no.      │
  └────────────────────────────────────────────────────────────────────────┘
  ┌─ FASE 2: MOTOR DE CÁLCULO LOCAL (Capa Pandas/NumPy) ──────────────────┐
  │  - Recibe el DataFrame validado + ContextoEstrategicoIADTO.            │
  │  - Ejecuta toda la matemática del módulo (SIN llamadas externas).      │
  │  - Retorna un Dict con métricas duras (nunca texto, nunca prompts).    │
  │  - 100% testeable en aislamiento (sin mocks de red).                   │
  └────────────────────────────────────────────────────────────────────────┘
  ┌─ FASE 3: ORQUESTACIÓN DE EMPATÍA (Capa Gemini) ───────────────────────┐
  │  - Recibe las métricas de Fase 2 + ContextoEstrategicoIADTO.           │
  │  - Construye el prompt personalizado (tono, nombre, meta).             │
  │  - NUNCA envía el DataFrame ni transacciones crudas a Gemini.          │
  │  - Retorna el string del prompt listo para enviar.                     │
  └────────────────────────────────────────────────────────────────────────┘

Ventajas de esta arquitectura:
  1. ESCALABILIDAD: Para un nuevo módulo, solo se escribe la Fase 2.
     La validación (F1) y la orquestación (F3) se heredan automáticamente.
  2. TESTABILIDAD: Cada fase es una función pura inyectable.
     Los tests de Fase 2 no necesitan mock de Gemini ni de la red.
  3. SEPARACIÓN DE RESPONSABILIDADES: Pandas no sabe de Gemini.
     Gemini no sabe de DataFrames.

Implementar un nuevo módulo:
    class MiNuevoModuloService(BaseAnalisisService):
        def __init__(self):
            super().__init__(nombre_modulo="MI_MODULO", meses_minimos=3)

        def ejecutar_calculos(self, df, contexto):
            # Solo matemática con Pandas/NumPy aquí
            return {"mi_metrica": 42.0}

        def orquestar_prompt(self, metricas, contexto):
            return f"Analiza {metricas['mi_metrica']} para {contexto.nombres}..."
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional

import pandas as pd

from app.libreria_comun.excepciones.base import ValidacionError
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.seguridad.contexto import get_correlation_id

logger = logging.getLogger(__name__)


# ── Constantes del Pipeline ───────────────────────────────────────────────────
DIAS_POR_MES: int = 30
COLUMNA_FECHA: str = "fecha_transaccion"
COLUMNA_MONTO: str = "monto"
COLUMNA_TIPO: str = "tipo"
COLUMNA_DESCRIPCION: str = "nombreCliente"   # campo Java → mapeado aquí


# ══════════════════════════════════════════════════════════════════════════════
# CLASE BASE DEL PIPELINE
# ══════════════════════════════════════════════════════════════════════════════

class BaseAnalisisService(ABC):
    """
    Clase base que implementa el Pipeline LUKA-COACH V4.

    Todos los módulos de análisis del ms-ia deben extender de aquí.
    La subclase solo necesita implementar dos métodos abstractos:
      - ejecutar_calculos(df, contexto) → Dict[str, Any]
      - orquestar_prompt(metricas, contexto) → str

    El método público run_pipeline() orquesta las 3 fases en orden
    y propaga el Correlation-ID en todos los logs para trazabilidad.
    """

    def __init__(self, nombre_modulo: str, meses_minimos: int = 3) -> None:
        """
        Inicializa el servicio del módulo.

        Args:
            nombre_modulo: Nombre legible del módulo (usado en logs y errores).
                           Ej: "GASTO_HORMIGA", "PREDICCION_FLUJO".
            meses_minimos: Rango histórico mínimo requerido en meses.
                           Por defecto 3 meses (90 días).
        """
        self.nombre_modulo = nombre_modulo
        self.meses_minimos = meses_minimos
        self._dias_requeridos = meses_minimos * DIAS_POR_MES

    # ══════════════════════════════════════════════════════════════════════════
    # MÉTODO PÚBLICO PRINCIPAL — Orquesta las 3 fases
    # ══════════════════════════════════════════════════════════════════════════

    def run_pipeline(
        self,
        transacciones: List[Dict[str, Any]],
        contexto: ContextoEstrategicoIADTO,
    ) -> Dict[str, Any]:
        """
        Ejecuta el pipeline completo de 3 fases.

        Este es el único método que los routers/servicios deben llamar.
        Propaga el X-Correlation-ID en todos los logs del pipeline.

        Args:
            transacciones: Lista de dicts crudos del ms-financiero (content del Page).
            contexto:      DTO con el perfil estratégico del usuario (del ms-cliente).

        Returns:
            Diccionario con:
              - "metricas": resultado de la Fase 2 (siempre presente)
              - "prompt":   string construido en la Fase 3 (siempre presente)
              - "modulo":   nombre del módulo

        Raises:
            ValidacionError: Si la Fase 1 detecta historial insuficiente.
        """
        correl_id = get_correlation_id()

        logger.info(
            "[PIPELINE:%s] Iniciando | usuario=%s | trace=%s",
            self.nombre_modulo,
            contexto.primer_nombre,
            correl_id,
        )

        # ── FASE 1: Ingestión y Validación ────────────────────────────────────
        df = self._fase1_ingestar_y_validar(transacciones, correl_id)

        # ── FASE 2: Motor de Cálculo Local ────────────────────────────────────
        metricas = self._fase2_calculos(df, contexto, correl_id)

        # ── FASE 3: Orquestación del Prompt ───────────────────────────────────
        prompt = self._fase3_orquestar_prompt(metricas, contexto, correl_id)

        logger.info(
            "[PIPELINE:%s] Completado exitosamente | trace=%s",
            self.nombre_modulo,
            correl_id,
        )

        return {
            "modulo": self.nombre_modulo,
            "metricas": metricas,
            "prompt": prompt,
        }

    # ══════════════════════════════════════════════════════════════════════════
    # FASE 1 — INGESTIÓN Y VALIDACIÓN (Implementación concreta)
    # ══════════════════════════════════════════════════════════════════════════

    def _fase1_ingestar_y_validar(
        self,
        transacciones: List[Dict[str, Any]],
        correl_id: str,
    ) -> pd.DataFrame:
        """
        Convierte la lista de transacciones a DataFrame y valida el rango histórico.

        Regla de calidad central del pipeline:
          Si el historial cubre menos días que (meses_minimos × 30),
          se lanza ValidacionError con código HISTORIAL_INSUFICIENTE.

        Args:
            transacciones: Lista de dicts crudos del ms-financiero.
            correl_id:     ID de correlación para trazabilidad en logs.

        Returns:
            DataFrame de Pandas con tipos de datos normalizados.

        Raises:
            ValidacionError: Historial vacío o rango de fechas insuficiente.
        """
        logger.debug(
            "[PIPELINE:%s | F1] Iniciando ingestión | "
            "%d transacciones recibidas | trace=%s",
            self.nombre_modulo,
            len(transacciones) if transacciones else 0,
            correl_id,
        )

        # ── Validación: lista no vacía ────────────────────────────────────────
        if not transacciones:
            msg = (
                f"Necesitamos al menos {self.meses_minimos} meses de movimientos "
                f"para analizar tus {self._nombre_modulo_amigable()} con precisión."
            )
            logger.warning(
                "[PIPELINE:%s | F1] Sin transacciones | trace=%s",
                self.nombre_modulo, correl_id,
            )
            raise ValidacionError(mensaje=msg, codigo_error="HISTORIAL_INSUFICIENTE")

        # ── Conversión a DataFrame ────────────────────────────────────────────
        df = pd.DataFrame(transacciones)

        # ── Normalización de la columna de fecha ─────────────────────────────
        columna_fecha = self._detectar_columna_fecha(df)
        if columna_fecha is None:
            msg = "No se encontró columna de fecha en las transacciones recibidas."
            logger.error(
                "[PIPELINE:%s | F1] Columna de fecha no encontrada | "
                "columnas=%s | trace=%s",
                self.nombre_modulo, list(df.columns), correl_id,
            )
            raise ValidacionError(mensaje=msg, codigo_error="DATOS_MALFORMADOS")

        df[columna_fecha] = pd.to_datetime(df[columna_fecha], errors="coerce")
        df = df.dropna(subset=[columna_fecha])  # Elimina filas con fechas inválidas

        if df.empty:
            msg = (
                f"Necesitamos al menos {self.meses_minimos} meses de movimientos "
                f"para analizar tus {self._nombre_modulo_amigable()} con precisión."
            )
            raise ValidacionError(mensaje=msg, codigo_error="HISTORIAL_INSUFICIENTE")

        # ── Validación central: rango de fechas ───────────────────────────────
        self._validar_rango_fechas(df, columna_fecha, correl_id)

        # ── Normalización de monto ────────────────────────────────────────────
        if COLUMNA_MONTO in df.columns:
            df[COLUMNA_MONTO] = pd.to_numeric(df[COLUMNA_MONTO], errors="coerce").fillna(0.0)

        logger.info(
            "[PIPELINE:%s | F1] Validación OK | "
            "%d transacciones válidas | rango=%.0f días | trace=%s",
            self.nombre_modulo,
            len(df),
            self._calcular_rango_dias(df, columna_fecha),
            correl_id,
        )

        return df

    def _validar_rango_fechas(
        self,
        df: pd.DataFrame,
        columna_fecha: str,
        correl_id: str,
    ) -> None:
        """
        Valida que el DataFrame cubra el rango mínimo de meses requerido.

        Raises:
            ValidacionError con código HISTORIAL_INSUFICIENTE si no cumple.
        """
        dias_cubiertos = self._calcular_rango_dias(df, columna_fecha)

        logger.debug(
            "[PIPELINE:%s | F1] Rango histórico: %.0f días "
            "(requerido: %d días | %d meses) | trace=%s",
            self.nombre_modulo,
            dias_cubiertos,
            self._dias_requeridos,
            self.meses_minimos,
            correl_id,
        )

        if dias_cubiertos <= self._dias_requeridos:
            meses_actuales = round(dias_cubiertos / DIAS_POR_MES, 1)
            msg = (
                f"Necesitamos al menos {self.meses_minimos} meses de movimientos "
                f"para analizar tus {self._nombre_modulo_amigable()} con precisión. "
                f"Solo encontramos {meses_actuales} mes(es) de historial."
            )
            logger.warning(
                "[PIPELINE:%s | F1] Historial insuficiente: %.1f meses "
                "(mínimo: %d) | trace=%s",
                self.nombre_modulo, meses_actuales, self.meses_minimos, correl_id,
            )
            raise ValidacionError(mensaje=msg, codigo_error="HISTORIAL_INSUFICIENTE")

    # ══════════════════════════════════════════════════════════════════════════
    # FASE 2 — MOTOR DE CÁLCULO LOCAL (Método abstracto)
    # ══════════════════════════════════════════════════════════════════════════

    @abstractmethod
    def ejecutar_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
    ) -> Dict[str, Any]:
        """
        ── FASE 2: Motor de Cálculo Local ──────────────────────────────────
        Implementación específica de cada módulo.

        Contrato:
          - INPUT: DataFrame validado + ContextoEstrategicoIADTO.
          - OUTPUT: Diccionario con métricas numéricas/categóricas.
          - RESTRICCIÓN: Sin llamadas HTTP, sin Gemini, sin efectos secundarios.
          - GARANTÍA: 100% testeable con DataFrames ficticios.

        Ejemplo de salida para GASTO_HORMIGA:
          {
              "total_hormiga": 234.50,
              "items_detectados": 47,
              "categoria_principal": "Delivery",
              "impacto_mensual_estimado": 78.17,
          }
        """
        ...

    # ══════════════════════════════════════════════════════════════════════════
    # FASE 3 — ORQUESTACIÓN DEL PROMPT (Método abstracto)
    # ══════════════════════════════════════════════════════════════════════════

    @abstractmethod
    def orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
    ) -> str:
        """
        ── FASE 3: Orquestación de Empatía ─────────────────────────────────
        Construye el prompt personalizado para Gemini.

        Contrato:
          - INPUT: Métricas de Fase 2 + ContextoEstrategicoIADTO.
          - OUTPUT: String del prompt listo para enviar a Gemini.
          - RESTRICCIÓN CRÍTICA: No se envían transacciones crudas ni DataFrames.
            Solo el resumen numérico de Fase 2 + datos del DTO de contexto.
          - PERSONALIZACIÓN: Usar contexto.tono_ia, contexto.nombres,
            contexto.nombre_meta_principal en CADA prompt.
        """
        ...

    # ══════════════════════════════════════════════════════════════════════════
    # ORQUESTADORES INTERNOS DE FASES 2 Y 3 (con logging)
    # ══════════════════════════════════════════════════════════════════════════

    def _fase2_calculos(
        self,
        df: pd.DataFrame,
        contexto: ContextoEstrategicoIADTO,
        correl_id: str,
    ) -> Dict[str, Any]:
        """Wrapper de Fase 2 con logging y manejo de errores."""
        logger.debug(
            "[PIPELINE:%s | F2] Ejecutando cálculos | trace=%s",
            self.nombre_modulo, correl_id,
        )
        try:
            metricas = self.ejecutar_calculos(df, contexto)
            logger.info(
                "[PIPELINE:%s | F2] Cálculos completados | "
                "claves_metricas=%s | trace=%s",
                self.nombre_modulo,
                list(metricas.keys()),
                correl_id,
            )
            return metricas
        except ValidacionError:
            raise  # Propagamos errores de negocio sin envolver
        except Exception as exc:
            logger.error(
                "[PIPELINE:%s | F2] Error en cálculos: %s | trace=%s",
                self.nombre_modulo, str(exc), correl_id, exc_info=True,
            )
            raise

    def _fase3_orquestar_prompt(
        self,
        metricas: Dict[str, Any],
        contexto: ContextoEstrategicoIADTO,
        correl_id: str,
    ) -> str:
        """Wrapper de Fase 3 con logging y validación del prompt."""
        logger.debug(
            "[PIPELINE:%s | F3] Orquestando prompt | trace=%s",
            self.nombre_modulo, correl_id,
        )
        try:
            prompt = self.orquestar_prompt(metricas, contexto)

            if not prompt or not prompt.strip():
                raise ValueError("El prompt generado está vacío.")

            logger.info(
                "[PIPELINE:%s | F3] Prompt construido | "
                "%d caracteres | trace=%s",
                self.nombre_modulo, len(prompt), correl_id,
            )
            return prompt

        except ValidacionError:
            raise
        except Exception as exc:
            logger.error(
                "[PIPELINE:%s | F3] Error al orquestar prompt: %s | trace=%s",
                self.nombre_modulo, str(exc), correl_id, exc_info=True,
            )
            raise

    # ══════════════════════════════════════════════════════════════════════════
    # UTILIDADES INTERNAS
    # ══════════════════════════════════════════════════════════════════════════

    def _detectar_columna_fecha(self, df: pd.DataFrame) -> Optional[str]:
        """
        Detecta la columna de fecha en el DataFrame de forma tolerante.
        Acepta tanto camelCase (Java) como snake_case (Python normalizado).
        """
        candidatas = [
            "fechaTransaccion",    # camelCase de Java (API directa)
            "fecha_transaccion",   # snake_case (preparador_datos.py)
            "fecha",               # campo genérico
            "createdAt",           # fallback por si cambia el contrato Java
        ]
        for columna in candidatas:
            if columna in df.columns:
                return columna
        return None

    @staticmethod
    def _calcular_rango_dias(df: pd.DataFrame, columna_fecha: str) -> float:
        """
        Calcula el rango en días entre la transacción más antigua y la más reciente.
        Retorna 0.0 si el DataFrame está vacío o hay un solo registro.
        """
        if df.empty or columna_fecha not in df.columns:
            return 0.0
        fechas_validas = df[columna_fecha].dropna()
        if len(fechas_validas) < 2:
            return 0.0
        delta = fechas_validas.max() - fechas_validas.min()
        return float(delta.days)

    def _nombre_modulo_amigable(self) -> str:
        """
        Convierte el nombre técnico del módulo a texto amigable para mensajes de error.
        Ej: "GASTO_HORMIGA" → "gastos hormiga"
        """
        return self.nombre_modulo.replace("_", " ").lower()