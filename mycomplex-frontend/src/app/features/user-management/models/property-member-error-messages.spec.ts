import { HttpErrorResponse } from '@angular/common/http';

import {
  invitationErrorMessage,
  unlinkErrorMessage
} from './property-member-error-messages';

describe('property member error messages', () => {
  it('maps stable invitation codes to safe Spanish messages', () => {
    const error = new HttpErrorResponse({
      status: 429,
      error: { code: 'AUTH0_RATE_LIMITED', message: 'technical detail' }
    });
    expect(invitationErrorMessage(error)).toBe(
      'Auth0 limitó temporalmente las solicitudes. Inténtalo más tarde.'
    );
  });

  it('maps last-administrator unlink protection', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { code: 'LAST_PROPERTY_ADMINISTRATOR' }
    });
    expect(unlinkErrorMessage(error)).toBe(
      'No puedes desvincular al último administrador activo.'
    );
  });
});
