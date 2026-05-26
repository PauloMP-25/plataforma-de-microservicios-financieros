import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PerfilCliente } from './perfil-cliente';

describe('PerfilCliente', () => {
  let component: PerfilCliente;
  let fixture: ComponentFixture<PerfilCliente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilCliente]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PerfilCliente);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
