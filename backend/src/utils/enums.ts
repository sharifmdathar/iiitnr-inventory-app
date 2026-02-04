import prismaDefault from '@prisma/client';

type EnumRuntime = Record<string, string>;

const {
  UserRole,
  RequestStatus,
  ComponentCategory,
  Location: PrismaLocation,
} = prismaDefault as {
  UserRole: EnumRuntime;
  RequestStatus: EnumRuntime;
  ComponentCategory: EnumRuntime;
  Location: EnumRuntime;
};

export { UserRole, RequestStatus, ComponentCategory, PrismaLocation };

export type UserRoleValue = string;
export type RequestStatusValue = string;
export type CategoryValue = string;
export type PrismaLocationValue = string;

export const requestStatusValues = Object.values(RequestStatus);
export const categoryValues = Object.values(ComponentCategory);

const locationEnumValues = Object.values(PrismaLocation) as PrismaLocationValue[];
export const locationValues = locationEnumValues.map((v) => v.replace(/_/g, ' '));
export type LocationValue = string;

export const toLocationEnum = (label: string): PrismaLocationValue =>
  label.replace(/\s+/g, '_') as PrismaLocationValue;
