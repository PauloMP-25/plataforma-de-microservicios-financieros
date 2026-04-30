package com.nucleo.financiero.aplicacion.servicios;


import com.nucleo.financiero.aplicacion.dtos.ia.RespuestaIaDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.SolicitudIaDTO;
import com.nucleo.financiero.infraestructura.clientes.ClienteIa;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.infraestructura.feign.ClienteContexto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServicioIa {

    private final ClienteIa clienteIa;
    private final ClienteContexto clienteContexto; // Tu nueva interfaz Feign
    private final PublicadorAuditoria publicadorAuditoria;

    public RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud) {
        // 1. Obtener contexto del cliente (Meta, ahorros, etc.)
        // Usamos UUID porque tu ClienteContexto lo requiere
        var contexto = clienteContexto.obtenerContexto(UUID.fromString(solicitud.getIdUsuario()));
        
        // 2. Enriquecer la solicitud (Opcional: puedes pasar datos del contexto a la IA)
        //solicitud.setHistorial(contexto.getTransacciones()); // Ejemplo

        // 3. Llamada síncrona a Python (Feign)
        RespuestaIaDTO respuesta = clienteIa.analizarFinanzas(solicitud);
        
        // 4. Registro Asíncrono en Auditoría (RabbitMQ)
        // Usamos tu método 'publicarAcceso' porque pedir un consejo IA 
        // es técnicamente un acceso a información analítica.
        publicadorAuditoria.publicarAcceso(
                UUID.fromString(solicitud.getIdUsuario()), 
                "CONSULTA_IA", 
                "El usuario consultó el módulo: " + solicitud.getModuloSolicitado(),
                "0.0.0.0" // Aquí podrías pasar la IP real si la tienes
        );

        return respuesta;
    }
}