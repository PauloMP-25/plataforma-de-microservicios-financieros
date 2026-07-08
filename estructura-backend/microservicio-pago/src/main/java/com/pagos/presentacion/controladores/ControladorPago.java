package com.pagos.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;
import com.pagos.aplicacion.puertos.IPasarelaPagoEstrategia;
import com.pagos.aplicacion.servicios.PasarelaPagoFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión de pagos y suscripciones desde la perspectiva del usuario.
 *
 * <p>Aplica el Principio de Inversión de Dependencia (DIP): depende de la abstracción
 * {@link PasarelaPagoFactory} y no de implementaciones concretas de Stripe o Mercado Pago.
 * La selección de la pasarela se delega dinámicamente en la fábrica según el campo
 * {@code proveedor} del cuerpo de la solicitud.</p>
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class ControladorPago {

    private final PasarelaPagoFactory pasarelaFactory;

    /**
     * Inicia el proceso de suscripción seleccionando dinámicamente la pasarela de pago.
     *
     * <p>Si el campo {@code proveedor} no se envía en el body, se usa {@code STRIPE}
     * por retrocompatibilidad con el frontend actual.</p>
     *
     * @param solicitud DTO con el plan y el proveedor de pago opcional.
     * @return DTO con la URL de checkout y detalles de la transacción.
     */
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaCheckoutDTO>> iniciarPago(
            @Valid @RequestBody SolicitudPagoDTO solicitud) {

        UUID usuarioId = UtilidadSeguridad.obtenerUsuarioId();
        String email = UtilidadSeguridad.obtenerUsuarioEmail();

        // Resolver estrategia: STRIPE por defecto si no se especifica proveedor
        IPasarelaPagoEstrategia estrategia = pasarelaFactory
                .obtenerEstrategia(solicitud.proveedorEfectivo());

        log.info("[PAGOS] Usuario {} iniciando suscripción al plan {} via {}",
                email, solicitud.plan(), solicitud.proveedorEfectivo());

        RespuestaCheckoutDTO respuesta = estrategia.crearSesionCheckout(solicitud, usuarioId, email);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta,
                "Sesión de checkout creada correctamente via " + solicitud.proveedorEfectivo()));
    }

    /**
     * Devuelve el estado actual de la suscripción del usuario.
     * Agnóstico a la pasarela: consulta el registro de pagos interno sin distinción de proveedor.
     *
     * @return DTO con el plan activo y su fecha de vencimiento.
     */
    @GetMapping("/mi-suscripcion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcionDTO>> obtenerMiSuscripcion() {
        UUID usuarioId = UtilidadSeguridad.obtenerUsuarioId();
        // Usamos STRIPE como proveedor predeterminado para la consulta
        // (la lógica de obtenerEstadoSuscripcion es idéntica en ambas estrategias)
        IPasarelaPagoEstrategia estrategia = pasarelaFactory
                .obtenerEstrategia(com.pagos.aplicacion.enums.ProveedorPago.STRIPE);
        RespuestaSuscripcionDTO respuesta = estrategia.obtenerEstadoSuscripcion(usuarioId);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Información de suscripción recuperada"));
    }
}
