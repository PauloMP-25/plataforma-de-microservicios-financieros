import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GastosPage } from './gastos-page';

describe('GastosPage', () => {
  let component: GastosPage;
  let fixture: ComponentFixture<GastosPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GastosPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GastosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
