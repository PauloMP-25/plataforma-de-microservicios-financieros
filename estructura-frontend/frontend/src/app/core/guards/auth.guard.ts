import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard para proteger rutas que requieren autenticación.
 * Redirige al inicio de sesión si el usuario no ha autenticado su sesión.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.logueado()) {
    return true;
  }

  console.warn('[AuthGuard] Acceso denegado a ruta protegida:', state.url);
  router.navigate(['/autenticacion/iniciar-sesion']);
  return false;
};
