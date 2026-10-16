import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { listMovies } from '#lib/api/movies.ts';
import { ApiError } from '#lib/api/errors.ts';
import type { Movie } from '#lib/types/graphql.ts';

vi.mock('#lib/api/movies.ts', () => ({
	listMovies: vi.fn(),
	PAGE_SIZE: 12,
	posterSrc: (u: string) => `http://api.test${u}`
}));

const alien = {
	id: '1',
	title: 'Alien',
	releaseYear: 1979,
	synopsis: null,
	posterUrl: '/posters/1.jpg'
};
const memento = { id: '2', title: 'Memento', releaseYear: 2000, synopsis: null, posterUrl: null };
const pageOf = (items: Movie[], page = 0, total = items.length) => ({
	items,
	page,
	size: 12,
	total
});

describe('Movies page', () => {
	beforeEach(() => vi.mocked(listMovies).mockReset());

	it('should show a loading indicator and then the poster grid in the order the server gave', async () => {
		vi.mocked(listMovies).mockResolvedValueOnce(pageOf([alien, memento]));
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading movies' })).toBeInTheDocument();

		const headings = await screen.findAllByRole('heading', { level: 2 });
		expect(headings.map((h) => h.textContent)).toEqual(['Alien', 'Memento']);
		expect(screen.getByRole('img', { name: 'Poster of Alien' })).toBeInTheDocument();
		expect(screen.getByRole('img', { name: 'No poster for Memento' })).toBeInTheDocument();
		expect(screen.queryByRole('status', { name: 'Loading movies' })).not.toBeInTheDocument();
		expect(listMovies).toHaveBeenCalledWith(0, 'TITLE_ASC');
	});

	it('should link to the add-movie form', async () => {
		vi.mocked(listMovies).mockResolvedValueOnce(pageOf([]));
		render(Page);

		expect(screen.getByRole('link', { name: '+ Add movie' })).toHaveAttribute(
			'href',
			'/movies/new'
		);
		await screen.findByText('No movies yet'); // let the load finish inside the test
	});

	it('should show "No movies yet" when the list is empty', async () => {
		vi.mocked(listMovies).mockResolvedValueOnce(pageOf([]));
		render(Page);

		expect(await screen.findByText('No movies yet')).toBeInTheDocument();
	});

	it('should show the error message with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(listMovies)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce(pageOf([alien]));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		await waitFor(() => expect(screen.getByRole('heading', { name: 'Alien' })).toBeInTheDocument());
		expect(listMovies).toHaveBeenCalledTimes(2);
	});

	it('should show a generic message when the failure is not an ApiError', async () => {
		vi.mocked(listMovies).mockRejectedValueOnce(new TypeError('weird'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
	});

	// E2-S6

	it('should show no page controls when everything fits on one page', async () => {
		vi.mocked(listMovies).mockResolvedValueOnce(pageOf([alien], 0, 12));
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		expect(screen.queryByRole('navigation', { name: 'Pages' })).not.toBeInTheDocument();
	});

	it('should show page controls with the current page number when there are more than 12 movies', async () => {
		vi.mocked(listMovies).mockResolvedValueOnce(pageOf([alien], 0, 13));
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		expect(screen.getByRole('navigation', { name: 'Pages' })).toHaveTextContent('Page 1 of 2');
	});

	it('should show a loading indicator and then the next page when the page changes', async () => {
		const secondPage = Promise.withResolvers<ReturnType<typeof pageOf>>();
		vi.mocked(listMovies)
			.mockResolvedValueOnce(pageOf([alien], 0, 13))
			.mockReturnValueOnce(secondPage.promise);
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		await fireEvent.click(screen.getByRole('button', { name: 'Next' }));

		expect(screen.getByRole('status', { name: 'Loading movies' })).toBeInTheDocument();
		secondPage.resolve(pageOf([memento], 1, 13));
		expect(await screen.findByRole('heading', { name: 'Memento' })).toBeInTheDocument();
		expect(screen.queryByRole('heading', { name: 'Alien' })).not.toBeInTheDocument();
		expect(screen.getByRole('navigation', { name: 'Pages' })).toHaveTextContent('Page 2 of 2');
		expect(listMovies).toHaveBeenLastCalledWith(1, 'TITLE_ASC');
	});

	it('should retry the requested page when a page change fails', async () => {
		vi.mocked(listMovies)
			.mockResolvedValueOnce(pageOf([alien], 0, 13))
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce(pageOf([memento], 1, 13));
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		await fireEvent.click(screen.getByRole('button', { name: 'Next' }));
		await screen.findByRole('alert');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(await screen.findByRole('heading', { name: 'Memento' })).toBeInTheDocument();
		expect(listMovies).toHaveBeenLastCalledWith(1, 'TITLE_ASC');
	});

	// E6-S1

	it('should reload in the chosen order from the first page and show that order', async () => {
		vi.mocked(listMovies)
			.mockResolvedValueOnce(pageOf([alien], 0, 13))
			.mockResolvedValueOnce(pageOf([memento], 1, 13))
			.mockResolvedValueOnce(pageOf([memento, alien], 0, 13));
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		await fireEvent.click(screen.getByRole('button', { name: 'Next' })); // go to page 2 first
		await screen.findByRole('heading', { name: 'Memento' });
		await fireEvent.change(screen.getByLabelText('Sort'), { target: { value: 'YEAR_DESC' } });

		await waitFor(() =>
			expect(screen.getByRole('navigation', { name: 'Pages' })).toHaveTextContent('Page 1 of 2')
		);
		const headings = screen.getAllByRole('heading', { level: 2 });
		expect(headings.map((h) => h.textContent)).toEqual(['Memento', 'Alien']);
		expect(listMovies).toHaveBeenLastCalledWith(0, 'YEAR_DESC');
		expect(screen.getByLabelText('Sort')).toHaveValue('YEAR_DESC');
	});

	it('should ignore an older answer that lands after the sort was changed again', async () => {
		const slow = Promise.withResolvers<ReturnType<typeof pageOf>>();
		vi.mocked(listMovies)
			.mockResolvedValueOnce(pageOf([alien], 0, 1))
			.mockReturnValueOnce(slow.promise) // YEAR_DESC, slow
			.mockResolvedValueOnce(pageOf([memento], 0, 1)); // TITLE_DESC, fast
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		await fireEvent.change(screen.getByLabelText('Sort'), { target: { value: 'YEAR_DESC' } });
		await fireEvent.change(screen.getByLabelText('Sort'), { target: { value: 'TITLE_DESC' } });
		await screen.findByRole('heading', { name: 'Memento' });
		slow.resolve(pageOf([alien], 0, 1)); // the old answer lands last

		await waitFor(() =>
			expect(screen.getByRole('heading', { name: 'Memento' })).toBeInTheDocument()
		);
		expect(screen.queryByRole('heading', { name: 'Alien' })).not.toBeInTheDocument();
	});

	it('should fall back to the last page when the requested page is past the end', async () => {
		vi.mocked(listMovies)
			.mockResolvedValueOnce(pageOf([alien], 0, 13))
			.mockResolvedValueOnce(pageOf([], 1, 12)) // page 2 asked for, but a delete elsewhere left only 12 movies
			.mockResolvedValueOnce(pageOf([memento], 0, 12));
		render(Page);

		await screen.findByRole('heading', { name: 'Alien' });
		await fireEvent.click(screen.getByRole('button', { name: 'Next' }));

		expect(await screen.findByRole('heading', { name: 'Memento' })).toBeInTheDocument();
		expect(listMovies).toHaveBeenLastCalledWith(0, 'TITLE_ASC');
	});
});
