import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PerfilLayout } from './perfil-layout';

describe('PerfilLayout', () => {
  let component: PerfilLayout;
  let fixture: ComponentFixture<PerfilLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilLayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PerfilLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
