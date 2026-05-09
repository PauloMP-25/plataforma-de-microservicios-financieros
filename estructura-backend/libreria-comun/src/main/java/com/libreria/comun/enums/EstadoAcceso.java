package com.libreria.comun.enums;

import lombok.Getter;

/**
 * Catálogo de estados posibles para un intento de autenticación o acceso.
 */
@Getter
public enum EstadoAcceso {
    /** El usuario ingresó correctamente. */
    EXITO,
    /** Las credenciales fueron incorrectas o el token inválido. */
    FALLO,
    /** El acceso fue rechazado por políticas de seguridad o bloqueo de IP. */
    BLOQUEADO,
    /** El usuario cerró su sesión voluntariamente. */
    LOGOUT
}
