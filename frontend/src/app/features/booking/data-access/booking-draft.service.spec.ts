import { PublicStylist, SalonService, Slot } from '../../../core/api/models';
import { BookingDraft } from './booking-draft.service';

const SERVICE: SalonService = { id: 's', name: 'Corte', durationMinutes: 45, price: 12, currency: 'USD', active: true };
const STYLIST: PublicStylist = { id: 'e', branchId: 'b', fullName: 'Ana', serviceIds: ['s'] };
const SLOT: Slot = { start: '2026-11-09T15:00:00Z', end: '2026-11-09T15:45:00Z' };

describe('BookingDraft', () => {
  it('avanza paso a paso a medida que se elige', () => {
    const draft = new BookingDraft();
    expect(draft.step()).toBe('service');

    draft.chooseService(SERVICE);
    expect(draft.step()).toBe('stylist');

    draft.chooseStylist(STYLIST);
    expect(draft.step()).toBe('slot');

    draft.chooseSlot(SLOT);
    expect(draft.step()).toBe('confirm');
  });

  it('volver deshace solo el último paso', () => {
    const draft = new BookingDraft();
    draft.chooseService(SERVICE);
    draft.chooseStylist(STYLIST);
    draft.chooseSlot(SLOT);

    draft.back();

    expect(draft.step()).toBe('slot');
    expect(draft.stylist()).toEqual(STYLIST);
  });

  it('cambiar de servicio descarta lo que dependía de él', () => {
    const draft = new BookingDraft();
    draft.chooseService(SERVICE);
    draft.chooseStylist(STYLIST);
    draft.chooseSlot(SLOT);

    draft.chooseService({ ...SERVICE, id: 'otro' });

    expect(draft.stylist()).toBeNull();
    expect(draft.slot()).toBeNull();
    expect(draft.step()).toBe('stylist');
  });
});
