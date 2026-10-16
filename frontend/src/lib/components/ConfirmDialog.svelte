<script lang="ts">
	// Confirmation before destructive actions (BRD N2: "confirmation before deletion").
	// Rendered only while open; focus moves to the Cancel button so a stray Enter cannot confirm.
	let {
		open,
		title,
		message,
		confirmLabel = 'Delete',
		busyLabel = 'Deleting...',
		busy = false,
		onconfirm,
		oncancel
	}: {
		open: boolean;
		title: string;
		message: string;
		confirmLabel?: string;
		/** What the confirm button reads while busy, e.g. "Removing..." for a credit */
		busyLabel?: string;
		/** True while the confirmed action runs: both buttons disable so it cannot be sent twice. */
		busy?: boolean;
		onconfirm: () => void;
		oncancel: () => void;
	} = $props();

	let cancelButton = $state<HTMLButtonElement | null>(null);
	let confirmButton = $state<HTMLButtonElement | null>(null);
	let opener: HTMLElement | null = null; // the button that opened the dialog, so focus can go back to it
	$effect(() => {
		if (open) {
			opener = document.activeElement as HTMLElement | null;
			cancelButton?.focus();
		} else {
			opener?.focus();
			opener = null;
		}
	});

	// Escape cancels; Tab cycles between the two buttons so keyboard focus never leaves the dialog (N5)
	function onkeydown(event: KeyboardEvent) {
		if (event.key === 'Escape' && !busy) oncancel();
		if (event.key === 'Tab' && cancelButton && confirmButton) {
			event.preventDefault();
			(document.activeElement === cancelButton ? confirmButton : cancelButton).focus();
		}
	}
</script>

{#if open}
	<div class="backdrop" role="presentation" {onkeydown}>
		<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
			<h2 id="confirm-title">{title}</h2>
			<p>{message}</p>
			<div class="actions">
				<button
					type="button"
					class="btn btn-secondary"
					bind:this={cancelButton}
					onclick={oncancel}
					disabled={busy}>Cancel</button
				>
				<button
					type="button"
					class="btn btn-danger"
					bind:this={confirmButton}
					onclick={onconfirm}
					disabled={busy}
				>
					{busy ? busyLabel : confirmLabel}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	.backdrop {
		position: fixed;
		inset: 0;
		background: rgb(0 0 0 / 0.4);
		display: grid;
		place-items: center;
		z-index: 50;
		padding: 1rem;
	}
	.dialog {
		background: var(--color-surface);
		border-radius: 0.75rem;
		padding: 1.5rem;
		width: min(100%, 24rem);
	}
	h2 {
		margin: 0 0 0.5rem;
		font-size: 1.125rem;
	}
	.actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.5rem;
		margin-top: 1rem;
	}
</style>
