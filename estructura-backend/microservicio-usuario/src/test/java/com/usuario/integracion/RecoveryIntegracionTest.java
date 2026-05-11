package com.usuario.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usuario.aplicacion.dtos.*;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class RecoveryIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ClienteMensajeria clienteMensajeria;

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
        Usuario usuario = Usuario.builder()
                .nombreUsuario("luka_user")
                .correo("user@luka.com")
                .password("encoded_pass")
                .habilitado(true)
                .build();
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Solicitud de Recuperación - Privacidad: Correo inexistente devuelve 200")
    void testSolicitarRecuperacionPrivacidad() throws Exception {
        SolicitudRecuperacion solicitud = new SolicitudRecuperacion("fake@luka.com", null, TipoVerificacion.EMAIL);

        mockMvc.perform(post("/api/v1/auth/recuperar-solicitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("Si el correo ingresado existe")));

        // Verificar que no se llamó al cliente de mensajeria para generar OTP
        verify(clienteMensajeria, never()).generarCodigo(any());
        // Pero sí se auditó el intento fallido por privacidad/seguridad
        verify(publicadorAuditoria, times(1)).publicarAcceso(isNull(), any(), eq(com.libreria.comun.enums.EstadoEvento.FALLO), contains("USUARIO_NO_EXISTE"));
    }

    @Test
    @DisplayName("Solicitud de Recuperación - Caso Exitoso")
    void testSolicitarRecuperacionExito() throws Exception {
        SolicitudRecuperacion solicitud = new SolicitudRecuperacion("user@luka.com", null, TipoVerificacion.EMAIL);
        
        when(clienteMensajeria.generarCodigo(any())).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/auth/recuperar-solicitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isOk());

        verify(clienteMensajeria, times(1)).generarCodigo(any());
        verify(publicadorAuditoria, times(1)).publicarSolicitudOtp(any());
    }

    @Test
    @DisplayName("Confirmar Recuperación - Error: Token Inválido")
    void testConfirmarRecuperacionTokenInvalido() throws Exception {
        UUID registroId = UUID.randomUUID();
        String otp = "123456";
        SolicitudRestablecerPassword solicitud = new SolicitudRestablecerPassword("NewPass123!", "NewPass123!");

        when(clienteMensajeria.validarCodigoYObtenerUsuario(eq(registroId), eq(otp))).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/recuperar-confirmar")
                .param("registroId", registroId.toString())
                .param("codigoOtp", otp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("TOKEN_INVALIDO")));
    }

    @Test
    @DisplayName("Confirmar Recuperación - Éxito: Cambia clave y notifica")
    void testConfirmarRecuperacionExito() throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo("user@luka.com").orElseThrow();
        UUID registroId = UUID.randomUUID();
        String otp = "654321";
        SolicitudRestablecerPassword solicitud = new SolicitudRestablecerPassword("LukaNew2024!", "LukaNew2024!");

        when(clienteMensajeria.validarCodigoYObtenerUsuario(eq(registroId), eq(otp))).thenReturn(usuario.getId());

        mockMvc.perform(post("/api/v1/auth/recuperar-confirmar")
                .param("registroId", registroId.toString())
                .param("codigoOtp", otp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isOk());

        verify(publicadorAuditoria, times(1)).publicarNotificacionSeguridad(eq(usuario.getId()), any(), eq("RESETEO_PASSWORD"));
    }
}
