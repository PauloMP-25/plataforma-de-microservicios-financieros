// =============================================
// LUKA - Models alineados al backend real
// microservicio-usuarios  → puerto 8081
// microservicio-nucleo-financiero → puerto 8085
// =============================================

// ── Auth (microservicio-usuarios) ──
export interface SolicitudLogin {
  correo:   string;
  password: string;
}

export interface SolicitudRegistro {
  nombreUsuario:    string;
  correo:           string;
  password:         string;
  confirmarPassword: string;
}

export interface RespuestaAutenticacion {
  tokenAcceso:   string;
  tipoToken:     string;   // "Bearer"
  expiraEn:      number;
  idUsuario:     string;   // UUID como string
  nombreUsuario: string;
  roles:         string[];
}

// ── Usuario en sesión (construido desde RespuestaAutenticacion) ──
export interface UsuarioSesion {
  id:           string;
  nombreUsuario: string;
  roles:        string[];
  token:        string;
  expiraEn:     number;
}

// ── Recuperación de contraseña ──
export interface SolicitudRecuperacion {
  correo: string;
}

export interface NuevoPasswordDTO {
  codigoOtp:        string;
  nuevoPassword:    string;
  confirmarPassword: string;
}
