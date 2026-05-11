package com.usuario.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class LoginIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private PublicadorAuditoria publicadorAuditoria;

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
        
        // Crear un usuario de prueba
        Usuario usuario = Usuario.builder()
                .nombreUsuario("paulo")
                .correo("paulo@luka.com")
                .password(passwordEncoder.encode("Luka2024!"))
                .habilitado(true)
                .build();
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Login Exitoso - Genera JWT y audita")
    void testLoginExitoso() throws Exception {
        SolicitudLogin solicitud = new SolicitudLogin("paulo@luka.com", "Luka2024!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito", is(true)))
                .andExpect(jsonPath("$.datos.token", notNullValue()));

        verify(publicadorAuditoria, times(1)).publicarAcceso(any(), any(), eq(com.libreria.comun.enums.EstadoEvento.EXITO), any());
    }

    @Test
    @DisplayName("Login Fallido - Usuario no encontrado (401)")
    void testLoginUsuarioNoEncontrado() throws Exception {
        SolicitudLogin solicitud = new SolicitudLogin("inexistente@luka.com", "Luka2024!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje", containsString("incorrectos")));
    }

    @Test
    @DisplayName("Login Fallido - Contraseña incorrecta (401)")
    void testLoginPasswordIncorrecto() throws Exception {
        SolicitudLogin solicitud = new SolicitudLogin("paulo@luka.com", "PasswordErroneo");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isUnauthorized());

        verify(publicadorAuditoria, times(1)).publicarAcceso(any(), any(), eq(com.libreria.comun.enums.EstadoEvento.FALLO), any());
    }

    @Test
    @DisplayName("Stress de Seguridad - 3 fallos consecutivos")
    void testStressSeguridad() throws Exception {
        SolicitudLogin solicitud = new SolicitudLogin("paulo@luka.com", "WrongPass");

        // Realizamos 3 intentos fallidos
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(solicitud)))
                    .andExpect(status().isUnauthorized());
        }

        // Verificamos que se publicaron 3 eventos de fallo con auditoría
        verify(publicadorAuditoria, times(3)).publicarAcceso(any(), any(), eq(com.libreria.comun.enums.EstadoEvento.FALLO), any());
    }
}
