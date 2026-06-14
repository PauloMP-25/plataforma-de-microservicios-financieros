package com.nucleo.financiero.infraestructura.configuracion;

import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Cargador de datos iniciales para el entorno de ejecución.
 * <p>
 * Implementa {@link CommandLineRunner} para insertar un catálogo base de categorías
 * financieras (Ingresos/Gastos) si no existen previamente en la base de datos.
 * Esto asegura que el sistema tenga una base operativa mínima al iniciar.
 * </p>
 * 
 * @author Luka-Dev-Backend
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CargadorDatosIniciales implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    private record DefinicionCategoria(String nombre, String descripcion, String icono, TipoMovimiento tipo) {}

    private static final List<DefinicionCategoria> CATEGORIAS_DEFAULT = List.of(
        new DefinicionCategoria("Alimentación",            "Supermercado, restaurantes y comida",     "utensils",        TipoMovimiento.GASTO),
        new DefinicionCategoria("Transporte",              "Gasolina, taxi, bus, peajes",             "car",             TipoMovimiento.GASTO),
        new DefinicionCategoria("Vivienda",                "Alquiler, hipoteca, servicios del hogar", "home",            TipoMovimiento.GASTO),
        new DefinicionCategoria("Salud",                   "Médicos, farmacia, seguros de salud",     "heart-pulse",     TipoMovimiento.GASTO),
        new DefinicionCategoria("Educación",               "Cursos, libros, colegiaturas",            "graduation-cap",  TipoMovimiento.GASTO),
        new DefinicionCategoria("Entretenimiento",         "Cine, juegos, salidas",                   "gamepad-2",       TipoMovimiento.GASTO),
        new DefinicionCategoria("Suscripciones Streaming", "Netflix, Spotify, Disney+, etc.",         "play-circle",     TipoMovimiento.GASTO),
        new DefinicionCategoria("Suscripciones",           "Pagos de suscripciones y membresías",      "credit-card",     TipoMovimiento.GASTO),
        new DefinicionCategoria("Ropa y Calzado",          "Vestimenta y accesorios",                 "shirt",           TipoMovimiento.GASTO),
        new DefinicionCategoria("Tecnología",              "Dispositivos, software, gadgets",         "laptop",          TipoMovimiento.GASTO),
        new DefinicionCategoria("Viajes",                  "Vuelos, hoteles, vacaciones",             "plane",           TipoMovimiento.GASTO),
        new DefinicionCategoria("Otros Gastos",            "Gastos no categorizados",                 "circle-ellipsis", TipoMovimiento.GASTO),
        new DefinicionCategoria("Salario",                 "Ingreso mensual principal",               "briefcase",       TipoMovimiento.INGRESO),
        new DefinicionCategoria("Freelance",               "Proyectos y trabajos independientes",     "code",            TipoMovimiento.INGRESO),
        new DefinicionCategoria("Inversiones",             "Dividendos, intereses, cripto",           "trending-up",     TipoMovimiento.INGRESO),
        new DefinicionCategoria("Ventas",                  "Venta de bienes o servicios",             "tag",             TipoMovimiento.INGRESO),
        new DefinicionCategoria("Otros Ingresos",          "Ingresos no categorizados",               "plus-circle",     TipoMovimiento.INGRESO)
    );

    @Override
    public void run(String... args) {
        int creadas = 0;
        for (DefinicionCategoria datos : CATEGORIAS_DEFAULT) {
            if (!categoriaRepository.existsByNombreIgnoreCase(datos.nombre())) {
                categoriaRepository.save(Categoria.builder()
                        .nombre(datos.nombre())
                        .descripcion(datos.descripcion())
                        .icono(datos.icono())
                        .tipo(datos.tipo())
                        .build());
                creadas++;
            }
        }
        if (creadas > 0) log.info("---- {} CATEGORÍAS DEFAULT CREADAS ----", creadas);
        else log.info("---- CATEGORÍAS DEFAULT: ya existen, sin cambios ----");
    }
}
