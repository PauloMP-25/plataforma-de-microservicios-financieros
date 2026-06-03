import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ClienteMetasLimitesService } from '../../../../core/services/cliente-metas-limites.service';
import { RespuestaMetaAhorro, SolicitudMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { FinancieroService } from '../../../../core/services/Financiero.service';

@Component({
  selector: 'app-meta-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './meta-form-page.html',
  styleUrl: './meta-form-page.scss',
})
export class MetaFormPage implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private metasService = inject(ClienteMetasLimitesService);
  private financieroService = inject(FinancieroService);

  formulario!: FormGroup;
  modoEdicion = false;
  metaId: string | null = null;
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');
  fechaMinima = '';

  protected readonly Math = Math;

  // Lista de categorías con sus íconos
  readonly categorias = [
    { id: 'Viaje', nombre: 'Viaje', icono: 'fa-solid fa-plane' },
    { id: 'Vivienda', nombre: 'Vivienda', icono: 'fa-solid fa-house' },
    { id: 'Auto', nombre: 'Auto', icono: 'fa-solid fa-car' },
    { id: 'Estudios', nombre: 'Estudios', icono: 'fa-solid fa-graduation-cap' },
    { id: 'Tecnología', nombre: 'Tecnología', icono: 'fa-solid fa-laptop' },
    { id: 'Emergencia', nombre: 'Emergencia', icono: 'fa-solid fa-piggy-bank' },
    { id: 'Otros', nombre: 'Otros', icono: 'fa-solid fa-bullseye' }
  ];

  // Mocks por defecto (sincronizados con la lista principal)
  readonly mockMetasIniciales: RespuestaMetaAhorro[] = [
    {
      id: 'mock-meta-1',
      nombre: '[Viaje] Viaje a Cancún',
      montoObjetivo: 2000,
      montoActual: 2000,
      porcentajeProgreso: 100,
      fechaLimite: '2026-11-29',
      completada: true,
      fechaCreacion: '2025-01-15',
      fechaActualizacion: '2026-11-29'
    },
    {
      id: 'mock-meta-2',
      nombre: '[Tecnología] Laptop',
      montoObjetivo: 300,
      montoActual: 300,
      porcentajeProgreso: 100,
      fechaLimite: '2025-08-15',
      completada: true,
      fechaCreacion: '2024-10-10',
      fechaActualizacion: '2025-08-15'
    },
    {
      id: 'mock-meta-3',
      nombre: '[Auto] Auto',
      montoObjetivo: 5000,
      montoActual: 1700,
      porcentajeProgreso: 34,
      fechaLimite: '2026-03-10',
      completada: false,
      fechaCreacion: '2025-02-01',
      fechaActualizacion: '2025-02-01'
    },
    {
      id: 'mock-meta-4',
      nombre: '[Estudios] Estudios',
      montoObjetivo: 5100,
      montoActual: 1700,
      porcentajeProgreso: 33,
      fechaLimite: '2027-04-20',
      completada: false,
      fechaCreacion: '2025-01-20',
      fechaActualizacion: '2025-01-20'
    },
    {
      id: 'mock-meta-5',
      nombre: '[Tecnología] Nuevo Celular',
      montoObjetivo: 1500,
      montoActual: 850,
      porcentajeProgreso: 57,
      fechaLimite: '2026-05-05',
      completada: false,
      fechaCreacion: '2025-03-01',
      fechaActualizacion: '2025-03-01'
    },
    {
      id: 'mock-meta-6',
      nombre: '[Otros] Muebles',
      montoObjetivo: 2500,
      montoActual: 250,
      porcentajeProgreso: 10,
      fechaLimite: '2025-09-20',
      completada: false,
      fechaCreacion: '2024-12-01',
      fechaActualizacion: '2024-12-01'
    }
  ];

  ngOnInit(): void {
    const hoy = new Date();
    this.fechaMinima = hoy.toISOString().split('T')[0];

    this.inicializarFormulario();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.modoEdicion = true;
        this.metaId = id;
        this.cargarMetaParaEditar(id);
      }
    });
  }

  inicializarFormulario(): void {
    const hoy = new Date();
    const unMesDespues = new Date(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());
    const fechaDefecto = unMesDespues.toISOString().split('T')[0];

    this.formulario = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      categoria: ['Viaje', Validators.required],
      montoObjetivo: [null, [Validators.required, Validators.min(1.00)]],
      montoActual: [0.00, [Validators.required, Validators.min(0.00)]],
      fechaLimite: [fechaDefecto, [Validators.required, this.validarFechaFutura.bind(this)]]
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

  cargarMetaParaEditar(id: string): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.metasService.obtenerMeta(id).subscribe({
      next: (meta) => {
        this.completarFormularioConMeta(meta);
        this.cargando.set(false);
      },
      error: () => {
        // Fallback a localStorage o mock iniciales
        const localMetasStr = localStorage.getItem('luka_mock_metas');
        let listaMetas = this.mockMetasIniciales;
        if (localMetasStr) {
          try {
            listaMetas = JSON.parse(localMetasStr);
          } catch (e) {
            console.error('Error al parsear luka_mock_metas de localStorage', e);
          }
        }
        
        const mock = listaMetas.find(m => m.id === id);
        if (mock) {
          this.completarFormularioConMeta(mock);
        } else {
          this.errorMensaje.set('No se pudo encontrar la meta seleccionada en el sistema.');
        }
        this.cargando.set(false);
      }
    });
  }

  completarFormularioConMeta(meta: RespuestaMetaAhorro): void {
    const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
    this.formulario.patchValue({
      nombre: datosVisuales.nombre,
      categoria: datosVisuales.categoria || 'Otros',
      montoObjetivo: meta.montoObjetivo,
      montoActual: meta.montoActual,
      fechaLimite: meta.fechaLimite
    });
  }

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

  setFechaRapida(meses: number): void {
    const hoy = new Date();
    const nuevaFecha = new Date(hoy.getFullYear(), hoy.getMonth() + meses, hoy.getDate());
    const fechaStr = nuevaFecha.toISOString().split('T')[0];
    this.formulario.get('fechaLimite')?.setValue(fechaStr);
  }

  seleccionarCategoriaForm(catId: string): void {
    this.formulario.get('categoria')?.setValue(catId);
  }

  obtenerColorProgreso(pct: number): string {
    if (pct >= 100) return '#22c55e';
    return '#5b6af0';
  }

  getAhorroSugeridoInfo(): { cuota: number; meses: number } {
    if (!this.formulario) return { cuota: 0, meses: 0 };
    
    const obj = this.formulario.get('montoObjetivo')?.value || 0;
    const act = this.formulario.get('montoActual')?.value || 0;
    const fecha = this.formulario.get('fechaLimite')?.value;

    if (!fecha || obj <= 0) return { cuota: 0, meses: 0 };

    const limite = new Date(fecha + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    let meses = (limite.getFullYear() - hoy.getFullYear()) * 12 + (limite.getMonth() - hoy.getMonth());
    
    if (meses <= 0) {
      const dias = Math.ceil((limite.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
      if (dias <= 0) return { cuota: 0, meses: 0 };
      return { cuota: Math.max(0, obj - act), meses: 1 };
    }

    const cuota = Math.max(0, (obj - act) / meses);
    return { cuota, meses };
  }

  guardarMeta(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const formVal = this.formulario.value;
    const nombreConPrefijo = `[${formVal.categoria}] ${formVal.nombre}`;

    const payload: SolicitudMetaAhorro = {
      nombre: nombreConPrefijo,
      montoObjetivo: formVal.montoObjetivo,
      montoActual: formVal.montoActual ?? 0,
      fechaLimite: formVal.fechaLimite
    };

    if (this.modoEdicion && this.metaId) {
      // Flujo de edición: eliminar + recrear en API, o mock offline
      this.metasService.eliminarMeta(this.metaId).subscribe({
        next: () => {
          this.metasService.crearMeta(payload).subscribe({
            next: () => {
              this.exitoMensaje.set(`Meta "${formVal.nombre}" actualizada con éxito.`);
              this.finalizarConExito();
            },
            error: (err) => {
              console.error('Error al recrear meta editada:', err);
              this.errorMensaje.set('No se pudo guardar la meta editada en el servidor.');
              this.cargando.set(false);
            }
          });
        },
        error: () => {
          // Edición offline/mock
          this.actualizarMockLocalmente(this.metaId!, payload);
          this.exitoMensaje.set(`Meta "${formVal.nombre}" actualizada con éxito (Modo Pruebas).`);
          this.finalizarConExito();
        }
      });
    } else {
      // Flujo de creación
      this.metasService.crearMeta(payload).subscribe({
        next: (nuevaMeta) => {
          const datosVisuales = this.obtenerCategoriaYNombre(nuevaMeta.nombre);
          this.exitoMensaje.set(`Meta "${datosVisuales.nombre}" creada con éxito.`);
          this.finalizarConExito();
        },
        error: () => {
          // Creación offline/mock
          this.crearMockLocalmente(payload);
          this.exitoMensaje.set(`Meta "${formVal.nombre}" creada con éxito (Modo Pruebas).`);
          this.finalizarConExito();
        }
      });
    }
  }

  private finalizarConExito(): void {
    this.cargando.set(false);
    setTimeout(() => {
      this.router.navigate(['/metas']);
    }, 1500);
  }

  private obtenerListaMockActual(): RespuestaMetaAhorro[] {
    const localMetasStr = localStorage.getItem('luka_mock_metas');
    if (localMetasStr) {
      try {
        return JSON.parse(localMetasStr);
      } catch (e) {
        console.error(e);
      }
    }
    return [...this.mockMetasIniciales];
  }

  private guardarListaMockActual(lista: RespuestaMetaAhorro[]): void {
    localStorage.setItem('luka_mock_metas', JSON.stringify(lista));
  }

  private crearMockLocalmente(payload: SolicitudMetaAhorro): void {
    const lista = this.obtenerListaMockActual();
    const nuevoMock: RespuestaMetaAhorro = {
      id: 'mock-meta-' + (lista.length + 1) + '-' + Math.random().toString(36).substring(2, 6),
      nombre: payload.nombre,
      montoObjetivo: payload.montoObjetivo,
      montoActual: payload.montoActual || 0,
      porcentajeProgreso: payload.montoObjetivo > 0 ? ((payload.montoActual || 0) / payload.montoObjetivo) * 100 : 0,
      fechaLimite: payload.fechaLimite,
      completada: false,
      fechaCreacion: new Date().toISOString(),
      fechaActualizacion: new Date().toISOString()
    };
    lista.unshift(nuevoMock);
    this.guardarListaMockActual(lista);
  }

  private actualizarMockLocalmente(id: string, payload: SolicitudMetaAhorro): void {
    let lista = this.obtenerListaMockActual();
    lista = lista.map(i => {
      if (i.id === id) {
        return {
          ...i,
          nombre: payload.nombre,
          montoObjetivo: payload.montoObjetivo,
          montoActual: payload.montoActual ?? 0,
          porcentajeProgreso: payload.montoObjetivo > 0 ? ((payload.montoActual ?? 0) / payload.montoObjetivo) * 100 : 0,
          fechaLimite: payload.fechaLimite,
          fechaActualizacion: new Date().toISOString()
        };
      }
      return i;
    });
    this.guardarListaMockActual(lista);
  }
}
