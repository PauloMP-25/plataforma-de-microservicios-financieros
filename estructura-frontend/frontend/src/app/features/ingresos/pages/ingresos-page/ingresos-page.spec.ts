import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IngresosPage } from './ingresos-page';

describe('IngresosPage', () => {
  let component: IngresosPage;
  let fixture: ComponentFixture<IngresosPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IngresosPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IngresosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
