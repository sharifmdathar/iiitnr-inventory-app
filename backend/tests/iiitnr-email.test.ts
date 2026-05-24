import { describe, expect, test } from 'bun:test';
import {
  deriveIiitnrProfileFromEmail,
  pickDerivedProfileUpdates,
} from '../src/utils/iiitnr-email.js';
import { UserRole } from '../src/utils/enums.js';

describe('deriveIiitnrProfileFromEmail', () => {
  test('parses student email with batch and branch', () => {
    const profile = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in');
    expect(profile).toEqual({
      role: UserRole.STUDENT,
      batch: '2024-2028',
      branch: 'CSE',
    });
  });

  test('parses ECE and DSAI branch codes', () => {
    expect(deriveIiitnrProfileFromEmail('eceStudent24101@iiitnr.edu.in')?.branch).toBe('ECE');
    expect(deriveIiitnrProfileFromEmail('dsaiStudent24102@iiitnr.edu.in')?.branch).toBe('DSAI');
  });

  test('parses non-student email as TA', () => {
    const profile = deriveIiitnrProfileFromEmail('faculty@iiitnr.edu.in');
    expect(profile).toEqual({
      role: UserRole.TA,
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
    const derived = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in')!;
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
    const derived = deriveIiitnrProfileFromEmail('cseStudent24100@iiitnr.edu.in')!;
    const updates = pickDerivedProfileUpdates(derived, {
      batch: '2023-2027',
      branch: 'ECE',
    });
    expect(updates).toEqual({});
  });
});
