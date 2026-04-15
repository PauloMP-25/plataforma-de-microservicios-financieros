package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.SolicitudCliente;
import com.cliente.aplicacion.dtos.RespuestaCliente;
import com.cliente.aplicacion.dtos.ErrorApi;
//import com.cliente.aplicacion.dtos.SunatResponseDTO;
import com.cliente.aplicacion.excepciones.AccesoDenegadoException;
import com.cliente.aplicacion.excepciones.ClienteNoEncontradoException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.aplicacion.servicios.ServicioCliente;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Slf4j
public class ControladorCliente {

    private final ServicioCliente clienteService;

    // =========================================================================
    // GET /sunat/{dni}
    // =========================================================================
//    @GetMapping("/sunat/{dni}")
//    public ResponseEntity<?> consultarSunat(
//            @PathVariable String dni,
//            HttpServletRequest request) {
//
//        try {
//            SunatResponseDTO resultado = clienteService.consultarSunat(dni);
//            return ResponseEntity.ok(resultado);
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(ErrorApi.of(503, "SUNAT_NO_DISPONIBLE",
//                            ex.getMessage(), request.getRequestURI()));
//        }
//    }
    // =========================================================================
    // POST /inicial  — Solo ADMIN/SYSTEM
    // =========================================================================
    @PostMapping("/inicial")
    public ResponseEntity<?> crearPerfilInicial(
            @RequestParam UUID usuarioId,
            HttpServletRequest request) {

        try {
            RespuestaCliente creado = clienteService.crearPerfilInicial(usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorApi.of(500, "ERROR_INTERNO",
                            ex.getMessage(), request.getRequestURI()));
        }
    }

    // =========================================================================
    // PUT /completar/{usuarioId}  — Actualización con validación de propiedad
    // =========================================================================
    @PutMapping("/completar/{usuarioId}")
    public ResponseEntity<?> completarPerfil(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudCliente requestDTO,
            HttpServletRequest request) {

        //Obtencion de la IP del cliente
        String ipCliente = UtilidadIp.obtenerIpRemota(request);

        // Si usas un Proxy o Gateway (como Nginx), la IP real viene aquí:
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            ipCliente = xForwardedFor.split(",")[0];
        }
        // ── Extraer usuarioId del Token (puesto por FiltroJwt) ─────────────
        UUID usuarioIdToken = (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);

        log.debug("PUT /completar/{} — tokenUsuarioId: {}", usuarioId, usuarioIdToken);

        try {
            RespuestaCliente actualizado
                    = clienteService.actualizarPerfil(usuarioId, usuarioIdToken, requestDTO, ipCliente);
            return ResponseEntity.ok(actualizado);

        } catch (AccesoDenegadoException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorApi.of(403, "ACCESO_DENEGADO",
                            ex.getMessage(), request.getRequestURI()));

        } catch (ClienteNoEncontradoException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "CLIENTE_NO_ENCONTRADO",
                            ex.getMessage(), request.getRequestURI()));

        } catch (DniDuplicadoException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorApi.of(409, "DNI_DUPLICADO",
                            ex.getMessage(), request.getRequestURI()));
        }
    }

    // =========================================================================
    // GET /perfil/{usuarioId}
    // =========================================================================
    @GetMapping("/perfil/{usuarioId}")
    public ResponseEntity<?> consultarPerfil(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioIdToken = (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);

        try {
            RespuestaCliente perfil
                    = clienteService.consultarPerfil(usuarioId, usuarioIdToken);
            return ResponseEntity.ok(perfil);

        } catch (AccesoDenegadoException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorApi.of(403, "ACCESO_DENEGADO",
                            ex.getMessage(), request.getRequestURI()));

        } catch (ClienteNoEncontradoException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "CLIENTE_NO_ENCONTRADO",
                            ex.getMessage(), request.getRequestURI()));
        }
    }
}
