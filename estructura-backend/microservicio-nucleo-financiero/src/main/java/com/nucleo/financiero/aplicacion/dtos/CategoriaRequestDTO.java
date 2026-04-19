package com.nucleo.financiero.aplicacion.dtos;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    String descripcion,

    @Size(max = 50, message = "El icono no puede superar 50 caracteres")
    String icono,

    @NotNull(message = "El tipo (INGRESO/GASTO) es obligatorio")
    TipoMovimiento tipo
) {}
