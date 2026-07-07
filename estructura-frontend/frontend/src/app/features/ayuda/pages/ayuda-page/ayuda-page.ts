import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AyudaService, RespuestaSoporte } from '../../../../core/services/ayuda.service';

export type AyudaTab = 'centro' | 'soporte' | 'reporte';

interface ContactoMock {
  tipo: string;
  valor: string;
  horario: string;
  badge: string;
  badgeClass: string;
  iconoClass: string;
  cardClass: string;
  iconWrapClass: string;
  ctaClass: string;
  textoBoton: string;
  href: string;
}

interface FaqMock {
  id: number;
  pregunta: string;
  respuesta: string;
  categoria: string;
  iconoClass: string;
  abierta: boolean;
}

@Component({
  selector: 'app-ayuda-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './ayuda-page.html',
  styleUrl: './ayuda-page.scss'
})
export class AyudaPageComponent implements OnInit {

  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb     = inject(FormBuilder);
  private readonly ayudaService = inject(AyudaService);

  // ── Tabs ──────────────────────────────────────────────────────────────────
  tabActiva = signal<AyudaTab>('centro');

  readonly tabs = [
    { id: 'centro'  as AyudaTab, label: 'Centro de ayuda',     icon: 'fa-solid fa-circle-question' },
    { id: 'soporte' as AyudaTab, label: 'Contactar soporte',   icon: 'fa-solid fa-headset' },
    { id: 'reporte' as AyudaTab, label: 'Reportar un problema',icon: 'fa-solid fa-triangle-exclamation' },
  ];

  // ── Formulario Contactar Soporte ──────────────────────────────────────────
  formContacto!: FormGroup;
  enviandoContacto = signal(false);
  exitoContacto    = signal(false);
  errorContacto    = signal('');

  readonly categoriasContacto = ['Cuenta', 'Pagos', 'Límites', 'Seguridad', 'IA', 'Otro'];

  // ── Formulario Reportar Problema ──────────────────────────────────────────
  formReporte!: FormGroup;
  enviandoReporte = signal(false);
  exitoReporte    = signal(false);
  errorReporte    = signal('');

  readonly seccionesProblema = [
    'Dashboard', 'Movimientos', 'Presupuestos', 'Reportes', 'IA', 'Configuración', 'Otro'
  ];

  readonly ejemplosProblema = [
    'No puedo iniciar sesión',
    'El pago fue rechazado',
    'No aparece mi transferencia',
    'Error al generar reportes',
    'La aplicación está lenta',
  ];

  readonly prioridades = [
    { value: 'baja',  label: 'Baja',  emoji: '🟢', desc: 'No impide utilizar la aplicación.' },
    { value: 'media', label: 'Media', emoji: '🟡', desc: 'Afecta una funcionalidad importante.' },
    { value: 'alta',  label: 'Alta',  emoji: '🔴', desc: 'Impide completar una operación o función principal.' },
  ];

  // ── Archivo adjunto ────────────────────────────────────────────────────────
  archivoAdjunto = signal<File | null>(null);
  previewUrl     = signal<string | null>(null);
  errorArchivo   = signal('');

  // ── FAQ ───────────────────────────────────────────────────────────────────
  busqueda: string = '';
  categoriaActiva: string = 'todos';

  readonly categorias = [
    { id: 'todos',     label: 'Todos',     icono: 'fa-solid fa-border-all' },
    { id: 'cuenta',    label: 'Cuenta',    icono: 'fa-solid fa-user' },
    { id: 'pagos',     label: 'Pagos',     icono: 'fa-solid fa-credit-card' },
    { id: 'limites',   label: 'Límites',   icono: 'fa-solid fa-shield-halved' },
    { id: 'seguridad', label: 'Seguridad', icono: 'fa-solid fa-lock' },
  ];

  readonly canalesContacto: ContactoMock[] = [
    {
      tipo: 'Correo Electrónico', valor: 'soporte@luka.pe',
      horario: 'Respuesta en 24 hrs', badge: '● En línea', badgeClass: 'badge-online',
      iconoClass: 'fa-solid fa-envelope', cardClass: 'card-email', iconWrapClass: 'icon-email',
      ctaClass: 'cta-email', textoBoton: 'Enviar correo', href: 'mailto:soporte@luka.pe'
    },
    {
      tipo: 'WhatsApp', valor: '923 480 568',
      horario: 'Lun – Vie, 9am – 6pm', badge: '● Rápido', badgeClass: 'badge-fast',
      iconoClass: 'fa-brands fa-whatsapp', cardClass: 'card-whatsapp', iconWrapClass: 'icon-wa',
      ctaClass: 'cta-wa', textoBoton: 'Chatear ahora', href: 'https://wa.me/51923480568?text=Hola,%20soy%20usuario%20de%20LukaApp.%20Necesito%20ayuda%20con%20la%20siguiente%20consulta:'
    },
    {
      tipo: 'Teléfono', valor: '01 800 000 (Lima)',
      horario: 'Lun – Vie, 9am – 5pm', badge: 'Horario', badgeClass: 'badge-horario',
      iconoClass: 'fa-solid fa-phone', cardClass: 'card-telefono', iconWrapClass: 'icon-tel',
      ctaClass: 'cta-tel', textoBoton: 'Llamar ahora', href: 'tel:+5101800000'
    }
  ];

