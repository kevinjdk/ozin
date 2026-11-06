# ADR-0004: Autenticación con JWT y control de acceso por roles

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
La API es consumida por una SPA en Angular y debe ser stateless para escalar horizontalmente.

## Decisión
- Spring Security como **OAuth2 Resource Server** validando JWT firmados.
- Access token de corta duración (15 min) y refresh token rotativo (7 días) almacenado con hash.
- Contraseñas con **BCrypt**.
- Autorización por roles (`ADMIN`, `STYLIST`, `CUSTOMER`) a nivel de endpoint y de caso de uso.
- Endurecimiento: CORS restringido, cabeceras de seguridad, rate limiting en login.

## Alternativas consideradas
- **Sesiones con cookie:** requiere estado en el servidor o un store compartido.
- **Keycloak u otro IdP externo:** muy usado en banca; se deja como evolución futura (el resource server ya es compatible).

## Consecuencias
- (+) API stateless y compatible con un proveedor de identidad externo en el futuro.
- (−) La revocación de access tokens requiere esperar su expiración (mitigado con duración corta).
