import { describe, it, expect, vi, beforeEach } from 'vitest';
import { search } from './search';
import { gql } from './client';
import { ApiError } from './errors';

vi.mock('./client', () => ({ gql: vi.fn() }));

beforeEach(() => vi.mocked(gql).mockReset());

describe('search', () => {
	it('should send the text and return both groups', async () => {
		const result = {
			movies: [{ id: '1', title: 'Inception', releaseYear: 2010, synopsis: null, posterUrl: null }],
			people: []
		};
		vi.mocked(gql).mockResolvedValueOnce({ search: result });

		expect(await search('incep')).toEqual(result);
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ text: 'incep' });
	});

	it('should pass an ApiError from the client through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'people-service is unavailable')
		);

		await expect(search('x')).rejects.toMatchObject({ code: 'UNAVAILABLE' });
	});
});
