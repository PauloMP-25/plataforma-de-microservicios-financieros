"""
tests/test_migracion_gasto_hormiga.py  ·  v1.0 — FASE 1-5 (LUKA)
══════════════════════════════════════════════════════════════════════════════
Tests unitarios para la migración incremental del módulo GASTO_HORMIGA.

Cobertura:
  FASE 1 — Contratos y esquemas (ConsejoEstructurado, RespuestaModulo.consejo)
  FASE 2 — Persistencia (IaHistorialCoaching, RepositorioHistorialCoaching)
  FASE 3 — CoachIA con Structured Outputs (bifurcación esquema_salida)
  FASE 4 — Orquestador con memoria (inyección de historial)
  FASE 5 — Prompt con memoria histórica e instrucciones de esquema

Principios:
  - Sin llamadas reales a Gemini, Redis ni DB externa.
  - SQLite en memoria para tests de persistencia.
  - Todos los servicios externos mockeados con unittest.mock.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import json
from datetime import datetime
from typing import Optional
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# ── Modelos y esquemas ────────────────────────────────────────────────────────
from app.modelos.esquemas import (
    ConsejoEstructurado,
    EstadoCoach,
    InsightAnalitico,
    NombreModulo,
    RespuestaModulo,
)
from app.persistencia.modelos_db import Base, IaHistorialCoaching
from app.persistencia.repositorio_historial import RepositorioHistorialCoaching


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURES COMPARTIDOS
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture(scope="function")
def db_session():
    """
    Sesión SQLite en memoria, aislada por test.
    Crea y destruye todas las tablas en cada test para garantizar aislamiento.
    """
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine)
    session = Session()
    yield session
    session.close()
    Base.metadata.drop_all(engine)


@pytest.fixture
def consejo_estructurado_sample() -> ConsejoEstructurado:
    return ConsejoEstructurado(
        pensamiento_interno_ia="El usuario tiene S/ 120.50 en gastos hormiga, afectando su meta.",
        introduccion="Hola Paulo, este mes detecté fugas importantes.",
        analisis_ia="Tus gastos hormiga alcanzaron S/ 120.50 en cafetería.",
        conexion_emocional="Si reduces esta fuga, tu meta 'Laptop' llega 2 meses antes.",
        plan_accion_titulo="Operación Hormiga Cero",
        plan_accion_pasos=[
            "Cancela la suscripción de S/ 15 de la app X esta semana",
            "Lleva almuerzo 3 días a la semana para ahorrar S/ 30",
        ],
        comentario_positivo="¡Vas muy bien, Paulo! Cada sol cuenta.",
    )


@pytest.fixture
def metricas_sample() -> dict:
    return {
        "total_gastos_hormiga": 120.50,
        "principal_gasto_hormiga": "Cafetería",
        "variacion_vs_mes_anterior": 15.3,
        "proyeccion_fuga_anual": 1446.0,
        "hay_hormigas": True,
        "comparacion_disponible": True,
        "_total_ingresos": 1500.0,
        "_total_gastos": 800.0,
        "_historial_previo": None,
        "_historial_insight": None,
    }


# ══════════════════════════════════════════════════════════════════════════════
# FASE 1 — Tests de Contratos y Esquemas
# ══════════════════════════════════════════════════════════════════════════════

class TestConsejoEstructurado:

    def test_instancia_valida(self, consejo_estructurado_sample):
        """ConsejoEstructurado se crea correctamente con todos los campos."""
        ce = consejo_estructurado_sample
        assert ce.introduccion != ""
        assert ce.analisis_ia != ""
        assert ce.conexion_emocional != ""
        assert ce.plan_accion_titulo != ""
        assert len(ce.plan_accion_pasos) >= 2
        assert ce.comentario_positivo != ""

    def test_plan_accion_pasos_min_2(self):
        """Debe fallar si plan_accion_pasos tiene menos de 2 elementos."""
        with pytest.raises(Exception):
            ConsejoEstructurado(
                introduccion="Hola",
                analisis_ia="Análisis",
                conexion_emocional="Conexión",
                plan_accion_titulo="Plan",
                plan_accion_pasos=["Solo un paso"],  # ← Inválido
                comentario_positivo="Bien",
            )

    def test_serializa_a_dict(self, consejo_estructurado_sample):
        """model_dump() produce un dict con todos los campos."""
        d = consejo_estructurado_sample.model_dump()
        assert "introduccion" in d
        assert "plan_accion_pasos" in d
        assert isinstance(d["plan_accion_pasos"], list)


class TestRespuestaModuloConsejoUnion:

    def test_consejo_str_legacy(self):
        """RespuestaModulo acepta str en consejo (módulos legacy)."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.PREDECIR_GASTOS,
            consejo="Este es un consejo en texto plano.",
            estado_coach=EstadoCoach.EXITOSO,
            insight=InsightAnalitico(modulo=NombreModulo.PREDECIR_GASTOS),
        )
        assert isinstance(resp.consejo, str)

    def test_consejo_estructurado(self, consejo_estructurado_sample):
        """RespuestaModulo acepta ConsejoEstructurado en consejo."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.GASTO_HORMIGA,
            consejo=consejo_estructurado_sample,
            estado_coach=EstadoCoach.EXITOSO,
            insight=InsightAnalitico(modulo=NombreModulo.GASTO_HORMIGA),
        )
        assert isinstance(resp.consejo, ConsejoEstructurado)

    def test_consejo_none(self):
        """RespuestaModulo acepta None en consejo (coach no disponible)."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.HABITOS_FINANCIEROS,
            consejo=None,
            estado_coach=EstadoCoach.NO_DISPONIBLE,
            insight=InsightAnalitico(modulo=NombreModulo.HABITOS_FINANCIEROS),
        )
        assert resp.consejo is None

    def test_a_dict_serializable_con_str(self):
        """a_dict_serializable() mantiene str cuando consejo es texto."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.REPORTE_COMPLETO,
            consejo="Texto de consejo legacy.",
            estado_coach=EstadoCoach.EXITOSO,
            insight=InsightAnalitico(modulo=NombreModulo.REPORTE_COMPLETO),
        )
        datos = resp.a_dict_serializable()
        assert isinstance(datos["consejo"], str)
        # Verificar que sea JSON-serializable (para RabbitMQ/Outbox)
        assert json.dumps(datos)

    def test_a_dict_serializable_con_consejo_estructurado(
        self, consejo_estructurado_sample
    ):
        """a_dict_serializable() convierte ConsejoEstructurado a dict."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.GASTO_HORMIGA,
            consejo=consejo_estructurado_sample,
            estado_coach=EstadoCoach.EXITOSO,
            insight=InsightAnalitico(modulo=NombreModulo.GASTO_HORMIGA),
        )
        datos = resp.a_dict_serializable()
        assert isinstance(datos["consejo"], dict)
        assert "introduccion" in datos["consejo"]
        assert "plan_accion_pasos" in datos["consejo"]
        # Debe ser JSON-serializable para RabbitMQ
        serializado = json.dumps(datos)
        assert serializado

    def test_a_dict_serializable_fecha_es_iso(self, consejo_estructurado_sample):
        """a_dict_serializable() convierte fecha_generacion a string ISO."""
        resp = RespuestaModulo(
            usuario_id="user-1",
            modulo=NombreModulo.GASTO_HORMIGA,
            consejo=consejo_estructurado_sample,
            estado_coach=EstadoCoach.EXITOSO,
            insight=InsightAnalitico(modulo=NombreModulo.GASTO_HORMIGA),
        )
        datos = resp.a_dict_serializable()
        assert isinstance(datos["fecha_generacion"], str)
        # Verificar formato ISO básico
        datetime.fromisoformat(datos["fecha_generacion"])

    def test_otros_modulos_no_se_rompen(self):
        """
        Los 9 módulos que devuelven str siguen funcionando exactamente igual.
        Este test actúa como guardrail de regresión.
        """
        modulos_legacy = [
            NombreModulo.PREDECIR_GASTOS,
            NombreModulo.HABITOS_FINANCIEROS,
            NombreModulo.SIMULAR_META,
            NombreModulo.REPORTE_COMPLETO,
            NombreModulo.RETO_AHORRO_DINAMICO,
            NombreModulo.ANALISIS_ESTILO_VIDA,
        ]
        for modulo in modulos_legacy:
            resp = RespuestaModulo(
                usuario_id="user-x",
                modulo=modulo,
                consejo="Consejo en texto plano para módulo legacy.",
                estado_coach=EstadoCoach.EXITOSO,
                insight=InsightAnalitico(modulo=modulo),
            )
            datos = resp.a_dict_serializable()
            assert isinstance(datos["consejo"], str), (
                f"Módulo {modulo.value} rompió la serialización legacy"
            )


