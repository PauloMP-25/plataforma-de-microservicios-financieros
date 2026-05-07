package com.usuario.infraestructura.clientes;


import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientePerfilExternoFallback implements ClientePerfilExterno {
    @Override
    public void crearPerfilInicial(UUID usuarioId) {
        log.error("No se pudo crear el perfil inicial para el usuario {}. Se deberá sincronizar posteriormente.", usuarioId);
    }

    @Override
    public void actualizarTelefono(UUID usuarioId, String telefono) {
        log.error("No se pudo actualizar el teléfono para el usuario {}. Se deberá sincronizar posteriormente.", usuarioId);
    }

    @Override
    public String obtenerTelefono(UUID usuarioId) {
        log.error("No se pudo obtener el teléfono para el usuario {}. Se deberá sincronizar posteriormente.", usuarioId);
        return null;
    }
}
