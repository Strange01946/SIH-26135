import { apiDelete, apiGet, apiPost } from './client';

const route = '/api/v1/venue-bookings';

export type BookingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED' | 'SUPERSEDED';
export type SessionInput = { startsAt: string; endsAt: string };
export type Session = SessionInput & { reservedAt?: string | null; releasedAt?: string | null };
export type TemporaryVenue = {
  name: string;
  address: string;
  stateId: number;
  districtId: number;
  capacity: number;
  permissionToUse: string;
};
export type SharedVenue = Omit<TemporaryVenue, 'permissionToUse'> & { id: number; code: string };
export type NewSharedVenue = Omit<SharedVenue, 'id'> & { pincode: string };
export type NewBooking = {
  centerId?: number;
  temporaryVenue?: TemporaryVenue;
  batchCode: string;
  seats: number;
  sessions: SessionInput[];
  skillsToTeach?: string;
  trainingNotes?: string;
};
export type BusySlot = SessionInput & { blackoutId?: number | null; reason?: string | null };
export type Booking = {
  id: number;
  providerId: number;
  providerName: string;
  centerId?: number | null;
  batchId?: number | null;
  replacesBookingId?: number | null;
  venueType: 'SHARED' | 'TEMPORARY';
  status: BookingStatus;
  batchCode: string;
  courseId?: number | null;
  courseName?: string | null;
  programId?: number | null;
  programName?: string | null;
  seats: number;
  venueName: string;
  venueAddress?: string | null;
  stateId: number;
  districtId: number;
  venueCapacity: number;
  premisesPermission?: string | null;
  decisionNote?: string | null;
  createdAt: string;
  reviewedAt?: string | null;
  batchStatus?: string | null;
  requirementId?: number | null;
  skillsToTeach?: string | null;
  trainingNotes?: string | null;
};
export type BookingDetails = { booking: Booking; sessions: Session[] };

export const getBookings = (status: BookingStatus | undefined, page: number, size: number, signal: AbortSignal) =>
  apiGet<Booking[]>(route, { params: { status, page, size }, signal });
export const getBookingDetails = (id: number, signal: AbortSignal) =>
  apiGet<BookingDetails>(route + '/' + id, { signal });
export const getSharedVenues = (stateId: number, districtId: number, page: number, size: number, signal: AbortSignal) =>
  apiGet<SharedVenue[]>(route + '/venues', {
    params: { stateId: stateId || undefined, districtId: districtId || undefined, page, size }, signal,
  });
export const getVenueAvailability = (id: number, from: string, to: string, signal: AbortSignal) =>
  apiGet<BusySlot[]>(route + '/venues/' + id + '/availability', { params: { from, to }, signal });
export const requestVenue = (payload: NewBooking) =>
  apiPost<NewBooking, BookingDetails>(route, payload);
export const reviewBooking = (id: number, approved: boolean, reason?: string) =>
  apiPost<{ approved: boolean; reason?: string }, BookingDetails>(route + '/' + id + '/review', { approved, reason });
export const rescheduleBooking = (id: number, sessions: SessionInput[]) =>
  apiPost<{ sessions: SessionInput[] }, BookingDetails>(route + '/' + id + '/reschedule', { sessions });
export const closeBooking = (id: number, action: 'COMPLETE' | 'CANCEL', reason?: string) =>
  apiPost<{ action: string; reason?: string }, BookingDetails>(route + '/' + id + '/close', { action, reason });
export const createSharedVenue = (payload: NewSharedVenue) =>
  apiPost<NewSharedVenue, SharedVenue>(route + '/venues', payload);
export const addVenueClosure = (id: number, payload: SessionInput & { reason: string }) =>
  apiPost<typeof payload, BusySlot>(route + '/venues/' + id + '/blackouts', payload);
export const releaseVenueClosure = (id: number, blackoutId: number) =>
  apiDelete(route + '/venues/' + id + '/blackouts/' + blackoutId);