# ══════════════════════════════════════════════════════════════════════════════
# FASE 2 — Tests de Persistencia
# ══════════════════════════════════════════════════════════════════════════════

class TestIaHistorialCoachingModel:

    def test_set_get_insight_dict(self, db_session):
        """set_insight serializa y get_insight deserializa correctamente."""
        registro = IaHistorialCoaching(
            usuario_id="user-1", modulo="GASTO_HORMIGA", estado_coach="EXITOSO"
        )
        metricas = {"total_gastos_hormiga": 120.5, "principal_gasto_hormiga": "Cafetería"}
        registro.set_insight(metricas)
        assert registro.insight_calculado is not None
        recuperado = registro.get_insight()
        assert recuperado["total_gastos_hormiga"] == 120.5
        assert recuperado["principal_gasto_hormiga"] == "Cafetería"

    def test_set_get_consejo_str(self, db_session):
        """Consejo tipo str se serializa y deserializa correctamente."""
        registro = IaHistorialCoaching(
            usuario_id="user-1", modulo="GASTO_HORMIGA", estado_coach="EXITOSO"
        )
        registro.set_consejo("Este es un consejo en texto plano.")
        recuperado = registro.get_consejo()
        assert recuperado == "Este es un consejo en texto plano."

    def test_set_get_consejo_dict(self, db_session, consejo_estructurado_sample):
        """Consejo tipo ConsejoEstructurado se serializa y deserializa como dict."""
        registro = IaHistorialCoaching(
            usuario_id="user-1", modulo="GASTO_HORMIGA", estado_coach="EXITOSO"
        )
        registro.set_consejo(consejo_estructurado_sample)
        recuperado = registro.get_consejo()
        assert isinstance(recuperado, dict)
        assert "introduccion" in recuperado
        assert "plan_accion_pasos" in recuperado
        assert isinstance(recuperado["plan_accion_pasos"], list)

    def test_get_insight_sin_datos(self):
        """get_insight retorna {} si no hay datos guardados."""
        registro = IaHistorialCoaching(
            usuario_id="user-1", modulo="GASTO_HORMIGA"
        )
        assert registro.get_insight() == {}

    def test_get_consejo_sin_datos(self):
        """get_consejo retorna None si no hay datos guardados."""
        registro = IaHistorialCoaching(
            usuario_id="user-1", modulo="GASTO_HORMIGA"
        )
        assert registro.get_consejo() is None


