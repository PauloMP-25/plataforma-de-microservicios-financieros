package com.mensajeria.presentacion.controladores;

import com.libreria.comun.enums.PropositoCodigo;
import com.libreria.comun.respuesta.ResultadoApi;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.especificaciones.MensajeriaSpecs;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador administrativo de Mensajería.
 * <p>
 * Proporciona endpoints para la auditoría, búsqueda avanzada de códigos OTP
 * y la gestión de usuarios bloqueados por intentos fallidos.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/mensajeria/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ControladorAdminMensajeria {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;

    /**
     * Búsqueda dinámica y paginada de códigos OTP.
     */
    @GetMapping("/codigos")
    public ResponseEntity<ResultadoApi<Page<CodigoVerificacion>>> buscarCodigos(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) PropositoCodigo proposito,
            @RequestParam(required = false) Boolean usado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Pageable pageable) {

        log.debug("[ADMIN] Búsqueda dinámica de códigos de verificación");

        Specification<CodigoVerificacion> spec = Specification.where(MensajeriaSpecs.porUsuario(usuarioId))
                .and(MensajeriaSpecs.porProposito(proposito))
                .and(MensajeriaSpecs.estaUsado(usado))
                .and(MensajeriaSpecs.creadoEntre(desde, hasta));

        Page<CodigoVerificacion> codigos = codigoRepository.findAll(spec, pageable);
        return ResponseEntity.ok(ResultadoApi.exito(codigos, "Búsqueda exitosa", null));
    }

    /**
     * Lista todos los usuarios actualmente bloqueados por exceder intentos fallidos de OTP.
     */
    @GetMapping("/bloqueados")
    public ResponseEntity<ResultadoApi<List<IntentoValidacion>>> listarBloqueados() {
        log.debug("[ADMIN] Listando usuarios con bloqueo activo de OTP");
        List<IntentoValidacion> bloqueados = intentoRepository.findByBloqueadoTrue();
        return ResponseEntity.ok(ResultadoApi.exito(bloqueados, "Usuarios bloqueados recuperados exitosamente", null));
    }

    /**
     * Desbloquea manualmente a un usuario sancionado.
     */
    @DeleteMapping("/bloqueos/{usuarioId}")
    public ResponseEntity<ResultadoApi<Void>> desbloquearUsuario(@PathVariable UUID usuarioId) {
        log.info("[ADMIN] Solicitud de desbloqueo manual para el usuario: {}", usuarioId);

        intentoRepository.findByUsuarioId(usuarioId).ifPresent(intento -> {
            intento.reiniciar();
            intentoRepository.save(intento);
            log.info("[ADMIN] Usuario {} desbloqueado exitosamente", usuarioId);
        });

        return ResponseEntity.ok(ResultadoApi.sinContenido("Usuario desbloqueado exitosamente"));
    }
}
