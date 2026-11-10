import { expect, test } from '@playwright/test';

import { adminCredentials, createSalon, fillLogin, logIn, PASSWORD, pickTomorrow, Salon } from './support';

test.describe.configure({ mode: 'serial' });

let salon: Salon;
const customerEmail = `cliente-${Date.now().toString(36)}@ozin.test`;

test.beforeAll(async ({ request }) => {
  salon = await createSalon(request);
});

test('un visitante se registra a mitad de la reserva, sin perder lo elegido, y reserva', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: salon.serviceName })).toBeVisible();

  await page.getByRole('link', { name: `Reservar ${salon.serviceName}` }).click();
  await page.getByRole('button', { name: salon.stylistName }).click();

  await pickTomorrow(page);
  await page.getByRole('button', { name: '10:00' }).click();

  await expect(page.getByText('Revisa tu cita')).toBeVisible();
  await expect(page.getByText(/\$\s?12,50|12,50/)).toBeVisible();
  await page.getByRole('button', { name: 'Inicia sesión para confirmar' }).click();

  await page.getByRole('link', { name: 'Crea una' }).click();
  await page.getByLabel('Nombre completo').fill('Eva Prueba');
  await page.getByLabel('Email').fill(customerEmail);
  await page.getByLabel('Teléfono').fill('+593991234567');
  await page.locator('#password').fill(PASSWORD);
  await page.getByRole('button', { name: 'Crear cuenta' }).click();

  // Vuelve a la reserva con todo lo elegido.
  await expect(page.getByText('Revisa tu cita')).toBeVisible();
  await page.getByRole('button', { name: 'Confirmar reserva' }).click();
  await expect(page.getByRole('heading', { name: '¡Tu cita está reservada!' })).toBeVisible();

  await page.getByRole('button', { name: 'Ver mis citas' }).click();
  await expect(page.getByRole('heading', { name: salon.serviceName })).toBeVisible();
  await expect(page.getByText('Pendiente', { exact: true })).toBeVisible();
});

test('la sesión sobrevive a recargar la página, gracias a la cookie HttpOnly', async ({ page, context }) => {
  await logIn(page, customerEmail, PASSWORD);

  const cookie = (await context.cookies()).find((candidate) => candidate.name === 'ozin_refresh');
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe('Strict');
  expect(await page.evaluate(() => document.cookie)).not.toContain('ozin_refresh');

  await page.goto('/mis-citas');
  await expect(page.getByRole('heading', { name: 'Mis citas' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
});

test('al cerrar sesión ya no se puede volver a las pantallas privadas', async ({ page }) => {
  await logIn(page, customerEmail, PASSWORD);
  await page.getByRole('button', { name: 'Salir' }).click();
  await expect(page.getByRole('link', { name: 'Ingresar' })).toBeVisible();

  await page.goto('/mis-citas');

  await expect(page).toHaveURL(/\/ingresar\?returnUrl=/);
});

test('un cliente no puede entrar al panel de administración', async ({ page }) => {
  await logIn(page, customerEmail, PASSWORD);

  await page.goto('/admin/servicios');

  await expect(page.getByRole('heading', { name: 'Mis citas' })).toBeVisible();
});

test('la estilista ve la cita en su agenda y la confirma', async ({ page }) => {
  await logIn(page, salon.stylistEmail, PASSWORD);
  await expect(page.getByRole('heading', { name: 'Mi agenda' })).toBeVisible();

  await page.getByRole('button', { name: 'Semana' }).click();
  const tomorrowInNextWeek = new Date(Date.now() + 24 * 60 * 60 * 1000).getDay() === 1;
  if (tomorrowInNextWeek) {
    await page.getByRole('button', { name: 'Siguiente' }).click();
  }
  await expect(page.getByText('Eva Prueba')).toBeVisible();

  await page.getByRole('button', { name: 'Confirmar' }).click();

  await expect(page.getByText('Confirmada', { exact: true })).toBeVisible();
});

test('el administrador crea un servicio que aparece en el catálogo público', async ({ page }) => {
  const { email, password } = adminCredentials();
  const name = `Tinte ${Date.now().toString(36)}`;
  await logIn(page, email, password);

  await page.getByRole('link', { name: 'Servicios' }).click();
  await page.getByRole('button', { name: 'Nuevo servicio' }).click();
  await page.getByLabel('Nombre').fill(name);
  await page.getByLabel('Precio (USD)').fill('35');
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();

  await page.goto('/');
  await expect(page.getByRole('heading', { name })).toBeVisible();
});

test('el administrador ve el error real de un inicio de sesión fallido', async ({ page }) => {
  await page.goto('/ingresar');
  await fillLogin(page, adminCredentials().email, 'ClaveIncorrecta1');

  await expect(page.getByText('Email o contraseña incorrectos')).toBeVisible();
});
