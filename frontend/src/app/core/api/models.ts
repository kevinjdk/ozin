/** Contratos de la API. Reflejan las respuestas documentadas en /swagger-ui.html. */

export type Role = 'ADMIN' | 'STYLIST' | 'CUSTOMER';

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  role: Role;
  profileId: string | null;
}

export interface Branch {
  id: string;
  name: string;
  address: string;
  timeZone: string;
}

export interface SalonService {
  id: string;
  name: string;
  durationMinutes: number;
  price: number;
  currency: string;
  active: boolean;
}

export interface SalonServiceInput {
  name: string;
  durationMinutes: number;
  price: number;
}

export interface PublicStylist {
  id: string;
  branchId: string;
  fullName: string;
  serviceIds: string[];
}

export interface WorkingHours {
  day: DayOfWeek;
  start: string;
  end: string;
}

export interface TimeOff {
  start: string;
  end: string;
  reason: string;
}

export interface Stylist extends PublicStylist {
  active: boolean;
  workingHours: WorkingHours[];
  timeOffs: TimeOff[];
}

export interface CreateStylistInput {
  branchId: string;
  fullName: string;
  email: string;
  password: string;
  serviceIds: string[];
  workingHours: WorkingHours[];
}

export interface UpdateStylistInput {
  fullName: string;
  active: boolean;
  serviceIds: string[];
  workingHours: WorkingHours[];
}

export interface Slot {
  start: string;
  end: string;
}

/** Cita recién creada o modificada. */
export interface Appointment {
  id: string;
  start: string;
  end: string;
  price: number;
  currency: string;
  status: AppointmentStatus;
}

/** Cita en listados y agendas, con los nombres ya resueltos. */
export interface AppointmentView extends Appointment {
  customerId: string;
  customerName: string;
  stylistId: string;
  stylistName: string;
  serviceId: string;
  serviceName: string;
  branchId: string;
}

export interface Page<T> {
  items: T[];
  totalItems: number;
  page: number;
  size: number;
}

export interface RegisterInput {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}
