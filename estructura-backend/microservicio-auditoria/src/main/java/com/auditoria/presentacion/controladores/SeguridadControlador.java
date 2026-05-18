package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import com.auditoria.dominio.entidades.ListaNegraIp;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone las operaciones de seguridad y defensa
 * perimetral.
 * <p>
 * Proporciona endpoints críticos para la evaluación de amenazas en tiempo real.
 * Principalmente utilizado por el API Gateway para consultar si una IP origen
 * se encuentra en la lista negra antes de permitir el enrutamiento de
 * peticiones.
 * </p>
 * 
 * @author Paulo Moron
 * @version 2.0.0
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/api/v1/seguridad")
@RequiredArgsConstructor
public class SeguridadControlador {

    private final ServicioSeguridadAuditoria servicioSeguridad;

    /**
     * Verifica el estado actual de una dirección IP frente a la lista negra.
     * <p>
     * Este endpoint es consultado de manera intensiva por el Gateway. Retorna un
     * DTO que indica si la IP está libre o si tiene un bloqueo activo (y hasta
     * cuándo).
     * </p>
     * 
     * @param ip Dirección IP a consultar.
     * @return {@link ResponseEntity} conteniendo el {@link ResultadoApi} con los
     *         datos de verificación.
     */
    @GetMapping("/verificar-ip/{ip}")
    public ResponseEntity<ResultadoApi<RespuestaVerificacionIpDTO>> verificarIp(@PathVariable String ip) {
        RespuestaVerificacionIpDTO respuesta = servicioSeguridad.verificarEstadoIp(ip);

        return ResponseEntity.ok(
                ResultadoApi.exito(
                        respuesta,
                        "Verificación de estado de IP completada.", null));
    }

    /**
     * Recupera la lista paginada de todos los bloqueos e historial en la lista negra.
     * Acceso restringido a administradores.
     * 
     * @param pagina  Número de página (0 por defecto).
     * @param tamanio Cantidad de registros por página (20 por defecto, máx 100).
     * @return {@link ResponseEntity} con el {@link ResultadoApi} y la paginación estándar.
     */
    @GetMapping("/lista-negra")
    public ResponseEntity<ResultadoApi<List<ListaNegraIp>>> listarBloqueos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        int tamanioSeguro = Math.min(tamanio, 100);
        Page<ListaNegraIp> resultadoPage =
                servicioSeguridad.listarBloqueos(PageRequest.of(pagina, tamanioSeguro));

        Paginacion<ListaNegraIp> metadata = Paginacion.desde(resultadoPage);

        return ResponseEntity.ok(
                ResultadoApi.exito(
                        metadata.contenido(),
                        "Listado de lista negra de IP recuperado con éxito.",
                        metadata));
    }

    /**
     * Bloquea manualmente una dirección IP por motivos de seguridad.
     * Acceso restringido a administradores.
     * 
     * @param ip      Dirección IP a bloquear.
     * @param motivo  Razón descriptiva del bloqueo.
     * @param minutos Duración del bloqueo en minutos (opcional).
     * @return Respuesta de éxito.
     */
    @PostMapping("/lista-negra/bloquear")
    public ResponseEntity<ResultadoApi<Void>> bloquearIp(
            @RequestParam String ip,
            @RequestParam(required = false) String motivo,
            @RequestParam(defaultValue = "60") int minutos) {

        servicioSeguridad.bloquearIpManualmente(ip, motivo, minutos);
        return ResponseEntity.ok(
                ResultadoApi.exito(
                        null,
                        String.format("IP %s bloqueada de forma manual exitosamente por %d minutos.", ip, minutos),
                        null));
    }

    /**
     * Desbloquea manualmente una dirección IP previamente bloqueada.
     * Acceso restringido a administradores.
     * 
     * @param ip Dirección IP a desbloquear.
     * @return Respuesta de éxito.
     */
    @DeleteMapping("/lista-negra/desbloquear")
    public ResponseEntity<ResultadoApi<Void>> desbloquearIp(@RequestParam String ip) {
        servicioSeguridad.desbloquearIpManualmente(ip);
        return ResponseEntity.ok(
                ResultadoApi.exito(
                        null,
                        String.format("IP %s desbloqueada manualmente con éxito.", ip),
                        null));
    }
}
