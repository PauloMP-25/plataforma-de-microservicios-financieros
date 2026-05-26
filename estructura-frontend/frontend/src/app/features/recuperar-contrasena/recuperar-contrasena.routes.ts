import { Routes } from '@angular/router';

export const RECUPERAR_CONTRASENA_ROUTES: Routes = [
  {
    path: 'correo',
    loadComponent: () => import('./solicitar-correo/solicitar-correo').then(m => m.SolicitarCorreo)
  },
  {
    path: 'codigo',
    loadComponent: () => import('./verificar-codigo/verificar-codigo').then(m => m.VerificarCodigo)
  },
  {
    path: 'nueva',
    loadComponent: () => import('./nueva-contrasena/nueva-contrasena').then(m => m.NuevaContrasena)
  },
  {
    path: '',
    redirectTo: 'correo',
    pathMatch: 'full'
  }
];