  preguntasFrecuentes: FaqMock[] = [
    { id: 2, categoria: 'cuenta',    iconoClass: 'fa-solid fa-key',            abierta: false, pregunta: '¿Cómo restablezco mi contraseña?',                 respuesta: 'En la pantalla de inicio de sesión, haz clic en "¿Olvidaste tu contraseña?". Ingresa tu correo y recibirás un enlace para crear una nueva contraseña en menos de 5 minutos. Si no llega, revisa tu carpeta de spam.' },
    { id: 5, categoria: 'limites',   iconoClass: 'fa-solid fa-shield-halved',  abierta: false, pregunta: '¿Cómo configuro mi límite de gasto mensual?',      respuesta: 'Desde el menú principal ve a Límites de Gasto. Define el monto máximo mensual con el slider o ingresándolo manualmente. Elige el porcentaje de alerta y haz clic en "Guardar Límite".' },
    { id: 7, categoria: 'seguridad', iconoClass: 'fa-solid fa-lock',           abierta: false, pregunta: '¿Mis datos financieros están seguros en Luka?',    respuesta: 'Sí. Luka utiliza encriptación AES-256 para proteger tu información financiera. Nunca almacenamos los datos completos de tu tarjeta; usamos tokenización. Además puedes activar 2FA en Configuración → Seguridad.' },
    
    // Nuevas preguntas solicitadas
    { id: 9, categoria: 'cuenta',    iconoClass: 'fa-solid fa-arrow-trend-up', abierta: false, pregunta: '¿Cómo registro un ingreso?',                        respuesta: 'Desde la sección Ingresos, presiona "Nuevo ingreso", completa la información solicitada y guarda el registro.' },
    { id: 10, categoria: 'pagos',    iconoClass: 'fa-solid fa-arrow-trend-down',abierta: false, pregunta: '¿Cómo registro un gasto?',                         respuesta: 'Ingresa a Gastos, selecciona "Nuevo gasto", elige la categoría correspondiente y guarda la información.' },
    { id: 11, categoria: 'limites',  iconoClass: 'fa-solid fa-scale-balanced', abierta: false, pregunta: '¿Cómo crear un presupuesto?',                      respuesta: 'Dirígete a Presupuestos, crea un nuevo presupuesto indicando el monto y el período que deseas controlar.' },
    { id: 12, categoria: 'cuenta',   iconoClass: 'fa-solid fa-bullseye',       abierta: false, pregunta: '¿Cómo crear una meta de ahorro?',                  respuesta: 'Accede a Metas, define el monto objetivo y la fecha estimada para alcanzarlo.' },
    { id: 13, categoria: 'pagos',    iconoClass: 'fa-solid fa-receipt',        abierta: false, pregunta: '¿Cómo registrar una suscripción?',                 respuesta: 'Ve a Suscripciones, selecciona "Agregar suscripción", elige el servicio o escríbelo manualmente y registra el costo y la frecuencia de pago.' },
    { id: 14, categoria: 'cuenta',   iconoClass: 'fa-solid fa-scale-unbalanced',abierta: false, pregunta: '¿Qué significa el Balance Neto?',                 respuesta: 'Es la diferencia entre todos tus ingresos y gastos registrados durante el período seleccionado.' }
  ];

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.formContacto = this.fb.group({
      nombre:    ['', [Validators.required]],
      correo:    ['', [Validators.required, Validators.email]],
      categoria: ['', Validators.required],
      asunto:    ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      mensaje:   ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]],
    });

    this.formReporte = this.fb.group({
      descripcionCorta: ['', [Validators.required, Validators.minLength(5)]],
      prioridad:        ['media', Validators.required],
      seccion:          ['', Validators.required],
      descripcion:      ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      queHacias:        ['', Validators.maxLength(500)],
    });

    // Leer query param para activar la tab correcta
    this.route.queryParams.subscribe(params => {
      const tab = params['tab'] as AyudaTab;
      if (tab === 'soporte' || tab === 'reporte') {
        this.tabActiva.set(tab);
      } else {
        this.tabActiva.set('centro');
      }
    });
  }

  // ── Navegación de tabs ────────────────────────────────────────────────────
  cambiarTab(tab: AyudaTab): void {
    this.tabActiva.set(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: tab === 'centro' ? {} : { tab },
      replaceUrl: true,
    });
  }

  /** Desde el CTA del centro de ayuda → navega a Contactar soporte */
  irAContactar(): void {
    this.cambiarTab('soporte');
  }

  // ── Contactar soporte ─────────────────────────────────────────────────────
  enviarContacto(): void {
    if (this.formContacto.invalid) {
      this.formContacto.markAllAsTouched();
      return;
    }
    this.enviandoContacto.set(true);
    this.errorContacto.set('');
    
    const dto = this.formContacto.value;
    const formData = new FormData();
    formData.append('solicitud', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    
    // Si hubiera un adjunto en este form en el futuro, se añadiría aquí.
    
    this.ayudaService.enviarContacto(formData).subscribe({
      next: (res: RespuestaSoporte) => {
        this.enviandoContacto.set(false);
        this.exitoContacto.set(true);
        this.formContacto.reset();
      },
      error: (err: any) => {
        this.enviandoContacto.set(false);
        this.errorContacto.set(err.error?.mensaje || 'Ocurrió un error al enviar tu consulta. Por favor, intenta de nuevo.');
        console.error('Error enviando contacto:', err);
      }
    });
  }

  resetContacto(): void {
    this.exitoContacto.set(false);
    this.formContacto.reset();
  }

  // ── Reportar problema ─────────────────────────────────────────────────────
  enviarReporte(): void {
    if (this.formReporte.invalid) {
      this.formReporte.markAllAsTouched();
      return;
    }
    this.enviandoReporte.set(true);
    this.errorReporte.set('');

    const dto = this.formReporte.value;
    const formData = new FormData();
    formData.append('solicitud', new Blob([JSON.stringify(dto)], { type: 'application/json' }));

    const file = this.archivoAdjunto();
    if (file) {
      formData.append('adjunto', file);
    }

    this.ayudaService.enviarReporte(formData).subscribe({
      next: (res: RespuestaSoporte) => {
        this.enviandoReporte.set(false);
        this.exitoReporte.set(true);
        this.formReporte.reset();
        this.formReporte.patchValue({ prioridad: 'media' });
        this.archivoAdjunto.set(null);
        this.previewUrl.set(null);
      },
      error: (err: any) => {
        this.enviandoReporte.set(false);
        this.errorReporte.set(err.error?.mensaje || 'Ocurrió un error al enviar el reporte. Por favor, intenta de nuevo.');
        console.error('Error enviando reporte:', err);
      }
    });
  }

  resetReporte(): void {
    this.exitoReporte.set(false);
    this.formReporte.reset();
    this.formReporte.patchValue({ prioridad: 'media' });
  }

  usarEjemplo(ejemplo: string): void {
    this.formReporte.patchValue({ descripcionCorta: ejemplo });
  }

  // ── Archivo adjunto ────────────────────────────────────────────────────────
  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    this.errorArchivo.set('');
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.errorArchivo.set('Solo se permiten archivos JPG, PNG o WEBP.');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorArchivo.set('El archivo no debe superar los 5 MB.');
      input.value = '';
      return;
    }
    this.archivoAdjunto.set(file);
    const reader = new FileReader();
    reader.onload = e => this.previewUrl.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  eliminarArchivo(): void {
    this.archivoAdjunto.set(null);
    this.previewUrl.set(null);
    this.errorArchivo.set('');
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024)    return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  // ── FAQ ────────────────────────────────────────────────────────────────────
  get preguntasFiltradas(): FaqMock[] {
    const q = this.busqueda.toLowerCase().trim();
    return this.preguntasFrecuentes.filter(faq => {
      const matchCat   = this.categoriaActiva === 'todos' || faq.categoria === this.categoriaActiva;
      const matchQuery = !q
        || faq.pregunta.toLowerCase().includes(q)
        || faq.respuesta.toLowerCase().includes(q);
      return matchCat && matchQuery;
    });
  }

  seleccionarCategoria(id: string): void { this.categoriaActiva = id; }

  conmutarFaq(id: number): void {
    this.preguntasFrecuentes = this.preguntasFrecuentes.map(faq =>
      faq.id === id ? { ...faq, abierta: !faq.abierta } : { ...faq, abierta: false }
    );
  }

  // ── Getters de validación ─────────────────────────────────────────────────
  get fContactoCategoria() { return this.formContacto.get('categoria')!; }
  get fContactoAsunto()    { return this.formContacto.get('asunto')!; }
  get fContactoMensaje()   { return this.formContacto.get('mensaje')!; }
  get fReporteDescCorta()  { return this.formReporte.get('descripcionCorta')!; }
  get fReportePrioridad()  { return this.formReporte.get('prioridad')!; }
  get fReporteSeccion()    { return this.formReporte.get('seccion')!; }
  get fReporteDesc()       { return this.formReporte.get('descripcion')!; }
  get fReporteQueHacias()  { return this.formReporte.get('queHacias')!; }
}