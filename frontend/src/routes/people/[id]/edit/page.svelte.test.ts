import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { getPerson, updatePerson } from '#lib/api/people.ts';
import { ApiError } from '#lib/api/errors.ts';
import { goto } from '$app/navigation';
import { toastSuccess } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/api/people.ts', () => ({ getPerson: vi.fn(), updatePerson: vi.fn() }));
vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('$app/state', () => ({
	page: { params: { id: '3' }, url: new URL('http://localhost/people/3/edit') }
}));
vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastSuccess: vi.fn(), toastError: vi.fn() }));

describe('Edit person page', () => {
	beforeEach(() => {
		vi.mocked(getPerson).mockReset();
		vi.mocked(updatePerson).mockReset();
		vi.mocked(goto).mockReset();
		vi.mocked(toastSuccess).mockReset();
	});

	it('should show a loading indicator, then the form filled with the person', async () => {
		vi.mocked(getPerson).mockResolvedValueOnce({ id: '3', name: 'Anna', bio: 'Actor' });
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading person' })).toBeInTheDocument();

		expect(await screen.findByLabelText('Name')).toHaveValue('Anna');
		expect(screen.getByLabelText('Bio (optional)')).toHaveValue('Actor');
		expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/people/3'); // same place as Back
		expect(getPerson).toHaveBeenCalledWith('3');
	});

	it('should show "not found" without Retry when the person does not exist', async () => {
		vi.mocked(getPerson).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 3 not found'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Person 3 not found');
		expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
		expect(screen.queryByRole('link', { name: /Back to the person/ })).not.toBeInTheDocument(); // nothing to go back to
	});

	it('should show the error with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(getPerson)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce({ id: '3', name: 'Anna', bio: null });
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(await screen.findByLabelText('Name')).toHaveValue('Anna');
		expect(getPerson).toHaveBeenCalledTimes(2);
	});

	it('should save the new name, show a success message and go to the person page', async () => {
		vi.mocked(getPerson).mockResolvedValueOnce({ id: '3', name: 'Anna', bio: null });
		vi.mocked(updatePerson).mockResolvedValueOnce({ id: '3', name: 'Anna B', bio: null });
		render(Page);

		await fireEvent.input(await screen.findByLabelText('Name'), { target: { value: 'Anna B' } });
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		await waitFor(() => expect(goto).toHaveBeenCalledWith('/people/3'));
		expect(updatePerson).toHaveBeenCalledWith('3', { name: 'Anna B', bio: null });
		expect(toastSuccess).toHaveBeenCalledWith('Anna B saved');
	});

	it('should show a "not found" error under the form when the person was deleted meanwhile', async () => {
		vi.mocked(getPerson).mockResolvedValueOnce({ id: '3', name: 'Anna', bio: null });
		vi.mocked(updatePerson).mockRejectedValueOnce(new ApiError('NOT_FOUND', 'Person 3 not found'));
		render(Page);

		await screen.findByLabelText('Name');
		await fireEvent.click(screen.getByRole('button', { name: 'Save' }));

		expect(await screen.findByRole('alert')).toHaveTextContent('Person 3 not found');
		expect(goto).not.toHaveBeenCalled();
	});
});
