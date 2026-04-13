package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.TokenConfirmacionEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenConfirmacionEmailRepository extends JpaRepository<TokenConfirmacionEmail, Long> {

    Optional<TokenConfirmacionEmail> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenConfirmacionEmail t WHERE t.expiraEn < :ahora AND t.confirmadoEn IS NULL")
    int eliminarTokensExpirados(LocalDateTime ahora);
    
    Optional<TokenConfirmacionEmail> findByTokenAndConfirmadoEnIsNull(String token);
    
    @Query("SELECT t FROM TokenConfirmacionEmail t " +
       "WHERE t.token = :token AND t.confirmadoEn IS NULL AND t.expiraEn > :ahora")
    Optional<TokenConfirmacionEmail> buscarTokenValido(String token, LocalDateTime ahora);
}
