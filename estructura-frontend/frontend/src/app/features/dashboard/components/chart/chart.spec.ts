import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Chart } from './chart';

describe('Chart', () => {
  let component: Chart;
  let fixture: ComponentFixture<Chart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Chart]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Chart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
