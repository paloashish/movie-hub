import { describe, it, expect } from 'vitest';
import { roleLabel } from './roles';

describe('roleLabel', () => {
	it('should turn the enum into a capitalised word', () => {
		expect(roleLabel('ACTOR')).toBe('Actor');
		expect(roleLabel('PRODUCER')).toBe('Producer');
	});
});
