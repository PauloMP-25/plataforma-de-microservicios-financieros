import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { PerfilLogros } from './perfil-logros';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

describe('PerfilLogros', () => {
  let component: PerfilLogros;
  let fixture: ComponentFixture<PerfilLogros>;
  let mockService: any;

  beforeEach(async () => {
    mockService = {
      logrosVisibles: signal([
        { id: '1', titulo: 'Logro 1', descripcion: 'Desc', icono: 'fa-star', iconoColor: 'success', desbloqueado: true, progreso: 1, meta: 1, category: 'cat' }
      ]),
      mostrarTodosLogros: signal(false),
      toggleLogros: jasmine.createSpy('toggleLogros')
    };

    await TestBed.configureTestingModule({
      imports: [PerfilLogros, RouterTestingModule],
      providers: [
        { provide: PerfilFinancieroService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilLogros);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
