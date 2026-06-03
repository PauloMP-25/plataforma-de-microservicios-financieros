// =============================================
// LUKA - Auth Service
// Conectado a: microservicio-usuarios (puerto 8081)
// Endpoints reales:
//   POST /api/v1/auth/login
//   POST /api/v1/auth/registrar
//   PUT  /api/v1/auth/activar/{usuarioId}
//   POST /api/v1/auth/recuperar-password
//   POST /api/v1/auth/reset-password
// =============================================

import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  SolicitudLogin, SolicitudRegistro,
  RespuestaAutenticacion, UsuarioSesion,
  SolicitudRecuperacion, SolicitudCambioPassword,
  ResultadoApi, SolicitudReenvioOtp
} from '../models/auth/user.model';

const TOKEN_KEY = 'luka_token';
const REFRESH_TOKEN_KEY = 'luka_refresh_token';
const USUARIO_KEY = 'luka_usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = `${environment.gatewayUrl}/api/v1/auth`;

  // ── Estado reactivo ──
  private _usuario = signal<UsuarioSesion | null>(this.cargarDesdStorage());
  usuario = this._usuario.asReadonly();
  logueado = computed(() => !!this._usuario());
  esPremium = computed(() => this._usuario()?.roles?.includes('PREMIUM') ?? false);

  constructor(private http: HttpClient, private router: Router) { }

  // ── Login ──
  login(solicitud: SolicitudLogin): Observable<ResultadoApi<RespuestaAutenticacion>> {
    return this.http.post<ResultadoApi<RespuestaAutenticacion>>(`${this.base}/login`, solicitud)
      .pipe(tap(resp => {
        if (resp.exito) {
          this.guardarSesion(resp.datos);
        }
      }));
  }

  // ── Registro ──
  registrar(solicitud: SolicitudRegistro): Observable<ResultadoApi<string>> {
    return this.http.post<ResultadoApi<string>>(`${this.base}/registrar`, solicitud);
  }

  // ── Activar cuenta ──
  activarCuenta(correo: string, codigoOtp: string, telefono?: string): Observable<ResultadoApi<string>> {
    const params: any = { correo, codigoOtp };
    if (telefono) params.telefono = telefono;
    return this.http.put<ResultadoApi<string>>(`${this.base}/activar`, null, { params });
  }

  // ── Solicitar OTP de activación ──
  solicitarOtpActivacion(solicitud: SolicitudReenvioOtp): Observable<ResultadoApi<string>> {
    return this.http.post<ResultadoApi<string>>(`${this.base}/solicitar-otp`, solicitud);
  }

  // ── Recuperar password ──
  solicitarRecuperacion(solicitud: any): Observable<ResultadoApi<string>> {
    return this.http.post<ResultadoApi<string>>(`${this.base}/recuperar-solicitar`, solicitud);
  }

  // ── Reset password ──
  resetPassword(registroId: string, codigoOtp: string, dto: any): Observable<ResultadoApi<string>> {
    const params = { registroId, codigoOtp };
    return this.http.post<ResultadoApi<string>>(`${this.base}/recuperar-confirmar`, dto, { params });
  }

  // ── Cambiar password (usuario autenticado) ──
  cambiarPassword(solicitud: SolicitudCambioPassword): Observable<any> {
    return this.http.put(`${this.base}/cambiar-password`, solicitud);
  }

  // ── Logout backend ──
  cerrarSesionBackend(): Observable<any> {
    return this.http.post(`${this.base}/logout`, {})
  };

  // ── Refrescar token ──
  refrescarToken(): Observable<ResultadoApi<RespuestaAutenticacion>> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<ResultadoApi<RespuestaAutenticacion>>(`${this.base}/refrescar-token`, { refreshToken })
      .pipe(tap(resp => {
        if (resp.exito) {
          this.guardarSesion(resp.datos);
        }
      }));
  }

  // ── Logout ──
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USUARIO_KEY);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  // ── Token para el interceptor ──
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  // ── Privados ──
  private guardarSesion(resp: RespuestaAutenticacion): void {
    const sesion: UsuarioSesion = {
      id: resp.idUsuario,
      nombreUsuario: resp.nombreUsuario,
      roles: resp.roles,
      token: resp.tokenAcceso,
      refreshToken: resp.refreshToken,
      expiraEn: resp.expiraEn,
      refreshExpiraEn: resp.refreshExpiraEn
    };
    localStorage.setItem(TOKEN_KEY, resp.tokenAcceso);
    localStorage.setItem(REFRESH_TOKEN_KEY, resp.refreshToken);
    localStorage.setItem(USUARIO_KEY, JSON.stringify(sesion));
    this._usuario.set(sesion);
  }

  private cargarDesdStorage(): UsuarioSesion | null {
    try {
      const raw = localStorage.getItem(USUARIO_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
