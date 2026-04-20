package com.usuario.infraestructura.configuracion;

import com.usuario.dominio.repositorios.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TareaLimpiezaUsuarios {

    private final UsuarioRepository repositorioUsuario;

    /**
     * Se ejecuta cada hora para limpiar la base de datos de "usuarios fantasma".
     * Un usuario se elimina si habilitado = false y han pasado más de 24 horas desde su creación.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional // Importante para operaciones de borrado
    public void eliminarUsuariosNoVerificados() {
        LocalDateTime limite = LocalDateTime.now().minusHours(24);
        
        log.debug("Iniciando purga de usuarios no habilitados creados antes de: {}", limite);

        try {
            // Usamos el método que definiremos en el repositorio
            int eliminados = repositorioUsuario.deleteByHabilitadoFalseAndFechaCreacionBefore(limite);
            
            if (eliminados > 0) {
                log.info("Limpieza exitosa: Se eliminaron {} registros de usuarios no verificados.", eliminados);
            }
        } catch (Exception e) {
            log.error("Error durante la ejecución de la tarea de limpieza: {}", e.getMessage());
        }
    }
}