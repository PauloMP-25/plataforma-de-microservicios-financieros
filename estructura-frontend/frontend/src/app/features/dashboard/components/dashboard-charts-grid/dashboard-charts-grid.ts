import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ChartCashflowComponent } from '../chart-cashflow/chart-cashflow';
import { ChartDistributionComponent } from '../chart-distribution/chart-distribution';
import { ChartExpenseIncomeRatioComponent } from '../chart-expense-income-ratio/chart-expense-income-ratio';
import { ChartHeatmapComponent } from '../chart-heatmap/chart-heatmap';
import { ChartPaymentMethodsComponent } from '../chart-payment-methods/chart-payment-methods';
import { ChartHistoricalComparisonComponent } from '../chart-historical-comparison/chart-historical-comparison';

@Component({
  selector: 'app-dashboard-charts-grid',
  standalone: true,
  imports: [
    CommonModule,
    ChartCashflowComponent,
    ChartDistributionComponent,
    ChartExpenseIncomeRatioComponent,
    ChartHeatmapComponent,
    ChartPaymentMethodsComponent,
    ChartHistoricalComparisonComponent
  ],
  templateUrl: './dashboard-charts-grid.html',
  styleUrls: ['./dashboard-charts-grid.scss']
})
export class DashboardChartsGridComponent {
  constructor(public stateService: DashboardStateService) {}
}
