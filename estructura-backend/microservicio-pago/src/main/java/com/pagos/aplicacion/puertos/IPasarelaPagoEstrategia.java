package com.pagos.aplicacion.puertos;

import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;
import com.pagos.aplicacion.enums.ProveedorPago;

import java.util.UUID;

/**
 * Puerto de estrategia para pasarelas de pago (Strategy Pattern + DIP).
 *
 * <p>Define el contrato que toda pasarela de pago debe implementar para integrarse
 * en LUKA APP. Aplicando el Principio de Inversión de Dependencia (DIP), la capa
 * de presentación ({@code ControladorPago}) depende de esta abstracción, nunca de
 * implementaciones concretas (Stripe, Mercado Pago, etc.).</p>
 *
 * <p>Aplicando el Principio de Abierto/Cerrado (OCP), añadir un nuevo proveedor
 * de pago solo requiere crear una nueva clase que implemente esta interfaz, sin
 * modificar el código existente.</p>
 *
 * @author LUKA APP Team
 * @see com.pagos.aplicacion.servicios.PasarelaPagoFactory
 */
public interface IPasarelaPagoEstrategia {

    /**
     * Identifica el proveedor de pago que implementa esta estrategia.
     * Usado por {@code PasarelaPagoFactory} para resolver la implementación correcta.
     *
     * @return El {@link ProveedorPago} asociado a esta estrategia.
     */
    ProveedorPago getProveedor();

    /**
     * Inicia el proceso de checkout para un plan de suscripción dado.
     * Cada implementación crea la sesión/preapproval en su pasarela correspondiente.
     *
     * @param solicitud    DTO con el plan solicitado y el proveedor de pago.
     * @param usuarioId    UUID del usuario autenticado.
     * @param emailUsuario Correo electrónico del usuario para la sesión de pago.
     * @return DTO con la URL de checkout y detalles de la transacción.
     */
    RespuestaCheckoutDTO crearSesionCheckout(SolicitudPagoDTO solicitud, UUID usuarioId, String emailUsuario);

    /**
     * Recupera el estado de suscripción activo del usuario.
     *
     * @param usuarioId UUID del usuario autenticado.
     * @return DTO con los detalles del plan activo y fecha de vencimiento.
     */
    RespuestaSuscripcionDTO obtenerEstadoSuscripcion(UUID usuarioId);
}
