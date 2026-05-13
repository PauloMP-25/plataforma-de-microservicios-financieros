package com.mensajeria.aplicacion.servicios;

import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import java.util.Map;

/**
 * Contrato para el envío de correos electrónicos.
 */
public interface IEmailService {
    void enviarEmail(String email, PropositoCodigo proposito, String codigo, Map<String, Object> variables);
    void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml);
}
