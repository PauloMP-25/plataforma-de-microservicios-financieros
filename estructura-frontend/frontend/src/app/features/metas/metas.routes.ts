import { Routes } from "@angular/router";
import { MetasPage } from "./pages/metas-page/metas-page";

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
}

]