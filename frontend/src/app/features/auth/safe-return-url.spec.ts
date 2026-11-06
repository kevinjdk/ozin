import { safeReturnUrl } from './safe-return-url';

describe('safeReturnUrl', () => {
  it('acepta rutas internas', () => {
    expect(safeReturnUrl('/reservar')).toBe('/reservar');
    expect(safeReturnUrl('/mis-citas?page=2')).toBe('/mis-citas?page=2');
  });

  it.each(['https://sitio-falso.example', '//sitio-falso.example', '/\\sitio-falso.example', 'reservar', '', undefined])(
    'rechaza %s',
    (url) => {
      expect(safeReturnUrl(url)).toBeNull();
    },
  );
});
