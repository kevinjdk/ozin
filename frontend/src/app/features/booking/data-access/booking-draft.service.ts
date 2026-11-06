import { computed, Injectable, signal } from '@angular/core';

import { PublicStylist, SalonService, Slot } from '../../../core/api/models';

export type BookingStep = 'service' | 'stylist' | 'slot' | 'confirm';

/**
 * Reserva en curso. Es un servicio de la aplicación, no de la página, para que
 * lo elegido siga ahí si el visitante debe iniciar sesión o registrarse antes de confirmar.
 */
@Injectable({ providedIn: 'root' })
export class BookingDraft {
  readonly service = signal<SalonService | null>(null);
  readonly stylist = signal<PublicStylist | null>(null);
  readonly slot = signal<Slot | null>(null);

  readonly step = computed<BookingStep>(() => {
    if (!this.service()) {
      return 'service';
    }
    if (!this.stylist()) {
      return 'stylist';
    }
    return this.slot() ? 'confirm' : 'slot';
  });

  chooseService(service: SalonService): void {
    this.service.set(service);
    this.stylist.set(null);
    this.slot.set(null);
  }

  chooseStylist(stylist: PublicStylist): void {
    this.stylist.set(stylist);
    this.slot.set(null);
  }

  chooseSlot(slot: Slot): void {
    this.slot.set(slot);
  }

  back(): void {
    switch (this.step()) {
      case 'confirm':
        this.slot.set(null);
        break;
      case 'slot':
        this.stylist.set(null);
        break;
      case 'stylist':
        this.service.set(null);
        break;
    }
  }

  reset(): void {
    this.service.set(null);
    this.stylist.set(null);
    this.slot.set(null);
  }
}
