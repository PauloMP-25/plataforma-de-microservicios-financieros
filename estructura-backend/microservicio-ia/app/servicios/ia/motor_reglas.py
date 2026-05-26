from typing import Dict, Any, Optional
from app.modelos.esquemas import NombreModulo
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class MotorReglasLocal:
    """
    Motor de Fallback Basado en Reglas (Graceful Degradation).
    Entra en acción cuando el Circuit Breaker corta el acceso a Gemini
    o Gemini no responde. Garantiza que el usuario siempre reciba 
    asesoría financiera, aunque sea estática.
    """
    
    @staticmethod
    def generar_fallback(
        modulo: NombreModulo, 
        metricas: Dict[str, Any], 
        nombres: str,
        contexto: Optional[ContextoEstrategicoIADTO] = None
    ) -> str:
        # 1. Información de Perfil (ms-cliente)
        perfil_info = ""
        meta_info = ""
        if contexto:
            perfil_info = f"👤 **Perfil:** {contexto.ocupacion} | **Ingreso declarado:** S/ {contexto.ingreso_mensual:.2f}\n"
            if contexto.nombre_meta_principal and contexto.nombre_meta_principal != "Ninguna":
                meta_info = f"🎯 **Meta Activa:** {contexto.nombre_meta_principal} (Progreso: {contexto.porcentaje_meta_principal:.1f}%)\n"

        # 2. Resumen Financiero del Mes (ms-financiero)
        resumen_fin = ""
        if "_total_ingresos" in metricas and "_total_gastos" in metricas:
            total_ing = metricas["_total_ingresos"]
            total_gas = metricas["_total_gastos"]
            balance = round(total_ing - total_gas, 2)
            resumen_fin = f"💵 **Resumen Financiero del Periodo:** Ingresos: S/ {total_ing:.2f} | Gastos: S/ {total_gas:.2f} | Balance: S/ {balance:.2f}\n"

        # 3. Encabezado común de resiliencia
        header = (
            f"### 🕵️‍♂️ Análisis de {modulo.value.replace('_', ' ').title()} (Modo de Resiliencia)\n\n"
            f"¡Hola {nombres}! Aunque nuestro servicio de IA principal (Gemini) está temporalmente ocupado o experimenta latencia, "
            f"nuestro motor analítico local ha procesado tu información financiera con éxito:\n\n"
            f"{perfil_info}"
            f"{resumen_fin}"
            f"{meta_info}\n"
        )

        if modulo == NombreModulo.GASTO_HORMIGA:
            fuga = metricas.get('total_gastos_hormiga', 0)
            top = metricas.get('principal_gasto_hormiga', 'Varios')
            proyeccion = metricas.get('proyeccion_fuga_anual', fuga * 12)
            
            if fuga > 0:
                return (
                    f"{header}"
                    f"📊 **Resultados del Análisis (Gastos Hormiga):**\n"
                    f"- **Fuga mensual detectada:** S/ {fuga:.2f}\n"
                    f"- **Principal categoría de fuga:** {top}\n"
                    f"- **Proyección de fuga anual:** S/ {proyeccion:.2f}\n\n"
                    f"💡 **Consejo Detective LUKA:**\n"
                    f"Esos pequeños gastos recurrentes en **{top}** parecen insignificantes, pero representan una fuga acumulada de **S/ {fuga:.2f}** este mes. "
                    f"Si reduces esta fuga a la mitad, estarás sumando dinero directo para impulsar tu meta "
                    f"**{contexto.nombre_meta_principal if contexto else 'de ahorro'}**. ¡La disciplina financiera supera cualquier caída tecnológica!"
                )
            return (
                f"{header}"
                f"🎉 **¡Felicidades!** Nuestro motor analítico local no ha detectado compras recurrentes ni gastos hormiga significativos en este período. "
                f"Estás manteniendo un excelente control sobre las fugas de dinero. ¡Sigue así!"
            )
            
        elif modulo == NombreModulo.SIMULAR_META:
            meta = metricas.get('nombre_meta', 'tu meta')
            meses = metricas.get('meses_para_meta', 0)
            return (
                f"{header}"
                f"📊 **Resultados del Análisis (Proyecciones de Meta):**\n"
                f"Las proyecciones estáticas indican que lograrás **{meta}** "
                f"en aproximadamente **{meses} meses** si mantienes tu ritmo actual.\n\n"
                f"💡 **Consejo Detective LUKA:**\n"
                f"Si deseas acelerar este plazo, intenta reducir gastos no esenciales este mes para destinar un porcentaje mayor a tus ahorros."
            )
            
        elif modulo == NombreModulo.HABITOS_FINANCIEROS:
            salud = metricas.get('salud_financiera', 50)
            return (
                f"{header}"
                f"📊 **Resultados del Análisis (Hábitos):**\n"
                f"Tu índice de salud financiera calculado es de **{salud}/100**.\n\n"
                f"💡 **Consejo Detective LUKA:**\n"
                f"Intenta seguir la regla básica 50/30/20: mantén tus ahorros en al menos un 20% de tus ingresos totales "
                f"y evita que tus gastos fijos (necesidades) superen el 50% de tus ingresos."
            )

        # Fallback genérico para otros módulos
        return (
            f"¡Hola {nombres}!\n\n"
            f"{perfil_info}"
            f"{resumen_fin}"
            f"{meta_info}\n"
            f"Por ahora no puedo darte un consejo hiper-personalizado por un retraso con la IA de Gemini, "
            f"pero nuestro motor matemático sigue analizando tus datos con éxito. "
            f"¡Mantén la disciplina financiera!"
        )
