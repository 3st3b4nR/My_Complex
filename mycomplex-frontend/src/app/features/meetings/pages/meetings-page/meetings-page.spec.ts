import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PropertyApiService }
  from '../../../property-selection/data-access/property-api.service';
import { MeetingApiService } from '../../data-access/meeting-api.service';
import { MeetingItem } from '../../models/meeting';
import { MeetingsPage } from './meetings-page';

const PROPERTY = {
  propertyId: 'property-1', name: 'Los Cerezos', city: 'Rionegro',
  role: 'PROPERTY_ADMIN' as const
};
const MEETING: MeetingItem = {
  meetingId: 'meeting-1', propertyId: 'property-1', title: 'Asamblea',
  description: 'Reunión general', meetingUrl: 'https://meet.example/1',
  scheduledAt: '2099-12-01T15:00:00Z',
  updatedAt: '2026-11-01T10:00:00Z', version: 0
};
const PAST_MEETING = {
  ...MEETING, meetingId: 'meeting-past', title: 'Reunión anterior',
  scheduledAt: '2020-01-01T10:00:00Z'
};

describe('MeetingsPage', () => {
  async function createPage(options: {
    propertyId?: string | null;
    role?: 'PROPERTY_ADMIN' | 'RESIDENT';
    properties?: unknown[];
    list?: unknown;
    api?: Record<string, unknown>;
  } = {}) {
    const router = { navigate: vi.fn() };
    const propertyApi = {
      getMyProperties: vi.fn().mockReturnValue(of(
        options.properties ?? [{ ...PROPERTY, role: options.role ?? PROPERTY.role }]
      ))
    };
    const api = {
      list: vi.fn().mockReturnValue(options.list ?? of([MEETING, PAST_MEETING])),
      create: vi.fn().mockReturnValue(of(MEETING)),
      update: vi.fn().mockReturnValue(of({ ...MEETING, title: 'Actualizada', version: 1 })),
      delete: vi.fn().mockReturnValue(of(undefined)),
      ...options.api
    };
    await TestBed.configureTestingModule({
      imports: [MeetingsPage],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: {
          get: () => options.propertyId === undefined
            ? 'property-1' : options.propertyId
        } } } },
        { provide: Router, useValue: router },
        { provide: PropertyApiService, useValue: propertyApi },
        { provide: MeetingApiService, useValue: api }
      ]
    }).overrideComponent(MeetingsPage, { set: { template: '' } })
      .compileComponents();
    const fixture: ComponentFixture<MeetingsPage> =
      TestBed.createComponent(MeetingsPage);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance as any,
      api, router, propertyApi };
  }

  beforeEach(() => {
    Object.defineProperty(window, 'scrollTo', {
      configurable: true, value: vi.fn()
    });
  });

  it('loads property and separates upcoming from past meetings', async () => {
    const { component, api } = await createPage();
    expect(component.loading()).toBe(false);
    expect(component.property()).toEqual(PROPERTY);
    expect(component.upcomingMeetings()).toEqual([MEETING]);
    expect(component.pastMeetings()).toEqual([PAST_MEETING]);
    expect(api.list).toHaveBeenCalledWith('property-1');
  });

  it('redirects without propertyId and reports unavailable property', async () => {
    const missingId = await createPage({ propertyId: null });
    expect(missingId.router.navigate).toHaveBeenCalledWith(['/select-property']);
    expect(missingId.api.list).not.toHaveBeenCalled();

    TestBed.resetTestingModule();
    const unavailable = await createPage({ properties: [] });
    expect(unavailable.component.pageError())
      .toBe('La copropiedad no está asociada con tu usuario.');
  });

  it('maps forbidden and generic loading errors', async () => {
    const forbidden = await createPage({
      list: throwError(() => new HttpErrorResponse({ status: 403 }))
    });
    expect(forbidden.component.pageError())
      .toBe('No tienes permisos para realizar esta acción.');

    TestBed.resetTestingModule();
    const failed = await createPage({ list: throwError(() => new Error()) });
    expect(failed.component.pageError())
      .toBe('No fue posible completar la operación. Inténtalo nuevamente.');
  });

  it('validates permissions and required fields before creating', async () => {
    const resident = await createPage({ role: 'RESIDENT' });
    resident.component.openCreateForm();
    resident.component.submit();
    expect(resident.api.create).not.toHaveBeenCalled();

    TestBed.resetTestingModule();
    const admin = await createPage();
    admin.component.openCreateForm();
    admin.component.submit();
    expect(admin.api.create).not.toHaveBeenCalled();
    expect(admin.component.meetingForm.controls.title.touched).toBe(true);

    admin.component.meetingForm.setValue({
      title: 'Consejo', description: 'Seguimiento mensual',
      meetingUrl: 'https://meet.example/council',
      scheduledAt: '2026-12-05T08:30'
    });
    admin.component.submit();
    expect(admin.api.create).toHaveBeenCalledWith('property-1', {
      title: 'Consejo', description: 'Seguimiento mensual',
      meetingUrl: 'https://meet.example/council',
      scheduledAt: '2026-12-05T08:30:00.000Z'
    });
    expect(admin.component.saving()).toBe(false);
    expect(admin.component.formVisible()).toBe(false);
  });

  it('opens, updates and sorts an existing meeting', async () => {
    const later = { ...MEETING, meetingId: 'meeting-2',
      scheduledAt: '2099-12-02T10:00:00Z' };
    const { component, api } = await createPage({ list: of([later, MEETING]) });
    component.openEditForm(MEETING);
    expect(component.meetingForm.controls.scheduledAt.value)
      .toBe('2099-12-01T15:00');
    component.meetingForm.controls.title.setValue('Actualizada');
    component.submit();
    expect(api.update).toHaveBeenCalledWith(
      'property-1', 'meeting-1', expect.objectContaining({
        title: 'Actualizada', version: 0
      })
    );
    expect(component.meetings()[0].title).toBe('Actualizada');
    expect(window.scrollTo).toHaveBeenCalled();
  });

  it('prevents closing while saving and maps save validation errors', async () => {
    const { component } = await createPage({ api: {
      create: vi.fn().mockReturnValue(throwError(() =>
        new HttpErrorResponse({ status: 400 })))
    } });
    component.openCreateForm();
    component.saving.set(true);
    component.closeForm();
    expect(component.formVisible()).toBe(true);
    component.saving.set(false);
    component.meetingForm.setValue({
      title: 'Consejo', description: 'Seguimiento',
      meetingUrl: 'https://meet.example/1', scheduledAt: '2026-12-05T08:30'
    });
    component.submit();
    expect(component.formError())
      .toBe('Verifica la fecha UTC y el enlace HTTPS de la reunión.');
    component.closeForm();
    expect(component.formVisible()).toBe(false);
  });

  it('confirms deletion and controls conflict failures', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { component, api } = await createPage();
    component.deleteMeeting(MEETING);
    expect(api.delete).not.toHaveBeenCalled();

    confirm.mockReturnValue(true);
    component.deleteMeeting(MEETING);
    expect(api.delete).toHaveBeenCalledWith('property-1', 'meeting-1', 0);
    expect(component.meetings()).not.toContainEqual(MEETING);
    expect(component.deletingMeetingId()).toBeNull();

    api.delete.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 409 })));
    component.meetings.set([MEETING]);
    component.deleteMeeting(MEETING);
    expect(component.pageError())
      .toBe('La reunión cambió. Recarga la página e inténtalo otra vez.');
  });

  it('does not mutate meetings for non-administrators', async () => {
    const confirm = vi.spyOn(window, 'confirm');
    const { component, api } = await createPage({ role: 'RESIDENT' });
    component.deleteMeeting(MEETING);
    expect(confirm).not.toHaveBeenCalled();
    expect(api.delete).not.toHaveBeenCalled();
  });
});
