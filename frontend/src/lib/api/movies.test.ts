import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
	listMovies,
	getMovie,
	getMovieDetail,
	createMovie,
	updateMovie,
	deleteMovie,
	posterSrc
} from './movies';
import { gql } from './client';
import { ApiError } from './errors';

vi.mock('./client', () => ({ gql: vi.fn() }));
vi.mock('#lib/api/config.ts', () => ({ apiBaseUrl: 'http://api.test' }));

beforeEach(() => vi.mocked(gql).mockReset());

describe('listMovies', () => {
	it('should ask for the first page of 12 by default and return the page', async () => {
		const movie = { id: '1', title: 'Alien', releaseYear: 1979, synopsis: null, posterUrl: null };
		const page = { items: [movie], page: 0, size: 12, total: 1 };
		vi.mocked(gql).mockResolvedValueOnce({ movies: page });

		const result = await listMovies();

		expect(result).toEqual(page);
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ page: 0, size: 12, sort: 'TITLE_ASC' });
	});

	it('should ask for the given sort order', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ movies: { items: [], page: 0, size: 12, total: 0 } });

		await listMovies(0, 'YEAR_DESC');

		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ page: 0, size: 12, sort: 'YEAR_DESC' });
	});

	it('should ask for the given page', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ movies: { items: [], page: 2, size: 12, total: 30 } });

		await listMovies(2);

		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ page: 2, size: 12, sort: 'TITLE_ASC' });
	});

	it('should pass an ApiError from the client through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'boom'));

		await expect(listMovies()).rejects.toMatchObject({ code: 'UNAVAILABLE', message: 'boom' });
	});
});

describe('getMovie', () => {
	it('should query one movie by id', async () => {
		vi.mocked(gql).mockResolvedValueOnce({
			movie: {
				id: '3',
				title: 'Alien',
				releaseYear: 1979,
				synopsis: null,
				posterUrl: null
			}
		});

		const movie = await getMovie('3');

		expect(movie.title).toBe('Alien');
		expect(vi.mocked(gql).mock.calls[0][0]).not.toContain('credits'); // the edit form pays no gRPC hop
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '3' });
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 99 not found'));

		await expect(getMovie('99')).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});

describe('getMovieDetail', () => {
	it('should ask for the credits as well', async () => {
		vi.mocked(gql).mockResolvedValueOnce({
			movie: {
				id: '3',
				title: 'Alien',
				releaseYear: 1979,
				synopsis: null,
				posterUrl: null,
				credits: []
			}
		});

		const movie = await getMovieDetail('3');

		expect(movie.credits).toEqual([]);
		expect(vi.mocked(gql).mock.calls[0][0]).toContain('credits');
	});
});

describe('createMovie', () => {
	it('should send the input as a mutation variable and return the created movie', async () => {
		const created = { id: '7', title: 'Alien', releaseYear: 1979, synopsis: null, posterUrl: null };
		vi.mocked(gql).mockResolvedValueOnce({ createMovie: created });

		const movie = await createMovie({ title: 'Alien', releaseYear: 1979, synopsis: null });

		expect(movie).toEqual(created);
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({
			input: { title: 'Alien', releaseYear: 1979, synopsis: null }
		});
	});

	it('should pass a VALIDATION ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('VALIDATION', 'title: must not be blank'));

		await expect(
			createMovie({ title: '', releaseYear: 1979, synopsis: null })
		).rejects.toMatchObject({ code: 'VALIDATION' });
	});
});

describe('updateMovie', () => {
	it('should send id and input and return the updated movie', async () => {
		vi.mocked(gql).mockResolvedValueOnce({
			updateMovie: { id: '3', title: 'Aliens', releaseYear: 1986, synopsis: null, posterUrl: null }
		});

		const movie = await updateMovie('3', { title: 'Aliens', releaseYear: 1986, synopsis: null });

		expect(movie.title).toBe('Aliens');
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({
			id: '3',
			input: { title: 'Aliens', releaseYear: 1986, synopsis: null }
		});
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 99 not found'));

		await expect(
			updateMovie('99', { title: 'x', releaseYear: 2000, synopsis: null })
		).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});

describe('deleteMovie', () => {
	it('should send the id as a mutation variable', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ deleteMovie: true });

		await deleteMovie('3');

		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '3' });
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Movie 99 not found'));

		await expect(deleteMovie('99')).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});

describe('posterSrc', () => {
	it('should prefix a relative poster URL with the API origin', () => {
		expect(posterSrc('/posters/42.jpg')).toBe('http://api.test/posters/42.jpg');
	});
});
