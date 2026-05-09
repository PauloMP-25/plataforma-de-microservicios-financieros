import { Routes } from '@angular/router';

export const AUTENTICACION_ROUTES: Routes = [
  {
    path: 'iniciar-sesion',
    loadComponent: () => import('./iniciar-sesion/iniciar-sesion').then(m => m.IniciarSesion)
  },
  {
    path: 'crear-cuenta',
    loadComponent: () => import('./crear-cuenta/crear-cuenta').then(m => m.CrearCuenta)
  },
  {
    path: '',
    redirectTo: 'iniciar-sesion',
    pathMatch: 'full'
  }
];
