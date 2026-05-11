package com.libreria.comun.autoconfiguracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.mensajeria.ConfiguracionRabbitBase;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import com.libreria.comun.seguridad.ServicioJwt;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Registro automático de los componentes de la librería LUKA COMMONS.
 * <p>
 * Gracias a esta configuración, los microservicios solo necesitan incluir
 * la dependencia para tener acceso a la seguridad, mensajería y manejo de
 * errores.
 * </p>
 */
@AutoConfiguration
@Import({ ConfiguracionRabbitBase.class }) // Importamos la base de Rabbit
public class LukaConfiguracion {

    @Bean
    @ConditionalOnMissingBean
    public ServicioJwt servicioJwt() {
        return new ServicioJwt();
    }

    @Bean
    @ConditionalOnMissingBean
    public FiltroJwt filtroJwt(ServicioJwt servicioJwt) {
        return new FiltroJwt(servicioJwt);
    }

    @Bean
    @ConditionalOnMissingBean
    public PuntoEntradaJwt puntoEntradaJwt(ObjectMapper objectMapper) {
        return new PuntoEntradaJwt(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ManejadorGlobalExcepcionesBase manejadorGlobalExcepciones() {
        return new ManejadorGlobalExcepcionesBase() {
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicadorEventosBase publicadorEventosBase(RabbitTemplate rabbitTemplate) {
        return new PublicadorEventosBase(rabbitTemplate);
    }
}
