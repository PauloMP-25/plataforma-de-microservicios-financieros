import { Routes } from "@angular/router";
import { GastosPage } from "./pages/gastos-page/gastos-page";

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
}
    
]