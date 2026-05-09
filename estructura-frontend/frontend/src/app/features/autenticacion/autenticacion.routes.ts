import { Routes } from '@angular/router';

export const AUTENTICACION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./contenedor-autenticacion/contenedor-autenticacion').then(m => m.ContenedorAutenticacion)
  },
  {
    path: 'iniciar-sesion',
    redirectTo: ''
  },
  {
    path: 'crear-cuenta',
    redirectTo: ''
  }
];
