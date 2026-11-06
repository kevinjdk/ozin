import { APIRequestContext, expect, Page } from '@playwright/test';

export const PASSWORD = 'Segura123';
const ADMIN_EMAIL = process.env['OZIN_ADMIN_EMAIL'] ?? 'admin@ozin.test';
const ADMIN_PASSWORD = process.env['OZIN_ADMIN_PASSWORD'] ?? 'Verificacion123';
const API = 'http://localhost:8080/api/v1';
const ALL_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export interface Salon {
  serviceName: string;
  stylistName: string;
  stylistEmail: string;
}

/** Prepara por API un servicio y una estilista que trabaja todos los días, con datos únicos por ejecución. */
export async function createSalon(request: APIRequestContext): Promise<Salon> {
  const suffix = Date.now().toString(36);
  const login = await request.post(`${API}/auth/login`, { data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD } });
  expect(login.ok(), 'El backend debe arrancar con OZIN_ADMIN_EMAIL y OZIN_ADMIN_PASSWORD').toBeTruthy();
  const headers = { Authorization: `Bearer ${(await login.json()).accessToken}` };

  const branches = await (await request.get(`${API}/branches`)).json();
  const serviceName = `Corte ${suffix}`;
  const service = await request.post(`${API}/services`, {
    headers,
    data: { name: serviceName, durationMinutes: 45, price: 12.5 },
  });
  expect(service.ok()).toBeTruthy();

  const stylistName = `Ana ${suffix}`;
  const stylistEmail = `ana-${suffix}@ozin.test`;
  const stylist = await request.post(`${API}/stylists`, {
    headers,
    data: {
      branchId: branches[0].id,
      fullName: stylistName,
      email: stylistEmail,
      password: PASSWORD,
      serviceIds: [(await service.json()).id],
      workingHours: ALL_DAYS.map((day) => ({ day, start: '09:00', end: '18:00' })),
    },
  });
  expect(stylist.ok()).toBeTruthy();
  return { serviceName, stylistName, stylistEmail };
}

export function adminCredentials(): { email: string; password: string } {
  return { email: ADMIN_EMAIL, password: ADMIN_PASSWORD };
}

export async function logIn(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/ingresar');
  await fillLogin(page, email, password);
  await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
}

export async function fillLogin(page: Page, email: string, password: string): Promise<void> {
  await page.getByLabel('Email').fill(email);
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
}

/** Elige en el calendario el día siguiente, que siempre está dentro de la anticipación permitida. */
export async function pickTomorrow(page: Page): Promise<void> {
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const today = new Date();
  if (tomorrow.getMonth() !== today.getMonth()) {
    await page.getByRole('button', { name: /next month|siguiente/i }).click();
  }
  await page
    .locator('.p-datepicker-day-cell:not(.p-datepicker-other-month) .p-datepicker-day')
    .getByText(String(tomorrow.getDate()), { exact: true })
    .click();
}
