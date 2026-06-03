package com.libreria.comun.autoconfiguracion;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

/**
 * Interceptor Feign que propaga el correlationId del Mapped Diagnostic Context (MDC)
 * actual a la cabecera X-Correlation-ID de las peticiones HTTP salientes hacia otros microservicios.
 */
public class InterceptorCorrelacion implements RequestInterceptor {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(MDC_KEY);
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            template.header(HEADER_CORRELATION_ID, correlationId);
        }
    }
}
