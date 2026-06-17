import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { PerfilWizardModal } from './perfil-wizard-modal';
import { PerfilWizardService } from '../../services/perfil-wizard.service';

describe('PerfilWizardModal', () => {
  let component: PerfilWizardModal;
  let fixture: ComponentFixture<PerfilWizardModal>;
  let mockWizardService: any;

  beforeEach(async () => {
    mockWizardService = {
      modalConfigAbierto: signal(true),
      pasoActual: signal(1),
      formConfig: signal({
        ocupacion: 'Ingeniero',
        ingresoMensual: 4500,
        estiloVida: 'MODERADO',
        tonoIA: 'AMIGABLE'
      }),
      erroresConfig: signal({}),
      mensajeConfig: signal(null),
      estiloVidaSliderVal: signal(2),
      configurando: signal(false),
      cerrarModalConfig: jasmine.createSpy('cerrarModalConfig'),
      actualizarCampoConfig: jasmine.createSpy('actualizarCampoConfig'),
      onEstiloVidaSliderChange: jasmine.createSpy('onEstiloVidaSliderChange'),
      seleccionarTonoIA: jasmine.createSpy('seleccionarTonoIA'),
      retrocederPaso: jasmine.createSpy('retrocederPaso'),
      avanzarPaso: jasmine.createSpy('avanzarPaso'),
      guardarConfiguracion: jasmine.createSpy('guardarConfiguracion')
    };

    await TestBed.configureTestingModule({
      imports: [PerfilWizardModal],
      providers: [
        { provide: PerfilWizardService, useValue: mockWizardService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilWizardModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
