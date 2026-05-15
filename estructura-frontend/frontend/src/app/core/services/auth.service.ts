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
  SolicitudRecuperacion,
  SolicitudCambioPassword,
  SolicitudResetPassword
} from '../models/auth/user.model';

const TOKEN_KEY   = 'luka_token';
const USUARIO_KEY = 'luka_usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = `${environment.gatewayUrl}/api/v1/auth`;

  // ── Estado reactivo ──
  private _usuario = signal<UsuarioSesion | null>(this.cargarDesdStorage());
  usuario   = this._usuario.asReadonly();
  logueado  = computed(() => !!this._usuario());
  esPremium = computed(() => this._usuario()?.roles?.includes('PREMIUM') ?? false);

  constructor(private http: HttpClient, private router: Router) {}

  // ── Login ──
  login(solicitud: SolicitudLogin): Observable<RespuestaAutenticacion> {
    return this.http.post<RespuestaAutenticacion>(`${this.base}/login`, solicitud)
      .pipe(tap(resp => this.guardarSesion(resp)));
  }

  // ── Registro ──
  registrar(solicitud: SolicitudRegistro): Observable<any> {
    return this.http.post(`${this.base}/registrar`, solicitud);
  }

  // ── Activar cuenta ──
  activarCuenta(usuarioId: string): Observable<any> {
    return this.http.put(`${this.base}/activar/${usuarioId}`, null);
  }

  // ── Recuperar password ──
  solicitarRecuperacion(solicitud: SolicitudRecuperacion): Observable<any> {
    return this.http.post(`${this.base}/recuperar-password`, solicitud);
  }

  // ── Reset password ──
  resetPassword(dto: SolicitudResetPassword): Observable<any> {
    const params = new URLSearchParams({
      registroId: dto.registroId,
      codigoOtp: dto.codigoOtp
    });

    return this.http.post(`${this.base}/reset-password?${params.toString()}`, dto.payload);
  }

  // ── Cambiar password (usuario autenticado) ──
  cambiarPassword(solicitud: SolicitudCambioPassword): Observable<any> {
    return this.http.put(`${this.base}/cambiar-password`, solicitud);
  }

  // ── Logout backend ──
  cerrarSesionBackend(): Observable<any> {
    return this.http.post(`${this.base}/logout`, {});
  }

  // ── Logout ──
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USUARIO_KEY);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  // ── Token para el interceptor ──
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  // ── Privados ──
  private guardarSesion(resp: RespuestaAutenticacion): void {
    const sesion: UsuarioSesion = {
      id:            resp.idUsuario,
      nombreUsuario: resp.nombreUsuario,
      roles:         resp.roles,
      token:         resp.tokenAcceso,
      expiraEn:      resp.expiraEn
    };
    localStorage.setItem(TOKEN_KEY,   resp.tokenAcceso);
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
