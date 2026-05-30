import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../../../core/models/financiero/ia.model';

@Component({
  selector: 'app-ia-resultado',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-resultado.html',
  styleUrl: './ia-resultado.scss'
})
export class IaResultadoComponent {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  get totalGraficoValor(): number {
    if (!this.resultado?.grafico?.datos) return 0;
    return this.resultado.grafico.datos.reduce((acc: number, d: any) => acc + d.valor, 0);
  }

  // Retorna el porcentaje para una barra en particular
  getPorcentaje(valor: number): number {
    if (!this.resultado?.grafico?.datos) return 0;
    const max = Math.max(...this.resultado.grafico.datos.map((d: any) => d.valor), 1);
    return (valor / max) * 100;
  }
}
