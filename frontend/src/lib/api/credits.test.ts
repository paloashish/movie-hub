import { describe, it, expect, vi, beforeEach } from 'vitest';
import { addCredit, removeCredit } from './credits';
import { gql } from './client';
import { ApiError } from './errors';

vi.mock('./client', () => ({ gql: vi.fn() }));

beforeEach(() => vi.mocked(gql).mockReset());

describe('addCredit', () => {
	it('should send movie, person and role and return the credit with its person', async () => {
		const credit = { id: '10', role: 'ACTOR', person: { id: '7', name: 'Anna' } };
		vi.mocked(gql).mockResolvedValueOnce({ addCredit: credit });

		const result = await addCredit('1', '7', 'ACTOR');

		expect(result).toEqual(credit);
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ movieId: '1', personId: '7', role: 'ACTOR' });
	});

	it('should pass a CONFLICT ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(
			new ApiError('CONFLICT', 'Anna is already credited as actor')
		);

		await expect(addCredit('1', '7', 'ACTOR')).rejects.toMatchObject({ code: 'CONFLICT' });
	});
});

describe('removeCredit', () => {
	it('should send the id as a mutation variable', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ removeCredit: true });

		await removeCredit('10');

		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '10' });
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Credit 99 not found'));

		await expect(removeCredit('99')).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});
