from typing import Dict, Any
from app.modelos.esquemas import NombreModulo

class MotorReglasLocal:
    """
    Motor de Fallback Basado en Reglas (Graceful Degradation).
    Entra en acción cuando el Circuit Breaker corta el acceso a Gemini
    o Gemini no responde. Garantiza que el usuario siempre reciba 
    asesoría financiera, aunque sea estática.
    """
    
    @staticmethod
    def generar_fallback(modulo: NombreModulo, metricas: Dict[str, Any], nombres: str) -> str:
        
        if modulo == NombreModulo.GASTO_HORMIGA:
            fuga = metricas.get('total_gastos_hormiga', 0)
            top = metricas.get('principal_gasto_hormiga', 'Varios')
            if fuga > 0:
                return (
                    f"¡Hola {nombres}! Aunque en este momento no puedo hacer un análisis profundo, "
                    f"hemos detectado que tus pequeños gastos hormiga suman **S/ {fuga}** este mes.\n\n"
                    f"💡 **Tu principal fuga es en:** {top}.\n"
                    f"Te sugerimos revisar estas pequeñas compras para no afectar tu meta de ahorro."
                )
            return f"¡Hola {nombres}! Excelente trabajo, no detectamos fugas de gastos hormiga este mes. Sigue así."
            
        elif modulo == NombreModulo.SIMULAR_META:
            meta = metricas.get('nombre_meta', 'tu meta')
            meses = metricas.get('meses_para_meta', 0)
            return (
                f"¡Hola {nombres}! Las proyecciones estáticas indican que lograrás **{meta}** "
                f"en aproximadamente **{meses} meses** si mantienes tu ritmo actual.\n\n"
                f"💡 Si deseas acelerarlo, intenta reducir gastos no esenciales este mes."
            )
            
        elif modulo == NombreModulo.HABITOS_FINANCIEROS:
            salud = metricas.get('salud_financiera', 50)
            return (
                f"¡Hola {nombres}! Tu índice actual de salud financiera es **{salud}/100**.\n"
                f"💡 Recomendación general: Mantén tus ahorros al menos en un 20% de tus ingresos totales "
                f"y evita que tus gastos fijos superen el 50%."
            )

        # Fallback genérico para otros módulos
        return (
            f"Hola {nombres}, por ahora no puedo darte un consejo hiper-personalizado, "
            f"pero nuestro motor matemático sigue analizando tus datos con éxito. "
            f"¡Mantén la disciplina financiera!"
        )
