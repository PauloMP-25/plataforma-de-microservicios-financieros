import { Component, OnInit, signal, computed, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { ClientePerfilService } from '../../../core/services/cliente-perfil.service';
import { DashboardStateService } from '../../../core/services/dashboard-state.service';
import { SolicitudDatosPersonales } from '../../../core/models/cliente/perfil-cliente.model';

@Component({
    selector: 'app-modal-completar-perfil',
    standalone: true,
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    templateUrl: './completar-perfil.html',
    styleUrl: './completar-perfil.scss'
})
export class ModalCompletarPerfil implements OnInit {
    private readonly fb = inject(FormBuilder);
    private readonly http = inject(HttpClient);
    private readonly authService = inject(AuthService);
    private readonly perfilService = inject(ClientePerfilService);
    private readonly dashboardState = inject(DashboardStateService);

    @Output() completado = new EventEmitter<void>();

    perfilForm!: FormGroup;

    // Wizard Step state (1: Verification, 2: Identity, 3: Location)
    readonly pasoActual = signal(1);

    // States
    readonly cargandoDni = signal(false);
    readonly guardando = signal(false);
    readonly errorMsg = signal<string | null>(null);
    readonly correoUsuario = signal<string>('');

    // Verification method selector ('CORREO' = email-verified register, 'CELULAR' = phone-verified register)
    readonly metodoRegistro = signal<'CORREO' | 'CELULAR'>('CORREO');

    // Phone OTP verification states
    readonly cargandoOtp = signal(false);
    readonly telefonoEnviado = signal(false);
    readonly telefonoVerificado = signal(false);
    readonly errorOtp = signal('');

    // Email OTP verification states
    readonly cargandoOtpCorreo = signal(false);
    readonly correoEnviado = signal(false);
    readonly correoVerificado = signal(false);
    readonly errorOtpCorreo = signal('');

    // Helper validation checks for each step
    readonly paso1Valido = computed(() => {
        if (this.metodoRegistro() === 'CORREO') {
            return this.telefonoVerificado() && this.perfilForm.get('telefono')?.valid;
        } else {
            return this.correoVerificado() && this.perfilForm.get('correo')?.valid;
        }
    });

    readonly paso2Valido = computed(() => {
        const dniVal = this.perfilForm.get('dni')?.valid;
        const nombresVal = this.perfilForm.get('nombres')?.valid;
        const apellidosVal = this.perfilForm.get('apellidos')?.valid;
        const generoVal = this.perfilForm.get('genero')?.valid;
        const edadVal = this.perfilForm.get('edad')?.valid;
        return !!(dniVal && nombresVal && apellidosVal && generoVal && edadVal);
    });

    readonly paso3Valido = computed(() => {
        const paisVal = this.perfilForm.get('pais')?.valid;
        const ciudadVal = this.perfilForm.get('ciudad')?.valid;
        return !!(paisVal && ciudadVal);
    });

    ngOnInit(): void {
        const usuario = this.authService.usuario();
        this.correoUsuario.set(usuario?.nombreUsuario || '');

        // Determine if they registered using email or phone
        const esCorreo = this.correoUsuario().includes('@');
        if (esCorreo) {
            this.metodoRegistro.set('CORREO');
            this.correoVerificado.set(true);
            this.telefonoVerificado.set(false);
        } else {
            this.metodoRegistro.set('CELULAR');
            this.correoVerificado.set(false);
            this.telefonoVerificado.set(true);
        }

        this.perfilForm = this.fb.group({
            correo: ['', [Validators.required, Validators.email]],
            dni: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
            nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
            apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
            telefono: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
            genero: ['', [Validators.required]],
            pais: ['', [Validators.required, Validators.maxLength(20)]],
            ciudad: ['', [Validators.required, Validators.maxLength(100)]],
            edad: ['', [Validators.required, Validators.min(18), Validators.max(120)]]
        });

        // Initialize values based on verification method
        if (this.metodoRegistro() === 'CORREO') {
            this.perfilForm.patchValue({ correo: this.correoUsuario() });
            this.perfilForm.get('correo')?.disable();
        } else {
            // Logged in with phone number (their identifier is in correoUsuario)
            this.perfilForm.patchValue({ telefono: this.correoUsuario() });
            this.perfilForm.get('telefono')?.disable();
        }

        // Listen to DNI input to query API Peru DNI endpoint
        this.perfilForm.get('dni')?.valueChanges.subscribe(val => {
            if (val && val.length === 8 && /^[0-9]{8}$/.test(val)) {
                this.consultarDniApiPeru(val);
            }
        });
    }

    irSiguiente(): void {
        if (this.pasoActual() === 1 && this.paso1Valido()) {
            this.pasoActual.set(2);
        } else if (this.pasoActual() === 2 && this.paso2Valido()) {
            this.pasoActual.set(3);
        }
    }

    irAtras(): void {
        if (this.pasoActual() > 1) {
            this.pasoActual.update(p => p - 1);
        }
    }

    // Consultar DNI real usando API Perú con fallback a simulación local
    consultarDniApiPeru(dni: string): void {
        this.cargandoDni.set(true);
        this.errorMsg.set(null);

        const token = localStorage.getItem('apiperu_token') || 'YOUR_API_PERU_TOKEN';
        const url = `https://apiperu.dev/api/dni/${dni}?api_token=${token}`;

        this.http.get<any>(url).subscribe({
            next: (res) => {
                this.cargandoDni.set(false);
                if (res && res.success && res.data) {
                    const nombres = res.data.nombres;
                    const apellidos = `${res.data.apellido_paterno || ''} ${res.data.apellido_materno || ''}`.trim();
                    this.perfilForm.patchValue({ nombres, apellidos });
                } else {
                    this.simularConsultaDNI(dni);
                }
            },
            error: (err) => {
                console.warn('Error al conectar con API Perú, usando simulador local:', err);
                this.simularConsultaDNI(dni);
            }
        });
    }

    // Simulación de API DNI (como RENIEC de Perú)
    simularConsultaDNI(dni: string): void {
        this.cargandoDni.set(true);
        this.errorMsg.set(null);

        // Mock API delay
        setTimeout(() => {
            this.cargandoDni.set(false);

            if (dni === '00000000') {
                this.errorMsg.set('DNI no encontrado o inválido.');
                return;
            }

            const seed = parseInt(dni.substring(5), 10) || 123;
            const nombresMock = [
                'Juan Carlos', 'María Fernanda', 'Luis Alberto', 'Ana Beatriz',
                'Jorge Eduardo', 'Sofía Irene', 'Carlos Miguel', 'Diana Patricia'
            ];
            const apellidosMock = [
                'Pérez Rodríguez', 'Gómez Sánchez', 'Quispe Mamani', 'Flores Díaz',
                'Vargas Llosa', 'Mendoza Castillo', 'Alvarado Ruiz', 'Romero Cruz'
            ];

            const indexNombres = seed % nombresMock.length;
            const indexApellidos = (seed + 3) % apellidosMock.length;

            this.perfilForm.patchValue({
                nombres: nombresMock[indexNombres],
                apellidos: apellidosMock[indexApellidos]
            });
        }, 1000);
    }

    // Solicitar verificación de teléfono simulada (OTP)
    solicitarVerificacionTelefono(): void {
        const tel = this.perfilForm.get('telefono')?.value || '';
        const digitos = tel.replace(/\D/g, '');

        if (digitos.length !== 9) {
            this.perfilForm.get('telefono')?.setErrors({ invalidPeruPhone: true });
            return;
        }

        this.cargandoOtp.set(true);
        this.errorOtp.set('');

        setTimeout(() => {
            this.cargandoOtp.set(false);
            this.telefonoEnviado.set(true);
        }, 800);
    }

    // Validar código OTP simulado (código exitoso: '1234')
    validarCodigoTelefono(codigo: string): void {
        if (codigo.length < 4) {
            this.errorOtp.set('');
            return;
        }

        if (codigo === '1234') {
            this.telefonoVerificado.set(true);
            this.errorOtp.set('');
            this.perfilForm.get('telefono')?.disable();
        } else {
            this.errorOtp.set('El código ingresado es incorrecto. Intente con 1234.');
        }
    }

    // Solicitar verificación de correo simulada (OTP)
    solicitarVerificacionCorreo(): void {
        const email = this.perfilForm.get('correo')?.value || '';
        if (!email || !email.includes('@')) {
            this.perfilForm.get('correo')?.setErrors({ invalidEmail: true });
            return;
        }

        this.cargandoOtpCorreo.set(true);
        this.errorOtpCorreo.set('');

        setTimeout(() => {
            this.cargandoOtpCorreo.set(false);
            this.correoEnviado.set(true);
        }, 800);
    }

    // Validar código OTP de correo simulado (código exitoso: '1234')
    validarCodigoCorreo(codigo: string): void {
        if (codigo.length < 4) {
            this.errorOtpCorreo.set('');
            return;
        }

        if (codigo === '1234') {
            this.correoVerificado.set(true);
            this.errorOtpCorreo.set('');
            this.perfilForm.get('correo')?.disable();
        } else {
            this.errorOtpCorreo.set('El código ingresado es incorrecto. Intente con 1234.');
        }
    }

    onSubmit(): void {
        if (this.perfilForm.invalid || this.guardando()) {
            return;
        }

        const usuario = this.authService.usuario();
        if (!usuario || !usuario.id) {
            this.errorMsg.set('No se pudo identificar la sesión del usuario.');
            return;
        }

        const rawForm = this.perfilForm.getRawValue();
        const edad = Number(rawForm.edad);

        if (Number.isNaN(edad) || edad < 18) {
            this.errorMsg.set('Debes tener al menos 18 años para completar tu perfil.');
            return;
        }

        this.guardando.set(true);
        this.errorMsg.set(null);

        const payload: SolicitudDatosPersonales = {
            dni: rawForm.dni,
            nombres: rawForm.nombres,
            apellidos: rawForm.apellidos,
            genero: rawForm.genero,
            edad: edad,
            telefono: rawForm.telefono,
            pais: rawForm.pais,
            ciudad: rawForm.ciudad
        };

        this.perfilService.actualizarPerfil(usuario.id, payload).subscribe({
            next: () => {
                this.dashboardState.invalidarCache();
                this.guardando.set(false);
                this.completado.emit();
            },
            error: (err: any) => {
                console.error('Error al guardar datos personales:', err);
                this.errorMsg.set(
                    err?.error?.mensaje ||
                    err?.error?.error ||
                    'Hubo un problema al guardar tus datos. Inténtalo de nuevo.'
                );
                this.guardando.set(false);
            }
        });
    }
}