import { Routes } from '@angular/router';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminDashboard,
    data: {
      title: 'Sistema Admin',
      breadcrumbs: [
        { label: 'Sistema Admin' }
      ]
    }
  }
];
