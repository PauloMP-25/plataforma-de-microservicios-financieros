package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.RegistroAuditoriaDTO;
import com.cliente.aplicacion.dtos.SolicitudCliente;
import com.cliente.aplicacion.dtos.RespuestaCliente;
//import com.cliente.aplicacion.dtos.SunatResponseDTO;
import com.cliente.aplicacion.excepciones.AccesoDenegadoException;
import com.cliente.aplicacion.excepciones.ClienteNoEncontradoException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.dominio.entidades.Cliente;
import com.cliente.dominio.repositorios.ClienteRepository;
import com.cliente.infraestructura.clientes.ClienteAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioCliente {

    private final ClienteRepository clienteRepository;
    private final ClienteAuditoria clienteAuditoria;
    private static final String MODULO = "MICROSERVICIO-CLIENTE";

    // =========================================================================
    // Crear perfil inicial (llamado por el IAM tras el registro)
    // =========================================================================
    @Transactional
    public RespuestaCliente crearPerfilInicial(UUID usuarioId) {
        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            log.warn("Intento de crear perfil duplicado para usuarioId={}", usuarioId);
            // Idempotente: si ya existe, devuelve el existente
            return convertirADTO(clienteRepository.findByUsuarioId(usuarioId).orElseThrow());
        }

        Cliente cliente = Cliente.builder()
                .usuarioId(usuarioId)
                .perfilCompleto(false)
                .build();

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Perfil inicial creado para usuarioId={}", usuarioId);
        return convertirADTO(guardado);
    }

//    // =========================================================================
//    // Consultar SUNAT (simulación)
//    // =========================================================================
//    public SunatResponseDTO consultarSunat(String dni) {
//        log.info("Consultando SUNAT para DNI={}", dni);
//
//        // ── Simulación: en producción, aquí va el WebClient a la API real ──
//        // Datos ficticios para pruebas
//        return new SunatResponseDTO(
//                dni,
//                "JUAN CARLOS",
//                "GARCIA",
//                "LOPEZ",
//                "JUAN CARLOS GARCIA LOPEZ"
//        );
//    }
    // =========================================================================
    // Actualizar perfil — VALIDACIÓN DE PROPIEDAD
    // =========================================================================
    @Transactional
    public RespuestaCliente actualizarPerfil(UUID usuarioIdRuta,
            UUID usuarioIdToken,
            SolicitudCliente request,
            String ipOrigen) {
        // ── VERIFICACIÓN DE PROPIEDAD ─────────────────────────────────────
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Intento de edición no autorizado: tokenUsuario={} vs rutaUsuario={}",
                    usuarioIdToken, usuarioIdRuta);
            throw new AccesoDenegadoException();
        }

        Cliente cliente = clienteRepository.findByUsuarioId(usuarioIdRuta)
                .orElseThrow(() -> new ClienteNoEncontradoException(usuarioIdRuta));

        // ── Validar DNI único si cambia ───────────────────────────────────
        if (request.getDni() != null
                && !request.getDni().equals(cliente.getDni())
                && clienteRepository.existsByDni(request.getDni())) {
            throw new DniDuplicadoException(request.getDni());
        }

        // ── Aplicar cambios (solo los campos no nulos) ────────────────────
        aplicarCambios(cliente, request);

        // ── Actualizar estado perfilCompleto ──────────────────────────────
        cliente.setPerfilCompleto(cliente.evaluarPerfilCompleto());

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Perfil actualizado — usuarioId={}, completo={}",
                usuarioIdRuta, actualizado.getPerfilCompleto());

        clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                cliente.getNombres() + " " + cliente.getApellidos(),
                "ACTUALIZAR_PERFIL",
                "Perfil completado exitosamente",
                ipOrigen, // <--- Usamos la IP que capturamos en el controlador
                MODULO
        ));

        return convertirADTO(actualizado);
    }

    // =========================================================================
    // Consultar perfil
    // =========================================================================
    @Transactional(readOnly = true)
    public RespuestaCliente consultarPerfil(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            throw new AccesoDenegadoException();
        }

        Cliente cliente = clienteRepository.findByUsuarioId(usuarioIdRuta)
                .orElseThrow(() -> new ClienteNoEncontradoException(usuarioIdRuta));

        return convertirADTO(cliente);
    }

    // =========================================================================
    // Privados
    // =========================================================================
    private void aplicarCambios(Cliente cliente, SolicitudCliente dto) {
        if (dto.getNombres() != null) {
            cliente.setNombres(dto.getNombres());
        }
        if (dto.getApellidos() != null) {
            cliente.setApellidos(dto.getApellidos());
        }
        if (dto.getDni() != null) {
            cliente.setDni(dto.getDni());
        }
        if (dto.getFotoPerfilUrl() != null) {
            cliente.setFotoPerfilUrl(dto.getFotoPerfilUrl());
        }
        if (dto.getBiografia() != null) {
            cliente.setBiografia(dto.getBiografia());
        }
        if (dto.getNumeroCelular() != null) {
            cliente.setNumeroCelular(dto.getNumeroCelular());
        }
        if (dto.getDireccion() != null) {
            cliente.setDireccion(dto.getDireccion());
        }
        if (dto.getCiudad() != null) {
            cliente.setCiudad(dto.getCiudad());
        }
        if (dto.getOcupacion() != null) {
            cliente.setOcupacion(dto.getOcupacion());
        }
        if (dto.getGenero() != null) {
            cliente.setGenero(dto.getGenero());
        }
    }

    private RespuestaCliente convertirADTO(Cliente c) {
        return new RespuestaCliente(
                c.getId(), c.getUsuarioId(),
                c.getNombres(), c.getApellidos(), c.getDni(),
                c.getFotoPerfilUrl(), c.getBiografia(),
                c.getNumeroCelular(), c.getDireccion(),
                c.getCiudad(), c.getOcupacion(), c.getGenero(),
                c.getPerfilCompleto(),
                c.getFechaCreacion(), c.getFechaActualizacion()
        );
    }
}
