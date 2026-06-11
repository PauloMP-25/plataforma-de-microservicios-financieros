import json
from sqlalchemy.orm import Session
from app.persistencia.modelos_db import IaRutinaMensual
from typing import Optional

class RepositorioRutinas:
    def __init__(self, db: Session):
        self.db = db

    def guardar_rutina(self, usuario_id: str, estado_fisico: str, ejercicios_json: str) -> IaRutinaMensual:
        # Marcar las activas previas como REEMPLAZADAS al generar una nueva (asume mes nuevo o reemplazo)
        rutinas_activas = self.db.query(IaRutinaMensual).filter(
            IaRutinaMensual.usuario_id == usuario_id,
            IaRutinaMensual.estado == "ACTIVA"
        ).all()
        for r in rutinas_activas:
            r.estado = "REEMPLAZADA"
        
        nueva = IaRutinaMensual(
            usuario_id=usuario_id,
            estado_fisico=estado_fisico,
            ejercicios_json=ejercicios_json,
            estado="ACTIVA"
        )
        self.db.add(nueva)
        self.db.commit()
        self.db.refresh(nueva)
        return nueva

    def obtener_rutina_activa(self, usuario_id: str) -> Optional[IaRutinaMensual]:
        return self.db.query(IaRutinaMensual).filter(
            IaRutinaMensual.usuario_id == usuario_id,
            IaRutinaMensual.estado == "ACTIVA"
        ).order_by(IaRutinaMensual.fecha_generacion.desc()).first()
