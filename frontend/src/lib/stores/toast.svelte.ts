// Global feedback messages (BRD N2). Any page can push one; the Toast component in the layout shows them.
// A .svelte.ts module so $state works outside components.

export type ToastKind = 'success' | 'error' | 'info';
export interface ToastMessage {
	id: number;
	kind: ToastKind;
	text: string;
}

const AUTO_DISMISS_MS = 4000;
let nextId = 1;

export const toasts = $state<ToastMessage[]>([]);

export function pushToast(kind: ToastKind, text: string, autoDismissMs = AUTO_DISMISS_MS): number {
	const id = nextId++;
	toasts.push({ id, kind, text });
	if (autoDismissMs > 0) setTimeout(() => dismissToast(id), autoDismissMs);
	return id;
}

export function dismissToast(id: number): void {
	const index = toasts.findIndex((t) => t.id === id);
	if (index >= 0) toasts.splice(index, 1);
}

export const toastSuccess = (text: string) => pushToast('success', text);
export const toastError = (text: string) => pushToast('error', text);
