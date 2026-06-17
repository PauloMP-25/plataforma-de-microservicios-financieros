import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { PerfilKpis } from './perfil-kpis';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

describe('PerfilKpis', () => {
  let component: PerfilKpis;
  let fixture: ComponentFixture<PerfilKpis>;
  let mockService: any;

  beforeEach(async () => {
    mockService = {
      indicesSalud: signal({ score: 85, etiqueta: 'Excelente' }),
      variacionSalud: signal(5),
      nombreMesAnterior: signal('Abril 2025'),
      progresoLogros: signal({ desbloqueados: 3, total: 8 }),
      capacidadAhorro: signal(15.5),
      variacionAhorro: signal(2.1),
      colorIndice: (score: number) => 'success'
    };

    await TestBed.configureTestingModule({
      imports: [PerfilKpis],
      providers: [
        { provide: PerfilFinancieroService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilKpis);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
