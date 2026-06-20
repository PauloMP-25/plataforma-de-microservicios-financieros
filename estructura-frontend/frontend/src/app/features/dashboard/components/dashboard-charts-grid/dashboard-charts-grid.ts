import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartCashflowComponent } from '../chart-cashflow/chart-cashflow';
import { ChartDistributionComponent } from '../chart-distribution/chart-distribution';
import { ChartFixedVsVariableComponent } from '../chart-fixed-vs-variable/chart-fixed-vs-variable';
import { ChartHeatmapComponent } from '../chart-heatmap/chart-heatmap';
import { ChartGoalsProgressComponent } from '../chart-goals-progress/chart-goals-progress';
import { ChartHistoricalComparisonComponent } from '../chart-historical-comparison/chart-historical-comparison';

@Component({
  selector: 'app-dashboard-charts-grid',
  standalone: true,
  imports: [
    CommonModule,
    ChartCashflowComponent,
    ChartDistributionComponent,
    ChartFixedVsVariableComponent,
    ChartHeatmapComponent,
    ChartGoalsProgressComponent,
    ChartHistoricalComparisonComponent
  ],
  templateUrl: './dashboard-charts-grid.html',
  styleUrls: ['./dashboard-charts-grid.scss']
})
export class DashboardChartsGridComponent {}
