import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { throwError, BehaviorSubject, Observable } from 'rxjs';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  let authReq = req;
  // Solo agregar token a las peticiones internas, evitar enviarlo a API Peru u otras APIs externas
  const isExternalApi = req.url.includes('apiperu.dev');
  
  if (token && !isExternalApi) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/refrescar-token') && !isExternalApi) {
        return handle401Error(authReq, next, auth);
      }
      return throwError(() => error);
    })
  );
};

const handle401Error = (req: any, next: any, auth: AuthService): Observable<any> => {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    const refreshToken = auth.getRefreshToken();

    if (refreshToken) {
      return auth.refrescarToken(refreshToken).pipe(
        switchMap((respuesta) => {
          isRefreshing = false;
          if (respuesta && respuesta.exito) {
            const newToken = respuesta.datos.tokenAcceso;
            refreshTokenSubject.next(newToken);
            return next(req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` }
            }));
          }
          auth.logout();
          return throwError(() => new Error('No se pudo refrescar la sesión'));
        }),
        catchError((err) => {
          isRefreshing = false;
          auth.logout();
          return throwError(() => err);
        })
      );
    } else {
      isRefreshing = false;
      auth.logout();
      return throwError(() => new Error('No hay token de refresco disponible'));
    }
  } else {
    return refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(jwt => {
        return next(req.clone({
          setHeaders: { Authorization: `Bearer ${jwt}` }
        }));
      })
    );
  }
};