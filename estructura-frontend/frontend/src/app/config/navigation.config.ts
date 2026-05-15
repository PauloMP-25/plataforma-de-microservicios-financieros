

export interface NavItem {
  label: string;
  route: string;
  icon:  string; 
  badge?: number | string;
  exact?: boolean;
}


export const NAV_ITEMS: NavItem[] = [
  {
    label: 'Resumen',
    route: '/dashboard',
    icon:  'fa-solid fa-chart-pie',
    exact: true
  },
  {
    label: 'Gastos',
    route: '/gastos',
    icon:  'fa-solid fa-credit-card',
  },
  {
    label: 'Ingresos',
    route: '/ingresos',
    icon:  'fa-solid fa-money-bill-trend-up'
  },
  {
    label: 'Metas',
    route: '/metas',
    icon:  'fa-solid fa-piggy-bank'
  },
  {
    label: 'Presupuestos',
    route: '/presupuestos',
    icon:  'fa-solid fa-wallet'
  },
];

// ── Navegación de cuenta  ──
export const BOTTOM_NAV_ITEMS: NavItem[] = [
  {
    label: 'Mi Perfil',
    route: '/perfil',
    icon:  'fa-solid fa-circle-user'
  },
  {
    label: 'Configuración',
    route: '/configuracion',
    icon:  'fa-solid fa-gear'
  },
  {
    label: 'Ayuda',
    route: '/ayuda',
    icon:  'fa-solid fa-circle-question'
  }
];