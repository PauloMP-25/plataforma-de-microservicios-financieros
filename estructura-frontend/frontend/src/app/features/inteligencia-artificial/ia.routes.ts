import { Routes } from '@angular/router';

export const IA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/ia-hub/ia-hub').then(m => m.IaHubComponent)
  }
];
