package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoTransaccionRegistradaDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PublicadorFinanciero extends PublicadorEventosBase {

    public PublicadorFinanciero(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    public void publicarTransaccionRegistrada(UUID id, UUID usuarioId, BigDecimal monto, String tipo, String fechaTransaccion) {
        EventoTransaccionRegistradaDTO evento = EventoTransaccionRegistradaDTO.builder()
                .transaccionId(id.toString())
                .usuarioId(usuarioId.toString())
                .monto(monto)
                .tipo(tipo)
                .fechaTransaccion(fechaTransaccion)
                .build();

        enviar(NombresExchange.FINANCIERO, RoutingKeys.TRANSACCION_REGISTRADA, evento);
    }
}
