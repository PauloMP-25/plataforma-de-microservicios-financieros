import { Routes } from "@angular/router";
import { MetasPage } from "./pages/metas-page/metas-page";
import { MetaFormPage } from "./pages/meta-form-page/meta-form-page";
import { pendingChangesGuard } from "../../core/guards/pending-changes.guard";

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
    canDeactivate: [pendingChangesGuard],
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
    canDeactivate: [pendingChangesGuard],
    data: {
        title: 'Editar Meta',
        breadcrumbs: [
            {label: 'Metas', route: '/metas'},
            {label: 'Editar Meta'}
        ]
    }
  }
];