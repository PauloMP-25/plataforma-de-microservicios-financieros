package com.cliente.infraestructura.mensajeria;

import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.libreria.comun.dtos.EventoTransaccionRegistradaDTO;
import com.libreria.comun.mensajeria.NombresCola;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorTransacciones {

    private final MetaAhorroRepositorio metaAhorroRepositorio;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = NombresCola.FINANCIERO_TRANSACCIONES_CLIENTE)
    @Transactional
    public void procesarTransaccionRegistrada(EventoTransaccionRegistradaDTO evento) {
        log.info("[MS-CLIENTE] Recibido evento de transacción registrada: {}", evento);
        
        try {
            UUID usuarioId = UUID.fromString(evento.getUsuarioId());
            BigDecimal monto = evento.getMonto();
            boolean esIngreso = "INGRESO".equalsIgnoreCase(evento.getTipo());

            // 1. Actualizar reactivamente el caché de transacciones en Redis (Estrategia modular .append)
            try {
                String hashKey = "usuario:" + usuarioId;
                String campo = "transacciones";
                
                log.info("[MS-CLIENTE] Actualizando caché modular de transacciones en Redis para usuarioId={}", usuarioId);
                
                String transaccionesJson = (String) redisTemplate.opsForHash().get(hashKey, campo);
                List<EventoTransaccionRegistradaDTO> listaTransacciones;
                
                if (transaccionesJson != null && !transaccionesJson.isBlank()) {
                    listaTransacciones = objectMapper.readValue(
                            transaccionesJson, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, EventoTransaccionRegistradaDTO.class)
                    );
                } else {
                    listaTransacciones = new java.util.ArrayList<>();
                }
                
                // Agregar la nueva transacción (.append)
                listaTransacciones.add(0, evento); // Se añade al inicio (más reciente)
                
                // Guardar la lista actualizada de nuevo en Redis
                redisTemplate.opsForHash().put(hashKey, campo, objectMapper.writeValueAsString(listaTransacciones));
                
                // Asegurar expiración del Hash (1 hora)
                redisTemplate.expire(hashKey, java.time.Duration.ofHours(1));
                
                log.info("[MS-CLIENTE] Transacción {} añadida exitosamente en Redis (Total caché: {})", 
                        evento.getTransaccionId(), listaTransacciones.size());
                        
            } catch (Exception re) {
                log.error("[MS-CLIENTE] Error no bloqueante al actualizar caché en Redis: {}", re.getMessage());
            }

            // 2. Lógica de Metas de Ahorro
            List<MetaAhorro> metasActivas = metaAhorroRepositorio
                    .findByUsuarioIdAndCompletadaAndActivaTrueOrderByFechaCreacionDesc(usuarioId, false);

            if (metasActivas.isEmpty()) {
                log.info("[MS-CLIENTE] No hay metas activas para el usuario {}. Evento de metas omitido.", usuarioId);
                return;
            }

            for (MetaAhorro meta : metasActivas) {
                BigDecimal montoActualizado;
                if (esIngreso) {
                    montoActualizado = meta.getMontoActual().add(monto);
                } else {
                    montoActualizado = meta.getMontoActual().subtract(monto);
                    if (montoActualizado.compareTo(BigDecimal.ZERO) < 0) {
                        montoActualizado = BigDecimal.ZERO;
                    }
                }

                // Tope visual al 100% - No mutamos el estado a completada para que el usuario elija concluirla
                if (montoActualizado.compareTo(meta.getMontoObjetivo()) > 0) {
                    montoActualizado = meta.getMontoObjetivo();
                }

                meta.setMontoActual(montoActualizado);
            }

            metaAhorroRepositorio.saveAll(metasActivas);
            log.info("[MS-CLIENTE] Se actualizaron {} metas activas con el monto de la transacción.", metasActivas.size());

        } catch (Exception e) {
            log.error("[MS-CLIENTE] Error procesando evento de transacción: {}", e.getMessage(), e);
            throw e; // Lanza excepción para que RabbitMQ pueda reintentar o enviar a DLQ si estuviera configurado
        }
    }
}
