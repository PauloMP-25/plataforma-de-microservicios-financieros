import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { PresupuestosPage } from './presupuestos-page';
import { PresupuestoService } from '../../../../core/services/presupuesto.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { PresupuestoDTO } from '../../../../core/models/financiero/presupuesto.model';

describe('PresupuestosPage', () => {
  let component: PresupuestosPage;
  let fixture: ComponentFixture<PresupuestosPage>;
  let mockPresupuestoService: jasmine.SpyObj<PresupuestoService>;
  let mockFinancieroService: jasmine.SpyObj<FinancieroService>;

  const mockPresupuestoActivo: PresupuestoDTO = {
    id: '1',
    montoLimite: 2000,
    porcentajeAlerta: 80,
    fechaInicio: '2026-06-01T00:00:00.000Z',
    fechaFin: '2026-06-30T00:00:00.000Z',
    activo: true,
    usuarioId: '10'
  };

  beforeEach(async () => {
    mockPresupuestoService = jasmine.createSpyObj('PresupuestoService', [
      'obtenerActivo',
      'listarHistorial',
      'crear',
      'actualizar',
      'eliminarActivo'
    ]);
    mockFinancieroService = jasmine.createSpyObj('FinancieroService', ['getResumen']);

    mockFinancieroService.getResumen.and.returnValue(of({ totalGastos: 1200 }));
    mockPresupuestoService.obtenerActivo.and.returnValue(of(mockPresupuestoActivo));
    mockPresupuestoService.listarHistorial.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PresupuestosPage, ReactiveFormsModule],
      providers: [
        { provide: PresupuestoService, useValue: mockPresupuestoService },
        { provide: FinancieroService, useValue: mockFinancieroService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PresupuestosPage);
    component = fixture.componentInstance;
  });

  it('debería crearse el componente', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('debería inicializar el formulario con valores por defecto', () => {
    fixture.detectChanges();
    expect(component.formulario).toBeDefined();
    expect(component.formulario.get('porcentajeAlerta')?.value).toBe(80);
  });

  it('debería cargar los datos correctamente en el ngOnInit', () => {
    fixture.detectChanges();
    expect(mockFinancieroService.getResumen).toHaveBeenCalled();
    expect(mockPresupuestoService.obtenerActivo).toHaveBeenCalled();
    expect(component.gastoTotalMes()).toBe(1200);
    expect(component.presupuestoActivo()).toEqual(mockPresupuestoActivo);
  });

  it('debería calcular el porcentaje de consumo e indicadores reactivos', () => {
    fixture.detectChanges();
    expect(component.porcentajeConsumo()).toBe(60);
    expect(component.margenDisponible()).toBe(800);
    expect(component.estadoAlerta()).toBe('seguro');
  });

  it('debería marcar el estado como alerta si supera el porcentaje definido', () => {
    mockFinancieroService.getResumen.and.returnValue(of({ totalGastos: 1700 }));
    fixture.detectChanges();
    expect(component.estadoAlerta()).toBe('alerta');
  });

  it('debería validar que la fecha fin no sea inferior a la fecha inicio', () => {
    fixture.detectChanges();
    component.formulario.patchValue({
      fechaInicio: '2026-06-10',
      fechaFin: '2026-06-05'
    });
    expect(component.formulario.errors?.['fechasInvalidas']).toBeTrue();
  });

  it('debería llamar al servicio crear cuando no hay un presupuesto activo', fakeAsync(() => {
    mockPresupuestoService.obtenerActivo.and.returnValue(of(null));
    mockPresupuestoService.crear.and.returnValue(of(mockPresupuestoActivo));
    fixture.detectChanges();

    component.formulario.patchValue({
      montoLimite: 2000,
      porcentajeAlerta: 80,
      fechaInicio: '2026-06-01',
      fechaFin: '2026-06-30'
    });

    component.guardarPresupuesto();
    tick();

    expect(mockPresupuestoService.crear).toHaveBeenCalled();
    expect(component.exitoMensaje()).toBe('Límite actualizado correctamente.');
  }));

  it('debería llamar al servicio eliminarActivo al confirmar la desactivación', fakeAsync(() => {
    mockPresupuestoService.eliminarActivo.and.returnValue(of(void 0));
    fixture.detectChanges();

    component.eliminarPresupuesto();
    tick();

    expect(mockPresupuestoService.eliminarActivo).toHaveBeenCalled();
    expect(component.presupuestoActivo()).toBeNull();
  }));
});