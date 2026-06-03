package com.mensajeria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio de Mensajería (Puerto 8084). Responsabilidades: - Generar y
 * persistir códigos OTP de 6 dígitos. - Enviar los códigos vía Email (JavaMail)
 * o SMS (Twilio). - Validar los códigos con protección de intentos por
 * usuarioId. - Activar la cuenta en el Microservicio-Usuario vía Feign Client.
 * - Reportar cada evento al Microservicio-Auditoría de forma asíncrona.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
@org.springframework.cache.annotation.EnableCaching
@ComponentScan(basePackages = {
        "com.mensajeria",
        "com.libreria.comun"
})
public class MicroservicioMensajeriaApplication {

    public static final String NOMBRE_SERVICIO = "MS-MENSAJERIA";

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioMensajeriaApplication.class, args);
    }
}
