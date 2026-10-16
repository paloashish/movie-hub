import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { toasts, pushToast, dismissToast, toastError } from './toast.svelte.ts';

describe('toast store', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		toasts.splice(0, toasts.length);
	});
	afterEach(() => vi.useRealTimers());

	it('should add a message with the given kind and text', () => {
		pushToast('success', 'Saved');

		expect(toasts).toHaveLength(1);
		expect(toasts[0]).toMatchObject({ kind: 'success', text: 'Saved' });
	});

	it('should remove a message when dismissed', () => {
		const id = pushToast('info', 'Hello');

		dismissToast(id);

		expect(toasts).toHaveLength(0);
	});

	it('should auto-dismiss after the timeout', () => {
		pushToast('success', 'Saved', 1000);

		vi.advanceTimersByTime(999);
		expect(toasts).toHaveLength(1);
		vi.advanceTimersByTime(1);
		expect(toasts).toHaveLength(0);
	});

	it('should ignore dismissing an unknown id', () => {
		pushToast('info', 'Hello');

		dismissToast(999);

		expect(toasts).toHaveLength(1);
	});

	it('should mark errors as kind error through the shortcut', () => {
		toastError('Failed');

		expect(toasts[0].kind).toBe('error');
	});
});
