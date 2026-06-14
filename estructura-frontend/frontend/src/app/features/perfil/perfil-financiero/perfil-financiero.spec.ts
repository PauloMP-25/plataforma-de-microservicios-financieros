import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PerfilFinanciero } from './perfil-financiero';

describe('PerfilFinanciero', () => {
  let component: PerfilFinanciero;
  let fixture: ComponentFixture<PerfilFinanciero>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilFinanciero],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
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
