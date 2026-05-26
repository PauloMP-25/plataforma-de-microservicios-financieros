import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteMetasLimitesService } from '../../../../core/services/cliente-metas-limites.service';
import { RespuestaMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';

@Component({
  selector: 'app-metas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './metas-page.html',
  styleUrl: './metas-page.scss',
})
export class MetasPage implements OnInit {
  private fb = inject(FormBuilder);
  private metasService = inject(ClienteMetasLimitesService);

  formulario!: FormGroup;
  metas = signal<RespuestaMetaAhorro[]>([]);
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');
  
  // Para aportes rápidos inline o globales
  editandoMetaId = signal<string | null>(null);
  montoAporte = signal<number | null>(null);

  // Formulario rápido global de aportes
  metaSeleccionadaId = signal<string>('');
  montoAporteGlobal = signal<number | null>(null);

  fechaMinima = '';

  // Computeds
  metasActivas = computed(() => {
    return this.metas()
      .filter(m => !m.completada)
      .sort((a, b) => {
        if (!a.fechaLimite) return 1;
        if (!b.fechaLimite) return -1;
        return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
      });
  });

  metasCompletadas = computed(() => {
    return this.metas()
      .filter(m => m.completada)
      .sort((a, b) => {
        const fechaA = new Date(a.fechaActualizacion || a.fechaCreacion).getTime();
        const fechaB = new Date(b.fechaActualizacion || b.fechaCreacion).getTime();
        return fechaB - fechaA;
      });
  });

  totalAhorrado = computed(() => {
    return this.metas().reduce((sum, m) => sum + (m.montoActual || 0), 0);
  });

  metaMasCercana = computed(() => {
    const activas = this.metasActivas();
    return activas.length > 0 ? activas[0] : null;
  });

  ngOnInit(): void {
    const hoy = new Date();
    this.fechaMinima = hoy.toISOString().split('T')[0];
    
    this.inicializarFormulario();
    this.cargarMetas();
  }

  inicializarFormulario(): void {
    const hoy = new Date();
    const unMesDespues = new Date(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());
    const fechaPorDefecto = unMesDespues.toISOString().split('T')[0];

    this.formulario = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      montoObjetivo: [null, [Validators.required, Validators.min(1.00)]],
      montoActual: [0.00, [Validators.required, Validators.min(0.00)]],
      fechaLimite: [fechaPorDefecto, [Validators.required, this.validarFechaFutura.bind(this)]]
    });
  }

  validarFechaFutura(control: any): { [key: string]: boolean } | null {
    if (control.value) {
      const fechaSeleccionada = new Date(control.value + 'T00:00:00');
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      if (fechaSeleccionada <= hoy) {
        return { fechaPasada: true };
      }
    }
    return null;
  }

  cargarMetas(): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.metasService.listarMetas().subscribe({
      next: (data) => {
        this.metas.set(data || []);
        // Si hay metas activas, inicializar la selección para aporte rápido
        const activas = this.metasActivas();
        if (activas.length > 0 && !this.metaSeleccionadaId()) {
          this.metaSeleccionadaId.set(activas[0].id);
        }
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar metas:', err);
        this.errorMensaje.set('No se pudieron recuperar tus metas de ahorro.');
        this.cargando.set(false);
      }
    });
  }

  guardarMeta(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const payload = this.formulario.value;

    this.metasService.crearMeta(payload).subscribe({
      next: (nuevaMeta) => {
        this.exitoMensaje.set(`Meta "${nuevaMeta.nombre}" creada con éxito.`);
        this.formulario.reset({
          nombre: '',
          montoObjetivo: null,
          montoActual: 0.00,
          fechaLimite: new Date(new Date().getFullYear(), new Date().getMonth() + 1, new Date().getDate()).toISOString().split('T')[0]
        });
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al guardar la meta de ahorro.');
      }
    });
  }

  eliminarMeta(metaId: string): void {
    if (!confirm('¿Estás seguro de que deseas eliminar esta meta de ahorro? Todo el progreso acumulado se perderá.')) {
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    this.metasService.eliminarMeta(metaId).subscribe({
      next: () => {
        this.exitoMensaje.set('Meta de ahorro eliminada.');
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al eliminar la meta de ahorro.');
      }
    });
  }

  toggleAporteInline(metaId: string): void {
    if (this.editandoMetaId() === metaId) {
      this.editandoMetaId.set(null);
      this.montoAporte.set(null);
    } else {
      this.editandoMetaId.set(metaId);
      this.montoAporte.set(null);
    }
  }

  guardarAporteInline(meta: RespuestaMetaAhorro): void {
    const aporte = this.montoAporte();
    if (!aporte || aporte <= 0) {
      alert('Ingresa un monto de aporte válido mayor a 0.');
      return;
    }

    this.ejecutarActualizacionProgreso(meta, aporte);
  }

  guardarAporteGlobal(): void {
    const metaId = this.metaSeleccionadaId();
    const aporte = this.montoAporteGlobal();
    
    if (!metaId) {
      alert('Selecciona una meta para aportar.');
      return;
    }
    if (!aporte || aporte <= 0) {
      alert('Ingresa un monto de aporte válido mayor a 0.');
      return;
    }

    const meta = this.metas().find(m => m.id === metaId);
    if (meta) {
      this.ejecutarActualizacionProgreso(meta, aporte);
    }
  }

  ejecutarActualizacionProgreso(meta: RespuestaMetaAhorro, aporte: number): void {
    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const nuevoMonto = Number(meta.montoActual) + Number(aporte);

    this.metasService.actualizarProgresoMeta(meta.id, nuevoMonto).subscribe({
      next: (metaActualizada) => {
        const esMetaCumplida = metaActualizada.completada && !meta.completada;
        if (esMetaCumplida) {
          this.exitoMensaje.set(`¡Felicidades! Has completado tu meta de ahorro: "${meta.nombre}" 🎉`);
        } else {
          this.exitoMensaje.set(`Aporte de S/ ${aporte.toFixed(2)} registrado en "${meta.nombre}".`);
        }
        
        // Resetear campos de aporte
        this.editandoMetaId.set(null);
        this.montoAporte.set(null);
        this.montoAporteGlobal.set(null);
        
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al actualizar el progreso de la meta.');
      }
    });
  }

  aportarMontoRapido(monto: number): void {
    this.montoAporteGlobal.set(monto);
  }

  calcularDiasRestantes(fechaLimiteStr: string): number {
    if (!fechaLimiteStr) return 0;
    const limite = new Date(fechaLimiteStr + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const dif = limite.getTime() - hoy.getTime();
    return Math.max(0, Math.ceil(dif / (1000 * 60 * 60 * 24)));
  }
}
