import { UserRole, type UserRoleValue } from './enums.js';

const IIITNR_DOMAIN = '@iiitnr.edu.in';
const STUDENT_LOCAL_RE = /^([a-z]+)(\d{2})(100|101|102|300|301|302)$/i;
const NON_STUDENT_LOCAL_RE = /^[a-z]+$/i;

interface CourseInfo {
  branch: string;
  durationYears: number;
}

const COURSE_INFO_BY_CODE: Record<string, CourseInfo> = {
  '100': { branch: 'CSE', durationYears: 4 },
  '101': { branch: 'ECE', durationYears: 4 },
  '102': { branch: 'DSAI', durationYears: 4 },
  '300': { branch: 'M.Tech CSE', durationYears: 2 },
  '301': { branch: 'M.Tech ECE', durationYears: 2 },
  '302': { branch: 'M.Tech DSAI', durationYears: 2 },
};

export interface IiitnrDerivedProfile {
  role: UserRoleValue;
  batch: string | null;
  branch: string | null;
}

export function deriveIiitnrProfileFromEmail(email: string): IiitnrDerivedProfile | null {
  const normalized = email.trim().toLowerCase();
  if (!normalized.endsWith(IIITNR_DOMAIN)) {
    return null;
  }

  const local = normalized.slice(0, -IIITNR_DOMAIN.length);
  const studentMatch = local.match(STUDENT_LOCAL_RE);
  if (studentMatch?.[2] != null && studentMatch[3] != null) {
    const yy = Number.parseInt(studentMatch[2], 10);
    const admissionYear = 2000 + yy;
    const courseCode = studentMatch[3];
    const info = COURSE_INFO_BY_CODE[courseCode];

    const branch = info?.branch ?? courseCode;
    const duration = info?.durationYears ?? 4;

    return {
      role: UserRole.STUDENT,
      batch: `${admissionYear}-${admissionYear + duration}`,
      branch,
    };
  }

  if (NON_STUDENT_LOCAL_RE.test(local)) {
    return {
      role: UserRole.LA,
      batch: null,
      branch: null,
    };
  }

  return null;
}

export function pickDerivedProfileUpdates(
  derived: IiitnrDerivedProfile,
  existing: {
    batch: string | null;
    branch: string | null;
  },
): Partial<{ batch: string; branch: string }> {
  const updates: Partial<{ batch: string; branch: string }> = {};

  if (existing.batch == null && derived.batch != null) {
    updates.batch = derived.batch;
  }
  if (existing.branch == null && derived.branch != null) {
    updates.branch = derived.branch;
  }

  return updates;
}
