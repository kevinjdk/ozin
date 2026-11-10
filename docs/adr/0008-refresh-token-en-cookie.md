# ADR-0008: Sesión del navegador con cookie HttpOnly y access token en memoria

- **Estado:** Aceptado
- **Fecha:** 2026-11-06

## Contexto
La aplicación Angular necesita conservar la sesión al recargar la página. Lo que se guarda en el navegador
es lo primero que busca un atacante que logre ejecutar código en la página (XSS).

## Decisión

| Token | Dónde vive | Motivo |
|---|---|---|
| Access token (15 min) | Una señal en memoria de Angular | Desaparece al cerrar o recargar la pestaña; caduca pronto si se filtra |
| Refresh token (7 días) | Cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` | JavaScript no puede leerla |

Al cargar la página, Angular pide `POST /auth/refresh`, que usa la cookie para recuperar la sesión.

**CSRF.** Una cookie se envía sola, así que otro sitio podría provocar una petición. Tres barreras lo impiden:
1. `SameSite=Strict`: el navegador no la envía en peticiones desde otros sitios.
2. `Path=/api/v1/auth`: solo llega a las rutas de sesión; el resto de la API usa Bearer y no es vulnerable.
3. Cabecera obligatoria `X-Ozin-Client` en `refresh` y `logout`: otro origen no puede añadirla sin que CORS lo autorice.

## Alternativas consideradas
- **`localStorage`:** no requiere cambios en el backend, pero cualquier XSS roba el token de 7 días.
- **Token CSRF sincronizado:** es lo clásico, pero añade estado y complejidad sin una ganancia real, dado
  que la cookie solo protege dos rutas sin efectos aprovechables por un tercero.

## Consecuencias
- (+) Un XSS no puede llevarse la sesión de larga duración. Sí podría actuar mientras la página esté abierta:
  esto reduce el daño de un XSS, no lo elimina.
- (−) Frontend y API deben servirse desde el **mismo sitio**, porque `SameSite=Strict` no se envía entre sitios
  distintos. En desarrollo lo resuelve el proxy de Angular, y en producción habrá que publicar ambos bajo un mismo
  dominio (Fase 6).
- (−) En desarrollo por HTTP en navegadores que no tratan `localhost` como seguro, hace falta `OZIN_COOKIE_SECURE=false`.
