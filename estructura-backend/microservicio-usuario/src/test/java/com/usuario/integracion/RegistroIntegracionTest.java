package com.usuario.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.aplicacion.dtos.TipoVerificacion;
import com.usuario.aplicacion.dtos.PropositoCodigo;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Suite de Pruebas de Integración para el Caso 1: Registro de Usuario.
 * Valida la resiliencia, mensajería y flujos críticos de seguridad (Puntos 1.3 a 1.6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class RegistroIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ClienteMensajeria clienteMensajeria;

    @MockBean
    private ClientePerfilExterno clientePerfilExterno;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("1.3: Rechazar registro si las contraseñas no coinciden")
    void testRegistroPasswordMismatch() throws Exception {
        SolicitudRegistro solicitud = new SolicitudRegistro(
                "testuser", "test@luka.com", "Luka123!", "Luka321!");

        mockMvc.perform(post("/api/v1/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito", is(false)))
                .andExpect(jsonPath("$.error", is("ERROR_VALIDACION")));
    }

    @Test
    @DisplayName("1.4: Escenario Email - Registro exitoso y perfil inicial")
    void testRegistroExitosoEmail() throws Exception {
        SolicitudRegistro solicitud = new SolicitudRegistro(
                "gabriel", "gabriel@luka.com", "Luka2024!", "Luka2024!");

        mockMvc.perform(post("/api/v1/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exito", is(true)));

        verify(clientePerfilExterno, times(1)).crearPerfilInicial(any());
    }

    @Test
    @DisplayName("1.4: Escenario SMS - Exigir teléfono antes de enviar OTP")
    void testRegistroSmsExigeTelefono() throws Exception {
        // 1. Registro inicial
        SolicitudRegistro registro = new SolicitudRegistro("cristina", "cristina@luka.com", "Luka123!", "Luka123!");
        String response = mockMvc.perform(post("/api/v1/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registro)))
                .andReturn().getResponse().getContentAsString();
        
        UUID usuarioId = UUID.fromString(objectMapper.readTree(response).get("datos").asText());

        // 2. Solicitar OTP SMS sin teléfono
        SolicitudGenerarOtp solicitudOtp = new SolicitudGenerarOtp(
            usuarioId, "cristina@luka.com", "", TipoVerificacion.SMS, PropositoCodigo.ACTIVACION_CUENTA
        );

        mockMvc.perform(post("/api/v1/auth/solicitar-otp/" + usuarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudOtp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("El teléfono es obligatorio")));
    }

    @Test
    @DisplayName("Resiliencia: Simular caída de ms-mensajeria y validar Fallback")
    void testResilienciaMensajeriaCaida() throws Exception {
        // Simulamos caída del ms-mensajeria
        when(clienteMensajeria.generarCodigo(any())).thenThrow(new RuntimeException("Service Down"));

        SolicitudRegistro solicitud = new SolicitudRegistro(
                "paul", "paul@luka.com", "Luka2024!", "Luka2024!");

        mockMvc.perform(post("/api/v1/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("1.5: Throttling - Propagación de error 429 desde ms-mensajeria")
    void testThrottlingBloqueo() throws Exception {
        UUID registroId = UUID.randomUUID();
        
        feign.FeignException feignEx = feign.FeignException.errorStatus(
            "validarCodigoYObtenerUsuario", 
            feign.Response.builder()
                .status(429)
                .reason("Too Many Requests")
                .request(feign.Request.create(feign.Request.HttpMethod.GET, "/url", java.util.Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                .build());
                
        when(clienteMensajeria.validarCodigoYObtenerUsuario(any(), any())).thenThrow(feignEx);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .param("registroId", registroId.toString())
                .param("codigoOtp", "123456")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevoPassword\":\"Luka2024!\", \"confirmarPassword\":\"Luka2024!\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("1.6: Efecto Dominó - Sincronización con ms-cliente tras activación")
    void testEfectoDominoActivacion() throws Exception {
        com.usuario.dominio.entidades.Usuario usuario = com.usuario.dominio.entidades.Usuario.builder()
                .nombreUsuario("paulo")
                .correo("paulo@luka.com")
                .password("encoded_pass")
                .habilitado(false)
                .build();
        usuario = usuarioRepository.save(usuario);

        mockMvc.perform(put("/api/v1/auth/activar/" + usuario.getId())
                .param("telefono", "+51999888777"))
                .andExpect(status().isOk());

        verify(clientePerfilExterno, times(1)).actualizarTelefono(eq(usuario.getId()), eq("+51999888777"));
        
        org.junit.jupiter.api.Assertions.assertTrue(usuarioRepository.findById(usuario.getId()).get().isHabilitado());
    }
}
