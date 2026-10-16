import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { createPerson } from '#lib/api/people.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/people.ts', () => ({ createPerson: vi.fn() }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

describe('Add person page', () => {
	beforeEach(() => {
		vi.mocked(createPerson).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
	});

	it('should create the person, show a success message and go to the list', async () => {
		vi.mocked(createPerson).mockResolvedValueOnce({ id: '7', name: 'Anna', bio: null });
		render(Page);

		await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Anna' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/people'));
		expect(createPerson).toHaveBeenCalledWith({ name: 'Anna', bio: null });
		expect(toastSuccess).toHaveBeenCalledWith('Anna added');
	});

	it('should stay on the page and show the message when the server rejects the input', async () => {
		vi.mocked(createPerson).mockRejectedValueOnce(
			new ApiError('VALIDATION', 'name: must not be blank')
		);
		render(Page);

		await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'x' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('name: must not be blank');
		expect(goto).not.toHaveBeenCalled();
	});
});
