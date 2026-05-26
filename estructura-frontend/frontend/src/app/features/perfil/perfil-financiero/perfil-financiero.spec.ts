import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PerfilFinanciero } from './perfil-financiero';

describe('PerfilFinanciero', () => {
  let component: PerfilFinanciero;
  let fixture: ComponentFixture<PerfilFinanciero>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilFinanciero]
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
