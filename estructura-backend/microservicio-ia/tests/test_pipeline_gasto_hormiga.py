"""
tests/test_pipeline_gasto_hormiga.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Suite de tests Pytest para el Pipeline LUKA-COACH V4.

Cubre los 3 escenarios críticos solicitados:
  TEST 1: Sistema FALLA si el historial es de solo 1 mes.
  TEST 2: Sistema GENERA el prompt si el historial es de 4 meses.
  TEST 3: X-Correlation-ID se mantiene durante todo el proceso.

Más tests adicionales de regresión y edge cases.

Cómo ejecutar:
    cd estructura-backend/microservicio-ia
    pip install pytest pytest-mock
    pytest tests/test_pipeline_gasto_hormiga.py -v

Principio de diseño de los tests:
  - Cada test usa DataFrames ficticios (sin red, sin Gemini, sin DB).
  - El ContextoEstrategicoIADTO se construye directamente en el test.
  - Los tests de validación no necesitan mock de ningún servicio externo.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Any, Dict, List
from unittest.mock import patch

import pandas as pd
import pytest

from app.libreria_comun.excepciones.base import ValidacionError
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.servicios.base_analisis import BaseAnalisisService
from app.servicios.gasto_hormiga import GastoHormigaService


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURES — Datos de prueba reutilizables
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture
def contexto_paulo() -> ContextoEstrategicoIADTO:
    """Perfil de prueba de un universitario peruano típico."""
    return ContextoEstrategicoIADTO(
        nombres="Paulo César",
        ocupacion="Estudiante de Ingeniería de Sistemas",
        ingreso_mensual=Decimal("1500.00"),   # S/ 1,500/mes
        tono_ia="MOTIVADOR",
        porcentaje_meta_principal=Decimal("35.50"),
        nombre_meta_principal="Laptop para mi tesis",
        porcentaje_alerta_gasto=80,
    )


@pytest.fixture
def contexto_sin_ingreso() -> ContextoEstrategicoIADTO:
    """Perfil de universitario sin ingreso declarado (edge case)."""
    return ContextoEstrategicoIADTO(
        nombres="Ana",
        ocupacion="Universitaria",
        ingreso_mensual=Decimal("0"),
        tono_ia="AMIGABLE",
        porcentaje_meta_principal=Decimal("10.00"),
        nombre_meta_principal="Viaje de graduación",
        porcentaje_alerta_gasto=75,
    )


def _generar_transacciones(
    dias_atras: int,
    con_hormigas: bool = True,
    cantidad_por_semana: int = 3,
) -> List[Dict[str, Any]]:
    """
    Genera una lista de transacciones ficticias para los tests.

    Incluye una transacción "ancla" en el primer día del rango para
    garantizar que el span real (max_fecha - min_fecha) sea exactamente
    `dias_atras` días, independientemente de cuándo caigan los salarios.

    Args:
        dias_atras:          Cuántos días hacia atrás cubre el historial.
        con_hormigas:        Si True, incluye gastos recurrentes pequeños.
        cantidad_por_semana: Frecuencia de las hormigas.

    Returns:
        Lista de dicts en el formato que devuelve el ms-financiero.
    """
    hoy = datetime.now()
    transacciones = []
    dia_actual = hoy - timedelta(days=dias_atras)

    # ── Transacción ancla en el primer día del rango ──────────────────────────
    # Garantiza que max_fecha - min_fecha == dias_atras exactamente,
    # sin depender de cuándo cae el día 1 del mes más antiguo.
    transacciones.append({
        "id": "ancla-inicio",
        "monto": 0.01,               # Monto mínimo para no distorsionar métricas
        "tipo": "GASTO",
        "nombreCliente": "Transaccion ancla de inicio",
        "categoriaNombre": "Otros",
        "fechaTransaccion": dia_actual.isoformat(),
    })

    # ── Ingresos base (sueldo mensual) ────────────────────────────────────────
    while dia_actual <= hoy:
        if dia_actual.day == 1:
            transacciones.append({
                "id": f"ingreso-{dia_actual.strftime('%Y-%m')}",
                "monto": 1500.00,
                "tipo": "INGRESO",
                "nombreCliente": "Empresa SRL",
                "categoriaNombre": "Salario",
                "fechaTransaccion": dia_actual.isoformat(),
            })
        dia_actual += timedelta(days=1)

    if not con_hormigas:
        return transacciones

    # ── Gastos hormiga recurrentes ────────────────────────────────────────────
    # (montos < S/ 15 = 1% de S/ 1,500)
    dia_actual = hoy - timedelta(days=dias_atras)
    semana = 0
    while dia_actual <= hoy:
        if semana % 7 < cantidad_por_semana:
            # Café recurrente
            transacciones.append({
                "id": f"cafe-{dia_actual.strftime('%Y-%m-%d')}",
                "monto": 8.50,
                "tipo": "GASTO",
                "nombreCliente": "Starbucks Campus",
                "categoriaNombre": "Alimentación",
                "fechaTransaccion": dia_actual.isoformat(),
            })
            # Snack recurrente
            transacciones.append({
                "id": f"snack-{dia_actual.strftime('%Y-%m-%d')}",
                "monto": 5.00,
                "tipo": "GASTO",
                "nombreCliente": "Mini Bodega UTP",
                "categoriaNombre": "Alimentación",
                "fechaTransaccion": dia_actual.isoformat(),
            })
        semana += 1
        dia_actual += timedelta(days=1)

    # ── Gastos grandes (NO son hormigas) ─────────────────────────────────────
    transacciones.append({
        "id": "alquiler-01",
        "monto": 450.00,
        "tipo": "GASTO",
        "nombreCliente": "Propietario Dpto",
        "categoriaNombre": "Vivienda",
        "fechaTransaccion": (hoy - timedelta(days=dias_atras // 2)).isoformat(),
    })

    return transacciones


# ══════════════════════════════════════════════════════════════════════════════
# TEST 1 — FALLA CON 1 MES DE HISTORIAL
# ══════════════════════════════════════════════════════════════════════════════

class TestValidacionHistorial:
    """Tests para la Fase 1: Validación del rango histórico."""

    def test_falla_con_historial_de_1_mes(self, contexto_paulo):
        """
        ESCENARIO: El usuario tiene solo 25 días de transacciones.
        RESULTADO ESPERADO: ValidacionError con código HISTORIAL_INSUFICIENTE.
        MENSAJE ESPERADO: Contiene "3 meses" y es amigable para el usuario.
        """
        service = GastoHormigaService()
        transacciones_cortas = _generar_transacciones(dias_atras=25, con_hormigas=True)

        with pytest.raises(ValidacionError) as exc_info:
            service.run_pipeline(transacciones_cortas, contexto_paulo)

        error = exc_info.value
        assert error.codigo_error == "HISTORIAL_INSUFICIENTE", (
            f"Se esperaba HISTORIAL_INSUFICIENTE, se obtuvo: {error.codigo_error}"
        )
        assert "3 meses" in error.mensaje, (
            f"El mensaje debería mencionar '3 meses': {error.mensaje}"
        )
        assert "precisión" in error.mensaje, (
            f"El mensaje debería ser amigable y mencionar 'precisión': {error.mensaje}"
        )

    def test_falla_con_historial_de_2_meses(self, contexto_paulo):
        """
        ESCENARIO: 59 días de historial (menos de 3 meses).
        RESULTADO ESPERADO: También debe fallar.
        """
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=59)

        with pytest.raises(ValidacionError) as exc_info:
            service.run_pipeline(transacciones, contexto_paulo)

        assert exc_info.value.codigo_error == "HISTORIAL_INSUFICIENTE"

    def test_falla_con_lista_vacia(self, contexto_paulo):
        """
        ESCENARIO: Lista de transacciones completamente vacía.
        RESULTADO ESPERADO: ValidacionError con código HISTORIAL_INSUFICIENTE.
        """
        service = GastoHormigaService()

        with pytest.raises(ValidacionError) as exc_info:
            service.run_pipeline([], contexto_paulo)

        assert exc_info.value.codigo_error == "HISTORIAL_INSUFICIENTE"

    def test_falla_en_el_limite_exacto(self, contexto_paulo):
        """
        ESCENARIO: Exactamente 90 días (igual al mínimo requerido).
        RESULTADO ESPERADO: Debe FALLAR. El umbral es estricto: se necesita
        estrictamente MÁS de 90 días para garantizar datos de 3 meses completos.
        Un rango de 90 días puede representar solo ~2.5 meses reales dependiendo
        de la distribución de transacciones.
        """
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=90)

        with pytest.raises(ValidacionError) as exc_info:
            service.run_pipeline(transacciones, contexto_paulo)

        assert exc_info.value.codigo_error == "HISTORIAL_INSUFICIENTE"

    def test_pasa_exactamente_por_encima_del_limite(self, contexto_paulo):
        """
        ESCENARIO: 92 días (estrictamente más que 90 = 3 meses × 30 días).
        RESULTADO ESPERADO: No debe lanzar excepción; el pipeline continúa.
        """
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=92, con_hormigas=True)

        # No debe lanzar excepción
        resultado = service.run_pipeline(transacciones, contexto_paulo)
        assert resultado is not None
        assert resultado["modulo"] == "GASTO_HORMIGA"


# ══════════════════════════════════════════════════════════════════════════════
# TEST 2 — GENERA PROMPT CON 4 MESES DE HISTORIAL
# ══════════════════════════════════════════════════════════════════════════════

class TestGeneracionPrompt:
    """Tests para las Fases 2 y 3: Cálculos y generación del prompt."""

    def test_genera_prompt_con_4_meses_de_historial(self, contexto_paulo):
        """
        ESCENARIO: Usuario con 4 meses de historial y gastos hormiga detectables.
        RESULTADO ESPERADO:
          - run_pipeline() retorna exitosamente.
          - El resultado contiene "metricas" y "prompt".
          - El prompt menciona el nombre del usuario.
          - El prompt menciona la meta del usuario.
          - Las métricas tienen total_hormiga > 0.
        """
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)

        resultado = service.run_pipeline(transacciones, contexto_paulo)

        # ── Estructura del resultado ──────────────────────────────────────────
        assert "metricas" in resultado, "El resultado debe tener 'metricas'"
        assert "prompt" in resultado, "El resultado debe tener 'prompt'"
        assert resultado["modulo"] == "GASTO_HORMIGA"

        # ── Métricas (Fase 2) ─────────────────────────────────────────────────
        metricas = resultado["metricas"]
        assert metricas["tiene_hormigas"] is True, (
            "Con gastos pequeños recurrentes, debe detectar hormigas"
        )
        assert metricas["total_hormiga"] > 0, (
            "El total hormiga debe ser positivo"
        )
        assert metricas["impacto_mensual_estimado"] > 0, (
            "El impacto mensual debe ser positivo"
        )
        assert metricas["items_detectados"] > 0, (
            "Debe haber items detectados"
        )

        # ── Prompt (Fase 3) ───────────────────────────────────────────────────
        prompt = resultado["prompt"]
        assert len(prompt) > 100, "El prompt debe tener contenido sustancial"
        assert "Paulo" in prompt, (
            "El prompt debe personalizar con el nombre del usuario"
        )
        assert "Laptop para mi tesis" in prompt, (
            "El prompt debe mencionar la meta del usuario"
        )
        assert "MOTIVADOR" in prompt.upper() or "motivador" in prompt.lower(), (
            "El prompt debe reflejar el tono configurado"
        )

    def test_prompt_no_contiene_transacciones_crudas(self, contexto_paulo):
        """
        TEST DE SEGURIDAD DEL PIPELINE:
        El prompt enviado a Gemini NUNCA debe contener transacciones individuales
        ni el DataFrame crudo. Solo métricas resumidas.
        """
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)
        resultado = service.run_pipeline(transacciones, contexto_paulo)
        prompt = resultado["prompt"]

        # El prompt no debe contener IDs de transacciones individuales
        for tx in transacciones[:5]:  # Verificamos los primeros 5
            tx_id = str(tx.get("id", ""))
            assert tx_id not in prompt, (
                f"El prompt NO debe contener IDs de transacciones individuales: {tx_id}"
            )

        # El prompt no debe contener listas JSON de transacciones
        assert '"fechaTransaccion"' not in prompt, (
            "El prompt no debe contener JSON crudo de transacciones"
        )

    def test_prompt_felicitacion_cuando_no_hay_hormigas(self, contexto_paulo):
        """
        ESCENARIO: Usuario con más de 4 meses y sin gastos hormiga detectables.
        RESULTADO ESPERADO: Prompt de felicitación (not de advertencia).
        Usamos 125 días para garantizar que el rango supera estrictamente
        los 90 días (3 meses × 30) aunque los ingresos solo caigan el día 1.
        """
        service = GastoHormigaService()
        # Solo ingresos y gastos grandes — sin hormigas
        transacciones = _generar_transacciones(dias_atras=125, con_hormigas=False)

        resultado = service.run_pipeline(transacciones, contexto_paulo)

        metricas = resultado["metricas"]
        assert metricas["tiene_hormigas"] is False

        prompt = resultado["prompt"]
        assert len(prompt) > 50
        assert "Paulo" in prompt

    def test_metricas_contienen_campos_requeridos(self, contexto_paulo):
        """Verifica que las métricas tienen todos los campos documentados."""
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)
        resultado = service.run_pipeline(transacciones, contexto_paulo)
        metricas = resultado["metricas"]

        campos_requeridos = [
            "total_hormiga",
            "items_detectados",
            "categoria_principal",
            "impacto_mensual_estimado",
            "impacto_anual_estimado",
            "tiene_hormigas",
        ]
        for campo in campos_requeridos:
            assert campo in metricas, (
                f"Las métricas deben contener el campo '{campo}'"
            )

    def test_impacto_anual_es_doce_veces_mensual(self, contexto_paulo):
        """El impacto anual debe ser aproximadamente 12x el mensual."""
        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)
        resultado = service.run_pipeline(transacciones, contexto_paulo)
        metricas = resultado["metricas"]

        if metricas["tiene_hormigas"]:
            anual = metricas["impacto_anual_estimado"]
            mensual = metricas["impacto_mensual_estimado"]
            assert abs(anual - mensual * 12) < 1.0, (
                f"Impacto anual ({anual}) debe ser ~12x mensual ({mensual})"
            )


# ══════════════════════════════════════════════════════════════════════════════
# TEST 3 — X-CORRELATION-ID SE MANTIENE EN LOS LOGS
# ══════════════════════════════════════════════════════════════════════════════

class TestTrazabilidadCorrelationId:
    """Tests para verificar que el X-Correlation-ID se propaga en los logs."""

    def test_correlation_id_aparece_en_logs_del_pipeline(
        self,
        contexto_paulo,
        caplog,
    ):
        """
        ESCENARIO: Pipeline ejecutado con un Correlation-ID específico.
        RESULTADO ESPERADO:
          - El Correlation-ID aparece en al menos 1 mensaje de log del pipeline.
          - El ID se mantiene consistente durante toda la ejecución.
        """
        correlation_id_prueba = "test-correl-abc123xyz"

        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)

        # Inyectamos el Correlation-ID en el contexto asíncrono
        with patch(
            "app.servicios.base_analisis.get_correlation_id",
            return_value=correlation_id_prueba,
        ):
            with caplog.at_level(logging.INFO):
                resultado = service.run_pipeline(transacciones, contexto_paulo)

        # ── Verificar que el pipeline se ejecutó ──────────────────────────────
        assert resultado is not None, "El pipeline debe completarse exitosamente"

        # ── Verificar que el Correlation-ID está en los logs ──────────────────
        logs_con_correlation = [
            record.message
            for record in caplog.records
            if correlation_id_prueba in record.message
        ]

        assert len(logs_con_correlation) >= 2, (
            f"El Correlation-ID '{correlation_id_prueba}' debe aparecer en al menos "
            f"2 mensajes de log (inicio y fin del pipeline). "
            f"Logs actuales: {[r.message for r in caplog.records]}"
        )

    def test_correlation_id_en_log_de_validacion_exitosa(
        self,
        contexto_paulo,
        caplog,
    ):
        """El Correlation-ID debe estar presente en el log de validación de Fase 1."""
        correlation_id_prueba = "trace-fase1-validation"

        service = GastoHormigaService()
        transacciones = _generar_transacciones(dias_atras=120, con_hormigas=True)

        with patch(
            "app.servicios.base_analisis.get_correlation_id",
            return_value=correlation_id_prueba,
        ):
            with caplog.at_level(logging.INFO):
                service.run_pipeline(transacciones, contexto_paulo)

        # Buscar log específico de la Fase 1
        logs_fase1 = [
            r.message for r in caplog.records
            if "F1" in r.message and correlation_id_prueba in r.message
        ]
        assert len(logs_fase1) >= 1, (
            f"Debe haber al menos 1 log de Fase 1 con el Correlation-ID. "
            f"Logs disponibles: {[r.message for r in caplog.records]}"
        )

    def test_correlation_id_en_log_de_error_historial(
        self,
        contexto_paulo,
        caplog,
    ):
        """El Correlation-ID debe estar presente incluso cuando falla la validación."""
        correlation_id_prueba = "trace-error-historial"

        service = GastoHormigaService()
        transacciones_cortas = _generar_transacciones(dias_atras=25)

        with patch(
            "app.servicios.base_analisis.get_correlation_id",
            return_value=correlation_id_prueba,
        ):
            with caplog.at_level(logging.WARNING):
                with pytest.raises(ValidacionError):
                    service.run_pipeline(transacciones_cortas, contexto_paulo)

        logs_con_correlation = [
            r.message for r in caplog.records
            if correlation_id_prueba in r.message
        ]
        assert len(logs_con_correlation) >= 1, (
            "El Correlation-ID debe aparecer en los logs incluso cuando el pipeline falla."
        )


# ══════════════════════════════════════════════════════════════════════════════
# TESTS ADICIONALES — ContextoEstrategicoIADTO
# ══════════════════════════════════════════════════════════════════════════════

class TestContextoEstrategicoIADTO:
    """Tests para el DTO de contexto estratégico."""

    def test_umbral_hormiga_es_1_por_ciento_del_ingreso(self):
        """El umbral de gasto hormiga debe ser exactamente el 1% del ingreso."""
        contexto = ContextoEstrategicoIADTO(
            nombres="Test",
            ocupacion="Estudiante",
            ingreso_mensual=Decimal("2000.00"),
            tono_ia="FORMAL",
            porcentaje_meta_principal=Decimal("50.00"),
            nombre_meta_principal="Meta test",
            porcentaje_alerta_gasto=80,
        )
        assert contexto.umbral_gasto_hormiga == Decimal("20.00"), (
            "El 1% de S/ 2,000 debe ser S/ 20.00"
        )

    def test_acepta_camelcase_de_java(self):
        """El DTO debe deserializarse correctamente desde JSON camelCase de Java."""
        datos_java = {
            "nombres": "María",
            "ocupacion": "Contadora",
            "ingresoMensual": "3000.00",   # camelCase automático
            "tonoIA": "DIRECTO",            # alias EXPLÍCITO: IA en mayúsculas, como en Java
            "porcentajeMetaPrincipal": "60.00",
            "nombreMetaPrincipal": "Auto",
            "porcentajeAlertaGasto": 75,
        }
        contexto = ContextoEstrategicoIADTO.model_validate(datos_java)
        assert contexto.ingreso_mensual == Decimal("3000.00")
        assert contexto.tono_ia == "DIRECTO"
        assert contexto.nombres == "María"

    def test_tono_invalido_lanza_error(self):
        """Un tono no reconocido debe lanzar un error de validación."""
        with pytest.raises(Exception):  # ValueError o ValidationError
            ContextoEstrategicoIADTO(
                nombres="Test",
                ocupacion="Test",
                ingreso_mensual=Decimal("1000"),
                tono_ia="AGRESIVO",  # No es un valor válido
                porcentaje_meta_principal=Decimal("50"),
                nombre_meta_principal="Test",
                porcentaje_alerta_gasto=80,
            )

    def test_primer_nombre_extrae_correctamente(self):
        """La propiedad primer_nombre debe retornar solo el primer nombre."""
        contexto = ContextoEstrategicoIADTO(
            nombres="Paulo César Moron Poma",
            ocupacion="Dev",
            ingreso_mensual=Decimal("1500"),
            tono_ia="AMIGABLE",
            porcentaje_meta_principal=Decimal("35"),
            nombre_meta_principal="Meta",
            porcentaje_alerta_gasto=80,
        )
        assert contexto.primer_nombre == "Paulo"

    def test_umbral_cero_cuando_no_hay_ingreso(self):
        """Con ingreso = 0, el umbral debe ser 0 (se manejará en el módulo)."""
        contexto = ContextoEstrategicoIADTO(
            nombres="Sin Ingreso",
            ocupacion="Desempleado",
            ingreso_mensual=Decimal("0"),
            tono_ia="AMIGABLE",
            porcentaje_meta_principal=Decimal("0"),
            nombre_meta_principal="Sin meta",
            porcentaje_alerta_gasto=80,
        )
        assert contexto.umbral_gasto_hormiga == Decimal("0")


# ══════════════════════════════════════════════════════════════════════════════
# TESTS DE INTEGRACIÓN — Inyección directa de DataFrames
# ══════════════════════════════════════════════════════════════════════════════

class TestInyeccionDirectaDataFrame:
    """
    Tests de integración que inyectan DataFrames directamente en la Fase 2.
    Útil para verificar la lógica de cálculo sin pasar por la Fase 1.
    """

    def test_calculos_con_dataframe_inyectado(self, contexto_paulo):
        """
        Inyecta un DataFrame pre-construido directamente en ejecutar_calculos().
        Esto permite testear la Fase 2 en aislamiento total.
        """
        service = GastoHormigaService()

        # Construimos el DataFrame manualmente
        hoy = datetime.now()
        datos = {
            "monto": [8.50, 8.50, 8.50, 5.00, 5.00, 5.00, 450.00],
            "tipo": ["GASTO"] * 7,
            "nombreCliente": [
                "Café Campus", "Café Campus", "Café Campus",
                "Snack Bar", "Snack Bar", "Snack Bar",
                "Alquiler",
            ],
            "categoriaNombre": [
                "Alimentación", "Alimentación", "Alimentación",
                "Alimentación", "Alimentación", "Alimentación",
                "Vivienda",
            ],
            "fechaTransaccion": [
                (hoy - timedelta(days=d)).isoformat()
                for d in [10, 25, 40, 12, 28, 43, 60]
            ],
        }
        df = pd.DataFrame(datos)

        # Llamamos directamente a la Fase 2
        metricas = service.ejecutar_calculos(df, contexto_paulo)

        assert metricas["tiene_hormigas"] is True
        assert metricas["total_hormiga"] == pytest.approx(25.50 + 15.00, abs=0.01)
        assert metricas["comercios_unicos"] == 2  # Café Campus + Snack Bar

    def test_calculos_ignoran_gastos_grandes(self, contexto_paulo):
        """
        Un gasto grande (> 1% del ingreso = > S/ 15) NO debe clasificarse como hormiga.
        """
        service = GastoHormigaService()
        hoy = datetime.now()

        datos = {
            "monto": [500.00, 500.00, 500.00],     # Gastos grandes
            "tipo": ["GASTO"] * 3,
            "nombreCliente": ["Renta", "Renta", "Renta"],
            "categoriaNombre": ["Vivienda"] * 3,
            "fechaTransaccion": [
                (hoy - timedelta(days=d)).isoformat()
                for d in [10, 40, 70]
            ],
        }
        df = pd.DataFrame(datos)
        metricas = service.ejecutar_calculos(df, contexto_paulo)

        # Los gastos de S/ 500 no son hormigas (superan el umbral de S/ 15)
        assert metricas["tiene_hormigas"] is False


# ══════════════════════════════════════════════════════════════════════════════
# RUNNER DIRECTO (para ejecutar sin pytest CLI)
# ══════════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    import sys
    sys.exit(pytest.main([__file__, "-v", "--tb=short"]))