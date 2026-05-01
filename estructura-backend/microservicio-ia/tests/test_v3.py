"""
tests/test_v3.py
══════════════════════════════════════════════════════════════════════════════
Tests unitarios para la refactorización v3.

Cobertura:
  1. EventoAnalisisIA v3     — historial, TipoSolicitud, propiedades calculadas
  2. ResumenMes              — variación, categoría con exceso, balance
  3. ResultadoAnalisisIA     — serialización, metadata_grafico
  4. IngenierioPrompt v3     — secciones historial, comparación, todos los módulos
  5. CoachIA._resolver_modulo — lógica de despacho
  6. CoachIA._calcular_kpi   — KPIs por módulo
  7. CoachIA._generar_metadata_grafico — por módulo
  8. ConsumidorIA._callback  — ACK/NACK dual-mode (sin RabbitMQ real)

Ejecutar:
    pytest tests/test_v3.py -v
══════════════════════════════════════════════════════════════════════════════
"""

import json
import pytest
from datetime import datetime
from unittest.mock import MagicMock, patch

from app.modelos.evento_analisis import (
    EventoAnalisisIA,
    ResumenMes,
    ResultadoAnalisisIA,
    MetadataGrafico,
    PuntoGrafico,
    TipoModulo,
    TipoSolicitud,
    TipoMovimiento,
    TransaccionEvento,
    ContextoUsuario,
    PerfilFinanciero,
    MetaAhorro,
    LimiteGlobal,
    TonoIA,
)
from app.servicios.ingeniero_prompt import IngenieroPrompt


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURES
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture
def historial_6_meses():
    """6 meses de datos con tendencia alcista en gastos."""
    return [
        {"anio": 2025, "mes": 11, "totalIngresos": 3000.0, "totalGastos": 1800.0,
         "gastosPorCategoria": {"Alimentación": 600.0, "Transporte": 400.0, "Entretenimiento": 800.0}},
        {"anio": 2025, "mes": 12, "totalIngresos": 3200.0, "totalGastos": 2000.0,
         "gastosPorCategoria": {"Alimentación": 650.0, "Transporte": 420.0, "Entretenimiento": 930.0}},
        {"anio": 2026, "mes": 1,  "totalIngresos": 3000.0, "totalGastos": 1900.0,
         "gastosPorCategoria": {"Alimentación": 700.0, "Transporte": 300.0, "Entretenimiento": 900.0}},
        {"anio": 2026, "mes": 2,  "totalIngresos": 3100.0, "totalGastos": 2100.0,
         "gastosPorCategoria": {"Alimentación": 750.0, "Transporte": 450.0, "Entretenimiento": 900.0}},
        {"anio": 2026, "mes": 3,  "totalIngresos": 3050.0, "totalGastos": 1950.0,
         "gastosPorCategoria": {"Alimentación": 700.0, "Transporte": 400.0, "Entretenimiento": 850.0}},
        {"anio": 2026, "mes": 4,  "totalIngresos": 3100.0, "totalGastos": 2200.0,
         "gastosPorCategoria": {"Alimentación": 800.0, "Transporte": 400.0, "Entretenimiento": 1000.0}},
    ]


@pytest.fixture
def evento_transaccion_completo(historial_6_meses):
    return EventoAnalisisIA.desde_json({
        "id_usuario": "user-abc-123",
        "tipo_solicitud": "TRANSACCION_RECIENTE",
        "transaccion": {
            "monto": 45.0,
            "descripcion": "Cine Cinepolis",
            "categoria": "Entretenimiento",
            "tipo": "GASTO",
        },
        "historial_mensual": historial_6_meses,
        "contexto": {
            "perfilFinanciero": {
                "ocupacion": "Desarrollador",
                "ingresoMensual": 3100.0,
                "tonoIA": "Motivador",
            },
            "metas": [{"nombre": "Viaje a Europa", "montoObjetivo": 8000.0, "montoActual": 1200.0}],
            "limiteGlobal": {"montoLimite": 2000.0, "porcentajeAlerta": 80, "activo": True},
        },
    })


