import { gql } from './client';
import { apiBaseUrl } from './config';
import type { Movie, MovieDetail, MovieInput, MoviePage, MovieSort } from '#lib/types/graphql.ts';

/** The grid shows 12 per page (BRD assumptions). */
export const PAGE_SIZE = 12;

// Fetches the whole Movie type so the grid and the movie page (E2-S2) share one shape; synopsis is small.
const LIST_MOVIES = /* GraphQL */ `
	query ListMovies($page: Int!, $size: Int!, $sort: MovieSort!) {
		movies(page: $page, size: $size, sort: $sort) {
			items {
				id
				title
				releaseYear
				synopsis
				posterUrl
			}
			page
			size
			total
		}
	}
`;

/** E2-S1 + E2-S6 + E6-S1: one page of movies in the chosen order. Pages count from 0. */
export async function listMovies(page = 0, sort: MovieSort = 'TITLE_ASC'): Promise<MoviePage> {
	const data = await gql<{ movies: MoviePage }>(LIST_MOVIES, { page, size: PAGE_SIZE, sort });
	return data.movies;
}

const GET_MOVIE = /* GraphQL */ `
	query GetMovie($id: ID!) {
		movie(id: $id) {
			id
			title
			releaseYear
			synopsis
			posterUrl
		}
	}
`;

/** E2-S2: one movie without credits (the edit form needs no people, so no gRPC hop); NOT_FOUND if missing. */
export async function getMovie(id: string): Promise<Movie> {
	const data = await gql<{ movie: Movie }>(GET_MOVIE, { id });
	return data.movie;
}

const GET_MOVIE_DETAIL = /* GraphQL */ `
	query GetMovieDetail($id: ID!) {
		movie(id: $id) {
			id
			title
			releaseYear
			synopsis
			posterUrl
			credits {
				id
				role
				person {
					id
					name
				}
			}
		}
	}
`;

/** E4-S1: the movie page's shape, credits included; people are resolved from people-service on the server. */
export async function getMovieDetail(id: string): Promise<MovieDetail> {
	const data = await gql<{ movie: MovieDetail }>(GET_MOVIE_DETAIL, { id });
	return data.movie;
}

const CREATE_MOVIE = /* GraphQL */ `
	mutation CreateMovie($input: MovieInput!) {
		createMovie(input: $input) {
			id
			title
			releaseYear
			synopsis
			posterUrl
		}
	}
`;

/** E2-S3: create a movie; the server validates and returns the saved movie with its id. */
export async function createMovie(input: MovieInput): Promise<Movie> {
	const data = await gql<{ createMovie: Movie }>(CREATE_MOVIE, { input });
	return data.createMovie;
}

const UPDATE_MOVIE = /* GraphQL */ `
	mutation UpdateMovie($id: ID!, $input: MovieInput!) {
		updateMovie(id: $id, input: $input) {
			id
			title
			releaseYear
			synopsis
			posterUrl
		}
	}
`;

/** E2-S4: edit a movie; rejects with code NOT_FOUND if it was deleted meanwhile. */
export async function updateMovie(id: string, input: MovieInput): Promise<Movie> {
	const data = await gql<{ updateMovie: Movie }>(UPDATE_MOVIE, { id, input });
	return data.updateMovie;
}

const DELETE_MOVIE = /* GraphQL */ `
	mutation DeleteMovie($id: ID!) {
		deleteMovie(id: $id)
	}
`;

/** E2-S5: resolves when the movie is gone; a missing id rejects with code NOT_FOUND. */
export async function deleteMovie(id: string): Promise<void> {
	await gql<{ deleteMovie: boolean }>(DELETE_MOVIE, { id });
}

/** Posters are served by movie-service, so a relative posterUrl needs the API origin in front. */
export function posterSrc(posterUrl: string): string {
	return `${apiBaseUrl}${posterUrl}`;
}
