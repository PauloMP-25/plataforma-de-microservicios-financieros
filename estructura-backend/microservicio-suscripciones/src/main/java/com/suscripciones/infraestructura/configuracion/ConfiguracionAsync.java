package com.suscripciones.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración del pool de hilos para ejecución asíncrona de tareas de fondo en Spring.
 * Configura un TaskExecutor explícito con límites controlados para evitar la creación ilimitada de hilos.
 */
@Configuration
@EnableAsync
public class ConfiguracionAsync {

    /**
     * Define el pool de ejecución para tareas asíncronas (@Async).
     * Configuración del pool: core=2, max=10, queue=50.
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("LukaSuscripcionesAsync-");
        executor.initialize();
        return executor;
    }
}
