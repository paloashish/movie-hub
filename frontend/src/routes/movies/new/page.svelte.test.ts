import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { createMovie } from '#lib/api/movies.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/movies.ts', () => ({ createMovie: vi.fn() }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

describe('Add movie page', () => {
	beforeEach(() => {
		vi.mocked(createMovie).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
	});

	it('should create the movie, show a success message and go to the grid', async () => {
		vi.mocked(createMovie).mockResolvedValueOnce({
			id: '7',
			title: 'Alien',
			releaseYear: 1979,
			synopsis: null,
			posterUrl: null
		});
		render(Page);

		await fireEvent.input(screen.getByLabelText('Title'), { target: { value: 'Alien' } });
		await fireEvent.input(screen.getByLabelText('Release year'), { target: { value: '1979' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/'));
		expect(createMovie).toHaveBeenCalledWith({ title: 'Alien', releaseYear: 1979, synopsis: null });
		expect(toastSuccess).toHaveBeenCalledWith('Alien added');
	});

	it('should stay on the page and show the message when the server rejects the input', async () => {
		vi.mocked(createMovie).mockRejectedValueOnce(
			new ApiError('VALIDATION', 'title: must not be blank')
		);
		render(Page);

		await fireEvent.input(screen.getByLabelText('Title'), { target: { value: 'x' } });
		await fireEvent.input(screen.getByLabelText('Release year'), { target: { value: '1979' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('title: must not be blank');
		expect(goto).not.toHaveBeenCalled();
	});
});
