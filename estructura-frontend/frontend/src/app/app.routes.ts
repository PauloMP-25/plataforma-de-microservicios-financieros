import { Routes } from '@angular/router';
import { Layout } from './layout/layout/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [


      // ── Dashboard ──
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
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

    ]
  },



];