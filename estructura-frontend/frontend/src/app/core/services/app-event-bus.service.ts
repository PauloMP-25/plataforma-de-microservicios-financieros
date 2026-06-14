import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';

export interface AppEvent {
  type: string;
  payload?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AppEventBus {
  private bus$ = new Subject<AppEvent>();

  /**
   * Emite un evento en el bus global.
   * @param event Evento con tipo y payload opcional.
   */
  emit(event: AppEvent): void {
    this.bus$.next(event);
  }

  /**
   * Retorna un observable filtrado por el tipo de evento especificado.
   * @param eventType Tipo del evento a escuchar (ej: 'TRANSACTION_MODIFIED').
   */
  on(eventType: string): Observable<any> {
    return this.bus$.asObservable().pipe(
      filter(event => event.type === eventType),
      map(event => event.payload)
    );
  }
}
