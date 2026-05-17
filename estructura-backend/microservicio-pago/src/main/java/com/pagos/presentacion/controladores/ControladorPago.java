package com.pagos.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;
import com.pagos.aplicacion.servicios.IServicioStripe;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión de pagos desde la perspectiva del usuario.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class ControladorPago {

    private final IServicioStripe servicioStripe;

    /**
     * Inicia el proceso de suscripción creando una sesión de Stripe Checkout.
     * Solo accesible para usuarios autenticados.
     */
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaCheckoutDTO>> iniciarPago(@Valid @RequestBody SolicitudPagoDTO solicitud) {
        UUID usuarioId = UtilidadSeguridad.obtenerUsuarioId();
        String email = UtilidadSeguridad.obtenerUsuarioEmail();

        log.info("[PAGOS] Usuario {} iniciando suscripción al plan {}", email, solicitud.plan());
        
        RespuestaCheckoutDTO respuesta = servicioStripe.crearSesionCheckout(solicitud, usuarioId, email);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Sesión de checkout creada correctamente"));
    }

    /**
     * Devuelve el estado actual de la suscripción del usuario.
     */
    @GetMapping("/mi-suscripcion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcionDTO>> obtenerMiSuscripcion() {
        UUID usuarioId = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaSuscripcionDTO respuesta = servicioStripe.obtenerEstadoSuscripcion(usuarioId);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Información de suscripción recuperada"));
    }
}
