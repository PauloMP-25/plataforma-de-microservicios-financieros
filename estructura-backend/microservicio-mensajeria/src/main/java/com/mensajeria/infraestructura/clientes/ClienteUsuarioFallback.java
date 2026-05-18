package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback resiliente para el cliente Feign de Usuario.
 */
@Component
@Slf4j
public class ClienteUsuarioFallback implements ClienteUsuario {

    @Override
    public String activarCuenta(UUID usuarioId, String telefono) {
        log.error("[FEIGN-FALLBACK] ms-usuario no disponible. Activación PENDIENTE para: {}", usuarioId);
        return "ACTIVACION_PENDIENTE";
    }
}
