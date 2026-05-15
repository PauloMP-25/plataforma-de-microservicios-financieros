import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Transacciones } from './transacciones';

describe('Transacciones', () => {
  let component: Transacciones;
  let fixture: ComponentFixture<Transacciones>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Transacciones]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Transacciones);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
