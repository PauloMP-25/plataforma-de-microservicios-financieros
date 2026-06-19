import { Injectable, signal } from '@angular/core';

export interface NotificacionInfo {
  titulo: string;
  mensaje: string;
  tipo: 'gasto' | 'ingreso' | 'login' | 'meta' | 'presupuesto' | 'guardado';
  icono: string;
  duracion: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {
  readonly activa = signal<NotificacionInfo | null>(null);
  private timeoutId: any = null;

  mostrar(
    titulo: string,
    mensaje: string,
    tipo: NotificacionInfo['tipo'],
    icono: string,
    duracionMs: number = 3000
  ): void {
    // Cancelar el timeout anterior si existiera uno activo
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }

    this.activa.set({
      titulo,
      mensaje,
      tipo,
      icono,
      duracion: duracionMs
    });

    this.timeoutId = setTimeout(() => {
      this.cerrar();
    }, duracionMs);
  }

  cerrar(): void {
    this.activa.set(null);
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }

  // --- MÉTODOS MODULARES / WRAPPERS DE ACCESO RÁPIDO ---

  mostrarGastoRegistrado(monto: number, categoria: string): void {
    this.mostrar(
      'Gasto Registrado',
      `Se registró un egreso de S/ ${monto.toFixed(2)} en la categoría ${categoria}.`,
      'gasto',
      'arrow-down-long',
      3500
    );
  }

  mostrarIngresoRegistrado(monto: number, categoria: string): void {
    this.mostrar(
      'Ingreso Registrado',
      `Se registró un ingreso de S/ ${monto.toFixed(2)} en la categoría ${categoria}.`,
      'ingreso',
      'arrow-up-long',
      3500
    );
  }

  mostrarLoginExitoso(usuario: string): void {
    this.mostrar(
      '¡Bienvenido de nuevo!',
      `Hola ${usuario}, has iniciado sesión correctamente en Luka.`,
      'login',
      'user-check',
      3500
    );
  }

  mostrarMetaCreada(nombre: string): void {
    this.mostrar(
      'Meta de Ahorro Creada',
      `¡Genial! Has establecido una nueva meta de ahorro: "${nombre}".`,
      'meta',
      'bullseye',
      4000
    );
  }

  mostrarPresupuestoCreado(nombre: string): void {
    this.mostrar(
      'Presupuesto Configurado',
      `El límite de presupuesto para "${nombre}" ha sido actualizado.`,
      'presupuesto',
      'scale-balanced',
      4000
    );
  }

  mostrarDatosGuardados(detalle: string = 'Tus cambios han sido guardados con éxito.'): void {
    this.mostrar(
      'Datos Actualizados',
      detalle,
      'guardado',
      'floppy-disk',
      3000
    );
  }
}
