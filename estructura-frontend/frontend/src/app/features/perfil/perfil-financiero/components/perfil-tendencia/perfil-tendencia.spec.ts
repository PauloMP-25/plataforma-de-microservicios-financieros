import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { PerfilTendencia } from './perfil-tendencia';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

describe('PerfilTendencia', () => {
  let component: PerfilTendencia;
  let fixture: ComponentFixture<PerfilTendencia>;
  let mockService: any;

  beforeEach(async () => {
    mockService = {
      tendencia: signal([
        { mes: 'Ene', anio: 2025, ingresos: 1000, gastos: 800, ahorro: 200 }
      ]),
      filtroTendencia: signal(6),
      tendenciaNormalizada: signal({
        ingresos: 'M 10,140 L 490,140',
        gastos: 'M 10,150 L 490,150',
        puntos: [{ mes: 'Ene', anio: 2025, ingresos: 1000, gastos: 800, ahorro: 200 }],
        maxVal: 1000,
        mapY: (v: number) => 140,
        mapX: (i: number) => 10,
        h: 160,
        w: 500
      }),
      cambiarTendencia: jasmine.createSpy('cambiarTendencia'),
      formatMoneda: (v: number) => '1,000.00'
    };

    await TestBed.configureTestingModule({
      imports: [PerfilTendencia],
      providers: [
        { provide: PerfilFinancieroService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilTendencia);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
