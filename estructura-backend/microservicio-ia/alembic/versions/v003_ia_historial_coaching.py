"""
alembic/versions/v003_ia_historial_coaching.py
══════════════════════════════════════════════════════════════════════════════
Migración: Crear tabla ia_historial_coaching

Propósito:
  Tabla de memoria histórica del Coach IA. Permite que Gemini recuerde
  el último consejo entregado y detecte si el usuario mejoró sus hábitos.

Índices:
  - ix_ia_historial_coaching_usuario_id  : filtros por usuario
  - ix_ia_historial_coaching_modulo      : filtros por módulo
  - ix_ia_historial_usuario_modulo       : índice COMPUESTO para la query
    ORDER BY (usuario_id, modulo) → fecha_generacion DESC LIMIT 1
    que el orquestador ejecuta en cada llamada.

Compatibilidad:
  - PostgreSQL: Text almacena JSON válido. Migrar a JSONB con una segunda
    migración si se requiere indexación nativa de campos JSON.
  - SQLite (tests): Text compatible.

Rollback seguro:
  downgrade() elimina la tabla completa. No afecta otras tablas.
══════════════════════════════════════════════════════════════════════════════
"""

from alembic import op
import sqlalchemy as sa

# Identificadores de revisión
revision = "v003_historial_coaching"
down_revision = "v002"   # ← ajustar al ID real de la migración anterior
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "ia_historial_coaching",

        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),

        # Clave de búsqueda — par (usuario_id, modulo) es el filtro principal
        sa.Column("usuario_id", sa.String(50), nullable=False),
        sa.Column("modulo", sa.String(50), nullable=False),

        # Temporalidad para ORDER BY al recuperar el último registro
        sa.Column(
            "fecha_generacion",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("NOW()"),
        ),

        # Métricas del motor Pandas serializadas como JSON
        sa.Column("insight_calculado", sa.Text(), nullable=True),

        # Consejo entregado al usuario serializado como JSON
        # str plano  → JSON string:  '"texto..."'
        # dict       → JSON object:  '{"introduccion": "...", ...}'
        sa.Column("consejo_solicitado", sa.Text(), nullable=True),

        # Estado del Coach en esa sesión (EXITOSO, NO_DISPONIBLE, etc.)
        sa.Column("estado_coach", sa.String(30), nullable=True),

        sa.PrimaryKeyConstraint("id"),
    )

    # Índices individuales (cubren filtros simples)
    op.create_index(
        "ix_ia_historial_coaching_usuario_id",
        "ia_historial_coaching",
        ["usuario_id"],
    )
    op.create_index(
        "ix_ia_historial_coaching_modulo",
        "ia_historial_coaching",
        ["modulo"],
    )

    # Índice COMPUESTO — clave para el rendimiento del orquestador.
    # Cubre: WHERE usuario_id = ? AND modulo = ? ORDER BY fecha_generacion DESC LIMIT 1
    op.create_index(
        "ix_ia_historial_usuario_modulo",
        "ia_historial_coaching",
        ["usuario_id", "modulo", "fecha_generacion"],
    )


def downgrade() -> None:
    op.drop_index("ix_ia_historial_usuario_modulo", table_name="ia_historial_coaching")
    op.drop_index("ix_ia_historial_coaching_modulo", table_name="ia_historial_coaching")
    op.drop_index("ix_ia_historial_coaching_usuario_id", table_name="ia_historial_coaching")
    op.drop_table("ia_historial_coaching")
