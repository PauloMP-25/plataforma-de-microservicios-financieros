package com.libreria.comun.dtos;

import java.math.BigDecimal;

/**
 * DTO optimizado (Principio de Menor Privilegio) diseñado específicamente para el
 * microservicio-ia.
 * <p>
 * Proporciona el contexto financiero y personal estrictamente necesario para que
 * la Inteligencia Artificial genere recomendaciones personalizadas, sin exponer
 * datos sensibles completos como identificadores internos u otra información privada.
 * </p>
 * 
 * @param nombres Nombre del cliente para personalizar la conversación.
 * @param ocupacion Ocupación del cliente para inferir estabilidad laboral y sector.
 * @param ingresoMensual Nivel de ingresos para ajustar las recomendaciones.
 * @param tonoIA Tono conversacional configurado por el usuario (ej: formal, amigable, motivador).
 * @param porcentajeMetaPrincipal Porcentaje de progreso de la meta de ahorro más cercana a cumplirse.
 * @param nombreMetaPrincipal Nombre de dicha meta de ahorro (ej: "Viaje a París").
 * @param porcentajeAlertaGasto Umbral porcentual actual del límite de gasto para emitir advertencias.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
public record ContextoEstrategicoIADTO(
    String nombres,
    String ocupacion,
    BigDecimal ingresoMensual,
    String tonoIA,
    BigDecimal porcentajeMetaPrincipal,
    String nombreMetaPrincipal,
    Integer porcentajeAlertaGasto
) {}
