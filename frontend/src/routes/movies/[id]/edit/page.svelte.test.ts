import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { getMovie, updateMovie } from '#lib/api/movies.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/movies.ts', () => ({ getMovie: vi.fn(), updateMovie: vi.fn() }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('$app/state', () => ({
	page: { params: { id: '3' }, url: new URL('http://localhost/movies/3/edit') }
}));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

const alien = {
	id: '3',
	title: 'Alien',
	releaseYear: 1979,
	synopsis: 'Space',
	posterUrl: null
};

describe('Edit movie page', () => {
	beforeEach(() => {
		vi.mocked(getMovie).mockReset();
		vi.mocked(updateMovie).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
	});

	it('should show a loading indicator, then the form filled with the movie', async () => {
		vi.mocked(getMovie).mockResolvedValueOnce(alien);
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading movie' })).toBeInTheDocument();

		expect(await screen.findByLabelText('Title')).toHaveValue('Alien');
		expect(screen.getByLabelText('Release year')).toHaveValue(1979);
		expect(screen.getByLabelText('Synopsis (optional)')).toHaveValue('Space');
		expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/movies/3'); // same place as Back
		expect(getMovie).toHaveBeenCalledWith('3');
	});

	it('should show "Movie not found" without Retry when the movie does not exist', async () => {
		vi.mocked(getMovie).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 3 not found'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Movie not found');
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
		expect(screen.getByRole('link', { name: 'Back to the catalogue' })).toBeInTheDocument();
	});

	it('should show the error with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(getMovie)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce(alien);
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(await screen.findByLabelText('Title')).toHaveValue('Alien');
		expect(getMovie).toHaveBeenCalledTimes(2);
	});

	it('should save the changes, show a success message and go back to the movie page', async () => {
		vi.mocked(getMovie).mockResolvedValueOnce(alien);
		vi.mocked(updateMovie).mockResolvedValueOnce({ ...alien, title: 'Aliens', releaseYear: 1986 });
		render(Page);

		await fireEvent.input(await screen.findByLabelText('Title'), { target: { value: 'Aliens' } });
		await fireEvent.input(screen.getByLabelText('Release year'), { target: { value: '1986' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/movies/3'));
		expect(updateMovie).toHaveBeenCalledWith('3', {
			title: 'Aliens',
			releaseYear: 1986,
			synopsis: 'Space'
		});
		expect(toastSuccess).toHaveBeenCalledWith('Aliens saved');
	});

	it('should show the matching validation error and not save when the year is out of range', async () => {
		vi.mocked(getMovie).mockResolvedValueOnce(alien);
		render(Page);

		await fireEvent.input(await screen.findByLabelText('Release year'), {
			target: { value: '1800' }
		});
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(screen.getByRole('alert')).toHaveTextContent(
			'Release year must be a whole year between'
		);
		expect(updateMovie).not.toHaveBeenCalled();
	});

	it('should show a "not found" error under the form when the movie was deleted meanwhile', async () => {
		vi.mocked(getMovie).mockResolvedValueOnce(alien);
		vi.mocked(updateMovie).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 3 not found'));
		render(Page);

		await screen.findByLabelText('Title');
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('Movie 3 not found');
		expect(goto).not.toHaveBeenCalled();
	});
});