@pytest.fixture
def evento_consulta_modulo(historial_6_meses):
    return EventoAnalisisIA.desde_json({
        "id_usuario": "user-xyz-456",
        "tipo_solicitud": "CONSULTA_MODULO",
        "modulo_solicitado": "PREDICCION_GASTOS",
        "historial_mensual": historial_6_meses,
        "contexto": {
            "perfilFinanciero": {
                "ocupacion": "Estudiante",
                "ingresoMensual": 1200.0,
                "tonoIA": "Amigable",
            },
        },
    })


@pytest.fixture
def ingeniero():
    return IngenieroPrompt()


# ══════════════════════════════════════════════════════════════════════════════
# TEST 1: ResumenMes
# ══════════════════════════════════════════════════════════════════════════════

class TestResumenMes:

    def test_balance_positivo(self):
        mes = ResumenMes(anio=2026, mes=4, totalIngresos=3000.0, totalGastos=2000.0)
        assert mes.balance == 1000.0

    def test_balance_negativo(self):
        mes = ResumenMes(anio=2026, mes=4, totalIngresos=1500.0, totalGastos=2000.0)
        assert mes.balance == -500.0

    def test_periodo_label(self):
        mes = ResumenMes(anio=2026, mes=4, totalIngresos=0, totalGastos=0)
        assert mes.periodo_label == "2026-04"

    def test_variacion_gastos_subio(self):
        anterior = ResumenMes(anio=2026, mes=3, totalIngresos=3000, totalGastos=1800)
        actual   = ResumenMes(anio=2026, mes=4, totalIngresos=3000, totalGastos=2200)
        assert actual.variacion_gastos_vs(anterior) == pytest.approx(22.2, abs=0.1)

    def test_variacion_gastos_bajo(self):
        anterior = ResumenMes(anio=2026, mes=3, totalIngresos=3000, totalGastos=2000)
        actual   = ResumenMes(anio=2026, mes=4, totalIngresos=3000, totalGastos=1600)
        assert actual.variacion_gastos_vs(anterior) == -20.0

    def test_variacion_sin_mes_anterior_retorna_cero(self):
        anterior = ResumenMes(anio=2026, mes=3, totalIngresos=0, totalGastos=0)
        actual   = ResumenMes(anio=2026, mes=4, totalIngresos=3000, totalGastos=2000)
        assert actual.variacion_gastos_vs(anterior) == 0.0

    def test_categoria_mayor_exceso(self):
        anterior = ResumenMes(anio=2026, mes=3, totalIngresos=3000, totalGastos=1900,
                              gastosPorCategoria={"Alimentación": 700, "Entretenimiento": 800})
        actual   = ResumenMes(anio=2026, mes=4, totalIngresos=3100, totalGastos=2200,
                              gastosPorCategoria={"Alimentación": 800, "Entretenimiento": 1000})
        cat = actual.categoria_con_mayor_exceso_vs(anterior)
        assert cat == "Entretenimiento"

    def test_categoria_mayor_exceso_sin_datos_retorna_none(self):
        anterior = ResumenMes(anio=2026, mes=3, totalIngresos=0, totalGastos=0)
        actual   = ResumenMes(anio=2026, mes=4, totalIngresos=0, totalGastos=0)
        assert actual.categoria_con_mayor_exceso_vs(anterior) is None


# ══════════════════════════════════════════════════════════════════════════════
# TEST 2: EventoAnalisisIA v3
# ══════════════════════════════════════════════════════════════════════════════

