import { HttpErrorResponse } from '@angular/common/http';

const INVITATION_ERROR_MESSAGES: Readonly<Record<string, string>> = {
  AUTH0_NOT_CONFIGURED: 'La integración con Auth0 aún no está configurada correctamente.',
  AUTH0_CONFIGURATION_ERROR: 'La integración con Auth0 aún no está configurada correctamente.',
  AUTH0_IDENTITY_CONFLICT: 'El correo ya pertenece a una identidad que requiere revisión manual.',
  AUTH0_RATE_LIMITED: 'Auth0 limitó temporalmente las solicitudes. Inténtalo más tarde.',
  AUTH0_UNAVAILABLE: 'Auth0 no está disponible temporalmente.',
  AUTH0_INVITATION_UNAVAILABLE: 'La identidad fue creada, pero no fue posible solicitar el correo. Puedes reintentarlo.',
  INVITATION_RESEND_TOO_SOON: 'Espera antes de reenviar la invitación.'
};

export function invitationErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'No pudimos procesar la invitación.';
  }
  if (error.status === 403) return 'No tienes permiso para invitar usuarios.';
  if (error.status === 404) return 'La vinculación ya no está disponible.';
  const code = error.error?.code as string | undefined;
  return (code && INVITATION_ERROR_MESSAGES[code])
    || error.error?.message
    || 'No pudimos procesar la invitación.';
}

export function unlinkErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'No pudimos desvincular al usuario.';
  if (error.status === 403) return 'No tienes permiso para desvincular usuarios.';
  if (error.status === 404) return 'La vinculación ya no está disponible.';
  if (error.status === 409) return 'La vinculación fue modificada. Actualiza el listado e inténtalo nuevamente.';
  if (error.status === 400 && error.error?.code === 'LAST_PROPERTY_ADMINISTRATOR') {
    return 'No puedes desvincular al último administrador activo.';
  }
  if (error.status === 400) return error.error?.message ?? 'La desvinculación no es válida.';
  return 'No pudimos desvincular al usuario.';
}
