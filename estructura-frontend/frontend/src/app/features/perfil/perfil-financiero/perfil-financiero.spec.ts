import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { PerfilFinanciero } from './perfil-financiero';
import { PerfilFinancieroService } from './services/perfil-financiero.service';
import { PerfilLogrosService } from './services/perfil-logros.service';
import { PerfilWizardService } from './services/perfil-wizard.service';
import { PerfilReporteService } from './services/perfil-reporte.service';
import { AppEventBus } from '../../../core/services/app-event-bus.service';

describe('PerfilFinanciero', () => {
  let component: PerfilFinanciero;
  let fixture: ComponentFixture<PerfilFinanciero>;
  let mockService: any;
  let mockLogrosService: any;
  let mockWizardService: any;
  let mockReporteService: any;
  let mockEventBus: any;

  beforeEach(async () => {
    mockService = {
      cargarDatos: jasmine.createSpy('cargarDatos'),
      mesSeleccionado: signal(5),
      anioSeleccionado: signal(2025),
      mesesDisponibles: [
        { valor: 5, label: 'Mayo' }
      ],
      aniosDisponibles: [2025],
      cargando: signal(false),
      resumenActual: signal(null),
      auth: {
        esPremium: signal(false),
        esPro: signal(false)
      },
      indicesSalud: signal(null),
      capacidadAhorro: signal(null),
      variacionSalud: signal(null),
      variacionAhorro: signal(null),
      nombreMesAnterior: signal(''),
      mostrarTodosLogros: signal(false),
      tendencia: signal([]),
      filtroTendencia: signal(6),
      cambiarTendencia: jasmine.createSpy('cambiarTendencia'),
      tendenciaNormalizada: signal({
        ingresos: '',
        gastos: '',
        puntos: [],
        maxVal: 1,
        mapY: (v: number) => 0,
        mapX: (i: number) => 0,
        h: 160,
        w: 500
      }),
      modalPlanesAbierto: signal(false),
      comprandoPlan: signal(false),
      abrirModalPlanes: jasmine.createSpy('abrirModalPlanes')
    };

    mockLogrosService = {
      logrosVisibles: signal([]),
      logrosFinancieros: signal([]),
      progresoLogros: signal({ desbloqueados: 0, total: 0 })
    };

    mockWizardService = {
      abrirModalConfig: jasmine.createSpy('abrirModalConfig'),
      modalConfigAbierto: signal(false),
      pasoActual: signal(1),
      formConfig: signal({
        ocupacion: '',
        ingresoMensual: 0,
        estiloVida: 'MODERADO',
        tonoIA: 'AMIGABLE'
      }),
      erroresConfig: signal({}),
      mensajeConfig: signal(null),
      estiloVidaSliderVal: signal(2),
      configurando: signal(false)
    };

    mockReporteService = {
      exportarPdf: jasmine.createSpy('exportarPdf')
    };

    mockEventBus = {
      on: jasmine.createSpy('on').and.returnValue(of())
    };

    await TestBed.configureTestingModule({
      imports: [PerfilFinanciero, RouterTestingModule],
      providers: [
        { provide: AppEventBus, useValue: mockEventBus },
        { provide: PerfilFinancieroService, useValue: mockService },
        { provide: PerfilLogrosService, useValue: mockLogrosService },
        { provide: PerfilWizardService, useValue: mockWizardService },
        { provide: PerfilReporteService, useValue: mockReporteService }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PerfilFinanciero);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
