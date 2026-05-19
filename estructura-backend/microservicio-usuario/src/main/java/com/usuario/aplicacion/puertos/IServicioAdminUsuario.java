package com.usuario.aplicacion.puertos;

import com.libreria.comun.respuesta.Paginacion;
import com.usuario.dominio.entidades.Usuario;

import java.time.LocalDateTime;

/**
 * Puerto que define las operaciones de administración de usuarios en el sistema.
 * Habilita la consulta y filtrado avanzado mediante especificaciones JPA dinámicas,
 * abstrayéndose por completo de tipos de infraestructura de Spring Data JPA como Pageable.
 */
public interface IServicioAdminUsuario {

    /**
     * Busca y filtra usuarios de forma paginada utilizando criterios dinámicos.
     *
     * @param habilitado Estado de habilitación (opcional).
     * @param rol        Nombre del rol a filtrar (opcional).
     * @param texto      Texto de coincidencia parcial en usuario o correo (opcional).
     * @param desde      Fecha inicial de creación (opcional).
     * @param hasta      Fecha final de creación (opcional).
     * @param pagina     Número de página actual (basado en cero).
     * @param tamanio    Cantidad de registros por página.
     * @return Paginación estandarizada de usuarios coincidentes.
     */
    Paginacion<Usuario> buscarUsuarios(
            Boolean habilitado,
            String rol,
            String texto,
            LocalDateTime desde,
            LocalDateTime hasta,
            int pagina,
            int tamanio
    );
}
