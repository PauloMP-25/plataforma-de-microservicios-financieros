import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IngresoKpisComponent } from '../../components/ingreso-kpis/ingreso-kpis';
import { IngresoChartComponent } from '../../components/ingreso-chart/ingreso-chart';
import { IngresoRecentListComponent } from '../../components/ingreso-recent-list/ingreso-recent-list';
import { IngresosTableComponent } from '../../components/ingresos-table/ingresos-table';
import { IngresosMockService } from '../../services/ingresos-mock.service';
import {
  DistribucionCategoria,
  IngresoKpi,
  IngresoReciente,
  IngresoRegistro,
  IngresoTendenciaPunto,
} from '../../types/ingresos.interfaces';
@Component({
  selector: 'app-ingresos-page',
  standalone:true,
  imports: [CommonModule, RouterLink, IngresoKpisComponent, IngresoChartComponent, IngresoRecentListComponent, IngresosTableComponent],
  templateUrl: './ingresos-page.html',
  styleUrl: './ingresos-page.scss',
})
export class IngresosPage {
  kpis: IngresoKpi[] = [];
  distribucion: DistribucionCategoria[] = [];
  tendencia: IngresoTendenciaPunto[] = [];
  recientes: IngresoReciente[] = [];
  tabla: IngresoRegistro[] = [];

  constructor(private mock: IngresosMockService) {
    this.mock.getKpis().subscribe(v => this.kpis = v);
    this.mock.getDistribucion().subscribe(v => this.distribucion = v);
    this.mock.getTendencia().subscribe(v => this.tendencia = v);
    this.mock.getRecientes(5).subscribe(v => this.recientes = v);
    this.mock.getTabla().subscribe(v => this.tabla = v);
  }

}
