package com.financiero.saas.gateway.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;

/**
 * Configuración de Spring Data Redis en entorno reactivo.
 * <p>
 * Define los templates necesarios para la gestión asíncrona de caché,
 * garantizando la serialización correcta de claves y valores.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Configuration
public class ConfiguracionRedis {

    /**
     * Configura el template reactivo de Redis para mapear IPs a un estado booleano
     * de bloqueo.
     * 
     * @param factory Factoría de conexiones reactiva inyectada por Spring Boot.
     * @return {@link ReactiveRedisTemplate} configurado con serializadores de
     *         String y Boolean.
     */
    @Bean
    public ReactiveRedisTemplate<String, Boolean> reactiveRedisTemplateBoolean(
            @NonNull ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Boolean> valueSerializer = new Jackson2JsonRedisSerializer<>(Boolean.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Boolean> builder = RedisSerializationContext
                .newSerializationContext(keySerializer);

        RedisSerializationContext<String, Boolean> context = builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Primary
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplateString(
            @NonNull ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .value(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
