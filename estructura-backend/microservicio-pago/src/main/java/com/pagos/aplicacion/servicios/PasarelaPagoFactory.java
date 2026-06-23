package com.pagos.aplicacion.servicios;

import com.pagos.aplicacion.enums.ProveedorPago;
import com.pagos.aplicacion.puertos.IPasarelaPagoEstrategia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fábrica de estrategias de pasarela de pago (Factory Method Pattern + DIP).
 *
 * <p>Inyecta dinámicamente todas las implementaciones de {@link IPasarelaPagoEstrategia}
 * presentes en el contexto de Spring y las indexa por su {@link ProveedorPago}. De esta
 * forma, añadir un nuevo proveedor de pago solo requiere crear una nueva implementación
 * de la interfaz — la fábrica la detecta y registra automáticamente.</p>
 *
 * <p>La resolución de la estrategia correcta se realiza en tiempo de ejecución según
 * el proveedor indicado en la solicitud de checkout ({@code SolicitudPagoDTO.proveedor}).</p>
 *
 * @author LUKA APP Team
 * @see IPasarelaPagoEstrategia
 */
@Slf4j
@Component
public class PasarelaPagoFactory {

    private final Map<ProveedorPago, IPasarelaPagoEstrategia> estrategias;

    /**
     * Constructor: recibe todas las implementaciones de {@link IPasarelaPagoEstrategia}
     * inyectadas por Spring y las mapea por proveedor para acceso O(1).
     *
     * @param listaEstrategias Lista de todas las estrategias disponibles en el contexto.
     */
    public PasarelaPagoFactory(List<IPasarelaPagoEstrategia> listaEstrategias) {
        this.estrategias = listaEstrategias.stream()
                .collect(Collectors.toMap(IPasarelaPagoEstrategia::getProveedor, Function.identity()));

        log.info("[PASARELA-FACTORY] Estrategias de pago registradas: {}",
                this.estrategias.keySet());
    }

    /**
     * Resuelve la estrategia de pago correspondiente al proveedor solicitado.
     *
     * @param proveedor El proveedor de pago deseado (STRIPE, MERCADOPAGO, etc.).
     * @return La implementación de {@link IPasarelaPagoEstrategia} para ese proveedor.
     * @throws IllegalArgumentException si no existe una estrategia registrada para el proveedor.
     */
    public IPasarelaPagoEstrategia obtenerEstrategia(ProveedorPago proveedor) {
        IPasarelaPagoEstrategia estrategia = estrategias.get(proveedor);
        if (estrategia == null) {
            throw new IllegalArgumentException(
                    "No existe estrategia de pago registrada para el proveedor: " + proveedor +
                    ". Proveedores disponibles: " + estrategias.keySet());
        }
        return estrategia;
    }
}
