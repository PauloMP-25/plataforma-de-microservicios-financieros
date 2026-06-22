import { Routes } from '@angular/router';

export const SUSCRIPCION_ROUTES: Routes = [
  {
    path: 'luka',
    data: {
      title: 'Mi Suscripción',
      breadcrumbs: [
        { label: 'Inicio', route: '/dashboard' },
        { label: 'Mi Suscripción' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripcion-luka/suscripcion-luka')
        .then(m => m.SuscripcionLuka)
  },
  {
    path: 'pagos',
    data: {
      title: 'Todas las Suscripciones',
      breadcrumbs: [
        { label: 'Inicio', route: '/dashboard' },
        { label: 'Suscripciones' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripciones-pagos/suscripciones-pagos')
        .then(m => m.SuscripcionesPagos)
  },
  {
    path: 'exito',
    data: {
      title: '¡Suscripción Creada!',
      breadcrumbs: [
        { label: 'Mi Suscripción', route: '/suscripcion/luka' },
        { label: 'Confirmación' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripcion-exito/suscripcion-exito')
        .then(m => m.SuscripcionExito)
  }
];
