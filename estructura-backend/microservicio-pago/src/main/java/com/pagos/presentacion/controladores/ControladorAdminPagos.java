package com.pagos.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.pagos.aplicacion.dtos.ResumenPagosDTO;
import com.pagos.aplicacion.servicios.IServicioAdminPagos;
import com.pagos.dominio.entidades.Pago;
import com.pagos.infraestructura.mensajeria.PublicadorAuditoriaPagosImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión administrativa de pagos.
 * Solo accesible por usuarios con ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/pagos/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ControladorAdminPagos {

    private final IServicioAdminPagos servicioAdmin;
    private final PublicadorAuditoriaPagosImpl publicadorAuditoria;

    @GetMapping("/resumen")
    public ResponseEntity<ResultadoApi<ResumenPagosDTO>> obtenerResumen() {
        ResumenPagosDTO resumen = servicioAdmin.obtenerResumenGeneral();
        return ResponseEntity.ok(ResultadoApi.exito(resumen, "Resumen administrativo generado"));
    }

    @GetMapping("/historial")
    public ResponseEntity<ResultadoApi<Page<Pago>>> listarPagos(Pageable pageable, HttpServletRequest request) {
        publicadorAuditoria.auditarAccesoAdmin(
                UtilidadSeguridad.obtenerUsuarioId(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );
        Page<Pago> pagos = servicioAdmin.listarTodosLosPagos(pageable);
        return ResponseEntity.ok(ResultadoApi.exito(pagos, "Historial de pagos recuperado"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultadoApi<Pago>> obtenerDetalle(@PathVariable UUID id) {
        Pago pago = servicioAdmin.buscarPagoPorId(id);
        return ResponseEntity.ok(ResultadoApi.exito(pago, "Detalle del pago recuperado"));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ResultadoApi<Void>> corregirEstado(
            @PathVariable UUID id,
            @RequestParam String nuevoEstado) {
        servicioAdmin.actualizarEstadoManual(id, nuevoEstado);
        return ResponseEntity.ok(ResultadoApi.sinContenido("Estado del pago actualizado manualmente"));
    }
}
