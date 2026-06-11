"""
servicios/ia/fallbacks/gestor_fallbacks.py
Gestor modular de fallbacks para la IA.
"""

from typing import Dict, Any, Optional
from app.modelos.esquemas import NombreModulo
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class GestorFallbacks:
    @staticmethod
    def generar_fallback(
        modulo: NombreModulo,
        datos: Dict[str, Any],
        nombres: str,
        contexto: Optional[ContextoEstrategicoIADTO] = None,
    ) -> Any:
        if modulo == NombreModulo.GASTO_HORMIGA:
            # Retorna un diccionario que coincida con ConsejoEstructurado
            return {
                "pensamiento_interno_ia": "El servicio no está disponible en este momento. Generando fallback.",
                "introduccion": f"Hola {nombres}, por el momento mis sistemas principales de análisis están descansando.",
                "analisis_ia": "He revisado rápidamente tus gastos y detectado algunas salidas hormiga que suman S/ " + str(datos.get('total_gastos_hormiga', 0)),
                "conexion_emocional": "Recuerda que cada pequeño ahorro nos acerca más a tu meta principal.",
                "plan_accion_titulo": "Plan de Contingencia Rápido",
                "plan_accion_pasos": [
                    "Revisa tus gastos más pequeños de la última semana",
                    "Evita gastos innecesarios por hoy"
                ],
                "comentario_positivo": "¡Sigue así! Pronto tendré un análisis más profundo para ti."
            }
            
        elif modulo == NombreModulo.COMPROBADOR_EVOLUCION:
            imf = datos.get("score_imf", 0)
            diag = datos.get("diagnostico_imf", "DATOS INCOMPLETOS")
            reincidentes = datos.get("categorias_reincidentes", [])
            
            recetas = []
            for r in reincidentes[:2]: # Máximo 2 para el fallback
                recetas.append({
                    "categoria": r.get("categoria", "Desconocida"),
                    "diagnostico": f"Aumento matemático del {r.get('aumento_pct', 0)}% detectado por el sistema base.",
                    "posologia": [
                        "1. Revisar transacciones recientes de esta categoría.",
                        "2. Evitar gastos impulsivos en esta área esta semana.",
                        "3. Establecer un límite temporal."
                    ],
                    "pronostico": f"Ahorro preventivo de S/ {r.get('gasto_extra', 0)} estimado."
                })
            
            if not recetas:
                recetas.append({
                    "categoria": "Salud Financiera General",
                    "diagnostico": "El sistema base no detectó reincidencias graves matemáticamente.",
                    "posologia": [
                        "1. Mantener los buenos hábitos actuales.",
                        "2. Revisar el balance al final del mes.",
                        "3. Disfrutar de tu estabilidad financiera."
                    ],
                    "pronostico": "Crecimiento patrimonial sostenido estimado."
                })

            return {
                "pensamiento_interno_ia": "Servicio Gemini inactivo. Generando diagnóstico básico estadístico usando Pandas.",
                "veredicto_narrativo": f"Basado puramente en cálculo estadístico, tu Índice de Madurez es de {imf}/100 ({diag}). Mis funciones avanzadas están pausadas, pero el motor analítico sugiere lo siguiente:",
                "recetas_medicas": recetas
            }
        elif modulo == NombreModulo.ZONA_ENTRENAMIENTO:
            estado = datos.get("estado_fisico", "Sedentario")
            return {
                "pensamiento_interno_ia": "Falla de red con la IA. Generando rutina de contingencia usando Pandas.",
                "estado_fisico": estado,
                "evaluacion_previa": "Resumen rápido base (modo offline).",
                "rutina": [
                    {
                        "nombre": "Cardio de Bolsillo",
                        "descripcion": "Revisa todos tus gastos menores a S/ 20 del mes pasado.",
                        "duracion_dias": 30,
                        "frecuencia": "1 vez por semana",
                        "metrica_exito": "Encontrar al menos 2 gastos innecesarios."
                    },
                    {
                        "nombre": "Ayuno de Suscripciones",
                        "descripcion": "Cancela 1 servicio de streaming que no hayas usado en 15 días.",
                        "duracion_dias": 30,
                        "frecuencia": "Única vez",
                        "metrica_exito": "1 suscripción cancelada."
                    },
                    {
                        "nombre": "Levantamiento de Ahorro",
                        "descripcion": "Transfiere S/ 10 a tu cuenta de ahorros al final del día.",
                        "duracion_dias": 30,
                        "frecuencia": "Diario",
                        "metrica_exito": "Transferencia completada."
                    }
                ]
            }
        
        # Fallback genérico para módulos legacy (texto plano)
        return f"Hola {nombres}, por el momento no puedo conectarme a mi motor de IA. Por favor, intenta de nuevo más tarde."
