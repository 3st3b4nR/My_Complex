import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { PropertyApiService } from '../../property-selection/data-access/property-api.service';
import { PropertyMemberApiService } from './property-member-api.service';
import { UserManagementFacade } from './user-management.facade';

describe('UserManagementFacade', () => {
  it('loads and reloads members while preserving the selected property', async () => {
    const getMembers = vi.fn(() => of([]));
    TestBed.configureTestingModule({
      providers: [
        UserManagementFacade,
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ propertyId: 'property-1' }))
          }
        },
        {
          provide: PropertyApiService,
          useValue: {
            getMyProperties: () => of([{
              propertyId: 'property-1',
              name: 'Los Cerezos',
              city: 'Rionegro',
              role: 'PROPERTY_ADMIN'
            }])
          }
        },
        {
          provide: PropertyMemberApiService,
          useValue: { getPropertyMembers: getMembers }
        }
      ]
    });

    const facade = TestBed.inject(UserManagementFacade);
    await vi.waitFor(() => expect(facade.state().status).toBe('success'));
    facade.reload();
    await vi.waitFor(() => expect(getMembers).toHaveBeenCalledTimes(2));
    expect(facade.state().property?.propertyId).toBe('property-1');
  });
});
