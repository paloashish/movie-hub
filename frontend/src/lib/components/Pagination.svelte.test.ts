import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Pagination from './Pagination.svelte';

describe('Pagination', () => {
	it('should render nothing when everything fits on one page', () => {
		render(Pagination, { props: { page: 0, size: 12, total: 12, onchange: vi.fn() } });

		expect(screen.queryByRole('navigation', { name: 'Pages' })).not.toBeInTheDocument();
	});

	it('should show the current page number and the page count', () => {
		render(Pagination, { props: { page: 1, size: 12, total: 30, onchange: vi.fn() } });

		expect(screen.getByText('Page 2 of 3')).toBeInTheDocument();
	});

	it('should disable Previous on the first page', () => {
		render(Pagination, { props: { page: 0, size: 12, total: 13, onchange: vi.fn() } });

		expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
		expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
	});

	it('should disable Next on the last page', () => {
		render(Pagination, { props: { page: 1, size: 12, total: 13, onchange: vi.fn() } });

		expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled();
		expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
	});

	it('should hand the neighbouring page to onchange', async () => {
		const onchange = vi.fn();
		render(Pagination, { props: { page: 1, size: 12, total: 30, onchange } });

		await fireEvent.click(screen.getByRole('button', { name: 'Next' }));
		await fireEvent.click(screen.getByRole('button', { name: 'Previous' }));

		expect(onchange).toHaveBeenNthCalledWith(1, 2);
		expect(onchange).toHaveBeenNthCalledWith(2, 0);
	});
});
