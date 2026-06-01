import { Routes } from '@angular/router';

export const IA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/ia-hub/ia-hub').then(m => m.IaHubComponent),
    data: {
      title: 'Coach Financiero',
      breadcrumbs: [
        { label: 'COACH FINANCIERO', route: '/inteligencia-artificial' }
      ]
    }
  }
];
