"""
servicios/coach_ia.py  ·  v3 — Multi-Módulos + MetadataGrafico + Persistencia
══════════════════════════════════════════════════════════════════════════════
Orquestador central del análisis IA.
 
Responsabilidades:
  1. Seleccionar el TipoModulo correcto según el evento recibido.
  2. Delegar a IngenierioPrompt la construcción del prompt.
  3. Llamar a Google Gemini 1.5 Flash y obtener el consejo.
  4. Construir el ResultadoAnalisisIA con metadata_grafico para el Dashboard.
  5. Retornar el resultado listo para persistir/publicar.
 
Patrón Estrategia:
  _GENERADORES_METADATA es un dict TipoModulo → método.
  Cada módulo sabe qué datos necesita del evento para construir su gráfico.
  Si un módulo no tiene gráfico, retorna None y el Dashboard muestra solo texto.
 
No conoce RabbitMQ ni la capa de persistencia.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
 
import logging
from typing import Callable, Dict, List, Optional
 
import google.generativeai as genai
from google.generativeai.types import GenerationConfig
 
from app.configuracion import obtener_configuracion
from app.modelos.evento_analisis import (
    EventoAnalisisIA,
    MetadataGrafico,
    PuntoGrafico,
    ResumenMes,
    ResultadoAnalisisIA,
    TipoModulo,
    TipoSolicitud,
)
from app.servicios.ingeniero_prompt import IngenieroPrompt
from google.api_core import exceptions as google_exceptions
from app.excepciones import AnalisisFinancieroError, GeminiCuotaExcedidaError, GeminiAutenticacionError


logger = logging.getLogger(__name__)

# ── Colores por defecto para gráficos ────────────────────────────────────────
_COLORES_SERIE_GASTOS   = "#E74C3C"
_COLORES_SERIE_INGRESOS = "#2ECC71"
_COLORES_SERIE_BALANCE  = "#3498DB"
_PALETA_CATEGORIAS = [
    "#E74C3C", "#3498DB", "#2ECC71", "#F39C12",
    "#9B59B6", "#1ABC9C", "#E67E22", "#95A5A6",
]


class CoachIA:
    """
    Punto de entrada único para todo análisis IA.
 
    Instanciar UNA sola vez y reutilizar (thread-safe).
    """
 
    def __init__(self) -> None:
        config = obtener_configuracion()
        genai.configure(api_key=config.gemini_api_key)
        self._modelo = genai.GenerativeModel(config.gemini_modelo)
        
        # Blindaje: Si los valores no están en config, usamos defaults seguros
        self._config_generacion = GenerationConfig(
            max_output_tokens=getattr(config, 'gemini_max_tokens', 1024),
            temperature=getattr(config, 'gemini_temperatura', 0.2),
        )
        self._ingeniero = IngenieroPrompt()
        logger.info(f"[COACH-IA] Inicializado con {config.gemini_modelo}")

    # ══════════════════════════════════════════════════════════════════════════
    # MÉTODO PRINCIPAL
    # ══════════════════════════════════════════════════════════════════════════
 
    def analizar(self, evento: EventoAnalisisIA) -> Optional[ResultadoAnalisisIA]:
        """
        Método de entrada principal.
 
        1. Resuelve el TipoModulo a ejecutar.
        2. Construye el prompt.
        3. Llama a Gemini.
        4. Construye la metadata del gráfico.
        5. Retorna ResultadoAnalisisIA o None si Gemini falla.
        """
        modulo = self._resolver_modulo(evento)
        try:
            prompt  = self._ingeniero.construir(evento, modulo)
            consejo = self._llamar_gemini(prompt, evento, modulo)
 
            if consejo is None:
                return None
 
            metadata = self._generar_metadata_grafico(evento, modulo)
            kpi_val, kpi_lab = self._calcular_kpi(evento, modulo)
 
            resultado = ResultadoAnalisisIA(
                id_usuario       = evento.id_usuario,
                consejo_texto    = consejo,
                tipo_modulo      = modulo,
                metadata_grafico = metadata,
                kpi_principal    = kpi_val,
                kpi_label        = kpi_lab,
            )
 
            logger.info(
                "[COACH-IA] Análisis completado | usuario=%s | módulo=%s | kpi=%s",
                evento.id_usuario, modulo.value, kpi_val if kpi_val is not None else "N/A",
            )
            return resultado
        
        except google_exceptions.ResourceExhausted:
            logger.warning("[COACH-IA] Cuota de Gemini agotada (429).")
            raise GeminiCuotaExcedidaError()
            
        except google_exceptions.Unauthenticated:
            logger.error("[COACH-IA] Error de autenticación. Revisa la API Key.")
            raise GeminiAutenticacionError()
            
        except (google_exceptions.InvalidArgument, google_exceptions.DeadlineExceeded) as exc:
            logger.error(f"[COACH-IA] Error técnico en la petición: {str(exc)}")
            raise AnalisisFinancieroError("Error al conectar con el motor de IA.", detalles=str(exc))

        except Exception as exc:
            logger.error(f"[COACH-IA] Error crítico no controlado: {str(exc)}", exc_info=True)
            return None

    # ══════════════════════════════════════════════════════════════════════════
    # RESOLUCIÓN DE MÓDULO
    # ══════════════════════════════════════════════════════════════════════════
 
    def _resolver_modulo(self, evento: EventoAnalisisIA) -> TipoModulo:
        """
        Decide qué módulo ejecutar.
 
        - CONSULTA_MODULO: usa el módulo explícito del evento.
        - TRANSACCION_RECIENTE: siempre ejecuta TRANSACCION_AUTOMATICA.
        """
        if (
            evento.tipo_solicitud == TipoSolicitud.CONSULTA_MODULO
            and evento.modulo_solicitado is not None
        ):
            logger.debug(
                "[COACH-IA] Módulo explícito solicitado: %s",
                evento.modulo_solicitado.value,
            )
            return evento.modulo_solicitado
 
        return TipoModulo.TRANSACCION_AUTOMATICA
    
    #══════════════════════════════════════════════════════════════════════════
    # LLAMADA A GEMINI
    # ══════════════════════════════════════════════════════════════════════════
 
    def _llamar_gemini(
        self,
        prompt: str,
        evento: EventoAnalisisIA,
        modulo: TipoModulo,
    ) -> Optional[str]:
        """Llama a Gemini y retorna el texto del consejo, o None si falla."""
        try:
            logger.debug(
                "[GEMINI] Enviando prompt | módulo=%s | usuario=%s | %d chars",
                modulo.value, evento.id_usuario, len(prompt),
            )
            respuesta = self._modelo.generate_content(
                prompt,
                generation_config=self._config_generacion,
            )
            texto = respuesta.text.strip()
            logger.debug(
                "[GEMINI] Respuesta recibida | %d chars", len(texto)
            )
            return texto
 
        except Exception as exc:
            logger.error(
                "[GEMINI] Error al generar consejo | módulo=%s | %s",
                modulo.value, str(exc), exc_info=True,
            )
            return None
        
        # ══════════════════════════════════════════════════════════════════════════
    # GENERACIÓN DE METADATA GRÁFICO (Patrón Estrategia)
    # ══════════════════════════════════════════════════════════════════════════
 
    def _generar_metadata_grafico(
        self, evento: EventoAnalisisIA, modulo: TipoModulo
    ) -> Optional[MetadataGrafico]:
        """
        Despacha la generación de metadata al método específico del módulo.
        Si el módulo no tiene gráfico o no hay suficientes datos, retorna None.
        """
        generadores: Dict[TipoModulo, Callable] = {
            TipoModulo.PREDICCION_GASTOS:    self._meta_prediccion,
            TipoModulo.GASTO_HORMIGA:        self._meta_gastos_hormiga,
            TipoModulo.COMPARACION_MENSUAL:  self._meta_comparacion,
            TipoModulo.CAPACIDAD_AHORRO:     self._meta_ahorro,
            TipoModulo.ESTACIONALIDAD:       self._meta_estacionalidad,
            TipoModulo.REPORTE_COMPLETO:     self._meta_reporte,
        }
        generador = generadores.get(modulo)
        if generador is None:
            return None
        try:
            return generador(evento)
        except Exception as exc:
            logger.warning(
                "[META] Error generando metadata para módulo %s: %s",
                modulo.value, str(exc),
            )
            return None
        
    def _meta_prediccion(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de líneas: ingresos vs gastos por mes + proyección.
        """
        if not evento.tiene_historial:
            return None
 
        historial = sorted(evento.historial_mensual, key=lambda m: (m.anio, m.mes))
 
        datos_gastos   = [PuntoGrafico(etiqueta=m.periodo_label, valor=m.total_gastos,   color=_COLORES_SERIE_GASTOS)   for m in historial]
        datos_ingresos = [PuntoGrafico(etiqueta=m.periodo_label, valor=m.total_ingresos, color=_COLORES_SERIE_INGRESOS) for m in historial]
 
        # Proyección simple: promedio de los últimos 3 meses como cierre estimado
        gastos_recientes = [m.total_gastos for m in historial[-3:]]
        proyeccion = sum(gastos_recientes) / len(gastos_recientes)
 
        return MetadataGrafico(
            tipo_grafico="line",
            titulo="Tendencia de Ingresos vs Gastos",
            datos=datos_gastos,
            datos_aux=datos_ingresos,
            unidad="S/",
            meta_linea=round(proyeccion, 2),
        )
    
    def _meta_gastos_hormiga(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de barras: gastos pequeños por categoría.
        Solo incluye las categorías con transacciones menores al umbral configurado.
        """
        if not evento.mes_actual:
            return None
 
        config   = obtener_configuracion()
        umbral   = config.umbral_gasto_hormiga
        mes      = evento.mes_actual
 
        # Filtrar categorías con gastos bajos (heurística: < umbral * 4 por categoría/mes)
        categorias_hormiga = {
            cat: monto
            for cat, monto in mes.gastos_por_categoria.items()
            if 0 < monto <= umbral * 4
        }
 
        if not categorias_hormiga:
            return None
 
        datos = [
            PuntoGrafico(
                etiqueta=cat,
                valor=round(monto, 2),
                color=_PALETA_CATEGORIAS[i % len(_PALETA_CATEGORIAS)],
            )
            for i, (cat, monto) in enumerate(
                sorted(categorias_hormiga.items(), key=lambda x: x[1], reverse=True)
            )
        ]
 
        total = sum(d.valor for d in datos)
        return MetadataGrafico(
            tipo_grafico="bar",
            titulo=f"Gastos Hormiga del Mes — Total: S/ {total:,.2f}",
            datos=datos,
            unidad="S/",
        )
    
    def _meta_comparacion(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de barras agrupadas: gastos por categoría mes actual vs anterior.
        """
        actual   = evento.mes_actual
        anterior = evento.mes_anterior
 
        if not actual or not anterior:
            return None
 
        # Unión de categorías de ambos meses
        todas_cats = set(actual.gastos_por_categoria) | set(anterior.gastos_por_categoria)
 
        datos_actual   = []
        datos_anterior = []
 
        for i, cat in enumerate(sorted(todas_cats)):
            color = _PALETA_CATEGORIAS[i % len(_PALETA_CATEGORIAS)]
            datos_actual.append(PuntoGrafico(
                etiqueta=cat,
                valor=round(actual.gastos_por_categoria.get(cat, 0.0), 2),
                color=color,
            ))
            datos_anterior.append(PuntoGrafico(
                etiqueta=cat,
                valor=round(anterior.gastos_por_categoria.get(cat, 0.0), 2),
                color=color + "88",  # transparencia para la serie anterior
            ))
 
        variacion = actual.variacion_gastos_vs(anterior)
        return MetadataGrafico(
            tipo_grafico="bar",
            titulo=f"Gastos por Categoría: {actual.periodo_label} vs {anterior.periodo_label} ({variacion:+.1f}%)",
            datos=datos_actual,
            datos_aux=datos_anterior,
            unidad="S/",
        )
    
    def _meta_ahorro(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de dona: distribución del ingreso (gastos / ahorro potencial).
        """
        if not evento.mes_actual:
            return None
 
        mes     = evento.mes_actual
        gastos  = mes.total_gastos
        ingreso = mes.total_ingresos
        ahorro  = max(ingreso - gastos, 0.0)
 
        datos = [
            PuntoGrafico(etiqueta="Gastos",          valor=round(gastos, 2), color=_COLORES_SERIE_GASTOS),
            PuntoGrafico(etiqueta="Ahorro potencial", valor=round(ahorro, 2), color=_COLORES_SERIE_INGRESOS),
        ]
        return MetadataGrafico(
            tipo_grafico="doughnut",
            titulo="Distribución del Ingreso Mensual",
            datos=datos,
            unidad="S/",
            meta_linea=round((ahorro / ingreso * 100) if ingreso > 0 else 0, 1),
        )
    
    def _meta_estacionalidad(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de líneas: balance mensual a lo largo del historial.
        """
        if not evento.tiene_historial:
            return None
 
        historial = sorted(evento.historial_mensual, key=lambda m: (m.anio, m.mes))
        datos = [
            PuntoGrafico(
                etiqueta=m.periodo_label,
                valor=m.balance,
                color=_COLORES_SERIE_BALANCE,
            )
            for m in historial
        ]
        return MetadataGrafico(
            tipo_grafico="line",
            titulo="Balance Mensual (Ingresos - Gastos)",
            datos=datos,
            unidad="S/",
            meta_linea=0.0,  # línea de referencia en 0
        )
    
    def _meta_reporte(self, evento: EventoAnalisisIA) -> Optional[MetadataGrafico]:
        """
        Gráfico de barras apiladas: ingresos y gastos totales por mes.
        """
        if not evento.tiene_historial:
            return None
 
        historial = sorted(evento.historial_mensual, key=lambda m: (m.anio, m.mes))
 
        datos_gastos   = [PuntoGrafico(etiqueta=m.periodo_label, valor=m.total_gastos,   color=_COLORES_SERIE_GASTOS)   for m in historial]
        datos_ingresos = [PuntoGrafico(etiqueta=m.periodo_label, valor=m.total_ingresos, color=_COLORES_SERIE_INGRESOS) for m in historial]
 
        promedio_ahorro = sum(
            max(m.total_ingresos - m.total_gastos, 0) for m in historial
        ) / len(historial)
 
        return MetadataGrafico(
            tipo_grafico="bar",
            titulo="Resumen Financiero — Últimos 6 Meses",
            datos=datos_gastos,
            datos_aux=datos_ingresos,
            unidad="S/",
            meta_linea=round(promedio_ahorro, 2),
        )
    
    # ══════════════════════════════════════════════════════════════════════════
    # KPIs DESTACADOS PARA EL HEADER DEL WIDGET
    # ══════════════════════════════════════════════════════════════════════════
 
    def _calcular_kpi(
        self, evento: EventoAnalisisIA, modulo: TipoModulo
    ) -> tuple[Optional[float], Optional[str]]:
        """
        Retorna (valor_kpi, etiqueta_kpi) para mostrar en el header del widget.
        Ejemplo: (-12.3, "% vs mes anterior")
        """
        try:
            if modulo == TipoModulo.COMPARACION_MENSUAL:
                actual   = evento.mes_actual
                anterior = evento.mes_anterior
                if actual and anterior:
                    return actual.variacion_gastos_vs(anterior), "% vs mes anterior"
 
            if modulo == TipoModulo.CAPACIDAD_AHORRO:
                mes = evento.mes_actual
                if mes and mes.total_ingresos > 0:
                    ahorro_pct = ((mes.total_ingresos - mes.total_gastos) / mes.total_ingresos) * 100
                    return round(ahorro_pct, 1), "% de ahorro estimado"
 
            if modulo == TipoModulo.GASTO_HORMIGA:
                config = obtener_configuracion()
                mes    = evento.mes_actual
                if mes:
                    total_hormiga = sum(
                        v for v in mes.gastos_por_categoria.values()
                        if 0 < v <= config.umbral_gasto_hormiga * 4
                    )
                    return round(total_hormiga, 2), "S/ en gastos hormiga este mes"
 
            if modulo == TipoModulo.PREDICCION_GASTOS:
                historial = sorted(evento.historial_mensual, key=lambda m: (m.anio, m.mes))
                if len(historial) >= 2:
                    proyeccion = sum(m.total_gastos for m in historial[-3:]) / min(len(historial), 3)
                    return round(proyeccion, 2), "S/ proyectados al cierre del mes"
 
        except Exception as exc:
            logger.debug("[KPI] No se pudo calcular KPI para %s: %s", modulo.value, exc)
 
        return None, None