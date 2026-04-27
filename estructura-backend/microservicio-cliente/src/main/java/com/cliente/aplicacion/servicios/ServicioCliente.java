package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.excepciones.*;
import com.cliente.dominio.entidades.Cliente;
import com.cliente.dominio.repositorios.ClienteRepository;
import com.cliente.infraestructura.clientes.ClienteAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de la lógica de negocio para la gestión de clientes.
 * @author Paulo
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioCliente {

    private final ClienteRepository clienteRepository;
    private final ClienteAuditoria clienteAuditoria;
    private static final String MODULO = "MICROSERVICIO-CLIENTE";

    /**
     * Crea un perfil vacío vinculado a un nuevo usuario. Es idempotente: si el
     * perfil existe, lo devuelve sin crear uno nuevo.
     * @param usuarioId
     * @return 
     */
    @Transactional
    public RespuestaCliente crearPerfilInicial(UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId)
                .map(this::convertirADTO)
                .orElseGet(() -> {
                    Cliente nuevoCliente = Cliente.builder()
                            .usuarioId(usuarioId)
                            .perfilCompleto(false)
                            .build();
                    log.info("Creando perfil inicial para usuarioId={}", usuarioId);
                    return convertirADTO(clienteRepository.save(nuevoCliente));
                });
    }

    /**
     * Actualiza la información del perfil del cliente con validación de
     * propiedad.
     * @param usuarioIdRuta
     * @param usuarioIdToken
     * @param request
     * @param ipOrigen
     * @return 
     */
    @Transactional
    public RespuestaCliente actualizarPerfil(UUID usuarioIdRuta, UUID usuarioIdToken, SolicitudCliente request, String ipOrigen) {

        validarPropiedad(usuarioIdRuta, usuarioIdToken);

        Cliente cliente = clienteRepository.findByUsuarioId(usuarioIdRuta)
                .orElseThrow(() -> new ClienteNoEncontradoException(usuarioIdRuta));

        validarDniUnico(request.getDni(), cliente);

        // Mapeo dinámico y actualización de estado
        actualizarDatosEntidad(cliente, request);
        cliente.setPerfilCompleto(cliente.evaluarPerfilCompleto());

        Cliente actualizado = clienteRepository.save(cliente);

        // Notificar al microservicio de auditoría
//        registrarAuditoria(actualizado, "ACTUALIZAR_PERFIL", ipOrigen);

        return convertirADTO(actualizado);
    }

    @Transactional(readOnly = true)
    public RespuestaCliente consultarPerfil(UUID usuarioIdRuta, UUID usuarioIdToken) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);

        return clienteRepository.findByUsuarioId(usuarioIdRuta)
                .map(this::convertirADTO)
                .orElseThrow(() -> new ClienteNoEncontradoException(usuarioIdRuta));
    }

    // =========================================================================
    // Métodos de Soporte (Limpieza de Código)
    // =========================================================================
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.error("Acceso no autorizado: Usuario {} intentó acceder al perfil de {}", usuarioIdToken, usuarioIdRuta);
            throw new AccesoDenegadoException();
        }
    }

    private void validarDniUnico(String nuevoDni, Cliente clienteActual) {
        if (nuevoDni != null && !nuevoDni.equals(clienteActual.getDni()) && clienteRepository.existsByDni(nuevoDni)) {
            throw new DniDuplicadoException(nuevoDni);
        }
    }

    /**
     * En lugar de múltiples IFs, centralizamos la lógica de actualización. Tip:
     * Si el proyecto crece, usa MapStruct con
     * @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
     */
    private void actualizarDatosEntidad(Cliente c, SolicitudCliente d) {
        if (d.getNombres() != null) {
            c.setNombres(d.getNombres());
        }
        if (d.getApellidos() != null) {
            c.setApellidos(d.getApellidos());
        }
        if (d.getDni() != null) {
            c.setDni(d.getDni());
        }
        if (d.getFotoPerfilUrl() != null) {
            c.setFotoPerfilUrl(d.getFotoPerfilUrl());
        }
        if (d.getBiografia() != null) {
            c.setBiografia(d.getBiografia());
        }
        if (d.getNumeroCelular() != null) {
            c.setNumeroCelular(d.getNumeroCelular());
        }
        if (d.getDireccion() != null) {
            c.setDireccion(d.getDireccion());
        }
        if (d.getCiudad() != null) {
            c.setCiudad(d.getCiudad());
        }
        if (d.getOcupacion() != null) {
            c.setOcupacion(d.getOcupacion());
        }
        if (d.getGenero() != null) {
            c.setGenero(d.getGenero());
        }
    }

//    private void registrarAuditoria(Cliente cliente, String accion, String ip) {
//        try {
//            String nombreCompleto = (cliente.getNombres() != null ? cliente.getNombres() : "Usuario")
//                    + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : "");
//
//            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
//                    nombreCompleto.trim(),
//                    accion,
//                    "Perfil actualizado correctamente",
//                    ip,
//                    MODULO
//            ));
//        } catch (Exception e) {
//            log.error("No se pudo enviar la auditoría, pero la transacción continúa: {}", e.getMessage());
//        }
//    }

    private RespuestaCliente convertirADTO(Cliente c) {
        return new RespuestaCliente(
                c.getId(), c.getUsuarioId(), c.getNombres(), c.getApellidos(), c.getDni(),
                c.getFotoPerfilUrl(), c.getBiografia(), c.getNumeroCelular(), c.getDireccion(),
                c.getCiudad(), c.getOcupacion(), c.getGenero(), c.getPerfilCompleto(),
                c.getFechaCreacion(), c.getFechaActualizacion()
        );
    }
}
