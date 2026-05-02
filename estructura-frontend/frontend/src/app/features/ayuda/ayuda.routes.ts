import { Routes } from "@angular/router";
import { AyudaPage } from "./pages/ayuda-page/ayuda-page";

export const AYUDA_ROUTES: Routes = [

{
    path: '',
    component: AyudaPage,
    data: {
        title: 'Ayuda',
        breadcrumbs: [
            {label: 'Inicio', route: '/ayuda'},
            {label: 'Sección'}
        ]
    }
}

]