import { Routes } from "@angular/router";
import { IngresosPage } from "./pages/ingresos-page/ingresos-page";
import { NuevoIngresoPage } from "./pages/nuevo-ingreso-page/nuevo-ingreso-page";
export const INGRESOS_ROUTES: Routes = [

{
    path: '',
    component: IngresosPage,
    data: {
        title: 'Ingresos',
        breadcrumbs: [
            {label: 'Ingresos', route: '/ingresos'},
            {label: 'Sección'}
        ]
    }
},
{
    path: 'nuevo',
    component: NuevoIngresoPage,
    data: {
        title: 'Nuevo ingreso',
        breadcrumbs: [
            {label: 'Ingresos', route: '/ingresos'},
            {label: 'Nuevo ingreso'}
        ]
    }
}

]
