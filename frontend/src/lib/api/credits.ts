import { gql } from './client';
import type { Credit, Role } from '#lib/types/graphql.ts';

const ADD_CREDIT = /* GraphQL */ `
	mutation AddCredit($movieId: ID!, $personId: ID!, $role: Role!) {
		addCredit(movieId: $movieId, personId: $personId, role: $role) {
			id
			role
			person {
				id
				name
			}
		}
	}
`;

/** E4-S1: credit a person on a movie; CONFLICT if they already hold that role there. */
export async function addCredit(movieId: string, personId: string, role: Role): Promise<Credit> {
	const data = await gql<{ addCredit: Credit }>(ADD_CREDIT, { movieId, personId, role });
	return data.addCredit;
}

const REMOVE_CREDIT = /* GraphQL */ `
	mutation RemoveCredit($id: ID!) {
		removeCredit(id: $id)
	}
`;

/** E4-S2: resolves when the credit is gone; a missing id rejects with code NOT_FOUND. */
export async function removeCredit(id: string): Promise<void> {
	await gql<{ removeCredit: boolean }>(REMOVE_CREDIT, { id });
}
