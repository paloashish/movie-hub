import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import MovieCard from './MovieCard.svelte';

vi.mock('#lib/api/config.ts', () => ({ apiBaseUrl: 'http://api.test' }));

describe('MovieCard', () => {
	it('should show the poster with the title as alt text, plus title and year', () => {
		render(MovieCard, {
			props: {
				movie: {
					id: '1',
					title: 'Alien',
					releaseYear: 1979,
					synopsis: null,
					posterUrl: '/posters/1.jpg'
				}
			}
		});

		expect(screen.getByRole('img', { name: 'Poster of Alien' })).toHaveAttribute(
			'src',
			'http://api.test/posters/1.jpg'
		);
		expect(screen.getByRole('heading', { name: 'Alien' })).toBeInTheDocument();
		expect(screen.getByText('1979')).toBeInTheDocument();
	});

	it('should link the whole card to the movie page', () => {
		render(MovieCard, {
			props: {
				movie: { id: '7', title: 'Alien', releaseYear: 1979, synopsis: null, posterUrl: null }
			}
		});

		expect(screen.getByRole('link', { name: 'Open Alien' })).toHaveAttribute('href', '/movies/7');
	});

	it('should show a placeholder when the movie has no poster', () => {
		render(MovieCard, {
			props: {
				movie: { id: '2', title: 'Memento', releaseYear: 2000, synopsis: null, posterUrl: null }
			}
		});

		expect(screen.getByRole('img', { name: 'No poster for Memento' })).toHaveTextContent(
			'No poster'
		);
		expect(screen.queryByRole('img', { name: /Poster of/ })).not.toBeInTheDocument();
	});
});
