import type { ErrorCode } from '#lib/types/graphql.ts';

/**
 * The one error shape the UI handles (application-architecture 4.1).
 * `code` drives what the page shows: NOT_FOUND page, VALIDATION under the field, anything else as a toast with Retry.
 */
export class ApiError extends Error {
	constructor(
		public readonly code: ErrorCode,
		message: string
	) {
		super(message);
		this.name = 'ApiError';
	}
}
