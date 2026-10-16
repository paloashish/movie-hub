import { gql } from './client';
import type { SearchResult } from '#lib/types/graphql.ts';

const SEARCH = /* GraphQL */ `
	query Search($text: String!) {
		search(text: $text) {
			movies {
				id
				title
				releaseYear
				synopsis
				posterUrl
			}
			people {
				id
				name
				bio
			}
		}
	}
`;

/** E5-S1: movies by title and people by name, partial and case-insensitive on the server. */
export async function search(text: string): Promise<SearchResult> {
	const data = await gql<{ search: SearchResult }>(SEARCH, { text });
	return data.search;
}
