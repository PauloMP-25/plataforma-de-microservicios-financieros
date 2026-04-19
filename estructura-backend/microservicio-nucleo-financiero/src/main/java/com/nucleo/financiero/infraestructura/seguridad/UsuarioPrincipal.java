package com.nucleo.financiero.infraestructura.seguridad;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;
import java.util.stream.Collectors;

public class UsuarioPrincipal implements UserDetails {

    private final String nombreUsuario;
    private final List<GrantedAuthority> autoridades;

    public UsuarioPrincipal(String nombreUsuario, List<String> roles) {
        this.nombreUsuario = nombreUsuario;
        this.autoridades = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return autoridades; }
    @Override public String  getPassword()              { return null; }
    @Override public String  getUsername()              { return nombreUsuario; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }
}
