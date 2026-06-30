package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.puertos.IEmailService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.NotificacionService;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Despachador central de notificaciones que coordina los diferentes canales.
 * <p>
 * Implementa el patrón <b>Strategy</b> de forma pura: inyecta todas las 
 * implementaciones de {@link CanalNotificacionStrategy} y selecciona la adecuada
 * en tiempo de ejecución.
 * </p>
 * 
 * @version 1.5.0
 */
import com.mensajeria.aplicacion.servicios.canales.NotificacionAdminService;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacionDispatcherImpl implements NotificacionService, NotificacionAdminService {

    private final List<CanalNotificacionStrategy> estrategias;
    private final IEmailService emailService; // Mantener para métodos específicos de admin

    @Override
    @org.springframework.scheduling.annotation.Async("notificacionExecutor")
    public void enviar(TipoNotificacion tipo, String destinatario, Map<String, Object> variables) {
        log.debug("[DISPATCHER] Buscando estrategia para canal: {}", tipo);

        estrategias.stream()
                .filter(s -> s.soporta(tipo))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Canal no soportado: " + tipo))
                .enviar(destinatario, variables);
    }

    @Override
    public void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml) {
        // Delegamos al servicio de email especializado para tareas de administración
        emailService.enviarEmailAdministrador(destinatario, asunto, cuerpo, esHtml);
    }
}
