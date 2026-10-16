import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Page from './+page.svelte';
import { listPeople } from '#lib/api/people.ts';
import { ApiError } from '#lib/api/errors.ts';

vi.mock('#lib/api/people.ts', () => ({ listPeople: vi.fn() }));

describe('People page', () => {
	beforeEach(() => vi.mocked(listPeople).mockReset());

	it('should show a loading indicator and then the people in the order the server gave', async () => {
		vi.mocked(listPeople).mockResolvedValueOnce([
			{ id: '1', name: 'Anna', bio: null },
			{ id: '2', name: 'Bob', bio: 'Director' }
		]);
		render(Page);

		expect(screen.getByRole('status', { name: 'Loading people' })).toBeInTheDocument();

		const headings = await screen.findAllByRole('heading', { level: 2 });
		expect(headings.map((h) => h.textContent)).toEqual(['Anna', 'Bob']);
		expect(screen.queryByRole('status', { name: 'Loading people' })).not.toBeInTheDocument();
	});

	it('should link to the add-person form', async () => {
		vi.mocked(listPeople).mockResolvedValueOnce([]);
		render(Page);

		expect(screen.getByRole('link', { name: '+ Add person' })).toHaveAttribute(
			'href',
			'/people/new'
		);
		await screen.findByText('No people yet'); // let the load finish inside the test
	});

	it('should show "No people yet" when the list is empty', async () => {
		vi.mocked(listPeople).mockResolvedValueOnce([]);
		render(Page);

		expect(await screen.findByText('No people yet')).toBeInTheDocument();
	});

	it('should show the error message with Retry when loading fails, and load again on Retry', async () => {
		vi.mocked(listPeople)
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'))
			.mockResolvedValueOnce([{ id: '1', name: 'Anna', bio: null }]);
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		await waitFor(() => expect(screen.getByRole('heading', { name: 'Anna' })).toBeInTheDocument());
		expect(listPeople).toHaveBeenCalledTimes(2);
	});

	it('should show a generic message when the failure is not an ApiError', async () => {
		// Once-only on purpose: see learning-notes (persistent rejections on a module mock trip Vitest's bookkeeping)
		vi.mocked(listPeople).mockRejectedValueOnce(new TypeError('weird'));
		render(Page);

		expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
	});
});