class TestEventoAnalisisIAv3:

    def test_mes_actual_es_el_mas_reciente(self, evento_transaccion_completo):
        mes = evento_transaccion_completo.mes_actual
        assert mes.anio == 2026
        assert mes.mes == 4

    def test_mes_anterior_es_el_segundo_mas_reciente(self, evento_transaccion_completo):
        mes = evento_transaccion_completo.mes_anterior
        assert mes.anio == 2026
        assert mes.mes == 3

    def test_tiene_historial_true(self, evento_transaccion_completo):
        assert evento_transaccion_completo.tiene_historial is True

    def test_sin_historial_mes_actual_es_none(self):
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "Test", "categoria": "X", "tipo": "GASTO"},
        })
        assert evento.mes_actual is None
        assert evento.mes_anterior is None

    def test_tipo_solicitud_default_es_transaccion_reciente(self):
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "Test", "categoria": "X", "tipo": "GASTO"},
        })
        assert evento.tipo_solicitud == TipoSolicitud.TRANSACCION_RECIENTE

    def test_consulta_modulo_tiene_modulo_solicitado(self, evento_consulta_modulo):
        assert evento_consulta_modulo.tipo_solicitud == TipoSolicitud.CONSULTA_MODULO
        assert evento_consulta_modulo.modulo_solicitado == TipoModulo.PREDICCION_GASTOS

    def test_evento_sin_transaccion_en_consulta_modulo(self, historial_6_meses):
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "user-1",
            "tipo_solicitud": "CONSULTA_MODULO",
            "modulo_solicitado": "REPORTE_COMPLETO",
            "historial_mensual": historial_6_meses,
        })
        assert evento.transaccion is None
        assert evento.tipo_solicitud == TipoSolicitud.CONSULTA_MODULO

    def test_id_usuario_requerido(self):
        with pytest.raises(Exception):
            EventoAnalisisIA.desde_json({
                "transaccion": {"monto": 10.0, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
            })

    def test_desde_json_bytes(self, historial_6_meses):
        data = json.dumps({
            "id_usuario": "user-test",
            "tipo_solicitud": "TRANSACCION_RECIENTE",
            "transaccion": {"monto": 25.0, "descripcion": "Cafe", "categoria": "Snacks", "tipo": "GASTO"},
            "historial_mensual": historial_6_meses,
        }).encode("utf-8")
        evento = EventoAnalisisIA.desde_json(data)
        assert evento.id_usuario == "user-test"
        assert evento.tiene_historial is True


# ══════════════════════════════════════════════════════════════════════════════
# TEST 3: ResultadoAnalisisIA
# ══════════════════════════════════════════════════════════════════════════════

class TestResultadoAnalisisIA:

    def test_serializacion_fecha_como_iso(self):
        resultado = ResultadoAnalisisIA(
            id_usuario="user-1",
            consejo_texto="Buen trabajo ahorrando este mes.",
            tipo_modulo=TipoModulo.COMPARACION_MENSUAL,
        )
        d = resultado.a_dict_serializable()
        assert isinstance(d["fecha_generacion"], str)
        # Verificar que es parseable como ISO
        datetime.fromisoformat(d["fecha_generacion"])

    def test_id_autogenerado(self):
        r1 = ResultadoAnalisisIA(id_usuario="u1", consejo_texto="X", tipo_modulo=TipoModulo.ANOMALIAS)
        r2 = ResultadoAnalisisIA(id_usuario="u1", consejo_texto="X", tipo_modulo=TipoModulo.ANOMALIAS)
        assert r1.id != r2.id

    def test_metadata_grafico_opcional(self):
        resultado = ResultadoAnalisisIA(
            id_usuario="user-1",
            consejo_texto="Consejo sin gráfico.",
            tipo_modulo=TipoModulo.AUTOCLASIFICACION,
        )
        assert resultado.metadata_grafico is None

    def test_metadata_grafico_con_datos(self):
        meta = MetadataGrafico(
            tipo_grafico="line",
            titulo="Test",
            datos=[PuntoGrafico(etiqueta="Ene", valor=100.0)],
        )
        resultado = ResultadoAnalisisIA(
            id_usuario="user-1",
            consejo_texto="Con gráfico.",
            tipo_modulo=TipoModulo.PREDICCION_GASTOS,
            metadata_grafico=meta,
        )
        assert resultado.metadata_grafico.tipo_grafico == "line"
        assert len(resultado.metadata_grafico.datos) == 1

    def test_kpi_opcional(self):
        resultado = ResultadoAnalisisIA(
            id_usuario="u", consejo_texto="X", tipo_modulo=TipoModulo.GASTO_HORMIGA,
            kpi_principal=250.0, kpi_label="S/ en gastos hormiga este mes"
        )
        assert resultado.kpi_principal == 250.0
        assert "hormiga" in resultado.kpi_label


# ══════════════════════════════════════════════════════════════════════════════
# TEST 4: IngenierioPrompt v3
# ══════════════════════════════════════════════════════════════════════════════

class TestIngenierioPromptV3:

    def test_prompt_contiene_historial(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.TRANSACCION_AUTOMATICA)
        assert "Historial financiero" in prompt

    def test_prompt_contiene_periodos_del_historial(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.TRANSACCION_AUTOMATICA)
        assert "2026-04" in prompt
        assert "2026-03" in prompt

    def test_prompt_comparacion_sube_menciona_exceso(self, ingeniero, evento_transaccion_completo):
        # El historial tiene subida en abril vs marzo
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.COMPARACION_MENSUAL)
        assert "subieron" in prompt.lower() or "exceso" in prompt.lower() or "subió" in prompt.lower()

    def test_prompt_sin_historial_no_falla(self, ingeniero):
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "Café", "categoria": "Snacks", "tipo": "GASTO"},
        })
        prompt = ingeniero.construir(evento, TipoModulo.TRANSACCION_AUTOMATICA)
        assert len(prompt) > 100
        assert "Sin historial" in prompt or "historial" in prompt.lower()

    def test_prompt_prediccion_no_incluye_transaccion(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.PREDICCION_GASTOS)
        # El módulo predicción no debe incluir la sección de transacción individual
        assert "Transacción analizada" not in prompt

    def test_prompt_autoclasificacion_incluye_transaccion(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.AUTOCLASIFICACION)
        assert "Transacción analizada" in prompt
        assert "Cine Cinepolis" in prompt

    def test_system_prompt_prediccion_menciona_tendencia(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.PREDICCION_GASTOS)
        assert "predictivo" in prompt.lower() or "proyect" in prompt.lower() or "tendencia" in prompt.lower()

    def test_system_prompt_hormiga_menciona_micro(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.GASTO_HORMIGA)
        assert "hormiga" in prompt.lower() or "micro" in prompt.lower() or "pequeño" in prompt.lower()

    def test_system_prompt_autoclasificacion_menciona_categoria(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.AUTOCLASIFICACION)
        assert "clasificación" in prompt.lower() or "taxonomía" in prompt.lower() or "categoría" in prompt.lower()

    def test_tono_motivador_en_prompt(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.TRANSACCION_AUTOMATICA)
        assert "energético" in prompt.lower() or "motivador" in prompt.lower() or "alentador" in prompt.lower()

    def test_tendencia_alcista_detectada(self, ingeniero, historial_6_meses):
        # Crear historial con tendencia alcista clara
        historial_alcista = [
            {"anio": 2026, "mes": 1, "totalIngresos": 3000, "totalGastos": 1000},
            {"anio": 2026, "mes": 2, "totalIngresos": 3000, "totalGastos": 1200},
            {"anio": 2026, "mes": 3, "totalIngresos": 3000, "totalGastos": 1400},
        ]
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "u1",
            "transaccion": {"monto": 50.0, "descripcion": "Test", "categoria": "X", "tipo": "GASTO"},
            "historial_mensual": historial_alcista,
        })
        prompt = ingeniero.construir(evento, TipoModulo.TRANSACCION_AUTOMATICA)
        assert "ALCISTA" in prompt or "aumentado" in prompt.lower()

    def test_consulta_modulo_usa_modulo_solicitado(self, ingeniero, evento_consulta_modulo):
        prompt = ingeniero.construir(evento_consulta_modulo, TipoModulo.PREDICCION_GASTOS)
        assert "predictivo" in prompt.lower() or "proyect" in prompt.lower()

    def test_prompt_reporte_completo_menciona_cfo(self, ingeniero, evento_transaccion_completo):
        prompt = ingeniero.construir(evento_transaccion_completo, TipoModulo.REPORTE_COMPLETO)
        assert "CFO" in prompt or "Director Financiero" in prompt


