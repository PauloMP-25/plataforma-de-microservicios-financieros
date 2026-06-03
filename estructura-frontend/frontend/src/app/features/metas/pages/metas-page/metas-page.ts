import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteMetasLimitesService } from '../../../../core/services/cliente-metas-limites.service';
import { RespuestaMetaAhorro, SolicitudMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { AuthService } from '../../../../core/services/auth.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { forkJoin } from 'rxjs';

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
  private financieroService = inject(FinancieroService);
  private transaccionesService = inject(Transacciones);
  private authService = inject(AuthService);
  private eventBus = inject(AppEventBus);

  // Formularios
  formularioCrear!: FormGroup;
  formularioEditar!: FormGroup;

  // Estado
  metas = signal<RespuestaMetaAhorro[]>([]);
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');

  // Exponer Math para la plantilla HTML
  protected readonly Math = Math;

  // Modales y Paneles
  mostrarFormulario = signal<boolean>(false);
  editandoMeta = signal<any | null>(null);
  metaSeleccionada = signal<any | null>(null);
  modalConfirmarCompletar = signal<any | null>(null);

  // Filtros reactivos
  filtroEstado = signal<string>('Todas');
  filtroMes = signal<string>('Todos');
  filtroAnio = signal<string>('Todos');
  filtroMontoMin = signal<number | null>(null);
  filtroMontoMax = signal<number | null>(null);

  fechaMinima = '';

  // Lista de categorías con sus íconos correspondientes
  readonly categorias = [
    { id: 'Viaje', nombre: 'Viaje', icono: 'fa-solid fa-plane' },
    { id: 'Vivienda', nombre: 'Vivienda', icono: 'fa-solid fa-house' },
    { id: 'Auto', nombre: 'Auto', icono: 'fa-solid fa-car' },
    { id: 'Estudios', nombre: 'Estudios', icono: 'fa-solid fa-graduation-cap' },
    { id: 'Tecnología', nombre: 'Tecnología', icono: 'fa-solid fa-laptop' },
    { id: 'Emergencia', nombre: 'Emergencia', icono: 'fa-solid fa-kit-medical' },
    { id: 'Otros', nombre: 'Otros', icono: 'fa-solid fa-bullseye' }
  ];

  // Ahorro disponible cargado desde FinancieroService (balance general)
  ahorroDisponible = computed(() => {
    const resumen = this.financieroService.resumen();
    return resumen ? resumen.balance : 1700; // fallback a 1700 si no ha cargado
  });

  // Cálculo secuencial/híbrido del avance de las metas activas usando el saldo restante
  metasCalculadas = computed(() => {
    const listado = this.metas();
    const disponibleGlobal = this.ahorroDisponible();

    const completadas = listado.filter(m => m.completada);
    const activas = listado.filter(m => !m.completada);

    // Ordenar activas por fecha límite más cercana (prioridad de llenado)
    const activasOrdenadas = [...activas].sort((a, b) => {
      if (!a.fechaLimite) return 1;
      if (!b.fechaLimite) return -1;
      return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
    });

    let saldoRestante = disponibleGlobal;

    const activasCalculadas = activasOrdenadas.map(meta => {
      const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
      const faltante = Math.max(0, meta.montoObjetivo - meta.montoActual);
      const adicionalAplicado = Math.min(faltante, saldoRestante);

      const montoAplicado = meta.montoActual + adicionalAplicado;
      saldoRestante = Math.max(0, saldoRestante - adicionalAplicado);

      const porcentaje = meta.montoObjetivo > 0 
        ? (montoAplicado / meta.montoObjetivo) * 100 
        : 0;

      return {
        ...meta,
        nombreVisual: datosVisuales.nombre,
        categoriaVisual: datosVisuales.categoria,
        iconoVisual: datosVisuales.icono,
        montoAplicado: montoAplicado,
        porcentajeProgreso: porcentaje,
        puedeCompletar: porcentaje >= 100
      };
    });

    const completadasMapeadas = completadas.map(meta => {
      const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
      return {
        ...meta,
        nombreVisual: datosVisuales.nombre,
        categoriaVisual: datosVisuales.categoria,
        iconoVisual: datosVisuales.icono,
        montoAplicado: meta.montoObjetivo,
        porcentajeProgreso: 100,
        puedeCompletar: false
      };
    });

    return [...completadasMapeadas, ...activasCalculadas];
  });

  // Metas después de aplicar los filtros del frontend
  metasFiltradas = computed(() => {
    let listado = this.metasCalculadas();

    // Filtro por Estado
    const est = this.filtroEstado();
    if (est !== 'Todas') {
      if (est === 'Activas') {
        listado = listado.filter(m => !m.completada && !this.esVencida(m));
      } else if (est === 'Cumplidas') {
        listado = listado.filter(m => m.completada);
      } else if (est === 'Vencidas') {
        listado = listado.filter(m => !m.completada && this.esVencida(m));
      }
    }

    // Filtro por Mes
    const mes = this.filtroMes();
    if (mes !== 'Todos') {
      const mesNum = parseInt(mes, 10);
      listado = listado.filter(m => {
        if (!m.fechaLimite) return false;
        const date = new Date(m.fechaLimite + 'T00:00:00');
        return date.getMonth() === mesNum;
      });
    }

    // Filtro por Año
    const anio = this.filtroAnio();
    if (anio !== 'Todos') {
      listado = listado.filter(m => {
        if (!m.fechaLimite) return false;
        const date = new Date(m.fechaLimite + 'T00:00:00');
        return date.getFullYear() === parseInt(anio, 10);
      });
    }

    // Filtro por Rango Mínimo
    const min = this.filtroMontoMin();
    if (min !== null && min >= 0) {
      listado = listado.filter(m => m.montoObjetivo >= min);
    }

    // Filtro por Rango Máximo
    const max = this.filtroMontoMax();
    if (max !== null && max >= 0) {
      listado = listado.filter(m => m.montoObjetivo <= max);
    }

    return listado;
  });

  // KPIs
  metasActivasCount = computed(() => this.metasCalculadas().filter(m => !m.completada).length);
  metasCumplidasCount = computed(() => this.metasCalculadas().filter(m => m.completada).length);
  
  metaMasCercana = computed(() => {
    const activas = this.metasCalculadas().filter(m => !m.completada);
    if (activas.length === 0) return null;
    return [...activas].sort((a, b) => {
      if (!a.fechaLimite) return 1;
      if (!b.fechaLimite) return -1;
      return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
    })[0];
  });

  ngOnInit(): void {
    const hoy = new Date();
    this.fechaMinima = hoy.toISOString().split('T')[0];

    this.inicializarFormularios();
    this.financieroService.cargarResumen();
    this.cargarMetas();
  }

  inicializarFormularios(): void {
    const hoy = new Date();
    const unMesDespues = new Date(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());
    const fechaDefecto = unMesDespues.toISOString().split('T')[0];

    // Formulario Crear
    this.formularioCrear = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      categoria: ['Otros', Validators.required],
      montoObjetivo: [null, [Validators.required, Validators.min(1.00)]],
      montoActual: [0.00, [Validators.required, Validators.min(0.00)]],
      fechaLimite: [fechaDefecto, [Validators.required, this.validarFechaFutura.bind(this)]]
    });

    // Formulario Editar
    this.formularioEditar = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      categoria: ['Otros', Validators.required],
      montoObjetivo: [null, [Validators.required, Validators.min(1.00)]],
      montoActual: [0.00, [Validators.required, Validators.min(0.00)]],
      fechaLimite: ['', [Validators.required, this.validarFechaFutura.bind(this)]]
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
        this.cargando.set(false);
        // Si hay una meta seleccionada en el detalle, actualizarla con los datos frescos
        const seleccionada = this.metaSeleccionada();
        if (seleccionada) {
          const fresca = this.metasCalculadas().find(m => m.id === seleccionada.id);
          if (fresca) this.metaSeleccionada.set(fresca);
        }
      },
      error: (err) => {
        console.error('Error al recuperar metas:', err);
        this.errorMensaje.set('No se pudieron recuperar las metas de ahorro.');
        this.cargando.set(false);
      }
    });
  }

  // Descomponer prefijo del nombre
  obtenerCategoriaYNombre(metaNombre: string): { categoria: string; nombre: string; icono: string } {
    const match = metaNombre.match(/^\[(.*?)\] (.*)$/);
    if (match) {
      const cat = match[1];
      const nom = match[2];
      return {
        categoria: cat,
        nombre: nom,
        icono: this.obtenerIconoCategoria(cat)
      };
    }
    return {
      categoria: 'Otros',
      nombre: metaNombre,
      icono: this.obtenerIconoCategoria('Otros')
    };
  }

  obtenerIconoCategoria(catId: string): string {
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? cat.icono : 'fa-solid fa-bullseye';
  }

  obtenerColorEstado(meta: any): string {
    if (meta.completada) return 'success';
    if (this.esVencida(meta)) return 'danger';
    return 'active';
  }

  esVencida(meta: RespuestaMetaAhorro): boolean {
    if (!meta.fechaLimite) return false;
    const limite = new Date(meta.fechaLimite + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return limite < hoy;
  }

  calcularDiasRestantes(fechaLimiteStr: string): number {
    if (!fechaLimiteStr) return 0;
    const limite = new Date(fechaLimiteStr + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const dif = limite.getTime() - hoy.getTime();
    return Math.max(0, Math.ceil(dif / (1000 * 60 * 60 * 24)));
  }

  calcularTiempoEmpleado(creacion: string, actualizacion: string): string {
    const inicio = new Date(creacion);
    const fin = new Date(actualizacion);
    const difAnios = fin.getFullYear() - inicio.getFullYear();
    const difMeses = fin.getMonth() - inicio.getMonth() + (difAnios * 12);
    
    if (difMeses <= 0) {
      const difDias = Math.ceil((fin.getTime() - inicio.getTime()) / (1000 * 60 * 60 * 24));
      return `${difDias} días`;
    }
    return `${difMeses} meses`;
  }

  abrirCrearMeta(): void {
    const hoy = new Date();
    const unMesDespues = new Date(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());
    const fechaDefecto = unMesDespues.toISOString().split('T')[0];

    this.formularioCrear.reset({
      nombre: '',
      categoria: 'Otros',
      montoObjetivo: null,
      montoActual: 0.00,
      fechaLimite: fechaDefecto
    });
    this.editandoMeta.set(null);
    this.mostrarFormulario.set(true);
  }

  guardarNuevaMeta(): void {
    if (this.formularioCrear.invalid) {
      this.formularioCrear.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const formVal = this.formularioCrear.value;
    const nombreConPrefijo = `[${formVal.categoria}] ${formVal.nombre}`;

    const payload: SolicitudMetaAhorro = {
      nombre: nombreConPrefijo,
      montoObjetivo: formVal.montoObjetivo,
      montoActual: formVal.montoActual ?? 0,
      fechaLimite: formVal.fechaLimite
    };

    this.metasService.crearMeta(payload).subscribe({
      next: (nuevaMeta) => {
        const datosVisuales = this.obtenerCategoriaYNombre(nuevaMeta.nombre);
        this.exitoMensaje.set(`Meta "${datosVisuales.nombre}" creada con éxito.`);
        this.mostrarFormulario.set(false);
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al crear la meta de ahorro.');
      }
    });
  }

  abrirEditarMeta(meta: any): void {
    const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
    this.editandoMeta.set(meta);
    this.formularioEditar.reset({
      nombre: datosVisuales.nombre,
      categoria: datosVisuales.categoria || 'Otros',
      montoObjetivo: meta.montoObjetivo,
      montoActual: meta.montoActual,
      fechaLimite: meta.fechaLimite
    });
    this.mostrarFormulario.set(true);
  }

  guardarEdicionMeta(): void {
    if (this.formularioEditar.invalid) {
      this.formularioEditar.markAllAsTouched();
      return;
    }

    const metaOriginal = this.editandoMeta();
    if (!metaOriginal) return;

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const formVal = this.formularioEditar.value;
    const nombreConPrefijo = `[${formVal.categoria}] ${formVal.nombre}`;

    // Al no tener endpoint general de edición (PUT) en el backend,
    // simulamos la actualización eliminando el registro previo y recreándolo.
    this.metasService.eliminarMeta(metaOriginal.id).subscribe({
      next: () => {
        const payload: SolicitudMetaAhorro = {
          nombre: nombreConPrefijo,
          montoObjetivo: formVal.montoObjetivo,
          montoActual: formVal.montoActual ?? 0,
          fechaLimite: formVal.fechaLimite
        };

        this.metasService.crearMeta(payload).subscribe({
          next: () => {
            this.exitoMensaje.set(`Meta "${formVal.nombre}" actualizada con éxito.`);
            this.mostrarFormulario.set(false);
            this.editandoMeta.set(null);
            
            // Si estaba seleccionada en el panel de detalle, deseleccionarla
            if (this.metaSeleccionada()?.id === metaOriginal.id) {
              this.metaSeleccionada.set(null);
            }

            this.cargarMetas();
            setTimeout(() => this.exitoMensaje.set(''), 4000);
          },
          error: (err) => {
            console.error('Error al recrear meta editada:', err);
            this.errorMensaje.set('No se pudo recrear la meta modificada.');
            this.cargando.set(false);
          }
        });
      },
      error: (err) => {
        console.error('Error al eliminar meta anterior para edición:', err);
        this.errorMensaje.set('No se pudo procesar la actualización de la meta.');
        this.cargando.set(false);
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
        this.exitoMensaje.set('Meta de ahorro eliminada con éxito.');
        if (this.metaSeleccionada()?.id === metaId) {
          this.metaSeleccionada.set(null);
        }
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al eliminar la meta de ahorro.');
      }
    });
  }

  // Selección para panel de detalle
  seleccionarMeta(meta: any): void {
    this.metaSeleccionada.set(meta);
  }

  cerrarDetalle(): void {
    this.metaSeleccionada.set(null);
  }

  // Apertura del modal de confirmación de completado
  solicitarCompletarMeta(meta: any): void {
    this.modalConfirmarCompletar.set(meta);
  }

  cerrarModalCompletar(): void {
    this.modalConfirmarCompletar.set(null);
  }

  // Confirmar y completar meta (descuenta saldo disponible, marca completada y genera gasto)
  confirmarCompletarMeta(): void {
    const meta = this.modalConfirmarCompletar();
    if (!meta) return;

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const usuario = this.authService.usuario();
    
    // Preparar el gasto para el historial
    const transaccionPayload: TransaccionRequestDTO = {
      usuarioId: usuario?.id ?? '',
      nombreCliente: usuario?.nombreUsuario ?? 'Cliente',
      monto: meta.montoObjetivo,
      tipo: 'GASTO',
      categoriaId: 'otros', // Se guarda en otros / compras generales
      fechaTransaccion: new Date().toISOString(),
      metodoPago: 'DIGITAL',
      notas: `Meta alcanzada: ${meta.nombreVisual}|Gasto registrado automáticamente al cumplir el objetivo financiero|DIARIO`
    };

    // Lanzar concurrentemente la creación de la transacción y la actualización de la meta
    forkJoin({
      gasto: this.transaccionesService.registrar(transaccionPayload),
      meta: this.metasService.actualizarProgresoMeta(meta.id, meta.montoObjetivo)
    }).subscribe({
      next: () => {
        this.exitoMensaje.set(`¡Felicidades! Has completado tu meta "${meta.nombreVisual}". Se registró un gasto de S/ ${meta.montoObjetivo.toFixed(2)}.`);
        this.modalConfirmarCompletar.set(null);
        this.metaSeleccionada.set(null);
        
        // Refrescar balance del FinancieroService y listado
        this.financieroService.cargarResumen();
        this.cargarMetas();
        
        // Notificar cambios de transacciones
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        
        setTimeout(() => this.exitoMensaje.set(''), 5000);
      },
      error: (err) => {
        console.error('Error al registrar cumplimiento de meta:', err);
        this.errorMensaje.set('No se pudo completar la meta. Inténtalo de nuevo.');
        this.cargando.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('Todas');
    this.filtroMes.set('Todos');
    this.filtroAnio.set('Todos');
    this.filtroMontoMin.set(null);
    this.filtroMontoMax.set(null);
  }
}
