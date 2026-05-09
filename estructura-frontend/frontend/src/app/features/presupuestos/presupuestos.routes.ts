import { Routes } from "@angular/router";
import { PresupuestosPage } from "./pages/presupuestos-page/presupuestos-page";

export const PRESUPUESTO_ROUTES : Routes = [

{
    path: '',
    component: PresupuestosPage,
    data: {
        title: 'Presupuesto',
        breadcrumbs: [
            {label: 'Presupuesto', route: '/presupuesto'},
            {label: 'Sección'}
        ]
    }
}

]