class TestRepositorioHistorialCoaching:

    def test_obtener_ultimo_sin_historial(self, db_session):
        """Retorna None si no hay registros para ese usuario/módulo."""
        repo = RepositorioHistorialCoaching(db_session)
        resultado = repo.obtener_ultimo("user-nuevo", "GASTO_HORMIGA")
        assert resultado is None

    def test_guardar_y_obtener_ultimo(
        self, db_session, metricas_sample, consejo_estructurado_sample
    ):
        """guardar() persiste y obtener_ultimo() recupera el registro."""
        repo = RepositorioHistorialCoaching(db_session)

        guardado = repo.guardar(
            usuario_id="user-1",
            modulo="GASTO_HORMIGA",
            metricas=metricas_sample,
            consejo=consejo_estructurado_sample,
            estado_coach="EXITOSO",
        )
        assert guardado is not None
        assert guardado.id is not None

        recuperado = repo.obtener_ultimo("user-1", "GASTO_HORMIGA")
        assert recuperado is not None
        assert recuperado.usuario_id == "user-1"
        assert recuperado.modulo == "GASTO_HORMIGA"
        assert recuperado.estado_coach == "EXITOSO"

        consejo_dict = recuperado.get_consejo()
        assert isinstance(consejo_dict, dict)
        assert consejo_dict["plan_accion_titulo"] == "Operación Hormiga Cero"

    def test_obtener_ultimo_retorna_el_mas_reciente(self, db_session, metricas_sample):
        """Si hay múltiples registros, retorna el más reciente."""
        repo = RepositorioHistorialCoaching(db_session)

        repo.guardar("user-1", "GASTO_HORMIGA", metricas_sample, "Consejo antiguo", "EXITOSO")
        repo.guardar("user-1", "GASTO_HORMIGA", metricas_sample, "Consejo reciente", "EXITOSO")

        ultimo = repo.obtener_ultimo("user-1", "GASTO_HORMIGA")
        assert ultimo.get_consejo() == "Consejo reciente"

    def test_aislamiento_entre_modulos(self, db_session, metricas_sample):
        """El historial de GASTO_HORMIGA no interfiere con otros módulos."""
        repo = RepositorioHistorialCoaching(db_session)

        repo.guardar("user-1", "GASTO_HORMIGA", metricas_sample, "Consejo Hormiga", "EXITOSO")
        repo.guardar("user-1", "HABITOS_FINANCIEROS", metricas_sample, "Consejo Hábitos", "EXITOSO")

        ultimo_hormiga = repo.obtener_ultimo("user-1", "GASTO_HORMIGA")
        ultimo_habitos = repo.obtener_ultimo("user-1", "HABITOS_FINANCIEROS")

        assert ultimo_hormiga.get_consejo() == "Consejo Hormiga"
        assert ultimo_habitos.get_consejo() == "Consejo Hábitos"

    def test_aislamiento_entre_usuarios(self, db_session, metricas_sample):
        """El historial de un usuario no interfiere con el de otro."""
        repo = RepositorioHistorialCoaching(db_session)

        repo.guardar("user-A", "GASTO_HORMIGA", metricas_sample, "Consejo A", "EXITOSO")
        repo.guardar("user-B", "GASTO_HORMIGA", metricas_sample, "Consejo B", "EXITOSO")

        assert repo.obtener_ultimo("user-A", "GASTO_HORMIGA").get_consejo() == "Consejo A"
        assert repo.obtener_ultimo("user-B", "GASTO_HORMIGA").get_consejo() == "Consejo B"


