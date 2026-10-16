import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { uploadPoster } from './upload';

vi.mock('#lib/api/config.ts', () => ({ apiBaseUrl: 'http://api.test' }));

// A stand-in for the browser's XMLHttpRequest that the test drives by hand.
class FakeXhr {
	static last: FakeXhr;
	method = '';
	url = '';
	status = 0;
	responseText = '';
	body: FormData | null = null;
	upload: { onprogress: ((e: ProgressEvent) => void) | null } = { onprogress: null };
	onload: (() => void) | null = null;
	onerror: (() => void) | null = null;
	onabort: (() => void) | null = null;
	constructor() {
		FakeXhr.last = this;
	}
	open(method: string, url: string) {
		this.method = method;
		this.url = url;
	}
	send(body: FormData) {
		this.body = body;
	}
	progress(loaded: number, total: number) {
		this.upload.onprogress?.({ lengthComputable: true, loaded, total } as ProgressEvent);
	}
	respond(status: number, text: string) {
		this.status = status;
		this.responseText = text;
		this.onload?.();
	}
}

const file = new File(['x'], 'poster.png', { type: 'image/png' });

describe('uploadPoster', () => {
	beforeEach(() => vi.stubGlobal('XMLHttpRequest', FakeXhr));
	afterEach(() => vi.unstubAllGlobals());

	it('should POST the file as multipart to the movie poster endpoint', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.respond(200, '{"posterUrl":"/posters/7.png"}');

		await expect(pending).resolves.toBe('/posters/7.png');
		expect(FakeXhr.last.method).toBe('POST');
		expect(FakeXhr.last.url).toBe('http://api.test/movies/7/poster');
		expect(FakeXhr.last.body?.get('file')).toBe(file);
	});

	it('should report progress as a whole percentage, ending at 100', async () => {
		const onProgress = vi.fn();
		const pending = uploadPoster('7', file, onProgress);

		FakeXhr.last.progress(0, 200);
		FakeXhr.last.progress(101, 200);
		FakeXhr.last.respond(200, '{"posterUrl":"/posters/7.png"}');
		await pending;

		expect(onProgress.mock.calls.map((c) => c[0])).toEqual([0, 51, 100]);
	});

	it('should reject with the problem detail code and message on a 4xx', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.respond(400, '{"code":"VALIDATION","detail":"file type must be JPG or PNG"}');

		await expect(pending).rejects.toMatchObject({
			code: 'VALIDATION',
			message: 'file type must be JPG or PNG'
		});
	});

	it('should reject with NOT_FOUND when the movie does not exist', async () => {
		const pending = uploadPoster('99', file, vi.fn());
		FakeXhr.last.respond(404, '{"code":"NOT_FOUND","detail":"Movie 99 not found"}');

		await expect(pending).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});

	it('should reject with INTERNAL when a success answer is not JSON', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.respond(200, 'not json');

		await expect(pending).rejects.toMatchObject({
			code: 'INTERNAL',
			message: 'Unexpected response from server'
		});
	});

	it('should reject with UNAVAILABLE when the upload is aborted', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.onabort?.();

		await expect(pending).rejects.toMatchObject({
			code: 'UNAVAILABLE',
			message: 'Upload was interrupted'
		});
	});

	it('should reject with UNAVAILABLE when the network fails part way', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.onerror?.();

		await expect(pending).rejects.toMatchObject({
			code: 'UNAVAILABLE',
			message: 'Cannot reach the server'
		});
	});

	it('should reject with INTERNAL when the server answers something that is not a problem detail', async () => {
		const pending = uploadPoster('7', file, vi.fn());
		FakeXhr.last.respond(500, '<html>boom</html>');

		await expect(pending).rejects.toMatchObject({
			code: 'INTERNAL',
			message: 'Server returned 500'
		});
	});
});