# ══════════════════════════════════════════════════════════════════════════════
# TEST 5: CoachIA — resolver_modulo y calcular_kpi (sin Gemini real)
# ══════════════════════════════════════════════════════════════════════════════

class TestCoachIALogica:
    """Prueba la lógica interna de CoachIA sin llamar a Gemini."""

    def _crear_coach_sin_gemini(self):
        with patch("app.servicios.coach_ia.genai") as mock_genai:
            from app.servicios.coach_ia import CoachIA
            coach = CoachIA.__new__(CoachIA)
            coach._ingeniero = IngenieroPrompt()
            coach._modelo = MagicMock()
            return coach

    def test_resolver_modulo_transaccion_reciente(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        modulo = coach._resolver_modulo(evento_transaccion_completo)
        assert modulo == TipoModulo.TRANSACCION_AUTOMATICA

    def test_resolver_modulo_consulta_usa_solicitado(self, evento_consulta_modulo):
        coach = self._crear_coach_sin_gemini()
        modulo = coach._resolver_modulo(evento_consulta_modulo)
        assert modulo == TipoModulo.PREDICCION_GASTOS

    def test_resolver_modulo_consulta_sin_modulo_solicitado(self):
        coach = self._crear_coach_sin_gemini()
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "u1",
            "tipo_solicitud": "CONSULTA_MODULO",
            # modulo_solicitado ausente
        })
        modulo = coach._resolver_modulo(evento)
        assert modulo == TipoModulo.TRANSACCION_AUTOMATICA

    def test_kpi_comparacion_mensual(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        kpi, label = coach._calcular_kpi(evento_transaccion_completo, TipoModulo.COMPARACION_MENSUAL)
        assert kpi is not None
        assert "%" in label

    def test_kpi_capacidad_ahorro(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        kpi, label = coach._calcular_kpi(evento_transaccion_completo, TipoModulo.CAPACIDAD_AHORRO)
        assert kpi is not None
        assert kpi >= 0

    def test_kpi_prediccion_gastos(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        kpi, label = coach._calcular_kpi(evento_transaccion_completo, TipoModulo.PREDICCION_GASTOS)
        assert kpi is not None
        assert kpi > 0

    def test_metadata_prediccion_retorna_line(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        meta = coach._meta_prediccion(evento_transaccion_completo)
        assert meta is not None
        assert meta.tipo_grafico == "line"
        assert len(meta.datos) == 6

    def test_metadata_comparacion_retorna_bar(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        meta = coach._meta_comparacion(evento_transaccion_completo)
        assert meta is not None
        assert meta.tipo_grafico == "bar"

    def test_metadata_ahorro_retorna_doughnut(self, evento_transaccion_completo):
        coach = self._crear_coach_sin_gemini()
        meta = coach._meta_ahorro(evento_transaccion_completo)
        assert meta is not None
        assert meta.tipo_grafico == "doughnut"

    def test_metadata_sin_historial_retorna_none(self):
        coach = self._crear_coach_sin_gemini()
        evento = EventoAnalisisIA.desde_json({
            "id_usuario": "u1",
            "transaccion": {"monto": 10, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
        })
        assert coach._meta_prediccion(evento) is None
        assert coach._meta_estacionalidad(evento) is None


# ══════════════════════════════════════════════════════════════════════════════
# TEST 6: ConsumidorIA — callback dual-mode (sin RabbitMQ real)
# ══════════════════════════════════════════════════════════════════════════════

class TestConsumidorDualMode:

    def _crear_consumidor_mock(self):
        with patch("app.mensajeria.consumidor_ia.CoachIA") as mock_coach:
            from app.mensajeria.consumidor_ia import ConsumidorIA
            import threading

            consumidor = ConsumidorIA.__new__(ConsumidorIA)
            consumidor._config  = MagicMock()
            consumidor._config.cola_dashboard_consejos = "cola.dashboard.consejos"
            consumidor._config.cola_dashboard_modulos  = "cola.dashboard.modulos"
            consumidor._config.exchange_dashboard       = "exchange.dashboard"
            consumidor._coach   = MagicMock()
            consumidor._activo  = threading.Event()

        canal   = MagicMock()
        metodo  = MagicMock()
        metodo.delivery_tag = 99

        return consumidor, canal, metodo

    def _resultado_mock(self, tipo_modulo=TipoModulo.TRANSACCION_AUTOMATICA):
        return ResultadoAnalisisIA(
            id_usuario="user-1",
            consejo_texto="Consejo de prueba generado.",
            tipo_modulo=tipo_modulo,
            kpi_principal=12.5,
            kpi_label="% vs mes anterior",
        )

    def test_transaccion_reciente_publica_en_cola_consejos(self, evento_transaccion_completo):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        consumidor._coach.analizar.return_value = self._resultado_mock()
        cuerpo = json.dumps({
            "id_usuario": "user-1",
            "tipo_solicitud": "TRANSACCION_RECIENTE",
            "transaccion": {"monto": 45.0, "descripcion": "Cine", "categoria": "Entret.", "tipo": "GASTO"},
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_ack.assert_called_once_with(delivery_tag=99)
        kwargs = canal.basic_publish.call_args.kwargs
        assert kwargs["routing_key"] == "cola.dashboard.consejos"

    def test_consulta_modulo_publica_en_cola_modulos(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        consumidor._coach.analizar.return_value = self._resultado_mock(TipoModulo.PREDICCION_GASTOS)
        cuerpo = json.dumps({
            "id_usuario": "user-2",
            "tipo_solicitud": "CONSULTA_MODULO",
            "modulo_solicitado": "PREDICCION_GASTOS",
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_ack.assert_called_once_with(delivery_tag=99)
        kwargs = canal.basic_publish.call_args.kwargs
        assert kwargs["routing_key"] == "cola.dashboard.modulos"

    def test_json_invalido_emite_ack_descarta(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        cuerpo = b"{ json: invalido }"

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_ack.assert_called_once_with(delivery_tag=99)
        consumidor._coach.analizar.assert_not_called()

    def test_sin_id_usuario_emite_ack_descarta(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        cuerpo = json.dumps({
            "transaccion": {"monto": 10.0, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_ack.assert_called_once_with(delivery_tag=99)
        consumidor._coach.analizar.assert_not_called()

    def test_coach_retorna_none_emite_nack_sin_requeue(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        consumidor._coach.analizar.return_value = None
        cuerpo = json.dumps({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_nack.assert_called_once_with(delivery_tag=99, requeue=False)
        canal.basic_ack.assert_not_called()

    def test_error_publicacion_emite_nack_con_requeue(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        consumidor._coach.analizar.return_value = self._resultado_mock()
        canal.basic_publish.side_effect = Exception("Broker no disponible")
        cuerpo = json.dumps({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        canal.basic_nack.assert_called_once_with(delivery_tag=99, requeue=True)

    def test_payload_publicado_contiene_consejo_y_modulo(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        resultado = self._resultado_mock()
        consumidor._coach.analizar.return_value = resultado
        cuerpo = json.dumps({
            "id_usuario": "user-1",
            "transaccion": {"monto": 10.0, "descripcion": "X", "categoria": "Y", "tipo": "GASTO"},
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        body = canal.basic_publish.call_args.kwargs["body"]
        payload = json.loads(body.decode("utf-8"))
        assert payload["consejo_texto"] == "Consejo de prueba generado."
        assert payload["tipo_modulo"] == TipoModulo.TRANSACCION_AUTOMATICA.value

    def test_headers_mensaje_contienen_tipo_modulo(self):
        consumidor, canal, metodo = self._crear_consumidor_mock()
        consumidor._coach.analizar.return_value = self._resultado_mock(TipoModulo.GASTO_HORMIGA)
        cuerpo = json.dumps({
            "id_usuario": "user-1",
            "tipo_solicitud": "CONSULTA_MODULO",
            "modulo_solicitado": "GASTO_HORMIGA",
        }).encode()

        consumidor._callback(canal, metodo, MagicMock(), cuerpo)

        props = canal.basic_publish.call_args.kwargs["properties"]
        assert props.headers["tipo_modulo"] == TipoModulo.GASTO_HORMIGA.value
        assert props.headers["version"] == "3.0"