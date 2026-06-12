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
        data: {
          title: 'Mi perfil',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Mi perfil' }
          ]
        },
        loadComponent: () =>
          import('./perfil-cliente/perfil-cliente')
            .then(m => m.PerfilCliente)
      },
      {
        path: 'financiero',
        data: {
          title: 'Perfil financiero',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Perfil financiero' }
          ]
        },
        loadComponent: () =>
          import('./perfil-financiero/perfil-financiero')
            .then(m => m.PerfilFinanciero)
      },
      {
        path: 'configuracion',
        data: {
          title: 'Configuración',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Configuración' }
          ]
        },
        loadComponent: () =>
          import('./configuracion/configuracion')
            .then(m => m.Configuracion)
      },
      {
        path: 'configuracion/privacidad',
        data: {
          title: 'Política de privacidad',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Configuración', route: '/perfil/configuracion' },
            { label: 'Política de privacidad' }
          ]
        },
        loadComponent: () =>
          import('./configuracion/politica-privacidad')
            .then(m => m.PoliticaPrivacidad)
      },
      {
        path: 'configuracion/terminos',
        data: {
          title: 'Términos y condiciones',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Configuración', route: '/perfil/configuracion' },
            { label: 'Términos y condiciones' }
          ]
        },
        loadComponent: () =>
          import('./configuracion/terminos-condiciones')
            .then(m => m.TerminosCondiciones)
      },
      {
        path: 'historial',
        data: {
          title: 'Historial',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Historial' }
          ]
        },
        loadComponent: () =>
          import('./historial/historial')
            .then(m => m.Historial)
      },
      {
        path: 'transacciones',
        data: {
          title: 'Transacciones',
          breadcrumbs: [
            { label: 'Perfil', route: '/perfil' },
            { label: 'Transacciones' }
          ]
        },
        loadComponent: () =>
          import('./transacciones/transacciones')
            .then(m => m.Transacciones)
      },
    ]
  }
];
