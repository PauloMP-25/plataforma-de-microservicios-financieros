package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.RachaDTO;
import com.nucleo.financiero.aplicacion.puertos.IResumenService;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación del servicio de resúmenes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumenServiceImpl implements IResumenService {

    private final TransaccionRepository transaccionRepository;

    @Override
    public RachaDTO calcularRacha(UUID usuarioId) {
        log.info("Calculando racha de días registrando gastos para el usuario: {}", usuarioId);

        List<java.sql.Date> fechasSql = transaccionRepository.findDistinctFechasTransaccionByUsuarioIdAsc(usuarioId);
        List<LocalDate> fechas = fechasSql != null ? fechasSql.stream().map(java.sql.Date::toLocalDate).toList() : new ArrayList<>();

        if (fechas.isEmpty()) {
            return RachaDTO.builder()
                    .diasRacha(0)
                    .oportunidadesRestantes(3)
                    .diasActivosMesActual(new ArrayList<>())
                    .build();
        }

        int rachaActual = 0;
        int oportunidades = 3;
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate primeraFecha = fechas.get(0);

        List<String> diasActivosMesActual = new ArrayList<>();
        Set<LocalDate> fechasSet = new HashSet<>(fechas);

        LocalDate iterador = primeraFecha;
        Month mesActualIte = iterador.getMonth();

        while (!iterador.isAfter(hoy)) {
            // Cambio de mes: resetear oportunidades
            if (!iterador.getMonth().equals(mesActualIte)) {
                mesActualIte = iterador.getMonth();
                oportunidades = 3;
            }

            boolean tieneTransaccion = fechasSet.contains(iterador);

            if (tieneTransaccion) {
                rachaActual++;
                
                // Si es del mes y año actual de "hoy", lo agregamos a la lista
                if (iterador.getMonth().equals(hoy.getMonth()) && iterador.getYear() == hoy.getYear()) {
                    diasActivosMesActual.add(iterador.toString());
                }
            } else {
                oportunidades--;
                if (oportunidades < 0) {
                    rachaActual = 0;
                    oportunidades = 0;
                } else {
                    rachaActual++;
                }
            }

            iterador = iterador.plusDays(1);
        }

        return RachaDTO.builder()
                .diasRacha(rachaActual)
                .oportunidadesRestantes(oportunidades)
                .diasActivosMesActual(diasActivosMesActual)
                .build();
    }
}
