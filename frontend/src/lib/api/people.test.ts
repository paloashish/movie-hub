import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
	listPeople,
	createPerson,
	getPerson,
	updatePerson,
	deletePerson,
	searchPeople,
	getPersonDetail
} from './people';
import { gql } from './client';
import { ApiError } from './errors';

vi.mock('./client', () => ({ gql: vi.fn() }));

beforeEach(() => vi.mocked(gql).mockReset());

describe('createPerson', () => {
	it('should send the input as a mutation variable and return the created person', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ createPerson: { id: '7', name: 'Anna', bio: null } });

		const person = await createPerson({ name: 'Anna', bio: null });

		expect(person).toEqual({ id: '7', name: 'Anna', bio: null });
		const [query, variables] = vi.mocked(gql).mock.calls[0];
		expect(query).toContain('mutation');
		expect(variables).toEqual({ input: { name: 'Anna', bio: null } });
	});

	it('should pass an ApiError from the client through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('VALIDATION', 'name: must not be blank'));

		await expect(createPerson({ name: '', bio: null })).rejects.toMatchObject({
			code: 'VALIDATION',
			message: 'name: must not be blank'
		});
	});
});

describe('listPeople', () => {
	it('should query the people field and return the list', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ people: [{ id: '1', name: 'Anna', bio: null }] });

		const people = await listPeople();

		expect(people).toEqual([{ id: '1', name: 'Anna', bio: null }]);
		expect(vi.mocked(gql).mock.calls[0][0]).toContain('people');
	});

	it('should pass an ApiError from the client through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'boom'));

		await expect(listPeople()).rejects.toMatchObject({ code: 'UNAVAILABLE', message: 'boom' });
	});
});

describe('getPerson', () => {
	it('should query one person by id', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ person: { id: '3', name: 'Anna', bio: null } });

		const person = await getPerson('3');

		expect(person.name).toBe('Anna');
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '3' });
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 99 not found'));

		await expect(getPerson('99')).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});

describe('updatePerson', () => {
	it('should send id and input and return the updated person', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ updatePerson: { id: '3', name: 'Anna B', bio: null } });

		const person = await updatePerson('3', { name: 'Anna B', bio: null });

		expect(person.name).toBe('Anna B');
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({
			id: '3',
			input: { name: 'Anna B', bio: null }
		});
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 99 not found'));

		await expect(updatePerson('99', { name: 'x', bio: null })).rejects.toMatchObject({
			code: 'NOT_FOUND'
		});
	});
});

describe('deletePerson', () => {
	it('should send the id as a mutation variable', async () => {
		vi.mocked(gql).mockResolvedValueOnce({ deletePerson: true });

		await deletePerson('3');

		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '3' });
	});

	it('should pass a NOT_FOUND ApiError through unchanged', async () => {
		vi.mocked(gql).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 99 not found'));

		await expect(deletePerson('99')).rejects.toMatchObject({ code: 'NOT_FOUND' });
	});
});

describe('searchPeople', () => {
	it('should send the text and return the matches', async () => {
		vi.mocked(gql).mockResolvedValueOnce({
			searchPeople: [{ id: '1', name: 'Christopher Nolan', bio: null }]
		});

		const people = await searchPeople('nol');

		expect(people.map((p) => p.name)).toEqual(['Christopher Nolan']);
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ text: 'nol' });
	});
});

describe('getPersonDetail', () => {
	it('should ask for the credits with their movies', async () => {
		vi.mocked(gql).mockResolvedValueOnce({
			person: { id: '7', name: 'Anna', bio: null, credits: [] }
		});

		const person = await getPersonDetail('7');

		expect(person.credits).toEqual([]);
		expect(vi.mocked(gql).mock.calls[0][0]).toContain('credits');
		expect(vi.mocked(gql).mock.calls[0][1]).toEqual({ id: '7' });
	});
});
