import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import Page from './+page.svelte';
import { getPersonDetail, deletePerson } from '#lib/api/people.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/people.ts', () => ({ getPersonDetail: vi.fn(), deletePerson: vi.fn() }));
vi.mock('#lib/api/movies.ts', () => ({ posterSrc: (u: string) => `http://api.test${u}` }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('$app/state', () => ({
	page: { params: { id: '7' }, url: new URL('http://localhost/people/7') }
}));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

const anna = { id: '7', name: 'Anna', bio: 'Actor', credits: [] };
const withMovies = {
	...anna,
	credits: [
		{
			id: '11',
			role: 'DIRECTOR' as const,
			movie: { id: '2', title: 'Aliens', releaseYear: 1986, posterUrl: '/posters/2.jpg?v=1' }
		},
		{
			id: '10',
			role: 'ACTOR' as const,
			movie: { id: '1', title: 'Alien', releaseYear: 1979, posterUrl: null }
		}
	]
};

describe('Person page', () => {
	beforeEach(() => {
		vi.mocked(getPersonDetail).mockReset();
		vi.mocked(deletePerson).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
		vi.mocked(toastError).mockReset();
	});

	it('should show a loading indicator, then name, bio and the movies with poster, title, year and role', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(withMovies);
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading person' })).toBeInTheDocument();

		expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Anna');
		expect(screen.getByText('Actor')).toBeInTheDocument();
		const links = screen.getAllByRole('link', { name: /^Open / });
		expect(links.map((l) => l.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
			'Aliens 1986 · Director',
			'Alien 1979 · Actor'
		]);
		expect(links[0]).toHaveAttribute('href', '/movies/2');
		expect(within(links[0]).getByRole('img', { name: 'Poster of Aliens' })).toHaveAttribute(
			'src',
			'http://api.test/posters/2.jpg?v=1'
		);
		expect(getPersonDetail).toHaveBeenCalledWith('7');
	});

	it('should say "No credits yet" for a person with none', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		render(Page);

		expect(await screen.findByText('No credits yet')).toBeInTheDocument();
	});

	it('should link to the edit page', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		render(Page);

		expect(await screen.findByRole('link', { name: 'Edit' })).toHaveAttribute(
			'href',
			'/people/7/edit'
		);
	});

	it('should show "Person not found" without Retry when the person does not exist', async () => {
		vi.mocked(getPersonDetail).mockRejectedValueOnce(
			new ApiError('NOT_FOUND', 'Person 7 not found')
		);
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Person not found');
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
	});

	it('should show the error with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(getPersonDetail)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce(anna);
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Anna');
		expect(getPersonDetail).toHaveBeenCalledTimes(2);
	});

	it('should show a generic message when the failure is not an ApiError', async () => {
		vi.mocked(getPersonDetail).mockRejectedValueOnce(new TypeError('weird'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
	});

	// Delete, moved here from the people list (E1-S4)

	it('should ask for confirmation and change nothing on Cancel', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		expect(screen.getByRole('dialog')).toHaveTextContent('Delete Anna?');
		await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

		expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
		expect(deletePerson).not.toHaveBeenCalled();
	});

	it('should delete on confirm with a success message and go to the list', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		vi.mocked(deletePerson).mockResolvedValueOnce(undefined);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/people'));
		expect(deletePerson).toHaveBeenCalledWith('7');
		expect(toastSuccess).toHaveBeenCalledWith('Anna deleted');
	});

	it('should keep the dialog open and disabled while the delete runs', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		const pending = Promise.withResolvers<void>();
		vi.mocked(deletePerson).mockReturnValueOnce(pending.promise);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		expect(screen.getByRole('button', { name: 'Deleting...' })).toBeDisabled();
		expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
		pending.resolve();
		await waitFor(() => expect(goto).toHaveBeenCalled());
		expect(deletePerson).toHaveBeenCalledTimes(1);
	});

	it('should show an error toast and stay on the page when people-service is unavailable', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		vi.mocked(deletePerson).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'Cannot reach the server')
		);
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('Cannot reach the server'));
		expect(goto).not.toHaveBeenCalled();
		expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Anna');
	});

	it('should switch to the not-found view when the person was already deleted elsewhere', async () => {
		vi.mocked(getPersonDetail).mockResolvedValueOnce(anna);
		vi.mocked(deletePerson).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 7 not found'));
		render(Page);

		await fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
		await fireEvent.click(
			within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' })
		);

		expect(await screen.findByRole('alert')).toHaveTextContent('Person not found');
		expect(goto).not.toHaveBeenCalled();
	});
});
