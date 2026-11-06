import { defineConfig, devices } from '@playwright/test';

/**
 * Recorridos completos contra la aplicación real. Necesita el backend en :8080 con
 * OZIN_ADMIN_EMAIL y OZIN_ADMIN_PASSWORD definidos (ver README); el frontend lo levanta Playwright.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4200',
    locale: 'es-EC',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 240_000,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
