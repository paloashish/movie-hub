<script lang="ts">
	import { toasts, dismissToast } from '#lib/stores/toast.svelte.ts';
</script>

<!-- aria-live so screen readers announce new messages without focus moving -->
<div class="toasts" aria-live="polite">
	{#each toasts as toast (toast.id)}
		<div class="toast {toast.kind}" role={toast.kind === 'error' ? 'alert' : 'status'}>
			<span>{toast.text}</span>
			<button type="button" aria-label="Dismiss" onclick={() => dismissToast(toast.id)}>×</button>
		</div>
	{/each}
</div>

<style>
	.toasts {
		position: fixed;
		bottom: 1rem;
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		z-index: 100;
		width: min(90vw, 28rem);
	}
	.toast {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
		padding: 0.75rem 1rem;
		border-radius: 0.5rem;
		color: #fff;
		box-shadow: 0 4px 12px rgb(0 0 0 / 0.2);
	}
	.success {
		background: var(--color-success);
	}
	.error {
		background: var(--color-danger);
	}
	.info {
		background: var(--color-header);
	}
	button {
		background: transparent;
		border: 0;
		color: inherit;
		font-size: 1.25rem;
		cursor: pointer;
	}
</style>
