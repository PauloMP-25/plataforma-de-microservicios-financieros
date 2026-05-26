-- V1__crear_tablas.sql
-- LUKA APP - Microservicio de Pagos

CREATE TABLE pagos (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id       UUID NOT NULL,
    plan_solicitado  VARCHAR(20) NOT NULL,  -- FREE, PREMIUM, PRO
    monto            NUMERIC(10, 2) NOT NULL,
    moneda           VARCHAR(3) NOT NULL DEFAULT 'PEN',
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                     -- PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO
    stripe_session_id VARCHAR(255) UNIQUE,
    stripe_evento_id  VARCHAR(255) UNIQUE,  -- para idempotencia de webhooks
    fecha_inicio_plan TIMESTAMPTZ,
    fecha_fin_plan    TIMESTAMPTZ,
    email_usuario    VARCHAR(255),
    fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ
);

CREATE INDEX idx_pagos_usuario   ON pagos(usuario_id);
CREATE INDEX idx_pagos_sesion    ON pagos(stripe_session_id);
CREATE INDEX idx_pagos_estado    ON pagos(estado);

-- Tabla Outbox para garantizar entrega de eventos (Fase 4)
CREATE TABLE bandeja_salida (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_evento  VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    procesado    BOOLEAN NOT NULL DEFAULT FALSE,
    intentos     INT NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_proceso  TIMESTAMPTZ
);
