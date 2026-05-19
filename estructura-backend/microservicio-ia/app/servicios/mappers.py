from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.modelos.esquemas import PerfilUsuario

class MapperContextoIA:
    """
    Mapper para convertir DTOs de otros servicios al contexto estratégico
    que necesita el coach IA. SRP (Principio de Responsabilidad Única).
    """
    
    @staticmethod
    def mapear_perfil(p: PerfilUsuario) -> ContextoEstrategicoIADTO:
        return ContextoEstrategicoIADTO(
            nombres=p.nombre,
            ocupacion=p.ocupacion or "Estudiante",
            ingreso_mensual=p.ingreso_mensual or 0.0,
            nombre_meta_principal=p.meta_ahorro_activa.nombre if p.meta_ahorro_activa else "Ninguna",
            porcentaje_meta_principal=p.meta_ahorro_activa.porcentaje_completado if p.meta_ahorro_activa else 0.0,
            tono_ia=p.configuracion_ia.tono_ia if p.configuracion_ia else "AMIGABLE",
            porcentaje_alerta_gasto=p.configuracion_ia.porcentaje_alerta_gasto if p.configuracion_ia else 80,
            rol=p.rol
        )
