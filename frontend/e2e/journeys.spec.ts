import { expect, Locator, Page, test } from '@playwright/test';

import { adminCredentials, createSalon, fillLogin, logIn, PASSWORD, pickTomorrow, Salon } from './support';

test.describe.configure({ mode: 'serial' });

let salon: Salon;
const customerEmail = `cliente-${Date.now().toString(36)}@ozin.test`;

test.beforeAll(async ({ request }) => {
  salon = await createSalon(request);
});

/** Botón de un día del mes visible en el calendario de la agenda. */
function calendarDay(page: Page, day: number): Locator {
  return page.locator('button.calendar__day:not(.calendar__day--outside)', { hasText: new RegExp(`^\\s*${day}\\s*$`) });
}

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
  // La cita nace confirmada, el caso normal, así que la tarjeta no lleva etiqueta de estado.
  await expect(page.getByRole('button', { name: 'Cancelar' })).toBeVisible();
  await expect(page.getByText('Confirmada', { exact: true })).toHaveCount(0);
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

test('la estilista ve en rojo el día con la cita y la encuentra ya confirmada', async ({ page }) => {
  await logIn(page, salon.stylistEmail, PASSWORD);
  await expect(page.getByRole('heading', { name: 'Mi agenda' })).toBeVisible();

  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
  if (tomorrow.getMonth() !== new Date().getMonth()) {
    await page.getByRole('button', { name: 'Mes siguiente' }).click();
  }
  const day = calendarDay(page, tomorrow.getDate());
  await expect(day).toHaveClass(/calendar__day--busy/);
  await day.click();

  await expect(page.getByText('Eva Prueba')).toBeVisible();
  await expect(page.getByText('Confirmada', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Confirmar' })).toHaveCount(0);
});

test('el administrador crea un estilista con el horario fijo y la pausa de mediodía ya puestos', async ({ page }) => {
  const { email, password } = adminCredentials();
  const suffix = Date.now().toString(36);
  await logIn(page, email, password);
  await expect(page.getByRole('heading', { name: 'Agenda del salón' })).toBeVisible();

  await page.getByRole('link', { name: 'Estilistas' }).click();
  await page.getByRole('button', { name: 'Nuevo estilista' }).click();
  await page.getByLabel('Nombre completo').fill(`Luis ${suffix}`);
  await page.getByLabel('Email de acceso').fill(`luis-${suffix}@ozin.test`);
  await page.locator('#stylist-password').fill(PASSWORD);
  await expect(page.getByText('Libre: domingo.')).toBeVisible();
  await expect(page.getByText('Este horario tiene definida la pausa al mediodía de 13:00 a 14:00.')).toBeVisible();
  await page.getByRole('button', { name: 'Guardar' }).click();

  const row = page.getByRole('row', { name: new RegExp(`Luis ${suffix}`) });
  await expect(row.getByText('Lun a Sáb · 09:00–18:00, pausa 13:00–14:00')).toBeVisible();

  // Personalizado parte del horario fijo: basta con cambiar el día distinto.
  await page.getByRole('button', { name: `Editar Luis ${suffix}` }).click();
  await page.getByRole('button', { name: 'Personalizado' }).click();
  await page.getByLabel('Salida · miércoles').fill('16:00');
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(row.getByText('Lun a Sáb · horario variable')).toBeVisible();

  await page.getByRole('button', { name: `Editar Luis ${suffix}` }).click();
  await expect(page.getByLabel('Salida · miércoles')).toHaveValue('16:00');
  await expect(page.getByLabel('Salida · martes')).toHaveValue('18:00');
  await expect(page.getByLabel('Inicio de la pausa · martes')).toHaveValue('13:00');
});

test('el administrador registra un día de ausencia y lo elimina', async ({ page }) => {
  const { email, password } = adminCredentials();
  await logIn(page, email, password);
  await expect(page.getByRole('heading', { name: 'Agenda del salón' })).toBeVisible();

  await page.getByRole('link', { name: 'Estilistas' }).click();
  await page.getByRole('button', { name: `Ausencias de ${salon.stylistName}` }).click();
  await page.getByLabel('Motivo (opcional)').fill('Capacitación');
  await page.getByRole('button', { name: 'Registrar ausencia' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog.getByText(/día completo/)).toBeVisible();
  await expect(dialog.getByText('Capacitación')).toBeVisible();

  await dialog.getByRole('button', { name: /Eliminar ausencia/ }).click();
  await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
  await expect(dialog.getByText('No tiene ausencias registradas.')).toBeVisible();
});

test('el administrador crea un servicio que aparece en el catálogo público', async ({ page }) => {
  const { email, password } = adminCredentials();
  const name = `Tinte ${Date.now().toString(36)}`;
  await logIn(page, email, password);
  await expect(page.getByRole('heading', { name: 'Agenda del salón' })).toBeVisible();

  await page.getByRole('link', { name: 'Servicios' }).click();
  await page.getByRole('button', { name: 'Nuevo servicio' }).click();
  await page.getByLabel('Nombre', { exact: true }).fill(name);
  await page.getByLabel('Descripción (opcional)').fill('Color uniforme con lavado incluido.');
  await page.getByLabel('Precio (USD)').fill('35');
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByText(name, { exact: true })).toBeVisible();

  await page.goto('/');
  await expect(page.getByRole('heading', { name })).toBeVisible();
  await expect(page.getByText('Color uniforme con lavado incluido.')).toBeVisible();
});

test('el administrador ve el error real de un inicio de sesión fallido', async ({ page }) => {
  await page.goto('/ingresar');
  await fillLogin(page, adminCredentials().email, 'ClaveIncorrecta1');

  await expect(page.getByText('Email o contraseña incorrectos')).toBeVisible();
});
