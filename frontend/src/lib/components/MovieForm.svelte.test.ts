import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import MovieForm from './MovieForm.svelte';
import { ApiError } from '#lib/api/errors.ts';
import { toastError } from '#lib/stores/toast.svelte.ts';

vi.mock('#lib/stores/toast.svelte.ts', () => ({ toastError: vi.fn() }));

const thisYear = new Date().getFullYear();

async function fillAndSave(title: string, year: string, synopsis = '') {
	await fireEvent.input(screen.getByLabelText('Title'), { target: { value: title } });
	await fireEvent.input(screen.getByLabelText('Release year'), { target: { value: year } });
	await fireEvent.input(screen.getByLabelText('Synopsis (optional)'), {
		target: { value: synopsis }
	});
	await fireEvent.click(screen.getByRole('button', { name: 'Save' }));
}

describe('MovieForm', () => {
	beforeEach(() => vi.mocked(toastError).mockReset());

	it('should show a validation error and not call onsave when the title is blank', async () => {
		const onsave = vi.fn();
		render(MovieForm, { props: { onsave } });

		await fillAndSave('   ', '2010');

		expect(screen.getByRole('alert')).toHaveTextContent('Title is required');
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should show a validation error and not call onsave when the year is empty', async () => {
		const onsave = vi.fn();
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Alien', '');

		expect(screen.getByRole('alert')).toHaveTextContent('Release year is required');
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should reject a decimal year with the range message', async () => {
		const onsave = vi.fn();
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Alien', '1979.5');

		expect(screen.getByRole('alert')).toHaveTextContent('whole year between');
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should reject a year before 1888', async () => {
		const onsave = vi.fn();
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Alien', '1887');

		expect(screen.getByRole('alert')).toHaveTextContent(
			`whole year between 1888 and ${thisYear + 5}`
		);
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should reject a year more than five years in the future', async () => {
		const onsave = vi.fn();
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Soon', String(thisYear + 6));

		expect(screen.getByRole('alert')).toHaveTextContent(
			'Release year must be a whole year between'
		);
		expect(onsave).not.toHaveBeenCalled();
	});

	it('should accept 1888', async () => {
		const onsave = vi.fn().mockResolvedValueOnce(undefined);
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Old', '1888');

		expect(onsave).toHaveBeenCalledTimes(1);
	});

	it('should accept exactly five years ahead', async () => {
		const onsave = vi.fn().mockResolvedValueOnce(undefined);
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Soon', String(thisYear + 5));

		expect(onsave).toHaveBeenCalledTimes(1);
	});

	it('should pass trimmed values with a numeric year and null synopsis to onsave, showing a saving state', async () => {
		let finish!: () => void; // the `!` tells TypeScript the Promise constructor below assigns it synchronously
		const onsave = vi.fn().mockReturnValueOnce(new Promise<void>((r) => (finish = r)));
		render(MovieForm, { props: { onsave } });

		await fillAndSave(' Alien ', '1979', '  ');

		expect(screen.getByRole('button', { name: 'Saving' })).toBeDisabled();
		expect(onsave).toHaveBeenCalledWith({ title: 'Alien', releaseYear: 1979, synopsis: null });
		finish();
		await waitFor(() => expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled());
	});

	it('should send Cancel to the grid by default and to the given page when asked', () => {
		const { unmount } = render(MovieForm, { props: { onsave: vi.fn() } });
		expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/');
		unmount();

		render(MovieForm, { props: { onsave: vi.fn(), cancelHref: '/movies/3' } });
		expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/movies/3');
	});

	it('should start with the initial values when editing', () => {
		render(MovieForm, {
			props: { initial: { title: 'Alien', releaseYear: 1979, synopsis: 'Space' }, onsave: vi.fn() }
		});

		expect(screen.getByLabelText('Title')).toHaveValue('Alien');
		expect(screen.getByLabelText('Release year')).toHaveValue(1979);
		expect(screen.getByLabelText('Synopsis (optional)')).toHaveValue('Space');
	});

	it('should show a server VALIDATION message under the form', async () => {
		const onsave = vi
			.fn()
			.mockRejectedValueOnce(new ApiError('VALIDATION', 'releaseYear: must be 1888 or later'));
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Alien', '1979');

		expect(await screen.findByRole('alert')).toHaveTextContent(
			'releaseYear: must be 1888 or later'
		);
	});

	it('should show an error toast and keep the values when the server is unavailable', async () => {
		const onsave = vi
			.fn()
			.mockRejectedValueOnce(new ApiError('UNAVAILABLE', 'Cannot reach the server'));
		render(MovieForm, { props: { onsave } });

		await fillAndSave('Alien', '1979');

		await waitFor(() => expect(toastError).toHaveBeenCalledWith('Cannot reach the server'));
		expect(screen.getByLabelText('Title')).toHaveValue('Alien');
	});
});
