// Falla si una dependencia de producción usa una licencia que no permita publicar el proyecto como
// código abierto. Protege frente a que una actualización introduzca, sin avisar, una licencia comercial,
// como ocurrió con PrimeNG 22 (ver docs/adr/0005-frontend-angular-primeng.md).
import { execSync } from 'node:child_process';

const ALLOWED = new Set(['MIT', '0BSD', 'ISC', 'Apache-2.0', 'BSD-2-Clause', 'BSD-3-Clause']);

/**
 * El package.json de PrimeNG remite a su archivo LICENSE.md, que declara MIT para las versiones sin sufijo -lts.
 * Se revisó a mano para la 21; una versión mayor distinta debe volver a revisarse.
 */
const REVIEWED = [/^primeng@21\.\d+\.\d+$/];

const tree = JSON.parse(
  execSync('npm ls --omit=dev --all --long --json', { encoding: 'utf8', maxBuffer: 1 << 28 }),
);

const licenses = new Map();
function visit(dependencies) {
  for (const [name, info] of Object.entries(dependencies ?? {})) {
    const id = `${name}@${info.version}`;
    if (!licenses.has(id)) {
      licenses.set(id, info.license);
      visit(info.dependencies);
    }
  }
}
visit(tree.dependencies);

const rejected = [...licenses].filter(
  ([id, license]) => !ALLOWED.has(license) && !REVIEWED.some((pattern) => pattern.test(id)),
);
for (const [id, license] of rejected) {
  console.error(`Licencia no permitida: ${id} -> ${JSON.stringify(license)}`);
}
console.log(`${licenses.size} dependencias de producción revisadas, ${rejected.length} rechazadas.`);
process.exit(rejected.length > 0 ? 1 : 0);
