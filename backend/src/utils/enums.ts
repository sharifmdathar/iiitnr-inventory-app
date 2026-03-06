import {
  UserRole,
  RequestStatus,
  ComponentCategory,
  Location as PrismaLocation,
} from '@prisma/client';

export { UserRole, RequestStatus, ComponentCategory, PrismaLocation };

export type UserRoleValue = UserRole;
export type RequestStatusValue = RequestStatus;
export type CategoryValue = ComponentCategory;
export type PrismaLocationValue = PrismaLocation;

export const requestStatusValues = Object.values(RequestStatus);
export const categoryValues = Object.values(ComponentCategory);

const locationEnumValues = Object.values(PrismaLocation) as PrismaLocationValue[];
export const locationValues = locationEnumValues.map((v) => v.replace(/_/g, ' '));
export type LocationValue = string;

export const toLocationEnum = (label: string): PrismaLocationValue =>
  label.replace(/\s+/g, '_') as PrismaLocationValue;
