package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.libreria.comun.respuesta.ResultadoApi;

/**
 * Fallback de Resilience4j para {@link ClienteActualizarTelefono}.
 * <p>
 * Si el ms-usuario no responde o falla, esta implementación es invocada
 * automáticamente. El flujo de negocio <strong>no muere</strong>: el OTP ya
 * fue validado y el usuario puede continuar. El fallo de sincronización del
 * teléfono se registra en logs para reintento manual o eventual reconciliación.
 * </p>
 *
 * @version 1.1.0
 */
@Component
@Slf4j
public class ClienteActualizarTelefonoFallback implements ClienteActualizarTelefono {

    /**
     * Fallback del endpoint de sincronización de teléfono.
     * Registra la sincronización como pendiente sin lanzar excepción,
     * para que el usuario no experimente un error en su flujo de activación.
     *
     * @param usuarioId UUID del usuario cuyo teléfono no pudo sincronizarse.
     * @param telefono  Número en formato E.164 que debía guardarse en ms-cliente.
     * @return Mensaje estático indicando que la sincronización quedó pendiente.
     */
    @Override
    public ResultadoApi<String> sincronizarTelefono(UUID usuarioId, String telefono) {
        log.error(
                "[FEIGN-FALLBACK] ms-cliente no disponible. Sincronización de teléfono PENDIENTE para usuario: {}",
                usuarioId);
        return ResultadoApi.exito("SINCRONIZACION_PENDIENTE", "Sincronización diferida");
    }
}
