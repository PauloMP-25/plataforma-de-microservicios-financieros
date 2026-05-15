import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AyudaPage } from './ayuda-page';

describe('AyudaPage', () => {
  let component: AyudaPage;
  let fixture: ComponentFixture<AyudaPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AyudaPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AyudaPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
