import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

/**
 * Guard para proteger rutas que requieren autenticación.
 * Redirige al inicio si el usuario no ha autenticado su sesión.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.esperarInicializacion().pipe(
    map((logueado) => {
      if (logueado) {
        return true;
      }
      console.warn('[AuthGuard] Acceso denegado a ruta protegida:', state.url);
      router.navigate(['/']);
      return false;
    })
  );
};
