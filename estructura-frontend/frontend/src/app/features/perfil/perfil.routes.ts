import { Routes } from '@angular/router';
import { PerfilLayout } from './perfil-layout/perfil-layout';

export const PERFIL_ROUTES: Routes = [
  {
    path: '',
    component: PerfilLayout,
    children: [
      { path: '', redirectTo: 'cliente', pathMatch: 'full' },
      {
        path: 'cliente',
        loadComponent: () =>
          import('./perfil-cliente/perfil-cliente')
            .then(m => m.PerfilCliente)
      },
      {
        path: 'financiero',
        loadComponent: () =>
          import('./perfil-financiero/perfil-financiero')
            .then(m => m.PerfilFinanciero)
      },
      {
        path: 'configuracion',
        loadComponent: () =>
          import('./configuracion/configuracion')
            .then(m => m.Configuracion)
      },
      {
        path: 'historial',
        loadComponent: () =>
          import('./historial/historial')
            .then(m => m.Historial)
      },
      {
        path: 'transacciones',
        loadComponent: () =>
          import('./transacciones/transacciones')
            .then(m => m.Transacciones)
      },
    ]
  }
];