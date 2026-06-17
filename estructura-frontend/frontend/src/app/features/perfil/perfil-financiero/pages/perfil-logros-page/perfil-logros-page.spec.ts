import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { PerfilLogrosPage } from './perfil-logros-page';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

describe('PerfilLogrosPage', () => {
  let component: PerfilLogrosPage;
  let fixture: ComponentFixture<PerfilLogrosPage>;
  let mockService: any;

  beforeEach(async () => {
    mockService = {
      resumenActual: signal({}),
      logrosFinancieros: signal([
        { id: '1', titulo: 'Logro 1', descripcion: 'Desc 1', icono: 'fa-star', iconoColor: 'success', desbloqueado: true, progreso: 1, meta: 1, categoria: 'cat' },
        { id: '2', titulo: 'Logro 2', descripcion: 'Desc 2', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '3', titulo: 'Logro 3', descripcion: 'Desc 3', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '4', titulo: 'Logro 4', descripcion: 'Desc 4', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '5', titulo: 'Logro 5', descripcion: 'Desc 5', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '6', titulo: 'Logro 6', descripcion: 'Desc 6', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '7', titulo: 'Logro 7', descripcion: 'Desc 7', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '8', titulo: 'Logro 8', descripcion: 'Desc 8', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' },
        { id: '9', titulo: 'Logro 9', descripcion: 'Desc 9', icono: 'fa-star', iconoColor: 'success', desbloqueado: false, progreso: 0, meta: 1, categoria: 'cat' }
      ]),
      progresoLogros: signal({ desbloqueados: 1, total: 9 }),
      cargarDatos: jasmine.createSpy('cargarDatos')
    };

    await TestBed.configureTestingModule({
      imports: [PerfilLogrosPage, RouterTestingModule],
      providers: [
        { provide: PerfilFinancieroService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilLogrosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute totalPaginas correctly based on itemsPorPagina', () => {
    expect(component.totalPaginas()).toBe(2);
  });

  it('should paginated items correctly', () => {
    expect(component.logrosPaginados().length).toBe(8);
    component.siguientePagina();
    expect(component.paginaActual()).toBe(2);
    expect(component.logrosPaginados().length).toBe(1);
  });
});
