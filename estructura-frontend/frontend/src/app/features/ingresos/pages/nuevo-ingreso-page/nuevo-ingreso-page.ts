import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IngresoFormComponent } from '../../components/ingreso-form/ingreso-form';
import { IngresoPreviewComponent } from '../../components/ingreso-preview/ingreso-preview';
import { IngresoRecentListComponent } from '../../components/ingreso-recent-list/ingreso-recent-list';
import { IngresosMockService } from '../../services/ingresos-mock.service';
import { DistribucionCategoria, IngresoFormData, IngresoReciente, OptionItem } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-nuevo-ingreso-page',
  standalone: true,
  imports: [CommonModule, RouterLink, IngresoFormComponent, IngresoPreviewComponent, IngresoRecentListComponent],
  templateUrl: './nuevo-ingreso-page.html',
  styleUrl: './nuevo-ingreso-page.scss',
})
export class NuevoIngresoPage {
  categorias: OptionItem[] = [];
  metodos: OptionItem[] = [];
  distribucion: DistribucionCategoria[] = [];
  recientes: IngresoReciente[] = [];

  form: IngresoFormData = {
    monto: 1500,
    fechaTransaccion: '31/05/2025',
    descripcion: 'Pago mensual de la empresa ABC',
    categoria: 'Salario',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: ['Trabajo', 'Mensual'],
  };

  sugerencia = { categoria: 'Salario', confianza: 0.98 };

  constructor(private mock: IngresosMockService, private router: Router) {
    this.mock.getCategorias().subscribe(v => (this.categorias = v));
    this.mock.getMetodosPago().subscribe(v => (this.metodos = v));
    this.mock.getDistribucion().subscribe(v => (this.distribucion = v));
    this.mock.getRecientes(5).subscribe(v => (this.recientes = v));
  }

  onDescripcionChange(): void {
    this.mock.sugerirCategoria(this.form.descripcion).subscribe(v => (this.sugerencia = v));
  }

  usarSugerencia(): void {
    this.form.categoria = this.sugerencia.categoria;
  }

  guardar(): void {
    this.mock.guardarIngreso(this.form).subscribe(() => this.router.navigate(['/ingresos']));
  }

  cancelar(): void {
    this.router.navigate(['/ingresos']);
  }
}

