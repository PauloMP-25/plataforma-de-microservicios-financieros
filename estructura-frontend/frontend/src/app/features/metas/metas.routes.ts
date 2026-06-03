import { Routes } from "@angular/router";
import { MetasPage } from "./pages/metas-page/metas-page";
import { MetaFormPage } from "./pages/meta-form-page/meta-form-page";

export const METAS_ROUTES: Routes = [
  {
    path: '',
    component: MetasPage,
    data: {
        title: 'Metas',
        breadcrumbs: [
            {label: 'Metas', route: '/metas'},
            {label: 'Sección'}
        ]
    }
  },
  {
    path: 'nueva',
    component: MetaFormPage,
    data: {
        title: 'Nueva Meta',
        breadcrumbs: [
            {label: 'Metas', route: '/metas'},
            {label: 'Nueva Meta'}
        ]
    }
  },
  {
    path: 'editar/:id',
    component: MetaFormPage,
    data: {
        title: 'Editar Meta',
        breadcrumbs: [
            {label: 'Metas', route: '/metas'},
            {label: 'Editar Meta'}
        ]
    }
  }
];