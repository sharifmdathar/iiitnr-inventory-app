import { describe, expect, test } from 'bun:test';
import {
  deriveIiitnrProfileFromEmail,
  pickDerivedProfileUpdates,
} from '../src/utils/iiitnr-email.js';
import { UserRole } from '../src/utils/enums.js';

describe('deriveIiitnrProfileFromEmail', () => {
  test('parses B.Tech student email with 4-year batch and branch', () => {
    const profile = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in');
    expect(profile).toEqual({
      role: UserRole.STUDENT,
      batch: '2024-2028',
      branch: 'CSE',
    });
  });

  test('parses ECE and DSAI B.Tech branch codes', () => {
    expect(deriveIiitnrProfileFromEmail('eceStudent24101@iiitnr.edu.in')?.branch).toBe('ECE');
    expect(deriveIiitnrProfileFromEmail('dsaiStudent24102@iiitnr.edu.in')?.branch).toBe('DSAI');
  });

  test('parses M.Tech course codes with 2-year batch and M.Tech branch label', () => {
    expect(deriveIiitnrProfileFromEmail('mtechCse24300@iiitnr.edu.in')).toEqual({
      role: UserRole.STUDENT,
      batch: '2024-2026',
      branch: 'M.Tech CSE',
    });
    expect(deriveIiitnrProfileFromEmail('mtechEce24301@iiitnr.edu.in')).toEqual({
      role: UserRole.STUDENT,
      batch: '2024-2026',
      branch: 'M.Tech ECE',
    });
    expect(deriveIiitnrProfileFromEmail('mtechDsai24302@iiitnr.edu.in')).toEqual({
      role: UserRole.STUDENT,
      batch: '2024-2026',
      branch: 'M.Tech DSAI',
    });
  });

  test('parses non-student email as LA', () => {
    const profile = deriveIiitnrProfileFromEmail('faculty@iiitnr.edu.in');
    expect(profile).toEqual({
      role: UserRole.LA,
      batch: null,
      branch: null,
    });
  });

  test('returns null for non-iiitnr domain', () => {
    expect(deriveIiitnrProfileFromEmail('user@example.com')).toBeNull();
  });
});

describe('pickDerivedProfileUpdates', () => {
  test('fills only missing batch and branch', () => {
    const derived = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in');
    if (!derived) throw new Error('Failed to derive profile');
    const updates = pickDerivedProfileUpdates(derived, {
      batch: null,
      branch: null,
    });
    expect(updates).toEqual({
      batch: '2024-2028',
      branch: 'CSE',
    });
  });

  test('skips batch and branch when already stored', () => {
    const derived = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in');
    if (!derived) throw new Error('Failed to derive profile');
    const updates = pickDerivedProfileUpdates(derived, {
      batch: '2023-2027',
      branch: 'ECE',
    });
    expect(updates).toEqual({});
  });
});
