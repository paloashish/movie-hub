// Hand-written types matching movie-service's schema.graphqls. Grows one story at a time.

/** Error codes set by the backend edge mappers (ADR 0007). */
export type ErrorCode = 'NOT_FOUND' | 'VALIDATION' | 'CONFLICT' | 'UNAVAILABLE' | 'INTERNAL';

export interface GraphQLErrorShape {
	message: string;
	extensions?: { code?: ErrorCode };
}

export interface GraphQLResponse<T> {
	data?: T;
	errors?: GraphQLErrorShape[];
}

/** GraphQL `Person` (E1-S1). IDs are strings on the wire, as GraphQL's ID scalar requires. */
export interface Person {
	id: string;
	name: string;
	bio: string | null;
}

/** GraphQL `PersonInput` (E1-S2). */
export interface PersonInput {
	name: string;
	bio: string | null;
}

/** GraphQL `Movie` (E2-S1). `posterUrl` is relative to movie-service and null until E3 uploads one. */
export interface Movie {
	id: string;
	title: string;
	releaseYear: number;
	synopsis: string | null;
	posterUrl: string | null;
}

/** GraphQL `MovieInput` (E2-S3). */
export interface MovieInput {
	title: string;
	releaseYear: number;
	synopsis: string | null;
}

/** GraphQL `MoviePage` (E2-S6): one page of the grid plus what the page controls need. */
export interface MoviePage {
	items: Movie[];
	page: number;
	size: number;
	total: number;
}

/** GraphQL `Role` (E4-S1). ACTOR is cast; the others are creators. */
export type Role = 'ACTOR' | 'DIRECTOR' | 'WRITER' | 'PRODUCER';
export const ROLES: Role[] = ['ACTOR', 'DIRECTOR', 'WRITER', 'PRODUCER'];

/** GraphQL `Credit` (E4-S1): a person's role on a movie, person resolved by the server. */
export interface Credit {
	id: string;
	role: Role;
	person: Pick<Person, 'id' | 'name'>; // the page shows names only, so bio is never fetched
}

/** The movie page's shape: a movie plus its credits. The grid never asks for credits. */
export interface MovieDetail extends Movie {
	credits: Credit[];
}

/** GraphQL `PersonCredit` (E4-S3): a credit seen from the person's side. */
export interface PersonCredit {
	id: string;
	role: Role;
	movie: Pick<Movie, 'id' | 'title' | 'releaseYear' | 'posterUrl'>;
}

/** The person page's shape: a person plus their credits, newest first. */
export interface PersonDetail extends Person {
	credits: PersonCredit[];
}

/** GraphQL `SearchResult` (E5-S1): one box, two groups. */
export interface SearchResult {
	movies: Movie[];
	people: Person[];
}

/** GraphQL `MovieSort` (E6-S1). */
export type MovieSort = 'TITLE_ASC' | 'TITLE_DESC' | 'YEAR_ASC' | 'YEAR_DESC';
export const MOVIE_SORTS: { value: MovieSort; label: string }[] = [
	{ value: 'TITLE_ASC', label: 'Title A to Z' },
	{ value: 'TITLE_DESC', label: 'Title Z to A' },
	{ value: 'YEAR_DESC', label: 'Newest first' },
	{ value: 'YEAR_ASC', label: 'Oldest first' }
];
