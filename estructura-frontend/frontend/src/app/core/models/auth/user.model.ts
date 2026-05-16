// =============================================
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
  tipoToken:     string;   
  expiraEn:      number;
  idUsuario:     string;   
  nombreUsuario: string;
  roles:         string[];
}

// ── Usuario en sesión ──
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

// ── Envoltura de Respuesta API ──
export interface ResultadoApi<T> {
  exito:   boolean;
  estado:  number;
  error?:  string;
  mensaje: string;
  datos:   T;
}

