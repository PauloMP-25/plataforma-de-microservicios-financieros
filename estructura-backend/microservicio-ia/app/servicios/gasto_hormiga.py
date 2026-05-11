import pandas as pd
from typing import List, Dict, Any
from app.servicios.base_analisis import BaseAnalisisService
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO

class GastoHormigaService(BaseAnalisisService):
    """
    Implementación del Módulo GASTO_HORMIGA.
    Modelo de referencia para el Pipeline LUKA-COACH V4.
    """
    
    def __init__(self):
        super().__init__(nombre_modulo="GASTO_HORMIGA", meses_minimos=3)

    def ejecutar_calculos(self, df: pd.DataFrame, contexto: ContextoEstrategicoIADTO) -> Dict[str, Any]:
        """
        Fase 2: Motor de Cálculo Local.
        Identifica gastos recurrentes menores al 1% del ingreso mensual.
        """
        # Regla de negocio: monto hormiga < 1% del ingreso mensual
        umbral_monto = float(contexto.ingreso_mensual) * 0.01
        
        # Filtramos solo egresos (gastos)
        df_gastos = df[df['monto'] > 0] # Asumiendo montos positivos para gastos en este flujo
        
        # 1. Identificar transacciones por debajo del umbral
        hormigas = df_gastos[df_gastos['monto'] <= umbral_monto]
        
        # 2. Agrupar por descripción para detectar recurrencia
        resumen_hormiga = hormigas.groupby('descripcion').agg({
            'monto': ['sum', 'count'],
            'categoria': 'first'
        }).reset_index()
        
        # Renombrar columnas para claridad
        resumen_hormiga.columns = ['descripcion', 'total_acumulado', 'frecuencia', 'categoria']
        
        # 3. Filtrar solo los que tienen recurrencia (frecuencia > 1)
        resumen_final = resumen_hormiga[resumen_hormiga['frecuencia'] > 1]
        
        if resumen_final.empty:
            return {
                "total_hormiga": 0.0,
                "items_detectados": 0,
                "categoria_principal": "Ninguna",
                "impacto_mensual_estimado": 0.0
            }

        # 4. Consolidar métricas duras
        total_hormiga = resumen_final['total_acumulado'].sum()
        categoria_mas_frecuente = resumen_final.groupby('categoria')['total_acumulado'].sum().idxmax()
        
        return {
            "total_hormiga": round(float(total_hormiga), 2),
            "items_detectados": int(resumen_final['frecuencia'].sum()),
            "categoria_principal": categoria_mas_frecuente,
            "impacto_mensual_estimado": round(float(total_hormiga / self.meses_minimos), 2)
        }

    def orquestar_prompt(self, metricas: Dict[str, Any], contexto: ContextoEstrategicoIADTO) -> str:
        """
        Fase 3: Orquestación de Empatía (Ingeniería de Prompts).
        Construye el input para Gemini sin enviar transacciones crudas.
        """
        if metricas["total_hormiga"] == 0:
            return (
                f"Genera un mensaje de felicitación en tono {contexto.tono_ia} para {contexto.nombres}. "
                f"Los datos muestran que no tiene gastos hormiga significativos. "
                f"Animalo a seguir así para cumplir su meta de '{contexto.nombre_meta_principal}'."
            )

        return (
            f"Actúa como un coach financiero experto. Genera un consejo en tono {contexto.tono_ia} para {contexto.nombres}, "
            f"quien trabaja como {contexto.ocupacion} y tiene como meta principal '{contexto.nombre_meta_principal}' "
            f"(actualmente al {contexto.porcentaje_meta_principal}% de progreso). "
            f"Los datos muestran que ha gastado un total de S/ {metricas['total_hormiga']} en 'gastos hormiga' "
            f"(compras pequeñas pero recurrentes), principalmente en la categoría de {metricas['categoria_principal']}. "
            f"Explícale cómo reducir estos S/ {metricas['impacto_mensual_estimado']} mensuales podría acelerar su meta."
        )
