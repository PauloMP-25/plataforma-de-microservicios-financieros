import { TestBed } from '@angular/core/testing';

import { Transacciones } from './transacciones';

describe('Transacciones', () => {
  let service: Transacciones;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Transacciones);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
