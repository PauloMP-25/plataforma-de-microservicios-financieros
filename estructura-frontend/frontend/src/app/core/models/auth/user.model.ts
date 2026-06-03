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
  refreshToken:  string;
  tipoToken:     string;   
  expiraEn:      number;
  refreshExpiraEn: number;
  idUsuario:     string;   
  nombreUsuario: string;
  roles:         string[];
}

export interface SolicitudRefreshToken {
  refreshToken: string;
}

export type TipoVerificacionOtp = 'EMAIL' | 'SMS' | 'WHATSAPP';

export interface SolicitudReenvioOtp {
  email: string;
  telefono?: string;
  tipo: TipoVerificacionOtp;
}

// ── Usuario en sesión ──
export interface UsuarioSesion {
  id:           string;
  nombreUsuario: string;
  roles:        string[];
  token:        string;
  refreshToken?: string;
  expiraEn:     number;
  refreshExpiraEn?: number;
}

// ── Recuperación de contraseña ──
export interface SolicitudRecuperacion {
  correo: string;
}

export interface SolicitudCambioPassword {
  passwordActual: string;
  nuevoPassword: string;
  confirmarPassword: string;
}

export interface ResultadoApi<T> {
  exito:   boolean;
  estado:  number;
  error?:  string;
  mensaje: string;
  datos:   T;
  detalles?: string[];
  pagina?: any;
}

