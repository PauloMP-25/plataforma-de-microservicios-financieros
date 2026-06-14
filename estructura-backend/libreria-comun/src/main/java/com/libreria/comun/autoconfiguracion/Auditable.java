package com.libreria.comun.autoconfiguracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar entidades JPA que deben ser auditadas automáticamente
 * en sus campos de fecha de creación y fecha de actualización.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
}
