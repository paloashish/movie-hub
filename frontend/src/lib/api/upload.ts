import { apiBaseUrl } from './config';
import { ApiError } from './errors';
import type { ErrorCode } from '#lib/types/graphql.ts';

/**
 * E3-S1: the one non-GraphQL call. XMLHttpRequest instead of fetch because only XHR fires upload
 * progress events, which is where the percentage comes from (ADR 0003).
 * Errors arrive as RFC 9457 problem details with the same `code` vocabulary as GraphQL.
 */
export function uploadPoster(
	movieId: string,
	file: File,
	onProgress: (percent: number) => void
): Promise<string> {
	return new Promise((resolve, reject) => {
		const xhr = new XMLHttpRequest();
		xhr.open('POST', `${apiBaseUrl}/movies/${movieId}/poster`);

		xhr.upload.onprogress = (event) => {
			if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
		};
		xhr.onerror = () => reject(new ApiError('UNAVAILABLE', 'Cannot reach the server'));
		xhr.onabort = () => reject(new ApiError('UNAVAILABLE', 'Upload was interrupted'));
		xhr.onload = () => {
			if (xhr.status >= 200 && xhr.status < 300) {
				try {
					const posterUrl = (JSON.parse(xhr.responseText) as { posterUrl: string }).posterUrl;
					onProgress(100);
					resolve(posterUrl);
				} catch {
					reject(new ApiError('INTERNAL', 'Unexpected response from server'));
				}
			} else {
				reject(problemToError(xhr.status, xhr.responseText));
			}
		};

		const body = new FormData();
		body.append('file', file);
		xhr.send(body);
	});
}

function problemToError(status: number, text: string): ApiError {
	try {
		const problem = JSON.parse(text) as { code?: ErrorCode; detail?: string };
		if (problem.code) return new ApiError(problem.code, problem.detail ?? problem.code);
	} catch {
		// not a problem detail: fall through
	}
	const code = status >= 502 && status <= 504 ? 'UNAVAILABLE' : 'INTERNAL';
	return new ApiError(code, `Server returned ${status}`);
}
