import { apiBaseUrl } from './config';
import { ApiError } from './errors';
import type { GraphQLResponse } from '#lib/types/graphql.ts';

/**
 * The only place the frontend speaks GraphQL. Pages never call fetch directly (coding-standards.md).
 * Plain fetch instead of a client library: ~30 lines, no dependency, easy to explain and to mock.
 */
export async function gql<T>(query: string, variables: Record<string, unknown> = {}): Promise<T> {
	let response: Response;
	try {
		response = await fetch(`${apiBaseUrl}/graphql`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ query, variables })
		});
	} catch {
		throw new ApiError('UNAVAILABLE', 'Cannot reach the server');
	}

	if (!response.ok) {
		// 502/503/504 mean the server is down or behind a struggling proxy: the UI should offer Retry.
		const code = response.status >= 502 && response.status <= 504 ? 'UNAVAILABLE' : 'INTERNAL';
		throw new ApiError(code, `Server returned ${response.status}`);
	}

	let body: GraphQLResponse<T>;
	try {
		body = (await response.json()) as GraphQLResponse<T>;
	} catch {
		throw new ApiError('INTERNAL', 'Unexpected response from server');
	}

	if (body.errors && body.errors.length > 0) {
		const first = body.errors[0];
		throw new ApiError(first.extensions?.code ?? 'INTERNAL', first.message);
	}
	if (body.data === undefined) {
		throw new ApiError('INTERNAL', 'Empty response from server');
	}
	return body.data;
}
