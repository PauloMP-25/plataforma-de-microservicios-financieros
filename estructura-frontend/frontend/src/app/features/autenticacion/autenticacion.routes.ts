import { Routes } from '@angular/router';

export const AUTENTICACION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./contenedor-autenticacion/contenedor-autenticacion').then(m => m.ContenedorAutenticacion)
  },
  {
    path: 'iniciar-sesion',
    loadComponent: () => import('./contenedor-autenticacion/contenedor-autenticacion').then(m => m.ContenedorAutenticacion),
    data: { vista: 'login' }
  },
  {
    path: 'crear-cuenta',
    loadComponent: () => import('./contenedor-autenticacion/contenedor-autenticacion').then(m => m.ContenedorAutenticacion),
    data: { vista: 'registro' }
  }
];
