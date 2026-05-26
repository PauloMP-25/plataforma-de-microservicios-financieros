import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Historial } from './historial';

describe('Historial', () => {
  let component: Historial;
  let fixture: ComponentFixture<Historial>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Historial]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Historial);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
