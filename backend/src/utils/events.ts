import { EventEmitter } from 'node:events';

export const events = new EventEmitter();

export enum EventType {
  REQUESTS_UPDATED = 'requests_updated',
}

export function notifyRequestsUpdated() {
  events.emit(EventType.REQUESTS_UPDATED);
}
