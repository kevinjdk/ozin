import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Appointment,
  AppointmentView,
  Branch,
  CreateStylistInput,
  Page,
  PublicStylist,
  SalonService,
  SalonServiceInput,
  Slot,
  Stylist,
  TimeOff,
  UpdateStylistInput,
} from './models';

const API = '/api/v1';

export interface AgendaRange {
  from: string;
  to: string;
}

export type AppointmentAction = 'confirm' | 'complete' | 'no-show' | 'cancel';

/** Puerta de entrada a la API de negocio. La autenticación la añade el interceptor. */
@Injectable({ providedIn: 'root' })
export class SalonApi {
  private readonly http = inject(HttpClient);

  branches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${API}/branches`);
  }

  services(): Observable<SalonService[]> {
    return this.http.get<SalonService[]>(`${API}/services`);
  }

  allServices(): Observable<SalonService[]> {
    return this.http.get<SalonService[]>(`${API}/services/manage`);
  }

  createService(input: SalonServiceInput): Observable<SalonService> {
    return this.http.post<SalonService>(`${API}/services`, input);
  }

  updateService(id: string, input: SalonServiceInput & { active: boolean }): Observable<SalonService> {
    return this.http.put<SalonService>(`${API}/services/${id}`, input);
  }

  stylistsFor(serviceId: string): Observable<PublicStylist[]> {
    const params = new HttpParams().set('serviceId', serviceId);
    return this.http.get<PublicStylist[]>(`${API}/stylists`, { params });
  }

  allStylists(): Observable<Stylist[]> {
    return this.http.get<Stylist[]>(`${API}/stylists/manage`);
  }

  createStylist(input: CreateStylistInput): Observable<Stylist> {
    return this.http.post<Stylist>(`${API}/stylists`, input);
  }

  updateStylist(id: string, input: UpdateStylistInput): Observable<Stylist> {
    return this.http.put<Stylist>(`${API}/stylists/${id}`, input);
  }

  addTimeOff(stylistId: string, timeOff: TimeOff): Observable<Stylist> {
    return this.http.post<Stylist>(`${API}/stylists/${stylistId}/time-offs`, timeOff);
  }

  availability(stylistId: string, serviceId: string, date: string): Observable<Slot[]> {
    const params = new HttpParams().set('serviceId', serviceId).set('date', date);
    return this.http.get<Slot[]>(`${API}/stylists/${stylistId}/availability`, { params });
  }

  book(stylistId: string, serviceId: string, start: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${API}/appointments`, { stylistId, serviceId, start });
  }

  applyAction(appointmentId: string, action: AppointmentAction): Observable<Appointment> {
    return this.http.post<Appointment>(`${API}/appointments/${appointmentId}/${action}`, null);
  }

  myAppointments(page: number, size: number): Observable<Page<AppointmentView>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<AppointmentView>>(`${API}/customers/me/appointments`, { params });
  }

  stylistAgenda(stylistId: string, range: AgendaRange): Observable<AppointmentView[]> {
    return this.http.get<AppointmentView[]>(`${API}/stylists/${stylistId}/agenda`, { params: { ...range } });
  }

  branchAgenda(branchId: string, range: AgendaRange): Observable<AppointmentView[]> {
    return this.http.get<AppointmentView[]>(`${API}/branches/${branchId}/agenda`, { params: { ...range } });
  }
}