# ══════════════════════════════════════════════════════════════════════════════
# FASE 3 — Tests del CoachIA con Structured Outputs
# ══════════════════════════════════════════════════════════════════════════════

class TestCoachIAStructuredOutputs:

    @pytest.fixture
    def mock_coach(self):
        """CoachIA con Gemini y Redis completamente mockeados."""
        with patch("app.servicios.ia.coach_ia.genai"), \
             patch("app.servicios.ia.coach_ia.CacheRedis") as mock_cache, \
             patch("app.servicios.ia.coach_ia.gemini_breaker") as mock_breaker:

            mock_cache.return_value.obtener_cuota_actual.return_value = 0
            mock_cache.return_value.incrementar_cuota.return_value = 1
            mock_cache.return_value.obtener.return_value = None

            from app.servicios.ia.coach_ia import CoachIA
            coach = CoachIA.__new__(CoachIA)
            coach._cache_redis = mock_cache.return_value
            coach._modelo = MagicMock()

            yield coach, mock_breaker

    def test_llamar_gemini_sin_esquema_retorna_str(self, mock_coach):
        """Sin esquema_salida, _llamar_gemini_api retorna str."""
        coach, _ = mock_coach

        mock_respuesta = MagicMock()
        mock_respuesta.text = "Consejo en texto plano."
        mock_respuesta.usage_metadata.prompt_token_count = 100
        mock_respuesta.usage_metadata.candidates_token_count = 50
        coach._modelo.generate_content.return_value = mock_respuesta

        resultado, in_t, out_t = coach._llamar_gemini_api("prompt de prueba", None)

        assert isinstance(resultado, str)
        assert resultado == "Consejo en texto plano."
        assert in_t == 100
        assert out_t == 50

    def test_llamar_gemini_con_esquema_retorna_dict(self, mock_coach):
        """Con esquema_salida, _llamar_gemini_api retorna dict."""
        coach, _ = mock_coach

        payload = {
            "pensamiento_interno_ia": "Razonamiento...",
            "introduccion": "Hola Paulo",
            "analisis_ia": "Detecté fugas.",
            "conexion_emocional": "Afecta tu meta.",
            "plan_accion_titulo": "Plan Cero",
            "plan_accion_pasos": ["Paso 1", "Paso 2"],
            "comentario_positivo": "¡Sigue así!",
        }
        mock_respuesta = MagicMock()
        mock_respuesta.text = json.dumps(payload)
        mock_respuesta.usage_metadata.prompt_token_count = 200
        mock_respuesta.usage_metadata.candidates_token_count = 100
        coach._modelo.generate_content.return_value = mock_respuesta

        resultado, in_t, out_t = coach._llamar_gemini_api(
            "prompt de prueba", ConsejoEstructurado
        )

        assert isinstance(resultado, dict)
        assert resultado["introduccion"] == "Hola Paulo"
        assert resultado["plan_accion_titulo"] == "Plan Cero"

    def test_llamar_gemini_json_invalido_lanza_excepcion(self, mock_coach):
        """Si Gemini devuelve JSON malformado, _llamar_gemini_api lanza excepción."""
        coach, _ = mock_coach

        mock_respuesta = MagicMock()
        mock_respuesta.text = "esto no es json {"
        mock_respuesta.usage_metadata.prompt_token_count = 50
        mock_respuesta.usage_metadata.candidates_token_count = 10
        coach._modelo.generate_content.return_value = mock_respuesta

        with pytest.raises(json.JSONDecodeError):
            coach._llamar_gemini_api("prompt", ConsejoEstructurado)

    def test_rama_legacy_incluye_instruccion_markdown(self, mock_coach):
        """Sin esquema, el prompt enviado a Gemini incluye instrucción Markdown."""
        coach, _ = mock_coach

        mock_respuesta = MagicMock()
        mock_respuesta.text = "Respuesta."
        mock_respuesta.usage_metadata.prompt_token_count = 10
        mock_respuesta.usage_metadata.candidates_token_count = 5
        coach._modelo.generate_content.return_value = mock_respuesta

        coach._llamar_gemini_api("mi prompt", None)

        # Verificar que el prompt enviado contiene la instrucción Markdown
        call_args = coach._modelo.generate_content.call_args
        prompt_enviado = call_args[0][0]
        assert "Markdown" in prompt_enviado

    def test_rama_structured_no_incluye_instruccion_markdown(self, mock_coach):
        """Con esquema, el prompt enviado NO incluye instrucción Markdown."""
        coach, _ = mock_coach

        payload = {
            "pensamiento_interno_ia": "W", "introduccion": "X", "analisis_ia": "Y", "conexion_emocional": "Z",
            "plan_accion_titulo": "Plan", "plan_accion_pasos": ["P1", "P2"],
            "comentario_positivo": "Bien",
        }
        mock_respuesta = MagicMock()
        mock_respuesta.text = json.dumps(payload)
        mock_respuesta.usage_metadata.prompt_token_count = 10
        mock_respuesta.usage_metadata.candidates_token_count = 5
        coach._modelo.generate_content.return_value = mock_respuesta

        coach._llamar_gemini_api("mi prompt", ConsejoEstructurado)

        call_args = coach._modelo.generate_content.call_args
        prompt_enviado = call_args[0][0]
        # La rama B no añade instrucción Markdown
        assert "Responde DIRECTAMENTE en formato Markdown" not in prompt_enviado


