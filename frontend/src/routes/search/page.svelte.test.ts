import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { search } from '#lib/api/search.ts';
import { ApiError } from '#lib/api/errors.ts';

vi.mock('#lib/api/search.ts', () => ({ search: vi.fn() }));
vi.mock('#lib/api/movies.ts', () => ({ posterSrc: (u: string) => `http://api.test${u}` }));
// vi.mock is hoisted above the imports, so anything it uses must be hoisted too
const pageState = vi.hoisted(() => ({ url: new URL('http://localhost/search?q=incep') }));
vi.mock('$app/state', () => ({ page: pageState }));

const inception = {
	id: '1',
	title: 'Inception',
	releaseYear: 2010,
	synopsis: null,
	posterUrl: null
};
const nolan = { id: '7', name: 'Christopher Nolan', bio: null };

describe('Search page', () => {
	beforeEach(() => {
		vi.mocked(search).mockReset();
		pageState.url = new URL('http://localhost/search?q=incep');
	});

	it('should show a loading indicator and then the results grouped under Movies and People, each a link', async () => {
		vi.mocked(search).mockResolvedValueOnce({ movies: [inception], people: [nolan] });
		render(Page);

		expect(screen.getByRole('status', { name: 'Searching' })).toBeInTheDocument();

		expect(await screen.findByRole('heading', { name: 'Movies (1)' })).toBeInTheDocument();
		expect(screen.getByRole('heading', { name: 'People (1)' })).toBeInTheDocument();
		expect(screen.getByRole('link', { name: 'Open Inception' })).toHaveAttribute(
			'href',
			'/movies/1'
		);
		expect(screen.getByRole('link', { name: 'Open Christopher Nolan' })).toHaveAttribute(
			'href',
			'/people/7'
		);
		expect(search).toHaveBeenCalledWith('incep');
	});

	it('should say which group is empty when only the other has matches', async () => {
		vi.mocked(search).mockResolvedValueOnce({ movies: [inception], people: [] });
		render(Page);

		expect(await screen.findByText('No people match')).toBeInTheDocument();
		expect(screen.getByRole('heading', { name: 'Movies (1)' })).toBeInTheDocument();
	});

	it('should show "No results for" the text when nothing matches', async () => {
		vi.mocked(search).mockResolvedValueOnce({ movies: [], people: [] });
		render(Page);

		expect(await screen.findByText("No results for 'incep'")).toBeInTheDocument();
	});

	it('should show the error with Retry when the search fails, and search again on Retry', async () => {
		vi.mocked(search)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'people-service is unavailable'))
			.mockResolvedValueOnce({ movies: [inception], people: [] });
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('people-service is unavailable');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		await waitFor(() =>
			expect(screen.getByRole('heading', { name: 'Movies (1)' })).toBeInTheDocument()
		);
		expect(search).toHaveBeenCalledTimes(2);
	});

	it('should ask for a search term and call nothing when the URL has no q', async () => {
		pageState.url = new URL('http://localhost/search');
		render(Page);

		expect(screen.getByRole('heading', { name: 'Search' })).toBeInTheDocument();
		expect(search).not.toHaveBeenCalled();
	});
});
