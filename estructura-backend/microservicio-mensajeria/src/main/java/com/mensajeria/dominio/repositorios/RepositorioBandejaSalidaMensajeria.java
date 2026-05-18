package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.BandejaSalidaMensajeria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositorioBandejaSalidaMensajeria extends JpaRepository<BandejaSalidaMensajeria, UUID> {
    
    /**
     * Encuentra todos los eventos de la bandeja de salida que no han sido procesados
     * y cuyo número de intentos es menor al máximo permitido.
     * 
     * @param maxIntentos el número máximo de intentos permitidos
     * @return lista de eventos pendientes de enviar
     */
    List<BandejaSalidaMensajeria> findByProcesadoFalseAndIntentosLessThan(int maxIntentos);
}
