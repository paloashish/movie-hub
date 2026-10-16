import { gql } from './client';
import type { Person, PersonDetail, PersonInput } from '#lib/types/graphql.ts';

const LIST_PEOPLE = /* GraphQL */ `
	query ListPeople {
		people {
			id
			name
			bio
		}
	}
`;

/** E1-S1: everyone, sorted by name on the server. */
export async function listPeople(): Promise<Person[]> {
	const data = await gql<{ people: Person[] }>(LIST_PEOPLE);
	return data.people;
}

const CREATE_PERSON = /* GraphQL */ `
	mutation CreatePerson($input: PersonInput!) {
		createPerson(input: $input) {
			id
			name
			bio
		}
	}
`;

/** E1-S2: create a person; the server validates and returns the saved person with its id. */
export async function createPerson(input: PersonInput): Promise<Person> {
	const data = await gql<{ createPerson: Person }>(CREATE_PERSON, { input });
	return data.createPerson;
}

const GET_PERSON = /* GraphQL */ `
	query GetPerson($id: ID!) {
		person(id: $id) {
			id
			name
			bio
		}
	}
`;

/** E1-S3: one person; a missing id rejects with an ApiError of code NOT_FOUND. */
export async function getPerson(id: string): Promise<Person> {
	const data = await gql<{ person: Person }>(GET_PERSON, { id });
	return data.person;
}

const UPDATE_PERSON = /* GraphQL */ `
	mutation UpdatePerson($id: ID!, $input: PersonInput!) {
		updatePerson(id: $id, input: $input) {
			id
			name
			bio
		}
	}
`;

/** E1-S3 */
export async function updatePerson(id: string, input: PersonInput): Promise<Person> {
	const data = await gql<{ updatePerson: Person }>(UPDATE_PERSON, { id, input });
	return data.updatePerson;
}

const DELETE_PERSON = /* GraphQL */ `
	mutation DeletePerson($id: ID!) {
		deletePerson(id: $id)
	}
`;

/** E1-S4: resolves when the person is gone; a missing id rejects with code NOT_FOUND. */
export async function deletePerson(id: string): Promise<void> {
	await gql<{ deletePerson: boolean }>(DELETE_PERSON, { id });
}

const SEARCH_PEOPLE = /* GraphQL */ `
	query SearchPeople($text: String!) {
		searchPeople(text: $text) {
			id
			name
			bio
		}
	}
`;

/** E4-S1 picker: people whose name contains the text. */
export async function searchPeople(text: string): Promise<Person[]> {
	const data = await gql<{ searchPeople: Person[] }>(SEARCH_PEOPLE, { text });
	return data.searchPeople;
}

const GET_PERSON_DETAIL = /* GraphQL */ `
	query GetPersonDetail($id: ID!) {
		person(id: $id) {
			id
			name
			bio
			credits {
				id
				role
				movie {
					id
					title
					releaseYear
					posterUrl
				}
			}
		}
	}
`;

/** E4-S3: the person page's shape, credits included (newest first on the server). */
export async function getPersonDetail(id: string): Promise<PersonDetail> {
	const data = await gql<{ person: PersonDetail }>(GET_PERSON_DETAIL, { id });
	return data.person;
}
