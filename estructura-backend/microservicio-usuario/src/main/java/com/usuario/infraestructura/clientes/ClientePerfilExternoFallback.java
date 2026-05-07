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
}
