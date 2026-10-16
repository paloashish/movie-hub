import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import { createRawSnippet } from 'svelte';
import Layout from './+layout.svelte';
import { goto } from '$app/navigation';

// The layout reads the current URL from SvelteKit's page state; replace it with a fixed one.
import { pageState } from '../test-support/page-state.svelte.ts';

// The factory imports lazily, so the reactive helper is ready even though vi.mock is hoisted above the imports
vi.mock('$app/state', async () => ({
	page: (await import('../test-support/page-state.svelte.ts')).pageState
}));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));

// SvelteKit passes data and params to every layout; this one uses neither, so they are empty.
const props = {
	children: createRawSnippet(() => ({ render: () => '<p>page content</p>' })),
	data: {},
	params: {}
};

describe('+layout', () => {
	beforeEach(() => {
		vi.mocked(goto).mockReset();
		pageState.url = new URL('http://localhost/people?q=nolan');
	});

	it('should render the shell: brand, nav, content and footer', () => {
		render(Layout, props);

		expect(screen.getByRole('link', { name: 'Movie Hub' })).toHaveAttribute('href', '/');
		expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument();
		expect(screen.getByText('page content')).toBeInTheDocument();
		expect(screen.getByRole('contentinfo')).toHaveTextContent('Movie Hub');
	});

	it('should mark only the current section link with aria-current', () => {
		render(Layout, props);

		expect(screen.getByRole('link', { name: 'Cast & Creators' })).toHaveAttribute(
			'aria-current',
			'page'
		);
		expect(screen.getByRole('link', { name: 'Movies' })).not.toHaveAttribute('aria-current');
	});

	it('should send the search text to the results page on Enter', async () => {
		render(Layout, props);

		await fireEvent.input(screen.getByRole('searchbox', { name: 'Search' }), {
			target: { value: ' nolan ' }
		});
		await fireEvent.submit(screen.getByRole('search'));

		expect(goto).toHaveBeenCalledWith('/search?q=nolan');
	});

	it('should pre-fill the search box from the URL', () => {
		render(Layout, props);

		expect(screen.getByRole('searchbox', { name: 'Search' })).toHaveValue('nolan');
	});

	it('should follow the URL when it changes, as after Back', async () => {
		render(Layout, props);

		pageState.url = new URL('http://localhost/search?q=alien');

		await waitFor(() =>
			expect(screen.getByRole('searchbox', { name: 'Search' })).toHaveValue('alien')
		);
	});

	it('should not navigate when the search text is blank', async () => {
		render(Layout, props);

		await fireEvent.input(screen.getByRole('searchbox', { name: 'Search' }), {
			target: { value: '   ' }
		});
		await fireEvent.submit(screen.getByRole('search'));

		expect(goto).not.toHaveBeenCalled();
	});
});
