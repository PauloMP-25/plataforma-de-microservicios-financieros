import { Routes } from "@angular/router";
import { AyudaPageComponent } from "./pages/ayuda-page/ayuda-page";

export const AYUDA_ROUTES: Routes = [
  {
    path: '',
    component: AyudaPageComponent,
    data: {
      title: 'Ayuda',
      breadcrumbs: [
        { label: 'Inicio', route: '/ayuda' },
        { label: 'Sección' }
      ]
    }
  }
];