import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CreditPicker from './CreditPicker.svelte';
import { searchPeople } from '#lib/api/people.ts';
import { addCredit } from '#lib/api/credits.ts';
import { ApiError } from '#lib/api/errors.ts';
import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/people.ts', () => ({ searchPeople: vi.fn() }));
vi.mock('#lib/api/credits.ts', () => ({ addCredit: vi.fn() }));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

const anna = { id: '7', name: 'Anna', bio: null };
const credit = { id: '10', role: 'ACTOR' as const, person: { id: anna.id, name: anna.name } };

async function type(text: string) {
	await fireEvent.input(screen.getByLabelText('Add a credit'), { target: { value: text } });
	await vi.advanceTimersByTimeAsync(300); // past the debounce
}

describe('CreditPicker', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		vi.mocked(searchPeople).mockReset();
		vi.mocked(addCredit).mockReset();
		vi.mocked(toastSuccess).mockReset();
		vi.mocked(toastError).mockReset();
	});
	afterEach(() => vi.useRealTimers());

	it('should search once the typing pauses and list the matches', async () => {
		vi.mocked(searchPeople).mockResolvedValueOnce([anna]);
		render(CreditPicker, { props: { movieId: '1', onadded: vi.fn() } });

		await type('an');

		expect(searchPeople).toHaveBeenCalledWith('an');
		expect(screen.getByRole('button', { name: 'Anna' })).toBeInTheDocument();
	});

	it('should say "No people found" with a link to add one when nothing matches', async () => {
		vi.mocked(searchPeople).mockResolvedValueOnce([]);
		render(CreditPicker, { props: { movieId: '1', onadded: vi.fn() } });

		await type('zzz');

		expect(screen.getByRole('status')).toHaveTextContent('No people found');
		expect(screen.getByRole('link', { name: 'Add a person' })).toHaveAttribute(
			'href',
			'/people/new'
		);
	});

	it('should keep Add disabled until a person is chosen, then add with the picked role', async () => {
		vi.mocked(searchPeople).mockResolvedValueOnce([anna]);
		vi.mocked(addCredit).mockResolvedValueOnce({ ...credit, role: 'DIRECTOR' });
		const onadded = vi.fn();
		render(CreditPicker, { props: { movieId: '1', onadded } });

		expect(screen.getByRole('button', { name: 'Add' })).toBeDisabled();
		await type('an');
		await fireEvent.click(screen.getByRole('button', { name: 'Anna' }));
		await fireEvent.change(screen.getByLabelText('Role'), { target: { value: 'DIRECTOR' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

		await waitFor(() => expect(onadded).toHaveBeenCalledWith({ ...credit, role: 'DIRECTOR' }));
		expect(addCredit).toHaveBeenCalledWith('1', '7', 'DIRECTOR');
		expect(toastSuccess).toHaveBeenCalledWith('Anna added as director');
	});

	it('should show "already credited" under the picker on CONFLICT', async () => {
		vi.mocked(searchPeople).mockResolvedValueOnce([anna]);
		vi.mocked(addCredit).mockRejectedValueOnce(
			new ApiError('CONFLICT', 'Anna is already credited as actor')
		);
		const onadded = vi.fn();
		render(CreditPicker, { props: { movieId: '1', onadded } });

		await type('an');
		await fireEvent.click(screen.getByRole('button', { name: 'Anna' }));
		await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('Anna is already credited as actor');
		expect(onadded).not.toHaveBeenCalled();
	});

	it('should show an error toast and keep the picker when adding fails because people-service is down', async () => {
		vi.mocked(searchPeople).mockResolvedValueOnce([anna]);
		vi.mocked(addCredit).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'people-service is unavailable')
		);
		const onadded = vi.fn();
		render(CreditPicker, { props: { movieId: '1', onadded } });

		await type('an');
		await fireEvent.click(screen.getByRole('button', { name: 'Anna' }));
		await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('people-service is unavailable'));
		expect(onadded).not.toHaveBeenCalled();
		expect(screen.queryByRole('alert')).not.toBeInTheDocument();
	});

	it('should ignore a search answer that arrives after the text changed', async () => {
		const slow = Promise.withResolvers<(typeof anna)[]>();
		vi.mocked(searchPeople)
			.mockReturnValueOnce(slow.promise)
			.mockResolvedValueOnce([{ id: '9', name: 'Annika', bio: null }]);
		render(CreditPicker, { props: { movieId: '1', onadded: vi.fn() } });

		await type('an');
		await type('anni');
		slow.resolve([anna]); // the old answer lands last

		await waitFor(() => expect(screen.getByRole('button', { name: 'Annika' })).toBeInTheDocument());
		expect(screen.queryByRole('button', { name: 'Anna' })).not.toBeInTheDocument();
	});

	it('should show an error toast when the search fails', async () => {
		vi.mocked(searchPeople).mockRejectedValueOnce(
			new ApiError('UNAVAILABLE', 'people-service is unavailable')
		);
		render(CreditPicker, { props: { movieId: '1', onadded: vi.fn() } });

		await type('an');

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('people-service is unavailable'));
	});

	it('should not search for blank text', async () => {
		render(CreditPicker, { props: { movieId: '1', onadded: vi.fn() } });

		await type('   ');

		expect(searchPeople).not.toHaveBeenCalled();
		expect(screen.queryByRole('status')).not.toBeInTheDocument();
	});
});
