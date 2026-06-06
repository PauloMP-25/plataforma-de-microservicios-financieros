import { Component, Input, OnInit, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PresupuestoService } from '../../../../core/services/presupuesto.service';

@Component({
  selector: 'app-balances-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balances-card.html',
  styleUrl: './balances-card.scss',
})
export class BalancesCard implements OnInit, OnChanges {
  @Input() totalBalance: number = 0;
  @Input() totalIngresos: number = 0;
  @Input() totalGastos: number = 0;
  @Input() tasaAhorro: number = 0;

  presupuestoActivo = 0;
  porcentajeUso = 0;

  private presupuestoService = inject(PresupuestoService);

  ngOnInit() {
    this.cargarPresupuesto();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['totalGastos']) {
      this.calcularPorcentaje();
    }
  }

  cargarPresupuesto() {
    this.presupuestoService.obtenerActivo().subscribe({
      next: (pres) => {
        if (pres && pres.activo) {
          this.presupuestoActivo = pres.montoLimite;
          this.calcularPorcentaje();
        }
      },
      error: () => {
        this.presupuestoActivo = 0;
        this.porcentajeUso = 0;
      }
    });
  }

  calcularPorcentaje() {
    if (this.presupuestoActivo > 0) {
      const calc = (this.totalGastos / this.presupuestoActivo) * 100;
      this.porcentajeUso = Math.min(Math.round(calc), 100);
    } else {
      this.porcentajeUso = 0;
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
