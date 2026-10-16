import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PosterUpload from './PosterUpload.svelte';
import { uploadPoster } from '#lib/api/upload.ts';
import { ApiError } from '#lib/api/errors.ts';
import { toastSuccess } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/upload.ts', () => ({ uploadPoster: vi.fn() }));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

const png = new File(['x'], 'poster.png', { type: 'image/png' });

async function pick(file: File) {
	const input = screen.getByLabelText('Poster') as HTMLInputElement;
	Object.defineProperty(input, 'files', { value: [file], configurable: true });
	await fireEvent.change(input);
}

describe('PosterUpload', () => {
	beforeEach(() => {
		vi.mocked(uploadPoster).mockReset();
		vi.mocked(toastSuccess).mockReset();
	});

	it('should show the hint and no progress bar before a file is picked', () => {
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });

		expect(screen.getByText('JPG or PNG, up to 10 MB')).toBeInTheDocument();
		expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
	});

	it('should show the percentage while uploading, then report the new url with a success message', async () => {
		const done = Promise.withResolvers<string>();
		vi.mocked(uploadPoster).mockImplementationOnce((_id, _file, onProgress) => {
			onProgress(40);
			return done.promise;
		});
		const onuploaded = vi.fn();
		render(PosterUpload, { props: { movieId: '7', onuploaded } });

		await pick(png);

		expect(screen.getByRole('progressbar', { name: 'Uploading' })).toHaveValue(40);
		expect(screen.getByText('40 %')).toBeInTheDocument();
		expect(screen.getByLabelText('Poster')).toBeDisabled();
		expect(uploadPoster).toHaveBeenCalledWith('7', png, expect.any(Function));

		done.resolve('/posters/7.png');
		await waitFor(() => expect(onuploaded).toHaveBeenCalledWith('/posters/7.png'));
		expect(toastSuccess).toHaveBeenCalledWith('Poster uploaded');
		expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
	});

	it('should show the error with Retry when the upload fails, and not report a url', async () => {
		vi.mocked(uploadPoster).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'Cannot reach the server')
		);
		const onuploaded = vi.fn();
		render(PosterUpload, { props: { movieId: '7', onuploaded } });

		await pick(png);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
		expect(onuploaded).not.toHaveBeenCalled();
	});

	it('should send the same file again on Retry', async () => {
		vi.mocked(uploadPoster)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce('/posters/7.png');
		const onuploaded = vi.fn();
		render(PosterUpload, { props: { movieId: '7', onuploaded } });

		await pick(png);
		await screen.findByRole('alert');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		await waitFor(() => expect(onuploaded).toHaveBeenCalledWith('/posters/7.png'));
		expect(uploadPoster).toHaveBeenCalledTimes(2);
		expect(vi.mocked(uploadPoster).mock.calls[1][1]).toBe(png);
	});

	it('should show the server message when the server rejects the file', async () => {
		vi.mocked(uploadPoster).mockRejectedValueOnce(
			new ApiError('VALIDATION', 'file type must be JPG or PNG')
		);
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });

		await pick(png);

		expect(await screen.findByRole('alert')).toHaveTextContent('file type must be JPG or PNG');
	});

	// E3-S2

	it('should reject a file that is not JPG or PNG at pick time and start no upload', async () => {
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });

		await pick(new File(['x'], 'clip.gif', { type: 'image/gif' }));

		expect(screen.getByRole('alert')).toHaveTextContent('file type must be JPG or PNG');
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
		expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
		expect(uploadPoster).not.toHaveBeenCalled();
	});

	it('should reject a file larger than 10 MB at pick time, naming the limit', async () => {
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });
		const big = new File(['x'], 'huge.png', { type: 'image/png' });
		Object.defineProperty(big, 'size', { value: 10 * 1024 * 1024 + 1 });

		await pick(big);

		expect(screen.getByRole('alert')).toHaveTextContent('file must be at most 10 MB');
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
		expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
		expect(uploadPoster).not.toHaveBeenCalled();
	});

	it('should accept a file of exactly 10 MB', async () => {
		vi.mocked(uploadPoster).mockResolvedValueOnce('/posters/7.png');
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });
		const limit = new File(['x'], 'limit.png', { type: 'image/png' });
		Object.defineProperty(limit, 'size', { value: 10 * 1024 * 1024 });

		await pick(limit);

		expect(uploadPoster).toHaveBeenCalledTimes(1);
	});

	it('should clear a pick-time error once a valid file is picked', async () => {
		vi.mocked(uploadPoster).mockResolvedValueOnce('/posters/7.png');
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });

		await pick(new File(['x'], 'clip.gif', { type: 'image/gif' }));
		await pick(png);

		await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
	});

	it('should do nothing when the picker is cancelled with no file', async () => {
		render(PosterUpload, { props: { movieId: '7', onuploaded: vi.fn() } });
		const input = screen.getByLabelText('Poster') as HTMLInputElement;
		Object.defineProperty(input, 'files', { value: [], configurable: true });

		await fireEvent.change(input);

		expect(screen.queryByRole('alert')).not.toBeInTheDocument();
		expect(uploadPoster).not.toHaveBeenCalled();
	});
});
