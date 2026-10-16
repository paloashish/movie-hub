import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PersonForm from './PersonForm.svelte';
import { ApiError } from '#lib/api/errors.ts';
import { toastError } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastError: vi.fn() }));

// Resolves only when the test says so, to observe the saving state in between.
function deferred() {
	let resolve!: () => void; // the `!` tells TypeScript the Promise constructor below assigns it synchronously
	const promise = new Promise<void>((r) => (resolve = r));
	return { promise, resolve };
}

async function fillAndSave(name: string, bio = '') {
	await fireEvent.input(screen.getByLabelText('Name'), { target: { value: name } });
	await fireEvent.input(screen.getByLabelText('Bio (optional)'), { target: { value: bio } });
	await fireEvent.click(screen.getByRole('button', { name: 'Save' }));
}

describe('PersonForm', () => {
	beforeEach(() => vi.mocked(toastError).mockReset());

	it('should show a validation error and not call onsave when the name is blank', async () => {
		const onsave = vi.fn();
		render(PersonForm, { props: { onsave } });

		await fillAndSave('   ');

		expect(screen.getByRole('alert')).toHaveTextContent('Name is required');
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should pass trimmed values to onsave and show a saving state until it resolves', async () => {
		const pending = deferred();
		const onsave = vi.fn().mockReturnValueOnce(pending.promise);
		render(PersonForm, { props: { onsave } });

		await fillAndSave(' Anna Example ', ' Actor ');

		expect(screen.getByRole('button', { name: 'Saving' })).toBeDisabled();
		expect(onsave).toHaveBeenCalledWith({ name: 'Anna Example', bio: 'Actor' });

		pending.resolve();
		await waitFor(() => expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled());
	});

	it('should send a null bio when the bio is left empty', async () => {
		const onsave = vi.fn().mockResolvedValueOnce(undefined);
		render(PersonForm, { props: { onsave } });

		await fillAndSave('Bob');

		expect(onsave).toHaveBeenCalledWith({ name: 'Bob', bio: null });
	});

	it('should start with the initial values when editing', () => {
		render(PersonForm, { props: { initial: { name: 'Anna', bio: 'Actor' }, onsave: vi.fn() } });

		expect(screen.getByLabelText('Name')).toHaveValue('Anna');
		expect(screen.getByLabelText('Bio (optional)')).toHaveValue('Actor');
	});

	it('should show a server VALIDATION message under the form', async () => {
		const onsave = vi
			.fn()
			.mockRejectedValueOnce(new ApiError('VALIDATION', 'name: must be at most 200 characters'));
		render(PersonForm, { props: { onsave } });

		await fillAndSave('Anna');

		expect(await screen.findByRole('alert')).toHaveTextContent(
			'name: must be at most 200 characters'
		);
		expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
	});

	it('should show a NOT_FOUND message under the form when the person was deleted meanwhile', async () => {
		const onsave = vi.fn().mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 3 not found'));
		render(PersonForm, { props: { initial: { name: 'Anna', bio: null }, onsave } });

		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('Person 3 not found');
	});

	it('should show an error toast and keep the values when the server is unavailable', async () => {
		const onsave = vi
			.fn()
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'people-service is unavailable'));
		render(PersonForm, { props: { onsave } });

		await fillAndSave('Anna');

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('people-service is unavailable'));
		expect(screen.getByLabelText('Name')).toHaveValue('Anna');
	});
});
