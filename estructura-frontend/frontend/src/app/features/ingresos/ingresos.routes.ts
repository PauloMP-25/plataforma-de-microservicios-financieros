import { Routes } from "@angular/router";
import { IngresosPage } from "./pages/ingresos-page/ingresos-page";
export const INGRESOS_ROUTES: Routes = [

{
    path: '',
    component: IngresosPage,
    data: {
        title: 'Ingresos',
        breadcrumbs: [
            {label: 'Ingresos', route: '/ingreso'},
            {label: 'Sección'}
        ]
    }
}

]