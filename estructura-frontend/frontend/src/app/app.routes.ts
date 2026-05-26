import { Routes } from '@angular/router';
import { Layout } from './layout/layout/layout/layout';

export const routes: Routes = [
  // ── Rutas Públicas ──
  {
    path: 'inicio',
    loadComponent: () => import('./features/inicio/inicio').then(m => m.Inicio)
  },
  {
    path: 'autenticacion',
    loadChildren: () => import('./features/autenticacion/autenticacion.routes').then(m => m.AUTENTICACION_ROUTES)
  },
  {
    path: 'recuperar-contrasena',
    loadChildren: () => import('./features/recuperar-contrasena/recuperar-contrasena.routes').then(m => m.RECUPERAR_CONTRASENA_ROUTES)
  },

  // ── Rutas Privadas (Dashboard) ──
  {
    path: '',
    component: Layout,
    children: [      // ── Dashboard ──
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes')
            .then(m => m.DASHBOARD_ROUTES)
      },

      // ── Gastos ──
      {
        path: 'gastos',
        loadChildren: () =>
          import('./features/gastos/gastos.routes')
            .then(m => m.GASTOS_ROUTES)
      },

      // ── Ingresos ──
      {
        path: 'ingresos',
        loadChildren: () =>
          import('./features/ingresos/ingresos.routes')
            .then(m => m.INGRESOS_ROUTES)
      },

      // ── Metas ──
      {
        path: 'metas',
        loadChildren: () =>
          import('./features/metas/metas.routes')
            .then(m => m.METAS_ROUTES)
      },

      // ── Presupuestos ──
      {
        path: 'presupuestos',
        loadChildren: () =>
          import('./features/presupuestos/presupuestos.routes')
            .then(m => m.PRESUPUESTO_ROUTES)
      },

      // ── perfil ──
      {
        path: 'perfil',
        loadChildren: () =>
          import('./features/perfil/perfil.routes')
            .then(m => m.PERFIL_ROUTES)
      },
      
      // -- Ayuda --
      {
        path: 'ayuda',
        loadChildren: () =>
          import('./features/ayuda/ayuda.routes')
            .then(m => m.AYUDA_ROUTES)
      },

      {
        path: '',
        redirectTo: '/inicio',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/inicio'
  }
];