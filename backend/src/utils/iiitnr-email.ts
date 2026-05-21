import { UserRole, type UserRoleValue } from './enums.js';

const IIITNR_DOMAIN = '@iiitnr.edu.in';
const STUDENT_LOCAL_RE = /^([a-z]+)(\d{2})(100|101|102)$/i;
const NON_STUDENT_LOCAL_RE = /^[a-z]+$/i;

const BRANCH_BY_CODE: Record<string, string> = {
  '100': 'CSE',
  '101': 'ECE',
  '102': 'DSAI',
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
    const branchCode = studentMatch[3];
    return {
      role: UserRole.STUDENT,
      batch: `${admissionYear}-${admissionYear + 4}`,
      branch: BRANCH_BY_CODE[branchCode] ?? branchCode,
    };
  }

  if (NON_STUDENT_LOCAL_RE.test(local)) {
    return {
      role: UserRole.TA,
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
