import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { PerfilPlanesModal } from './perfil-planes-modal';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

describe('PerfilPlanesModal', () => {
  let component: PerfilPlanesModal;
  let fixture: ComponentFixture<PerfilPlanesModal>;
  let mockService: any;

  beforeEach(async () => {
    mockService = {
      modalPlanesAbierto: signal(true),
      comprandoPlan: signal(false),
      cerrarModalPlanes: jasmine.createSpy('cerrarModalPlanes'),
      comprarPlan: jasmine.createSpy('comprarPlan')
    };

    await TestBed.configureTestingModule({
      imports: [PerfilPlanesModal],
      providers: [
        { provide: PerfilFinancieroService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilPlanesModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
