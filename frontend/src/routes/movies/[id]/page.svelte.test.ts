import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import Page from './+page.svelte';
import { getMovieDetail, deleteMovie } from '#lib/api/movies.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';
import { uploadPoster } from '#lib/api/upload.ts';
import { searchPeople } from '#lib/api/people.ts';
import { addCredit, removeCredit } from '#lib/api/credits.ts';

vi.mock('#lib/api/movies.ts', () => ({
	getMovieDetail: vi.fn(),
	deleteMovie: vi.fn(),
	posterSrc: (u: string) => `http://api.test${u}`
}));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));
vi.mock('#lib/api/upload.ts', () => ({ uploadPoster: vi.fn() }));
vi.mock('#lib/api/people.ts', () => ({ searchPeople: vi.fn() }));
vi.mock('#lib/api/credits.ts', () => ({ addCredit: vi.fn(), removeCredit: vi.fn() }));
vi.mock('$app/state', () => ({
	page: { params: { id: '3' }, url: new URL('http://localhost/movies/3') }
}));

const alien = {
	id: '3',
	title: 'Alien',
	releaseYear: 1979,
	synopsis: 'In space no one can hear you scream.',
	posterUrl: '/posters/3.jpg',
	credits: []
};

describe('Movie page', () => {
	beforeEach(() => {
		vi.mocked(getMovieDetail).mockReset();
		vi.mocked(deleteMovie).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
		vi.mocked(toastError).mockReset();
		vi.mocked(searchPeople).mockReset();
		vi.mocked(addCredit).mockReset();
		vi.mocked(removeCredit).mockReset();
	});

	it('should show a loading indicator, then poster, title, year, synopsis and the two empty credit lists', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading movie' })).toBeInTheDocument();

		expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Alien (1979)');
		expect(screen.getByRole('img', { name: 'Poster of Alien' })).toHaveAttribute(
			'src',
			'http://api.test/posters/3.jpg'
		);
		expect(screen.getByText('In space no one can hear you scream.')).toBeInTheDocument();
		expect(screen.getByRole('heading', { name: 'Cast' })).toBeInTheDocument();
		expect(screen.getByRole('heading', { name: 'Creators' })).toBeInTheDocument();
		expect(screen.getAllByText('None yet')).toHaveLength(2);
		expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/movies/3/edit');
		expect(getMovieDetail).toHaveBeenCalledWith('3');
	});

	it('should show a placeholder and omit the synopsis when the movie has neither', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce({ ...alien, posterUrl: null, synopsis: null });
		render(Page);

		expect(await screen.findByRole('img', { name: 'No poster for Alien' })).toBeInTheDocument();
		expect(screen.queryByText(alien.synopsis)).not.toBeInTheDocument();
	});

	it('should show "Movie not found" with a link back to the catalogue, and no Retry', async () => {
		vi.mocked(getMovieDetail).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 3 not found'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Movie not found');
		expect(screen.getByRole('link', { name: 'Back to the catalogue' })).toHaveAttribute(
			'href',
			'/'
		);
		expect(screen.queryByRole('link', { name: /Back to movies/ })).not.toBeInTheDocument(); // one way back, not two
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
	});

	it('should show the error with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(getMovieDetail)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce(alien);
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Alien');
		expect(getMovieDetail).toHaveBeenCalledTimes(2);
	});

	// E2-S5

	it('should ask for confirmation and change nothing on Cancel', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		expect(screen.getByRole('dialog')).toHaveTextContent('Delete Alien?');
		await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

		expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
		expect(deleteMovie).not.toHaveBeenCalled();
		expect(goto).not.toHaveBeenCalled();
	});

	it('should delete on confirm, show a success message and go to the grid', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		vi.mocked(deleteMovie).mockResolvedValueOnce(undefined);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/'));
		expect(deleteMovie).toHaveBeenCalledWith('3');
		expect(toastSuccess).toHaveBeenCalledWith('Alien deleted');
	});

	it('should keep the dialog open and disabled while the delete runs', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		let finish!: () => void; // the `!` tells TypeScript the Promise constructor below assigns it synchronously
		vi.mocked(deleteMovie).mockReturnValueOnce(new Promise<void>((r) => (finish = r)));
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		expect(screen.getByRole('button', { name: 'Deleting...' })).toBeDisabled();
		expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
		finish();
		await waitFor(() => expect(goto).toHaveBeenCalled());
		expect(deleteMovie).toHaveBeenCalledTimes(1);
	});

	it('should show an error toast and stay on the page when the delete fails', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		vi.mocked(deleteMovie).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'Cannot reach the server')
		);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('Cannot reach the server'));
		expect(goto).not.toHaveBeenCalled();
		expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Alien');
	});

	it('should switch to the not-found view when the movie was already deleted elsewhere', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		vi.mocked(deleteMovie).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 3 not found'));
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		expect(await screen.findByRole('alert')).toHaveTextContent('Movie not found');
		expect(screen.queryByRole('link', { name: 'Edit' })).not.toBeInTheDocument();
		expect(goto).not.toHaveBeenCalled();
	});

	// E3-S1

	it('should show the new poster once the upload succeeds', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce({ ...alien, posterUrl: null });
		vi.mocked(uploadPoster).mockResolvedValueOnce('/posters/3.png');
		render(Page);

		await screen.findByRole('img', { name: 'No poster for Alien' });
		const input = screen.getByLabelText('Poster') as HTMLInputElement;
		Object.defineProperty(input, 'files', {
			value: [new File(['x'], 'p.png', { type: 'image/png' })],
			configurable: true
		});
		await fireEvent.change(input);

		expect(await screen.findByRole('img', { name: 'Poster of Alien' })).toHaveAttribute(
			'src',
			'http://api.test/posters/3.png'
		);
		expect(screen.queryByRole('img', { name: 'No poster for Alien' })).not.toBeInTheDocument();
	});

	it('should keep the previous poster when the upload fails', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		vi.mocked(uploadPoster).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'Cannot reach the server')
		);
		render(Page);

		await screen.findByRole('img', { name: 'Poster of Alien' });
		const input = screen.getByLabelText('Poster') as HTMLInputElement;
		Object.defineProperty(input, 'files', {
			value: [new File(['x'], 'p.png', { type: 'image/png' })],
			configurable: true
		});
		await fireEvent.change(input);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		expect(screen.getByRole('img', { name: 'Poster of Alien' })).toHaveAttribute(
			'src',
			'http://api.test/posters/3.jpg'
		);
	});

	// E3-S3

	it('should show the replacement poster on the page once a new upload succeeds', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce({ ...alien, posterUrl: '/posters/3.jpg?v=1' });
		vi.mocked(uploadPoster).mockResolvedValueOnce('/posters/3.jpg?v=2');
		render(Page);

		await screen.findByRole('img', { name: 'Poster of Alien' });
		const input = screen.getByLabelText('Poster') as HTMLInputElement;
		Object.defineProperty(input, 'files', {
			value: [new File(['x'], 'p.jpg', { type: 'image/jpeg' })],
			configurable: true
		});
		await fireEvent.change(input);

		await waitFor(() =>
			expect(screen.getByRole('img', { name: 'Poster of Alien' })).toHaveAttribute(
				'src',
				'http://api.test/posters/3.jpg?v=2'
			)
		);
	});

	// E4-S1

	it('should list cast and creators from the credits, with each role', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce({
			...alien,
			credits: [
				{ id: '10', role: 'ACTOR', person: { id: '7', name: 'Sigourney Weaver' } },
				{ id: '11', role: 'DIRECTOR', person: { id: '8', name: 'Ridley Scott' } }
			]
		});
		render(Page);

		await screen.findByRole('heading', { level: 1 });
		const cast = screen.getByRole('heading', { name: 'Cast' }).parentElement as HTMLElement;
		const creators = screen.getByRole('heading', { name: 'Creators' }).parentElement as HTMLElement;
		expect(cast).toHaveTextContent('Sigourney Weaver');
		expect(cast).toHaveTextContent('Actor');
		expect(creators).toHaveTextContent('Ridley Scott');
		expect(creators).toHaveTextContent('Director');
		expect(screen.queryByText('None yet')).not.toBeInTheDocument();
	});

	it('should show the new credit under Cast after adding an actor', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(alien);
		vi.mocked(searchPeople).mockResolvedValueOnce([{ id: '7', name: 'Anna', bio: null }]);
		vi.mocked(addCredit).mockResolvedValueOnce({
			id: '10',
			role: 'ACTOR',
			person: { id: '7', name: 'Anna' }
		});
		render(Page);

		await screen.findByRole('heading', { level: 1 });
		vi.useFakeTimers();
		await fireEvent.input(screen.getByLabelText('Add a credit'), { target: { value: 'an' } });
		await vi.advanceTimersByTimeAsync(300);
		vi.useRealTimers();
		await fireEvent.click(await screen.findByRole('button', { name: 'Anna' }));
		await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

		const cast = screen.getByRole('heading', { name: 'Cast' }).parentElement as HTMLElement;
		await waitFor(() => expect(cast).toHaveTextContent('Anna'));
	});

	// E4-S2

	const withAnna = {
		...alien,
		credits: [{ id: '10', role: 'ACTOR' as const, person: { id: '7', name: 'Anna' } }]
	};

	it('should ask for confirmation before removing a credit and change nothing on Cancel', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(withAnna);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Remove Anna as Actor' }));
		expect(screen.getByRole('dialog')).toHaveTextContent('Remove Anna?');
		await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

		expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
		expect(removeCredit).not.toHaveBeenCalled();
		expect(screen.getByRole('heading', { name: 'Cast' }).parentElement).toHaveTextContent('Anna');
	});

	it('should remove the credit on confirm with a success message and keep the page', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(withAnna);
		vi.mocked(removeCredit).mockResolvedValueOnce(undefined);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Remove Anna as Actor' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove' })
		);

		await waitFor(() =>
			expect(screen.getByRole('heading', { name: 'Cast' }).parentElement).toHaveTextContent(
				'None yet'
			)
		);
		expect(removeCredit).toHaveBeenCalledWith('10');
		expect(toastSuccess).toHaveBeenCalledWith('Anna removed');
		expect(goto).not.toHaveBeenCalled();
	});

	it('should show an error toast and keep the credit when the removal fails', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(withAnna);
		vi.mocked(removeCredit).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'Cannot reach the server')
		);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Remove Anna as Actor' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove' })
		);

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('Cannot reach the server'));
		expect(screen.getByRole('heading', { name: 'Cast' }).parentElement).toHaveTextContent('Anna');
	});

	it('should keep the dialog open and disabled while the removal runs', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(withAnna);
		const pending = Promise.withResolvers<void>();
		vi.mocked(removeCredit).mockReturnValueOnce(pending.promise);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Remove Anna as Actor' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove' })
		);

		expect(screen.getByRole('button', { name: 'Removing...' })).toBeDisabled();
		expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
		pending.resolve();
		await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
		expect(removeCredit).toHaveBeenCalledTimes(1);
	});

	it('should drop a credit that was already removed elsewhere', async () => {
		vi.mocked(getMovieDetail).mockResolvedValueOnce(withAnna);
		vi.mocked(removeCredit).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Credit 10 not found'));
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Remove Anna as Actor' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove' })
		);

		await waitFor(() =>
			expect(screen.getByRole('heading', { name: 'Cast' }).parentElement).toHaveTextContent(
				'None yet'
			)
		);
		expect(toastError).not.toHaveBeenCalled();
	});
});
