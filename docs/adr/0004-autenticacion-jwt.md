# ADR-0004: Autenticación con JWT y control de acceso por roles

- **Estado:** Aceptado
- **Fecha:** 2026-11-05 (implementado y actualizado el 2026-11-06)

## Contexto
La API es consumida por una SPA en Angular y debe ser stateless para escalar horizontalmente.
Maneja datos personales, por lo que un cliente no puede ver ni modificar lo de otro.

## Decisión

**Autenticación**
- Spring Security como **OAuth2 Resource Server** valida JWT firmados con HS256.
- Access token de 15 minutos con los claims `role` y `profileId`.
- Refresh token opaco de 7 días. Solo se guarda su hash SHA-256.
- **Rotación con detección de reutilización:** cada refresh token sirve una vez. Si llega uno ya usado,
  se asume robo y se revoca toda esa sesión; las sesiones de otros dispositivos no se ven afectadas.
- Contraseñas con **BCrypt** (coste 12) y política de 8 o más caracteres, una mayúscula y un número.

**Autorización, en dos niveles**
1. **Por ruta** (`SecurityConfig`): qué rol puede llamar a cada endpoint. Lo que no aparece allí queda denegado.
2. **Por recurso** (casos de uso y dominio): un cliente solo cancela sus citas y un estilista solo gestiona las suyas.

La identidad sale siempre del token validado (`Actor`), nunca del cuerpo de la petición.

**Modelo.** `User` apunta a su perfil (`Customer` o `Stylist`) mediante `profileId`; los perfiles no saben nada
de autenticación. Una restricción `CHECK` en la base de datos garantiza que el perfil corresponda al rol.

**Endurecimiento**
- Tras 5 intentos fallidos se bloquea la combinación email + IP durante 15 minutos.
- La respuesta y el trabajo de BCrypt son iguales si el email no existe o si la contraseña es incorrecta,
  para que no se pueda averiguar qué cuentas están registradas.
- CORS limitado a los orígenes configurados.
- El secreto JWT es obligatorio y no tiene valor por defecto: sin él, la aplicación no arranca.
- El administrador inicial se crea desde variables de entorno; el registro público solo crea clientes.
- Dar de baja a un estilista le impide iniciar sesión y revoca sus sesiones.

## Alternativas consideradas
- **Sesiones con cookie:** requiere estado en el servidor o un almacén compartido, y protección CSRF.
- **Keycloak u otro IdP externo:** muy usado en banca; queda como evolución futura. El resource server ya es
  compatible: bastaría cambiar el decodificador por uno basado en JWKS (RS256).
- **RS256 en lugar de HS256:** necesario si otros servicios validaran los tokens. Con una sola API que emite y
  valida, una clave simétrica es más simple.

## Consecuencias
- (+) API stateless, con la revocación concentrada en el refresh token.
- (+) La autorización por recurso se prueba sin servidor HTTP.
- (−) Un access token sigue siendo válido hasta 15 minutos después de revocar la sesión o dar de baja a un estilista.
- (−) El límite de intentos vive en memoria: se reinicia con la aplicación y no se comparte entre instancias.
  Se resuelve con otra implementación de `LoginThrottlePort`, por ejemplo con Redis.
- (−) Detrás de un proxy, la IP observada es la del proxy, así que el límite actuaría solo por email.
  Se debe configurar `server.forward-headers-strategy` al desplegar (Fase 6).
