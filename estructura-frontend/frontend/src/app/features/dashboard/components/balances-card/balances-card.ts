import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-balances-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balances-card.html',
  styleUrl: './balances-card.scss',
})
export class BalancesCard {
  @Input() totalBalance: number = 0;
  @Input() totalIngresos: number = 0;
  @Input() totalGastos: number = 0;
  @Input() tasaAhorro: number = 0;

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
