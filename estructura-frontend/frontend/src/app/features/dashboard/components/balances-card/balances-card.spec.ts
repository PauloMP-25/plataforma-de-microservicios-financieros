import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BalancesCard } from './balances-card';

describe('BalancesCard', () => {
  let component: BalancesCard;
  let fixture: ComponentFixture<BalancesCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalancesCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BalancesCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
