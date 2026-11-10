# ADR-0005: Frontend con Angular 21 y PrimeNG 21

- **Estado:** Aceptado
- **Fecha:** 2026-11-05 (versión revisada el 2026-11-06)

## Contexto
El frontend necesita formularios complejos (reserva, horarios), tablas y calendarios,
y debe seguir convenciones habituales en aplicaciones empresariales.

Al probar la aplicación con PrimeNG 22 apareció en todas las pantallas el aviso *Invalid PrimeUI License*.
Desde esa versión, PrimeNG, `@primeuix/themes` 3 y `primeicons` 8 se distribuyen bajo una licencia comercial
que exige una clave. Hay una licencia Community gratuita para particulares, pero se renueva cada año y sus términos
no aclaran si la clave puede estar en un repositorio público. PrimeTek indica que los proyectos de código abierto
pueden usar las versiones MIT: PrimeNG 21 y anteriores.

## Decisión
- **Angular 21** con standalone components, **signals** y lazy loading por feature.
- **PrimeNG 21**, `@primeuix/themes` 2 y `primeicons` 7, todos bajo licencia MIT.
- Organización por features con separación de páginas y datos; lo compartido vive en `core` y `shared`.
- `npm run check:licenses` falla en la CI si una dependencia de producción deja de tener una licencia permisiva.

## Alternativas consideradas
- **PrimeNG 22 con clave Community:** obliga a registrarse y renovar cada año, y quien clone el repositorio
  vería el aviso de licencia al no tener clave. No es aceptable ocultar el aviso: lo prohíbe la licencia.
- **Angular Material (MIT):** no requiere clave y sigue la última versión de Angular, pero implicaría rehacer
  las pantallas y ofrece menos componentes listos, como tablas y calendarios avanzados.

## Consecuencias
- (+) Cualquiera puede clonar y ejecutar el proyecto sin claves ni avisos.
- (+) Stack alineado con el usado en el sector financiero, donde Angular y PrimeNG son habituales.
- (−) No se puede actualizar a Angular 22 mientras se use PrimeNG 21. Antes de que Angular 21 pierda soporte habrá
  que decidir entre la licencia Community y otra librería.
- (−) PrimeNG 21 dejará de recibir mejoras en algún momento, porque el desarrollo sigue en las versiones comerciales.
