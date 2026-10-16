import { describe, it, expect, vi, beforeEach } from 'vitest';
import { gql } from './client';
import { ApiError } from './errors';

vi.mock('./config', () => ({ apiBaseUrl: 'http://test' }));

function mockFetch(status: number, body: unknown) {
	return vi.fn().mockResolvedValue({
		ok: status >= 200 && status < 300,
		status,
		json: async () => body
	});
}

describe('gql', () => {
	beforeEach(() => vi.unstubAllGlobals());

	it('should return data when the server answers without errors', async () => {
		vi.stubGlobal('fetch', mockFetch(200, { data: { ping: 'pong' } }));

		const data = await gql<{ ping: string }>('{ ping }');

		expect(data.ping).toBe('pong');
	});

	it('should post the query and variables to /graphql', async () => {
		const fetchMock = mockFetch(200, { data: {} });
		vi.stubGlobal('fetch', fetchMock);

		await gql('query M($id: ID!) { movie(id: $id) { id } }', { id: '42' });

		const [url, init] = fetchMock.mock.calls[0];
		expect(url).toBe('http://test/graphql');
		expect(JSON.parse(init.body)).toEqual({
			query: 'query M($id: ID!) { movie(id: $id) { id } }',
			variables: { id: '42' }
		});
	});

	it('should throw ApiError with the backend code when the response has GraphQL errors', async () => {
		vi.stubGlobal(
			'fetch',
			mockFetch(200, {
				errors: [{ message: 'Movie 42 not found', extensions: { code: 'NOT_FOUND' } }]
			})
		);

		await expect(gql('{ movie(id: 42) { id } }')).rejects.toMatchObject({
			name: 'ApiError',
			code: 'NOT_FOUND',
			message: 'Movie 42 not found'
		});
	});

	it('should throw INTERNAL when a GraphQL error carries no code', async () => {
		vi.stubGlobal('fetch', mockFetch(200, { errors: [{ message: 'boom' }] }));

		await expect(gql('{ ping }')).rejects.toMatchObject({ code: 'INTERNAL' });
	});

	it('should throw UNAVAILABLE when the network request fails', async () => {
		vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

		await expect(gql('{ ping }')).rejects.toMatchObject({
			code: 'UNAVAILABLE',
			message: 'Cannot reach the server'
		});
	});

	it('should throw INTERNAL with the status when the server returns a 500', async () => {
		vi.stubGlobal('fetch', mockFetch(500, {}));

		await expect(gql('{ ping }')).rejects.toBeInstanceOf(ApiError);
		await expect(gql('{ ping }')).rejects.toMatchObject({
			code: 'INTERNAL',
			message: 'Server returned 500'
		});
	});

	it('should throw UNAVAILABLE when the server returns 502, 503 or 504 so the UI offers Retry', async () => {
		for (const status of [502, 503, 504]) {
			vi.stubGlobal('fetch', mockFetch(status, {}));

			await expect(gql('{ ping }')).rejects.toMatchObject({ code: 'UNAVAILABLE' });
		}
	});

	it('should throw INTERNAL when the body is not JSON', async () => {
		vi.stubGlobal(
			'fetch',
			vi.fn().mockResolvedValue({
				ok: true,
				status: 200,
				json: async () => {
					throw new SyntaxError('bad json');
				}
			})
		);

		await expect(gql('{ ping }')).rejects.toMatchObject({
			code: 'INTERNAL',
			message: 'Unexpected response from server'
		});
	});

	it('should throw INTERNAL when the response has neither data nor errors', async () => {
		vi.stubGlobal('fetch', mockFetch(200, {}));

		await expect(gql('{ ping }')).rejects.toMatchObject({ code: 'INTERNAL' });
	});
});
