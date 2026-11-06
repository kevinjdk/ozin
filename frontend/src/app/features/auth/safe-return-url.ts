/**
 * Solo acepta rutas internas. Sin esta comprobación, un enlace como
 * /ingresar?returnUrl=//sitio-falso.example podría sacar al usuario de la aplicación tras iniciar sesión.
 */
export function safeReturnUrl(url: string | undefined): string | null {
  if (!url || !url.startsWith('/') || url.startsWith('//') || url.includes('\\')) {
    return null;
  }
  return url;
}
