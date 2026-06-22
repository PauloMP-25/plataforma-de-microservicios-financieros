import { CanDeactivateFn } from '@angular/router';

/**
 * Interfaz que deben implementar los componentes que desean proteger
 * la pérdida accidental de datos no guardados al navegar fuera.
 */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/**
 * Guard para impedir la navegación accidental si existen cambios sin guardar en un formulario.
 */
export const pendingChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (component && component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    return confirm('Tienes cambios sin guardar en el formulario. ¿Estás seguro de que deseas salir de esta página? Todo el progreso no guardado se perderá.');
  }
  return true;
};
