import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Transacciones } from '../../../../core/services/transacciones';
import { TransaccionDTO, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-gastos-page',
  standalone:true,
  imports: [CommonModule],
  templateUrl: './gastos-page.html',
  styleUrl: './gastos-page.scss',
})
export class GastosPage {
  private readonly transaccionesService = inject(Transacciones);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly cargando = signal(false);
  readonly terminoBusqueda = signal('');
  readonly tabActiva = signal<'transacciones' | 'analisis' | 'presupuestos'>('transacciones');
  readonly gastos = signal<TransaccionDTO[]>([]);
  readonly modalAbierto = signal(false);
  readonly guardandoGasto = signal(false);
  readonly mensajeFormulario = signal('');

  readonly categoria = signal('');
  readonly monto = signal('');
  readonly descripcion = signal('');
  readonly fecha = signal('');
  readonly tipoFrecuencia = signal<'DIARIO' | 'RECURRENTE'>('DIARIO');
  readonly errores = signal<Record<string, string>>({});

  readonly categoriasDisponibles = [
    { id: 'alimentos', nombre: 'Alimentos' },
    { id: 'transporte', nombre: 'Transporte' },
    { id: 'servicios', nombre: 'Servicios' },
    { id: 'hogar', nombre: 'Hogar' },
    { id: 'otros', nombre: 'Otros' },
  ];

  readonly totalGastado = computed(() =>
    this.gastos().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  readonly totalPendiente = computed(() => 0);
  readonly totalPagado = computed(() => this.totalGastado());

  readonly gastosPendientes = computed(() => [] as TransaccionDTO[]);
  readonly gastosPagados = computed(() => this.gastos());

  constructor() {
    this.cargarGastos();
  }

  seleccionarTab(tab: 'transacciones' | 'analisis' | 'presupuestos'): void {
    this.tabActiva.set(tab);

    if (tab === 'transacciones') {
      this.router.navigateByUrl('/perfil/transacciones');
      return;
    }
    if (tab === 'analisis') {
      this.router.navigateByUrl('/dashboard');
      return;
    }
    this.router.navigateByUrl('/presupuestos');
  }

  actualizarBusqueda(valor: string): void {
    this.terminoBusqueda.set(valor);
  }

  abrirModal(): void {
    this.resetFormulario();
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  guardarGasto(): void {
    const errores = this.validarFormulario();
    this.errores.set(errores);
    this.mensajeFormulario.set('');

    if (Object.keys(errores).length > 0) {
      return;
    }

    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.mensajeFormulario.set('No se encontró sesión activa.');
      return;
    }

    const request: TransaccionRequestDTO = {
      usuarioId,
      nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
      monto: Number(this.monto()),
      tipo: 'GASTO',
      categoriaId: this.categoria(),
      fechaTransaccion: new Date(this.fecha()).toISOString(),
      metodoPago: 'DIGITAL',
      notas: `${this.tipoFrecuencia()} - ${this.descripcion().trim()}`,
    };

    this.guardandoGasto.set(true);
    this.transaccionesService.registrar(request).subscribe({
      next: () => {
        this.guardandoGasto.set(false);
        this.modalAbierto.set(false);
        this.cargarGastos();
      },
      error: () => {
        this.guardandoGasto.set(false);
        this.mensajeFormulario.set('No se pudo registrar el gasto.');
      },
    });
  }

  private validarFormulario(): Record<string, string> {
    const out: Record<string, string> = {};

    if (!this.categoria().trim()) {
      out['categoria'] = 'Selecciona una categoría.';
    }
    if (!this.monto().trim() || Number(this.monto()) <= 0) {
      out['monto'] = 'Ingresa un monto válido mayor a 0.';
    }
    if (!this.descripcion().trim()) {
      out['descripcion'] = 'Ingresa una descripción del gasto.';
    }
    if (!this.fecha().trim()) {
      out['fecha'] = 'Selecciona una fecha.';
    }

    return out;
  }

  private resetFormulario(): void {
    this.categoria.set('');
    this.monto.set('');
    this.descripcion.set('');
    this.fecha.set('');
    this.tipoFrecuencia.set('DIARIO');
    this.errores.set({});
    this.mensajeFormulario.set('');
  }

  private cargarGastos(): void {
    this.cargando.set(true);

    this.transaccionesService.listarHistorial({ tipo: 'GASTO', pagina: 0, tamanio: 20 }).subscribe({
      next: (pagina) => {
        this.gastos.set(pagina?.content ?? []);
        this.cargando.set(false);
      },
      error: () => {
        this.gastos.set([]);
        this.cargando.set(false);
      },
    });
  }

}
