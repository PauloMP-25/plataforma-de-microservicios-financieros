class RoutingKeys:
    """
    Constantes de Routing Keys para RabbitMQ.
    Espejo de RoutingKeys.java en la librería común de Java.
    """
    
    # Auditoría (Patrón: auditoria.evento.#)
    AUDITORIA_ACCESO = "auditoria.acceso.ia"
    AUDITORIA_EVENTO = "auditoria.evento.ia"
    AUDITORIA_TRANSACCION = "auditoria.transaccion.ia"
    
    # IA - Procesamiento de análisis
    IA_ANALISIS_SOLICITAR = "ia.analisis.solicitar"
    IA_ANALISIS_RESULTADO = "ia.analisis.resultado"
    
    # Sincronización Cliente → IA
    CLIENTE_PERFIL_ACTUALIZADO = "cliente.perfil.actualizado"
    
    # Mensajería
    MENSAJERIA_OTP_GENERAR = "mensaje.otp.generar"
