import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

/**
 * Guard para proteger rutas restringidas por roles específicos.
 * Redirige al dashboard si el usuario no tiene los roles requeridos.
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.esperarInicializacion().pipe(
      map((logueado) => {
        if (!logueado) {
          router.navigate(['/']);
          return false;
        }

        const usuario = authService.usuario();
        const hasRole = usuario?.roles?.some(r => allowedRoles.includes(r));
        if (hasRole) {
          return true;
        }

        console.warn('[RoleGuard] Rol no autorizado para la ruta:', state.url);
        router.navigate(['/dashboard']);
        return false;
      })
    );
  };
};
