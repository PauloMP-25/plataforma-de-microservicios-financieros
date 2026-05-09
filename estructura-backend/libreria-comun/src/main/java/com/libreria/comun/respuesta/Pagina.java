package com.libreria.comun.respuesta;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltura estandarizada para respuestas paginadas de LUKA APP.
 * <p>
 * Transforma el objeto {@link org.springframework.data.domain.Page} de Spring Data
 * en un formato simplificado y consistente para el consumo desde el Frontend.
 * </p>
 * 
 * @param <T>            Tipo de los elementos contenidos en la página.
 * @param contenido      Lista de elementos de la página actual.
 * @param numeroPagina   Índice de la página actual (basado en cero).
 * @param tamañoPagina   Cantidad de elementos solicitados por página.
 * @param totalElementos Cantidad total de registros existentes en la base de datos.
 * @param totalPaginas   Cantidad total de páginas disponibles.
 * @param esUltima       Indica si la página actual es la última de la colección.
 * 
 * @author Paulo Moron
 */
public record Pagina<T>(
    List<T> contenido,
    int numeroPagina,
    int tamañoPagina,
    long totalElementos,
    int totalPaginas,
    boolean esUltima
) {
    /**
     * Convierte una instancia de {@code Page} de Spring Data a nuestro formato {@code Pagina}.
     * 
     * @param <T>  Tipo de dato.
     * @param page Objeto de paginación de Spring Data.
     * @return Una nueva instancia de {@code Pagina} con los metadatos mapeados.
     */
    public static <T> Pagina<T> desde(Page<T> page) {
        return new Pagina<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}