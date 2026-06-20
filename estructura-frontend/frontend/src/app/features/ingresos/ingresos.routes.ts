import { Routes } from "@angular/router";
import { IngresosPage } from "./pages/ingresos-page/ingresos-page";
import { NuevoIngresoPage } from "./pages/nuevo-ingreso-page/nuevo-ingreso-page";
import { HistorialIngresosPage } from "./pages/historial-ingresos-page/historial-ingresos-page";
import { pendingChangesGuard } from "../../core/guards/pending-changes.guard";

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
    path: 'historial',
    component: HistorialIngresosPage,
    data: {
        title: 'Historial de ingresos',
        breadcrumbs: [
            {label: 'Ingresos', route: '/ingresos'},
            {label: 'Historial'}
        ]
    }
},
{
    path: 'nuevo',
    component: NuevoIngresoPage,
    canDeactivate: [pendingChangesGuard],
    data: {
        title: 'Nuevo ingreso',
        breadcrumbs: [
            {label: 'Ingresos', route: '/ingresos'},
            {label: 'Nuevo ingreso'}
        ]
    }
}

];
