import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Interceptor global para desinfectar (sanitize) de forma sistemática y recursiva
 * todos los cuerpos de solicitud (JSON payloads) de tipo POST, PUT y PATCH.
 * Elimina etiquetas <script> y de marcado HTML para mitigar vulnerabilidades XSS.
 */
export const sanitizeInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  if (req.body && (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH')) {
    const sanitizedBody = sanitizeValue(req.body);
    const clonedReq = req.clone({ body: sanitizedBody });
    return next(clonedReq);
  }
  return next(req);
};

function sanitizeValue(val: any): any {
  if (val === null || val === undefined) {
    return val;
  }
  if (typeof val === 'string') {
    return sanitizeString(val);
  }
  if (Array.isArray(val)) {
    return val.map(item => sanitizeValue(item));
  }
  if (typeof val === 'object') {
    const sanitizedObj: any = {};
    for (const key in val) {
      if (Object.prototype.hasOwnProperty.call(val, key)) {
        sanitizedObj[key] = sanitizeValue(val[key]);
      }
    }
    return sanitizedObj;
  }
  return val;
}

function sanitizeString(str: string): string {
  if (!str) return str;
  // Eliminar etiquetas <script> y sus contenidos
  let sanitized = str.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');
  // Eliminar cualquier otra etiqueta HTML (ej. <img src="..." onerror="..." /> o <div>)
  sanitized = sanitized.replace(/<[^>]+>/g, '');
  return sanitized;
}
