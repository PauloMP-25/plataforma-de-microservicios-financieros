import { Routes } from "@angular/router";
import { GastosPage } from "./pages/gastos-page/gastos-page";
import { HistorialGastosPage } from "./pages/historial-gastos-page/historial-gastos-page";
import { NuevoGastoPage } from "./pages/nuevo-gasto-page/nuevo-gasto-page";
import { pendingChangesGuard } from "../../core/guards/pending-changes.guard";

export const GASTOS_ROUTES:Routes = [
{
    path: '',
    component: GastosPage,
    data: {
        title: 'Gastos',
        breadcrumbs: [
            {label: 'Gastos', routes:'/gastos'},
            {label: 'Sección'}

        ]
    }
},
{
    path: 'historial',
    redirectTo: '/perfil/historial',
    pathMatch: 'full'
},
{
    path: 'nuevo',
    component: NuevoGastoPage,
    canDeactivate: [pendingChangesGuard],
    data: {
        title: 'Nuevo gasto',
        breadcrumbs: [
            {label: 'Gastos', routes:'/gastos'},
            {label: 'Nuevo gasto'}
        ]
    }
}

]
