import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContenedorAutenticacion } from './contenedor-autenticacion';

describe('ContenedorAutenticacion', () => {
  let component: ContenedorAutenticacion;
  let fixture: ComponentFixture<ContenedorAutenticacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContenedorAutenticacion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContenedorAutenticacion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
