import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import EmptyState from './EmptyState.svelte';
import ErrorState from './ErrorState.svelte';
import ConfirmDialog from './ConfirmDialog.svelte';
import Toast from './Toast.svelte';
import { toasts, pushToast } from '#lib/stores/toast.svelte.ts';

describe('EmptyState', () => {
	it('should show the message as a status', () => {
		render(EmptyState, { props: { message: 'No movies yet' } });

		expect(screen.getByRole('status')).toHaveTextContent('No movies yet');
	});
});

describe('ErrorState', () => {
	it('should show the message as an alert and call onretry when Retry is clicked', async () => {
		const onretry = vi.fn();
		render(ErrorState, { props: { message: 'Cannot reach the server', onretry } });

		expect(screen.getByRole('alert')).toHaveTextContent('Cannot reach the server');
		await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

		expect(onretry).toHaveBeenCalledOnce();
	});
});

describe('ConfirmDialog', () => {
	const props = {
		title: 'Delete movie?',
		message: 'This cannot be undone.',
		onconfirm: vi.fn(),
		oncancel: vi.fn()
	};

	it('should render nothing when closed', () => {
		render(ConfirmDialog, { props: { ...props, open: false } });

		expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
	});

	it('should show title and message and call onconfirm when the confirm button is clicked', async () => {
		const onconfirm = vi.fn();
		render(ConfirmDialog, { props: { ...props, open: true, onconfirm } });

		expect(screen.getByRole('dialog')).toHaveAccessibleName('Delete movie?');
		await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

		expect(onconfirm).toHaveBeenCalledOnce();
	});

	it('should call oncancel when Cancel is clicked and nothing else', async () => {
		const onconfirm = vi.fn();
		const oncancel = vi.fn();
		render(ConfirmDialog, { props: { ...props, open: true, onconfirm, oncancel } });

		await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

		expect(oncancel).toHaveBeenCalledOnce();
		expect(onconfirm).not.toHaveBeenCalled();
	});

	it('should call oncancel when Escape is pressed', async () => {
		const oncancel = vi.fn();
		render(ConfirmDialog, { props: { ...props, open: true, oncancel } });

		await fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

		expect(oncancel).toHaveBeenCalledOnce();
	});

	it('should focus Cancel when opened so Enter cannot confirm by accident', () => {
		render(ConfirmDialog, { props: { ...props, open: true } });

		expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
	});

	it('should return focus to the button that opened it when it closes', async () => {
		const trigger = document.createElement('button');
		trigger.textContent = 'Delete movie';
		document.body.appendChild(trigger);
		trigger.focus();
		const { rerender } = render(ConfirmDialog, { props: { ...props, open: true } });
		expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();

		await rerender({ ...props, open: false });

		expect(trigger).toHaveFocus();
		trigger.remove();
	});

	it('should keep Tab inside the dialog, cycling between Cancel and the confirm button', async () => {
		render(ConfirmDialog, { props: { ...props, open: true } });
		const cancel = screen.getByRole('button', { name: 'Cancel' });
		const confirm = screen.getByRole('button', { name: 'Delete' });

		expect(cancel).toHaveFocus();
		await fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Tab' });
		expect(confirm).toHaveFocus();
		await fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Tab' });
		expect(cancel).toHaveFocus();
	});
});

describe('Toast', () => {
	beforeEach(() => toasts.splice(0, toasts.length));

	it('should show a pushed error as an alert and remove it on dismiss', async () => {
		render(Toast);
		pushToast('error', 'Upload failed', 0);

		expect(await screen.findByRole('alert')).toHaveTextContent('Upload failed');
		await fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

		expect(screen.queryByRole('alert')).not.toBeInTheDocument();
	});
});
