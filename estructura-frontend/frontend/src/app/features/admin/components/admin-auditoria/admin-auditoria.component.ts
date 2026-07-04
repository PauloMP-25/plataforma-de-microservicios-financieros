import { Component, OnInit, ChangeDetectionStrategy, input, signal, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../../../../core/services/auditoria.service';
import { 
  RegistroAuditoriaDTO, 
  AuditoriaAccesoDTO, 
  AuditoriaTransaccionalDTO 
} from '../../../../core/models/auditoria/auditoria.model';

@Component({
  selector: 'app-admin-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-auditoria.component.html',
  styleUrl: './admin-auditoria.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminAuditoriaComponent implements OnInit {
  private readonly auditoriaService = inject(AuditoriaService);

  // Input de tema visual (claro/oscuro)
  modoClaro = input(false);

  // Pestaña Activa
  pestanaActiva = signal<'registros' | 'accesos' | 'transacciones'>('registros');

  // Listas de datos
  registros = signal<RegistroAuditoriaDTO[]>([]);
  accesos = signal<AuditoriaAccesoDTO[]>([]);
  transacciones = signal<AuditoriaTransaccionalDTO[]>([]);

  // Estado de carga
  cargando = signal(true);

  // Registro seleccionado para el Modal
  registroSeleccionado = signal<any | null>(null);

  // Paginación y Filtros
  paginaActual = signal(0);
  tamanioPagina = signal(10);
  totalElementos = signal(0);
  totalPaginas = signal(0);

  // Filtros específicos
  moduloFiltro = signal<string>('');
  servicioFiltro = signal<string>('');

  constructor() {
    // Escuchar cambios de pestaña para recargar datos automáticamente
    effect(() => {
      this.pestanaActiva();
      this.cargarDatos();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    // Carga inicial manejada por el effect
  }

  cambiarPestana(tab: 'registros' | 'accesos' | 'transacciones'): void {
    this.paginaActual.set(0);
    this.moduloFiltro.set('');
    this.servicioFiltro.set('');
    this.pestanaActiva.set(tab);
  }

  cargarDatos(): void {
    this.cargando.set(true);
    const tab = this.pestanaActiva();
    const pag = this.paginaActual();
    const tam = this.tamanioPagina();

    if (tab === 'registros') {
      this.auditoriaService.listarRegistros({
        modulo: this.moduloFiltro() || undefined,
        pagina: pag,
        tamanio: tam
      }).subscribe({
        next: (pagina) => {
          this.registros.set(pagina.content || []);
          this.totalElementos.set(pagina.totalElements);
          this.totalPaginas.set(pagina.totalPages);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
        }
      });
    } else if (tab === 'accesos') {
      this.auditoriaService.listarAccesos(pag, tam).subscribe({
        next: (pagina) => {
          this.accesos.set(pagina.content || []);
          this.totalElementos.set(pagina.totalElements);
          this.totalPaginas.set(pagina.totalPages);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
        }
      });
    } else if (tab === 'transacciones') {
      this.auditoriaService.listarTransacciones({
        servicioOrigen: this.servicioFiltro() || undefined,
        pagina: pag,
        tamanio: tam
      }).subscribe({
        next: (pagina) => {
          this.transacciones.set(pagina.content || []);
          this.totalElementos.set(pagina.totalElements);
          this.totalPaginas.set(pagina.totalPages);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
        }
      });
    }
  }

  abrirDetalle(item: any): void {
    this.registroSeleccionado.set(item);
  }

  cerrarDetalle(): void {
    this.registroSeleccionado.set(null);
  }

  anteriorPagina(): void {
    if (this.paginaActual() > 0) {
      this.paginaActual.update(p => p - 1);
      this.cargarDatos();
    }
  }

  siguientePagina(): void {
    if (this.paginaActual() + 1 < this.totalPaginas()) {
      this.paginaActual.update(p => p + 1);
      this.cargarDatos();
    }
  }

  aplicarFiltros(): void {
    this.paginaActual.set(0);
    this.cargarDatos();
  }

  formatoFecha(fechaStr?: string): string {
    if (!fechaStr) return '-';
    try {
      return new Date(fechaStr).toLocaleString('es-PE', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch {
      return fechaStr;
    }
  }

  formatoJSON(jsonStr?: string): string {
    if (!jsonStr) return '{}';
    try {
      const obj = JSON.parse(jsonStr);
      return JSON.stringify(obj, null, 2);
    } catch {
      return jsonStr;
    }
  }

  formatoUsuarioId(uuid: string): string {
    if (!uuid) return '-';
    if (uuid.length <= 8) return uuid;
    return `USR-${uuid.slice(0, 8).toUpperCase()}`;
  }

  esTransaccional(item: any): boolean {
    return 'servicioOrigen' in item;
  }

  esAcceso(item: any): boolean {
    return 'ipOrigen' in item && 'navegador' in item && 'estado' in item;
  }
}
