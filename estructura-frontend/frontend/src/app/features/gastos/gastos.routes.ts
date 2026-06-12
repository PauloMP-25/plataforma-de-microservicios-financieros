import { Routes } from "@angular/router";
import { GastosPage } from "./pages/gastos-page/gastos-page";
import { HistorialGastosPage } from "./pages/historial-gastos-page/historial-gastos-page";

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
    component: HistorialGastosPage,
    data: {
        title: 'Historial de gastos',
        breadcrumbs: [
            {label: 'Gastos', routes:'/gastos'},
            {label: 'Historial'}

        ]
    }
}

]
