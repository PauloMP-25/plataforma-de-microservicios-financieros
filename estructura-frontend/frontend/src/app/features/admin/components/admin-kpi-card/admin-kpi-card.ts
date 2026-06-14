import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminKpiCard } from '../../models/admin-dashboard.model';

@Component({
  selector: 'app-admin-kpi-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-kpi-card.html'
})
export class AdminKpiCardComponent {
  @Input({ required: true }) kpi!: AdminKpiCard;
  @Input() modoClaro = false;

  get tonoClases(): string {
    const mapaOscuro = {
      primary: 'text-primary bg-primary/25 border-primary/70 shadow-primary/40',
      info: 'text-cyan-300 bg-cyan-400/25 border-cyan-300/70 shadow-cyan-400/40',
      success: 'text-emerald-300 bg-emerald-400/25 border-emerald-300/70 shadow-emerald-400/40',
      warning: 'text-amber-300 bg-amber-400/25 border-amber-300/70 shadow-amber-400/40',
      danger: 'text-red-300 bg-red-400/25 border-red-300/70 shadow-red-400/40',
      purple: 'text-purple-300 bg-purple-400/25 border-purple-300/70 shadow-purple-400/40'
    } as const;
    const mapaClaro = {
      primary: 'text-primary bg-primary/15 border-primary/45 shadow-primary/20',
      info: 'text-cyan-700 bg-cyan-100 border-cyan-300 shadow-cyan-200/70',
      success: 'text-emerald-700 bg-emerald-100 border-emerald-300 shadow-emerald-200/70',
      warning: 'text-amber-700 bg-amber-100 border-amber-300 shadow-amber-200/70',
      danger: 'text-red-700 bg-red-100 border-red-300 shadow-red-200/70',
      purple: 'text-purple-700 bg-purple-100 border-purple-300 shadow-purple-200/70'
    } as const;
    return (this.modoClaro ? mapaClaro : mapaOscuro)[this.kpi.tono];
  }

  get cardClases(): string {
    const mapaOscuro = {
      primary: 'border-primary/70 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.45),transparent_36%),linear-gradient(135deg,#050816,#090d2a_55%,rgba(99,102,241,0.28))] hover:border-primary',
      info: 'border-cyan-300/70 bg-[radial-gradient(circle_at_top_left,rgba(34,211,238,0.42),transparent_36%),linear-gradient(135deg,#04111f,#071426_55%,rgba(34,211,238,0.25))] hover:border-cyan-300',
      success: 'border-emerald-300/70 bg-[radial-gradient(circle_at_top_left,rgba(52,211,153,0.42),transparent_36%),linear-gradient(135deg,#04140f,#071f19_55%,rgba(52,211,153,0.25))] hover:border-emerald-300',
      warning: 'border-amber-300/70 bg-[radial-gradient(circle_at_top_left,rgba(251,191,36,0.42),transparent_36%),linear-gradient(135deg,#171004,#211807_55%,rgba(251,191,36,0.25))] hover:border-amber-300',
      danger: 'border-red-300/70 bg-[radial-gradient(circle_at_top_left,rgba(248,113,113,0.42),transparent_36%),linear-gradient(135deg,#190709,#24090d_55%,rgba(248,113,113,0.25))] hover:border-red-300',
      purple: 'border-purple-300/70 bg-[radial-gradient(circle_at_top_left,rgba(192,132,252,0.45),transparent_36%),linear-gradient(135deg,#0f0821,#160c2e_55%,rgba(168,85,247,0.32))] hover:border-purple-300'
    } as const;
    const mapaClaro = {
      primary: 'border-primary/35 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.22),transparent_36%),linear-gradient(135deg,#ffffff,#eef2ff_60%,rgba(99,102,241,0.16))] hover:border-primary/70 shadow-indigo-200/60',
      info: 'border-cyan-300/60 bg-[radial-gradient(circle_at_top_left,rgba(34,211,238,0.24),transparent_36%),linear-gradient(135deg,#ffffff,#ecfeff_60%,rgba(34,211,238,0.17))] hover:border-cyan-500 shadow-cyan-200/60',
      success: 'border-emerald-300/60 bg-[radial-gradient(circle_at_top_left,rgba(52,211,153,0.24),transparent_36%),linear-gradient(135deg,#ffffff,#ecfdf5_60%,rgba(52,211,153,0.17))] hover:border-emerald-500 shadow-emerald-200/60',
      warning: 'border-amber-300/60 bg-[radial-gradient(circle_at_top_left,rgba(251,191,36,0.26),transparent_36%),linear-gradient(135deg,#ffffff,#fffbeb_60%,rgba(251,191,36,0.18))] hover:border-amber-500 shadow-amber-200/60',
      danger: 'border-red-300/60 bg-[radial-gradient(circle_at_top_left,rgba(248,113,113,0.24),transparent_36%),linear-gradient(135deg,#ffffff,#fef2f2_60%,rgba(248,113,113,0.17))] hover:border-red-500 shadow-red-200/60',
      purple: 'border-purple-300/60 bg-[radial-gradient(circle_at_top_left,rgba(192,132,252,0.25),transparent_36%),linear-gradient(135deg,#ffffff,#faf5ff_60%,rgba(168,85,247,0.18))] hover:border-purple-500 shadow-purple-200/60'
    } as const;
    return (this.modoClaro ? mapaClaro : mapaOscuro)[this.kpi.tono];
  }

  get haloClases(): string {
    const mapa = {
      primary: 'bg-primary',
      info: 'bg-info',
      success: 'bg-success',
      warning: 'bg-warning',
      danger: 'bg-danger',
      purple: 'bg-purple-500'
    } as const;
    return mapa[this.kpi.tono];
  }

  get tendenciaClases(): string {
    if (this.kpi.tendenciaTipo === 'up') return 'text-success';
    if (this.kpi.tendenciaTipo === 'down') return 'text-danger';
    return 'text-text-muted';
  }
}