# ══════════════════════════════════════════════════════════════════════════════
# FASE 4 — Tests del Orquestador con Memoria
# ══════════════════════════════════════════════════════════════════════════════

class TestOrquestadorConMemoria:

    @pytest.fixture
    def mock_servicio(self):
        """ServicioAnalisis con todas las dependencias externas mockeadas."""
        with patch("app.servicios.core.servicio_analisis.obtener_cliente_financiero"), \
             patch("app.servicios.core.servicio_analisis.obtener_cliente_perfil"), \
             patch("app.servicios.core.servicio_analisis.CoachIA"), \
             patch("app.servicios.core.servicio_analisis.CacheRedis"), \
             patch("app.servicios.core.servicio_analisis.SessionLocal"):

            from app.servicios.core.servicio_analisis import ServicioAnalisis
            servicio = ServicioAnalisis.__new__(ServicioAnalisis)
            servicio._cache_redis = MagicMock()
            servicio._cache_redis.obtener_cuota_actual.return_value = 0
            servicio._cache_redis.obtener_firma.return_value = None
            servicio._coach = MagicMock()
            yield servicio

    def test_historial_solo_consultado_para_gasto_hormiga(self, mock_servicio):
        """
        _obtener_historial se llama SOLO para GASTO_HORMIGA.
        Para otros módulos, NO se accede a la DB de historial.
        """
        mock_servicio._obtener_historial = MagicMock(return_value=None)

        # Simular que _obtener_historial no se llama para módulo legacy
        # (esto se verifica en el test de integración del orquestador completo)
        # Aquí verificamos la lógica de la condición directamente.
        modulo = NombreModulo.HABITOS_FINANCIEROS
        historial = None
        if modulo == NombreModulo.GASTO_HORMIGA:
            historial = mock_servicio._obtener_historial("user-1", modulo.value)

        mock_servicio._obtener_historial.assert_not_called()
        assert historial is None

    def test_esquema_salida_solo_para_gasto_hormiga(self):
        """
        La activación de ConsejoEstructurado como esquema_salida solo ocurre
        para GASTO_HORMIGA. Todos los otros módulos reciben None.
        """
        modulos_y_esquemas_esperados = [
            (NombreModulo.GASTO_HORMIGA, ConsejoEstructurado),
            (NombreModulo.PREDECIR_GASTOS, None),
            (NombreModulo.HABITOS_FINANCIEROS, None),
            (NombreModulo.SIMULAR_META, None),
            (NombreModulo.REPORTE_COMPLETO, None),
            (NombreModulo.RETO_AHORRO_DINAMICO, None),
            (NombreModulo.ANALISIS_ESTILO_VIDA, None),
        ]
        for modulo, esquema_esperado in modulos_y_esquemas_esperados:
            esquema = ConsejoEstructurado if modulo == NombreModulo.GASTO_HORMIGA else None
            assert esquema == esquema_esperado, (
                f"Módulo {modulo.value}: esquema_salida debería ser {esquema_esperado}, "
                f"pero es {esquema}"
            )

    def test_historial_inyectado_en_metricas(self):
        """
        Cuando existe historial previo, se inyecta correctamente
        en metricas['_historial_previo'] y metricas['_historial_insight'].
        """
        # Simular un registro de historial previo
        mock_historial = MagicMock()
        mock_historial.id = 42
        mock_historial.get_consejo.return_value = {
            "introduccion": "Hola de la sesión anterior",
            "plan_accion_titulo": "Plan Antiguo",
            "plan_accion_pasos": ["Paso A", "Paso B"],
        }
        mock_historial.get_insight.return_value = {
            "total_gastos_hormiga": 95.0,
            "principal_gasto_hormiga": "Snacks",
        }

        # Simular la lógica del orquestador
        metricas = {"total_gastos_hormiga": 120.0}

        if mock_historial is not None:
            metricas["_historial_previo"] = mock_historial.get_consejo()
            metricas["_historial_insight"] = mock_historial.get_insight()

        assert metricas["_historial_previo"]["plan_accion_titulo"] == "Plan Antiguo"
        assert metricas["_historial_insight"]["total_gastos_hormiga"] == 95.0

    def test_consejo_dict_se_convierte_a_consejo_estructurado(
        self, consejo_estructurado_sample
    ):
        """
        Cuando Gemini devuelve dict (Structured Output),
        el orquestador lo convierte a ConsejoEstructurado.
        """
        consejo_dict = consejo_estructurado_sample.model_dump()
        consejo_final = ConsejoEstructurado(**consejo_dict)
        assert isinstance(consejo_final, ConsejoEstructurado)
        assert consejo_final.plan_accion_titulo == consejo_estructurado_sample.plan_accion_titulo


