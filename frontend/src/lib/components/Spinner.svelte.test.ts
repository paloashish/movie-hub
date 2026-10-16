import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import Spinner from './Spinner.svelte';

describe('Spinner', () => {
	it('should announce "Loading" by default when rendered', () => {
		render(Spinner);

		const status = screen.getByRole('status');

		expect(status).toBeInTheDocument();
		expect(status).toHaveAccessibleName('Loading');
	});

	it('should announce the given label when one is passed', () => {
		render(Spinner, { props: { label: 'Uploading poster' } });

		expect(screen.getByRole('status')).toHaveAccessibleName('Uploading poster');
	});
});
