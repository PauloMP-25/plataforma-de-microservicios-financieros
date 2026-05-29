package com.libreria.comun.autoconfiguracion;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Listener JPA que intercepta las operaciones de persistencia y actualización de las entidades.
 * Si la entidad está anotada con {@link Auditable}, establece automáticamente
 * los valores de 'fechaCreacion' y 'fechaActualizacion' usando reflexión.
 */
public class AuditoriaListener {

    @PrePersist
    public void prePersist(Object entidad) {
        if (entidad.getClass().isAnnotationPresent(Auditable.class)) {
            LocalDateTime ahora = LocalDateTime.now();
            establecerCampo(entidad, "fechaCreacion", ahora);
            establecerCampo(entidad, "fechaActualizacion", ahora);
        }
    }

    @PreUpdate
    public void preUpdate(Object entidad) {
        if (entidad.getClass().isAnnotationPresent(Auditable.class)) {
            establecerCampo(entidad, "fechaActualizacion", LocalDateTime.now());
        }
    }

    private void establecerCampo(Object target, String nombreCampo, Object valor) {
        try {
            Field field = obtenerCampoRecursivo(target.getClass(), nombreCampo);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, valor);
            }
        } catch (Exception e) {
            // Ignorar errores silenciosamente si el campo no es accesible o no existe
        }
    }

    private Field obtenerCampoRecursivo(Class<?> clazz, String nombreCampo) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(nombreCampo);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
