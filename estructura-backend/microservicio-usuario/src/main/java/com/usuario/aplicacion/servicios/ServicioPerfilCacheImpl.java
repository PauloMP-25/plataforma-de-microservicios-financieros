package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.puertos.IServicioPerfilCache;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementación concreta del puerto IServicioPerfilCache.
 * Utiliza anotaciones de caché de Spring con Redis para interceptar y optimizar consultas síncronas.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioPerfilCacheImpl implements IServicioPerfilCache {

    private final ClientePerfilExterno clientePerfilExterno;

    @Override
    @Cacheable(value = "telefonos", key = "#usuarioId", unless = "#result == null")
    public String obtenerTelefono(UUID usuarioId) {
        log.info("[CACHE-MISS] Consultando teléfono de usuario {} de forma síncrona en microservicio-cliente", usuarioId);
        return clientePerfilExterno.obtenerTelefono(usuarioId);
    }
}
