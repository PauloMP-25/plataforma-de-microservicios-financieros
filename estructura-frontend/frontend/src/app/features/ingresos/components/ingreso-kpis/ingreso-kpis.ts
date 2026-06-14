import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { IngresoKpi } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingreso-kpis',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ingreso-kpis.html',
})
export class IngresoKpisComponent {
  @Input() items: IngresoKpi[] = [];

  cardClass(color: IngresoKpi['color'], title: string): string {
    if (this.isRacha(title)) {
      return 'border-orange-400/30 bg-gradient-to-r from-[#231f2f] to-[#1c2032] text-slate-100 shadow-[0_14px_30px_rgba(251,113,40,.18)]';
    }

    switch (color) {
      case 'emerald':
        return 'border-emerald-200/80 bg-emerald-50/70 dark:border-emerald-500/30 dark:bg-emerald-500/10';
      case 'violet':
        return 'border-violet-200/80 bg-violet-50/70 dark:border-violet-500/30 dark:bg-violet-500/10';
      case 'sky':
        return 'border-sky-200/80 bg-sky-50/70 dark:border-sky-500/30 dark:bg-sky-500/10';
      case 'amber':
        return 'border-amber-200/80 bg-amber-50/70 dark:border-amber-500/30 dark:bg-amber-500/10';
      default:
        return 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800';
    }
  }

  iconClass(title: string): string {
    const t = title.toLowerCase();
    if (t.includes('total')) return 'fa-solid fa-sack-dollar';
    if (t.includes('registrados')) return 'fa-regular fa-folder-open';
    if (t.includes('racha')) return 'fa-solid fa-fire-flame-curved';
    if (t.includes('categoria')) return 'fa-solid fa-wallet';
    return 'fa-solid fa-chart-line';
  }

  iconColorClass(color: IngresoKpi['color']): string {
    switch (color) {
      case 'emerald': return 'text-emerald-600 dark:text-emerald-300';
      case 'violet': return 'text-violet-600 dark:text-violet-300';
      case 'sky': return 'text-sky-600 dark:text-sky-300';
      case 'amber': return 'text-amber-600 dark:text-amber-300';
      default: return 'text-slate-600 dark:text-slate-300';
    }
  }

  isRacha(title: string): boolean {
    return title.toLowerCase().includes('racha');
  }

  colorClass(color: IngresoKpi['color']): string {
    switch (color) {
      case 'emerald': return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300';
      case 'violet': return 'bg-violet-50 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300';
      case 'sky': return 'bg-sky-50 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300';
      case 'amber': return 'bg-amber-50 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300';
      default: return 'bg-slate-50 text-slate-700 dark:bg-slate-700 dark:text-slate-200';
    }
  }
}