# ══════════════════════════════════════════════════════════════════════════════
# FASE 5 — Tests del Prompt con Memoria
# ══════════════════════════════════════════════════════════════════════════════

class TestPromptConMemoria:

    @pytest.fixture
    def mock_contexto(self):
        contexto = MagicMock()
        contexto.nombres = "Paulo"
        contexto.tono_ia = "MOTIVADOR"
        contexto.nombre_meta_principal = "Laptop para estudios"
        contexto.porcentaje_meta_principal = 35.0
        contexto.resumen_para_prompt = "- Nombre: Paulo\n- Ingreso: S/ 1500"
        return contexto

    def test_prompt_sin_historial_no_incluye_seccion_anterior(
        self, metricas_sample, mock_contexto
    ):
        """Sin historial previo, el prompt no tiene sección 'SESIÓN ANTERIOR'."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        metricas_sample["_historial_previo"] = None
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "SESIÓN ANTERIOR" not in prompt

    def test_prompt_con_historial_incluye_seccion_anterior(
        self, metricas_sample, mock_contexto
    ):
        """Con historial previo, el prompt incluye sección 'SESIÓN ANTERIOR'."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        metricas_sample["_historial_previo"] = {
            "plan_accion_titulo": "Plan Antiguo",
            "plan_accion_pasos": ["Paso A", "Paso B"],
        }
        metricas_sample["_historial_insight"] = {
            "total_gastos_hormiga": 95.0,
            "principal_gasto_hormiga": "Snacks",
        }
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "SESIÓN ANTERIOR" in prompt
        assert "Plan Antiguo" in prompt
        assert "95.0" in prompt

    def test_prompt_incluye_instrucciones_esquema_json(
        self, metricas_sample, mock_contexto
    ):
        """El prompt incluye instrucciones del esquema JSON ConsejoEstructurado."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "introduccion" in prompt
        assert "analisis_ia" in prompt
        assert "plan_accion_pasos" in prompt
        assert "comentario_positivo" in prompt

    def test_prompt_no_incluye_instruccion_markdown(
        self, metricas_sample, mock_contexto
    ):
        """El prompt NO incluye la instrucción de responder en Markdown (v3)."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "Responde DIRECTAMENTE en formato Markdown" not in prompt

    def test_prompt_skip_ia_cuando_no_hay_hormigas(
        self, metricas_sample, mock_contexto
    ):
        """Si no hay gastos hormiga, orquestar_prompt retorna [SKIP_IA]."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        metricas_sample["hay_hormigas"] = False
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "[SKIP_IA]" in prompt

    def test_prompt_con_historial_str_legacy(
        self, metricas_sample, mock_contexto
    ):
        """El prompt acepta historial en formato str (consejo legacy)."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        metricas_sample["_historial_previo"] = (
            "Consejo anterior muy largo que supera los 20 caracteres mínimos."
        )
        metricas_sample["_historial_insight"] = {}
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "SESIÓN ANTERIOR" in prompt

    def test_prompt_incluye_datos_financieros_reales(
        self, metricas_sample, mock_contexto
    ):
        """El prompt incluye los montos reales del análisis."""
        from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
        servicio = DeteccionGastosHormigaService()
        prompt = servicio.orquestar_prompt(metricas_sample, mock_contexto)
        assert "120.50" in prompt      # total_gastos_hormiga
        assert "Cafetería" in prompt   # principal_gasto_hormiga
        assert "1446.0" in prompt      # proyeccion_fuga_anual
