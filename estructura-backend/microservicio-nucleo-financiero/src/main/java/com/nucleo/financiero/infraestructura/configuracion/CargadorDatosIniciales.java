package com.nucleo.financiero.infraestructura.configuracion;

import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CargadorDatosIniciales implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    private static final List<Object[]> CATEGORIAS_DEFAULT = List.of(
        new Object[]{"Alimentación",            "Supermercado, restaurantes y comida",     "utensils",        TipoMovimiento.GASTO},
        new Object[]{"Transporte",              "Gasolina, taxi, bus, peajes",             "car",             TipoMovimiento.GASTO},
        new Object[]{"Vivienda",                "Alquiler, hipoteca, servicios del hogar", "home",            TipoMovimiento.GASTO},
        new Object[]{"Salud",                   "Médicos, farmacia, seguros de salud",     "heart-pulse",     TipoMovimiento.GASTO},
        new Object[]{"Educación",               "Cursos, libros, colegiaturas",            "graduation-cap",  TipoMovimiento.GASTO},
        new Object[]{"Entretenimiento",         "Cine, juegos, salidas",                   "gamepad-2",       TipoMovimiento.GASTO},
        new Object[]{"Suscripciones Streaming", "Netflix, Spotify, Disney+, etc.",         "play-circle",     TipoMovimiento.GASTO},
        new Object[]{"Ropa y Calzado",          "Vestimenta y accesorios",                 "shirt",           TipoMovimiento.GASTO},
        new Object[]{"Tecnología",              "Dispositivos, software, gadgets",         "laptop",          TipoMovimiento.GASTO},
        new Object[]{"Viajes",                  "Vuelos, hoteles, vacaciones",             "plane",           TipoMovimiento.GASTO},
        new Object[]{"Otros Gastos",            "Gastos no categorizados",                 "circle-ellipsis", TipoMovimiento.GASTO},
        new Object[]{"Salario",                 "Ingreso mensual principal",               "briefcase",       TipoMovimiento.INGRESO},
        new Object[]{"Freelance",               "Proyectos y trabajos independientes",     "code",            TipoMovimiento.INGRESO},
        new Object[]{"Inversiones",             "Dividendos, intereses, cripto",           "trending-up",     TipoMovimiento.INGRESO},
        new Object[]{"Ventas",                  "Venta de bienes o servicios",             "tag",             TipoMovimiento.INGRESO},
        new Object[]{"Otros Ingresos",          "Ingresos no categorizados",               "plus-circle",     TipoMovimiento.INGRESO}
    );

    @Override
    public void run(String... args) {
        int creadas = 0;
        for (Object[] datos : CATEGORIAS_DEFAULT) {
            String nombre = (String) datos[0];
            if (!categoriaRepository.existsByNombreIgnoreCase(nombre)) {
                categoriaRepository.save(Categoria.builder()
                        .nombre(nombre)
                        .descripcion((String) datos[1])
                        .icono((String) datos[2])
                        .tipo((TipoMovimiento) datos[3])
                        .build());
                creadas++;
            }
        }
        if (creadas > 0) log.info("---- {} CATEGORÍAS DEFAULT CREADAS ----", creadas);
        else log.info("---- CATEGORÍAS DEFAULT: ya existen, sin cambios ----");
    }
}
