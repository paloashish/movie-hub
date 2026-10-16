import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import UploadProgress from './UploadProgress.svelte';

describe('UploadProgress', () => {
	it('should show the bar and the percentage', () => {
		render(UploadProgress, { props: { percent: 42 } });

		expect(screen.getByRole('progressbar', { name: 'Uploading' })).toHaveValue(42);
		expect(screen.getByText('42 %')).toBeInTheDocument();
	});

	it('should render 0 % at the start', () => {
		render(UploadProgress, { props: { percent: 0 } });

		expect(screen.getByRole('progressbar')).toHaveValue(0);
		expect(screen.getByText('0 %')).toBeInTheDocument();
	});

	it('should render 100 % when done', () => {
		render(UploadProgress, { props: { percent: 100 } });

		expect(screen.getByRole('progressbar')).toHaveValue(100);
		expect(screen.getByText('100 %')).toBeInTheDocument();
	});
});
