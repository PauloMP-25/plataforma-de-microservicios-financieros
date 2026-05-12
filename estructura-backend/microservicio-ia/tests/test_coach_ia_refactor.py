import unittest
from unittest.mock import MagicMock, patch
from app.servicios.coach_ia import CoachIA
from app.modelos.esquemas import (
    InsightAnalitico, 
    NombreModulo, 
    PerfilUsuario, 
    NivelRiesgo,
    MetaAhorro,
    EstadoCoach
)
from app.persistencia.modelos_db import IaAnalisisCache

class TestCoachIARefactor(unittest.TestCase):

    def setUp(self):
        # Parcheamos la inicialización de genai y redis en el constructor
        with patch('google.generativeai.configure'), \
             patch('google.generativeai.GenerativeModel'), \
             patch('app.persistencia.cache_redis.redis.Redis'):
            self.coach = CoachIA()

    @patch('app.servicios.coach_ia.CacheRedis.obtener_consejo')
    def test_doble_memoria_redis_hit(self, mock_redis_get):
        """Verifica que si está en Redis, no consulta DB ni Gemini."""
        mock_redis_get.return_value = "Consejo desde Redis"
        
        insight = InsightAnalitico(
            modulo=NombreModulo.OPTIMIZAR_SUSCRIPCIONES,
            periodo_analizado="Mayo 2026",
            hallazgos={"total_gastos_hormiga": 100.0}
        )
        perfil = PerfilUsuario(nombre="Paulo", edad=25, ocupacion="Ingeniero", ingreso_mensual=5000.0)
        
        with patch('app.servicios.coach_ia.SessionLocal') as mock_db:
            respuesta = self.coach.generar_respuesta("user123", insight, perfil)
            
            self.assertEqual(respuesta.consejo, "Consejo desde Redis")
            self.assertEqual(respuesta.estado_coach, EstadoCoach.EXITOSO)
            # No debió llamar a la DB
            mock_db.assert_not_called()

    @patch('app.servicios.coach_ia.CacheRedis.obtener_consejo')
    @patch('app.servicios.coach_ia.SessionLocal')
    @patch('app.servicios.coach_ia.CacheRedis.guardar_consejo')
    def test_doble_memoria_db_hit(self, mock_redis_save, mock_db_session, mock_redis_get):
        """Verifica que si no está en Redis pero sí en DB, se recupera y actualiza Redis."""
        mock_redis_get.return_value = None
        
        # Mock de la DB
        mock_session = MagicMock()
        mock_db_session.return_value.__enter__.return_value = mock_session
        
        mock_cache_db = IaAnalisisCache(
            hash_datos="hash123",
            consejo_gemini="Consejo desde DB",
            usando_fallback=False
        )
        mock_session.query.return_value.filter.return_value.first.return_value = mock_cache_db
        
        insight = InsightAnalitico(
            modulo=NombreModulo.OPTIMIZAR_SUSCRIPCIONES,
            periodo_analizado="Mayo 2026",
            hallazgos={"total_gastos_hormiga": 100.0}
        )
        perfil = PerfilUsuario(nombre="Paulo", edad=25, ocupacion="Ingeniero", ingreso_mensual=5000.0)
        
        respuesta = self.coach.generar_respuesta("user123", insight, perfil)
        
        self.assertEqual(respuesta.consejo, "Consejo desde DB")
        # Se debió guardar en Redis el resultado de la DB
        mock_redis_save.assert_called_once_with(unittest.mock.ANY, "Consejo desde DB")

    @patch('app.servicios.coach_ia.CacheRedis.obtener_consejo')
    @patch('app.servicios.coach_ia.SessionLocal')
    @patch('app.servicios.coach_ia.CoachIA._llamar_gemini_api')
    @patch('app.servicios.coach_ia.CacheRedis.guardar_consejo')
    def test_doble_memoria_miss_calls_gemini(self, mock_redis_save, mock_gemini, mock_db_session, mock_redis_get):
        """Verifica que si no hay caché, se llama a Gemini y se guarda en ambas memorias."""
        mock_redis_get.return_value = None
        mock_session = MagicMock()
        mock_db_session.return_value.__enter__.return_value = mock_session
        mock_session.query.return_value.filter.return_value.first.return_value = None # Miss en DB
        
        mock_gemini.return_value = "Consejo desde Gemini AI"
        
        insight = InsightAnalitico(
            modulo=NombreModulo.OPTIMIZAR_SUSCRIPCIONES,
            periodo_analizado="Mayo 2026",
            hallazgos={"total_gastos_hormiga": 120.0}
        )
        perfil = PerfilUsuario(nombre="Paulo", edad=25, ocupacion="Ingeniero", ingreso_mensual=5000.0)
        
        respuesta = self.coach.generar_respuesta("user123", insight, perfil)
        
        self.assertEqual(respuesta.consejo, "Consejo desde Gemini AI")
        # Guardado en Redis
        mock_redis_save.assert_called_once()
        # Guardado en DB (merge + commit)
        mock_session.merge.assert_called_once()
        mock_session.commit.assert_called_once()

    def test_generacion_prompt_template_exacto(self):
        """Verifica que el prompt generado sigue el template de la Fase 2."""
        insight = InsightAnalitico(
            modulo=NombreModulo.OPTIMIZAR_SUSCRIPCIONES,
            periodo_analizado="Junio 2026",
            hallazgos={
                "total_gastos_hormiga": 85.50,
                "variacion_vs_mes_anterior_pct": 12.5,
                "top_categorias_hormiga": [{"categoria": "Café", "monto": 45.0}]
            }
        )
        perfil = PerfilUsuario(
            nombre="Gabriel", 
            edad=22, 
            ocupacion="Estudiante", 
            ingreso_mensual=1200.0,
            meta_ahorro_activa=MetaAhorro(nombre="Laptop Gamer")
        )
        
        datos = self.coach._preparar_datos_input(insight, perfil)
        prompt = self.coach._construir_prompt_fase2(datos)
        
        # Verificaciones del contenido del prompt
        self.assertIn("Rol: Eres coach financiero peruano", prompt)
        self.assertIn("- Cliente: Gabriel, 22 años, Estudiante", prompt)
        self.assertIn("- Ingreso mensual: S/ 1200.0", prompt)
        self.assertIn("- Meta: Laptop Gamer", prompt)
        self.assertIn("Se detectó S/ 85.5", prompt)
        self.assertIn("Equivale al 7.1%", prompt) # 85.5 / 1200 = 0.07125
        self.assertIn("Top: Café S/45.0", prompt)
        self.assertIn("Variación vs mes anterior: 12.5% subió", prompt)
        self.assertIn("Restricciones: No des consejos de inversión", prompt)
        self.assertIn("No saludes con \"Hola\"", prompt)

    @patch('app.servicios.coach_ia.gemini_breaker.call')
    def test_circuit_breaker_fallback(self, mock_breaker_call):
        """Verifica que si el breaker salta, se activa el fallback."""
        from pybreaker import CircuitBreakerError
        mock_breaker_call.side_effect = CircuitBreakerError()
        
        insight = InsightAnalitico(
            modulo=NombreModulo.OPTIMIZAR_SUSCRIPCIONES,
            periodo_analizado="Mayo 2026",
            hallazgos={"total_gastos_hormiga": 60.0, "top_categorias_hormiga": [{"categoria": "Streaming", "monto": 40.0}]}
        )
        perfil = PerfilUsuario(nombre="Cristina", meta_ahorro_activa=MetaAhorro(nombre="Viaje"))
        
        # Mock de caché para forzar la llamada
        with patch('app.servicios.coach_ia.CacheRedis.obtener_consejo', return_value=None), \
             patch('app.servicios.coach_ia.SessionLocal') as mock_db:
            
            mock_session = MagicMock()
            mock_db.return_value.__enter__.return_value = mock_session
            mock_session.query.return_value.filter.return_value.first.return_value = None
            
            respuesta = self.coach.generar_respuesta("user456", insight, perfil)
            
            self.assertTrue(respuesta.usando_fallback)
            self.assertIn("Cristina", respuesta.consejo)
            self.assertIn("identificamos S/60.0 en gastos hormiga", respuesta.consejo)
            self.assertIn("Viaje", respuesta.consejo)
            self.assertEqual(respuesta.estado_coach, EstadoCoach.TIMEOUT)

if __name__ == '__main__':
    unittest.main()
