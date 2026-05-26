import { Routes } from "@angular/router";
import { DashboardPage } from "./pages/dashboard-page/dashboard-page";

export const DASHBOARD_ROUTES: Routes = [

{
    path: '',
    component: DashboardPage,
    data: {
        title: 'dashboard',
        breadcrumbs: [
            {label: 'Inicio', route: '/dashboard'},
            {label: 'Sección'}
        ]
    }
}

]