package com.libreria.comun.respuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.libreria.comun.enums.CodigoError;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoltura universal para todas las respuestas de la plataforma LUKA APP.
 * <p>
 * Esta clase garantiza un contrato único entre el backend y los consumidores
 * (Frontend/IA), permitiendo manejar de forma consistente tanto respuestas
 * exitosas como errores. Utiliza anotaciones de Jackson para omitir campos
 * nulos.
 * </p>
 *
 * @param <T>         Tipo de dato que contiene la respuesta exitosa.
 * @param exito       Indica si la operación fue satisfactoria.
 * @param estado      Código de estado HTTP (ej. 200, 201, 404, 500).
 * @param error       Etiqueta semántica del error (ej.
 *                    "USUARIO_NO_REGISTRADO").
 * @param mensaje     Mensaje descriptivo en español para el usuario final.
 * @param datos       Carga útil de la respuesta (solo en caso de éxito).
 * @param detalles    Lista de errores específicos (principalmente para
 *                    validaciones).
 * @param ruta        URI del endpoint que originó la respuesta.
 * @param marcaTiempo Momento exacto en que se generó la respuesta.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultadoApi<T>(
        boolean exito,
        int estado,
        String error,
        String mensaje,
        T datos,
        List<String> detalles,
        @SuppressWarnings("rawtypes") Paginacion pagina,
        String ruta,
        LocalDateTime marcaTiempo) {

    // =========================================================================
    // FÁBRICAS DE ÉXITO (HTTP 2xx)
    // =========================================================================
    /**
     * Crea una respuesta de éxito estándar (HTTP 200 OK).
     *
     * @param <T>     Tipo de dato.
     * @param datos   Carga útil de la respuesta.
     * @param mensaje Descripción de la operación exitosa.
     * @param pagina  Número de página solicitado en metodos GET.
     * @return Instancia de ResultadoApi parametrizada.
     */
    public static <T> ResultadoApi<T> exito(T datos, String mensaje, Paginacion<?> pagina) {
        return new ResultadoApi<>(true, 200, null, mensaje, datos, null, pagina, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de éxito para creación de recursos (HTTP 201 Created).
     *
     * @param <T>     Tipo de dato.
     * @param datos   El recurso recién creado.
     * @param mensaje Confirmación de creación.
     * @return Instancia de ResultadoApi con estado 201.
     */
    public static <T> ResultadoApi<T> creado(T datos, String mensaje) {
        return new ResultadoApi<>(true, 201, null, mensaje, datos, null, null, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de éxito para solicitudes aceptadas pero no procesadas
     * aún (HTTP 202 Accepted). Útil para tareas asíncronas o colas.
     *
     * @param mensaje Estado del proceso aceptado.
     * @return Instancia de ResultadoApi con estado 202.
     */
    public static ResultadoApi<Void> aceptado(String mensaje) {
        return new ResultadoApi<>(true, 202, null, mensaje, null, null, null, null, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de éxito sin contenido (HTTP 204 No Content).
     *
     * @param mensaje Descripción de la operación (ej. eliminación exitosa).
     * @return Instancia de ResultadoApi con estado 204.
     */
    public static ResultadoApi<Void> sinContenido(String mensaje) {
        return new ResultadoApi<>(true, 204, null, mensaje, null, null, null, null, LocalDateTime.now());
    }

    // =========================================================================
    // FÁBRICAS DE ERROR BASADAS EN ENUM (RECOMENDADAS)
    // =========================================================================
    /**
     * Crea una falla utilizando el catálogo oficial de errores de LUKA APP.
     *
     * @param <T>     Tipo genérico (usualmente {@code Void}).
     * @param cod     Constante del Enum {@link CodigoError}.
     * @param mensaje Mensaje específico del error.
     * @param ruta    URI solicitada.
     * @return Instancia de ResultadoApi parametrizada como error.
     */
    public static <T> ResultadoApi<T> falla(CodigoError cod, String mensaje, String ruta) {
        return new ResultadoApi<>(false, cod.getStatus().value(), cod.name(), mensaje, null, null, null, ruta,
                LocalDateTime.now());
    }

    /**
     * Crea una falla con lista de detalles específicos utilizando el Enum
     * oficial. Ideal para errores de validación de negocio.
     *
     * @param <T>      Tipo genérico.
     * @param cod      Constante del Enum {@link CodigoError}.
     * @param mensaje  Mensaje general del error.
     * @param ruta     URI solicitada.
     * @param detalles Lista de strings con detalles técnicos o de campo.
     * @return Instancia de ResultadoApi con lista de detalles poblada.
     */
    public static <T> ResultadoApi<T> fallaConDetalles(CodigoError cod, String mensaje, String ruta,
            List<String> detalles) {
        return new ResultadoApi<>(false, cod.getStatus().value(), cod.name(), mensaje, null, detalles, null, ruta,
                LocalDateTime.now());
    }

    // =========================================================================
    // FÁBRICAS DE ERROR GENÉRICAS (FLEXIBILIDAD)
    // =========================================================================
    /**
     * Crea una respuesta de error estándar usando tipos primitivos.
     *
     * @param <T>     Tipo genérico.
     * @param estado  Código de estado HTTP manual.
     * @param error   Etiqueta de error manual.
     * @param mensaje Mensaje descriptivo.
     * @param ruta    URI solicitada.
     * @return Instancia de ResultadoApi con exito=false.
     */
    public static <T> ResultadoApi<T> falla(int estado, String error, String mensaje, String ruta) {
        return new ResultadoApi<>(false, estado, error, mensaje, null, null, null, ruta, LocalDateTime.now());
    }

    /**
     * Crea una respuesta de error enriquecida con detalles usando tipos
     * primitivos.
     *
     * @param <T>      Tipo genérico.
     * @param estado   Código de estado HTTP manual.
     * @param error    Etiqueta de error manual.
     * @param mensaje  Mensaje descriptivo.
     * @param ruta     URI solicitada.
     * @param detalles Lista de detalles.
     * @return Instancia de ResultadoApi con exito=false y detalles.
     */
    public static <T> ResultadoApi<T> fallaConDetalles(int estado, String error, String mensaje, String ruta,
            List<String> detalles) {
        return new ResultadoApi<>(false, estado, error, mensaje, null, detalles, null ,ruta, LocalDateTime.now());
    }
}
