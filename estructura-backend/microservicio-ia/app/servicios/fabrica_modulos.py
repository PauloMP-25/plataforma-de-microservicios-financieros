from typing import Dict
from app.servicios.core.base_analisis import BaseAnalisisService
from app.modelos.esquemas import NombreModulo

class FabricaModulosAnalisis:
    """
    Patrón Factory con Lazy Loading para los módulos de análisis.
    Evita cargar todos los módulos en memoria al iniciar la aplicación,
    reduciendo dependencias y tiempo de arranque.
    """
    _instancias: Dict[NombreModulo, BaseAnalisisService] = {}

    @classmethod
    def obtener_modulo(cls, modulo: NombreModulo) -> BaseAnalisisService:
        if modulo not in cls._instancias:
            if modulo == NombreModulo.GASTO_HORMIGA:
                from app.servicios.modulos.deteccion_gastos_hormiga import DeteccionGastosHormigaService
                cls._instancias[modulo] = DeteccionGastosHormigaService()
            elif modulo == NombreModulo.PREDECIR_GASTOS:
                from app.servicios.modulos.predecir_gastos import PredecirGastosService
                cls._instancias[modulo] = PredecirGastosService()
            elif modulo == NombreModulo.REPORTE_COMPLETO:
                from app.servicios.modulos.reporte_completo import ReporteCompletoService
                cls._instancias[modulo] = ReporteCompletoService()
            elif modulo == NombreModulo.HABITOS_FINANCIEROS:
                from app.servicios.modulos.habitos_financieros import HabitosFinancierosService
                cls._instancias[modulo] = HabitosFinancierosService()
            elif modulo == NombreModulo.SIMULAR_META:
                from app.servicios.modulos.simular_meta import SimularMetaService
                cls._instancias[modulo] = SimularMetaService()
            elif modulo == NombreModulo.RETO_AHORRO_DINAMICO:
                from app.servicios.modulos.reto_ahorro_dinamico import RetoAhorroDinamicoService
                cls._instancias[modulo] = RetoAhorroDinamicoService()
            elif modulo == NombreModulo.ANALISIS_ESTILO_VIDA:
                from app.servicios.modulos.analisis_estilo_de_vida import AnalisisEstiloVidaService
                cls._instancias[modulo] = AnalisisEstiloVidaService()
            elif modulo == NombreModulo.AUTO_CLASIFICACION:
                # Si existe en un futuro
                raise NotImplementedError(f"Módulo {modulo} aún no implementado en Fabrica")
            else:
                raise ValueError(f"Módulo {modulo} no reconocido por la Fabrica")
                
        return cls._instancias[modulo]
