import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Tema de marca de Ozin basado en Aura de PrimeNG.
 * Centralizar el preset permite cambiar la identidad visual sin tocar componentes.
 */
export const OzinPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{rose.50}',
      100: '{rose.100}',
      200: '{rose.200}',
      300: '{rose.300}',
      400: '{rose.400}',
      500: '{rose.500}',
      600: '{rose.600}',
      700: '{rose.700}',
      800: '{rose.800}',
      900: '{rose.900}',
      950: '{rose.950}',
    },
    colorScheme: {
      light: {
        // Tonos oscuros para que el texto blanco de los botones cumpla el contraste AA.
        primary: {
          color: '{rose.700}',
          contrastColor: '#ffffff',
          hoverColor: '{rose.800}',
          activeColor: '{rose.900}',
        },
        highlight: {
          background: '{rose.100}',
          focusBackground: '{rose.200}',
          color: '{rose.900}',
          focusColor: '{rose.950}',
        },
        surface: {
          0: '#ffffff',
          50: '{stone.50}',
          100: '{stone.100}',
          200: '{stone.200}',
          300: '{stone.300}',
          400: '{stone.400}',
          500: '{stone.500}',
          600: '{stone.600}',
          700: '{stone.700}',
          800: '{stone.800}',
          900: '{stone.900}',
          950: '{stone.950}',
        },
      },
    },
  },
});
