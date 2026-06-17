import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { PerfilFinanciero } from './perfil-financiero';
import { PerfilFinancieroService } from './services/perfil-financiero.service';
import { AppEventBus } from '../../../core/services/app-event-bus.service';

describe('PerfilFinanciero', () => {
  let component: PerfilFinanciero;
  let fixture: ComponentFixture<PerfilFinanciero>;
  let mockService: any;
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
      progresoLogros: signal({ desbloqueados: 0, total: 0 }),
      capacidadAhorro: signal(null),
      variacionSalud: signal(null),
      variacionAhorro: signal(null),
      nombreMesAnterior: signal(''),
      logrosVisibles: signal([]),
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
      configurando: signal(false),
      modalPlanesAbierto: signal(false),
      comprandoPlan: signal(false),
      exportarPdf: jasmine.createSpy('exportarPdf'),
      abrirModalConfig: jasmine.createSpy('abrirModalConfig'),
      abrirModalPlanes: jasmine.createSpy('abrirModalPlanes'),
      formatMoneda: (v: number) => '0.00'
    };

    mockEventBus = {
      on: jasmine.createSpy('on').and.returnValue(of())
    };

    await TestBed.configureTestingModule({
      imports: [PerfilFinanciero],
      providers: [
        { provide: AppEventBus, useValue: mockEventBus }
      ]
    })
    .overrideComponent(PerfilFinanciero, {
      set: {
        providers: [
          { provide: PerfilFinancieroService, useValue: mockService }
        ]
      }
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
