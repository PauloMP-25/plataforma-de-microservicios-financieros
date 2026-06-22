import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistorialComponent } from './historial'; // <--- Cambiado aquí

describe('HistorialComponent', () => { // <--- Cambiado aquí
  let component: HistorialComponent; // <--- Cambiado aquí
  let fixture: ComponentFixture<HistorialComponent>; // <--- Cambiado aquí

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistorialComponent] // <--- Cambiado aquí
    })
    .compileComponents();

    fixture = TestBed.createComponent(HistorialComponent); // <--- Cambiado aquí
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
