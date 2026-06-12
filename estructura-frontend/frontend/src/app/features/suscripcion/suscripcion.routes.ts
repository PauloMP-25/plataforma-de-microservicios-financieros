import { Routes } from '@angular/router';

export const SUSCRIPCION_ROUTES: Routes = [
  {
    path: 'exito',
    data: {
      title: '¡Éxito de Suscripción!',
      breadcrumbs: [
        { label: 'Suscripción', route: '/suscripcion/luka' },
        { label: 'Pago Exitoso' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripcion-exito/suscripcion-exito')
        .then(m => m.SuscripcionExito)
  },
  {
    path: 'luka',
    data: {
      title: 'Membresía',
      breadcrumbs: [
        { label: 'Inicio', route: '/dashboard' },
        { label: 'Membresía' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripcion-luka/suscripcion-luka')
        .then(m => m.SuscripcionLuka)
  },
  {
    path: 'pagos',
    data: {
      title: 'Suscripciones',
      breadcrumbs: [
        { label: 'Inicio', route: '/dashboard' },
        { label: 'Suscripciones' }
      ]
    },
    loadComponent: () =>
      import('./pages/suscripciones-pagos/suscripciones-pagos')
        .then(m => m.SuscripcionesPagos)
  }
];
