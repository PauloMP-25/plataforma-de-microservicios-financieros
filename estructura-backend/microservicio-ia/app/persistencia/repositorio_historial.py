"""
persistencia/repositorio_historial.py  ·  v1.0 — FASE 2 (LUKA)
══════════════════════════════════════════════════════════════════════════════
Repositorio de acceso a datos para IaHistorialCoaching.

Responsabilidades:
  - Encapsular las queries SQLAlchemy sobre ia_historial_coaching.
  - Proveer una interfaz simple para el orquestador (servicio_analisis.py).
  - Ser mockeable en tests sin tocar el orquestador.

Principio:
  El orquestador NO debe importar SQLAlchemy directamente.
  Solo importa este repositorio y llama a sus métodos.
══════════════════════════════════════════════════════════════════════════════
"""
from __future__ import annotations

import logging
from typing import Optional

from sqlalchemy.orm import Session

from app.persistencia.modelos_db import IaHistorialCoaching

logger = logging.getLogger(__name__)


class RepositorioHistorialCoaching:
    """
    Repositorio de acceso a datos para el historial de coaching IA.

    Uso desde servicio_analisis.py:

        repo = RepositorioHistorialCoaching(db_session)
        historial = repo.obtener_ultimo(usuario_id, "GASTO_HORMIGA")
        if historial:
            metricas["_historial_previo"] = historial.get_consejo()

        # ... lógica de negocio ...

        repo.guardar(
            usuario_id=usuario_id,
            modulo="GASTO_HORMIGA",
            metricas=metricas,
            consejo=consejo_resultado,
            estado_coach="EXITOSO",
        )
    """

    def __init__(self, db: Session) -> None:
        self._db = db

    # ── Lectura ───────────────────────────────────────────────────────────────

    def obtener_ultimo(
        self,
        usuario_id: str,
        modulo: str,
    ) -> Optional[IaHistorialCoaching]:
        """
        Retorna el registro más reciente de (usuario_id, modulo).

        Usa el índice compuesto (usuario_id, modulo) + ORDER BY fecha_generacion DESC
        con LIMIT 1 para la consulta más eficiente posible.

        Retorna None si no existe historial previo (primera sesión del usuario).
        """
        try:
            return (
                self._db.query(IaHistorialCoaching)
                .filter(
                    IaHistorialCoaching.usuario_id == usuario_id,
                    IaHistorialCoaching.modulo == modulo,
                )
                .order_by(IaHistorialCoaching.fecha_generacion.desc())
                .first()
            )
        except Exception as e:
            # Fallo de DB no debe romper el flujo principal del orquestador.
            logger.warning(
                "[REPO-HISTORIAL] Error al obtener historial para usuario=%s modulo=%s: %s",
                usuario_id, modulo, e,
            )
            return None

    # ── Escritura ─────────────────────────────────────────────────────────────

    def guardar(
        self,
        usuario_id: str,
        modulo: str,
        metricas: dict,
        consejo,
        estado_coach: str,
    ) -> Optional[IaHistorialCoaching]:
        """
        Persiste una nueva interacción de coaching en la base de datos.

        Parámetros
        ----------
        usuario_id   : ID del usuario propietario del análisis.
        modulo       : Valor del enum NombreModulo (ej: "GASTO_HORMIGA").
        metricas     : Dict de hallazgos del motor Pandas. Se serializa a JSON.
        consejo      : str, dict o ConsejoEstructurado. Se serializa a JSON.
        estado_coach : Valor del enum EstadoCoach (ej: "EXITOSO").

        Retorna el objeto guardado, o None si la escritura falla.
        La excepción NO se propaga: un fallo de historial no debe degradar
        la experiencia del usuario.
        """
        try:
            registro = IaHistorialCoaching(
                usuario_id=usuario_id,
                modulo=modulo,
                estado_coach=estado_coach,
            )
            # Usar los helpers de serialización del modelo
            registro.set_insight(metricas)
            registro.set_consejo(consejo)

            self._db.add(registro)
            self._db.commit()
            self._db.refresh(registro)

            logger.info(
                "[REPO-HISTORIAL] Historial guardado: id=%d usuario=%s modulo=%s",
                registro.id, usuario_id, modulo,
            )
            return registro

        except Exception as e:
            self._db.rollback()
            logger.error(
                "[REPO-HISTORIAL] Error al guardar historial para usuario=%s modulo=%s: %s",
                usuario_id, modulo, e,
            )
            return None
