<script lang="ts">
	// Shown while a page or action is loading (BRD N2). Announced to screen readers through role="status".
	let { label = 'Loading' }: { label?: string } = $props();
</script>

<div class="spinner" role="status" aria-label={label}>
	<span class="dot"></span>
	<span class="sr-only">{label}</span>
</div>

<style>
	.spinner {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		padding: 1rem;
	}
	.dot {
		width: 1.25rem;
		height: 1.25rem;
		border: 3px solid var(--color-border);
		border-top-color: var(--color-accent);
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}
	.sr-only {
		position: absolute;
		width: 1px;
		height: 1px;
		overflow: hidden;
		clip: rect(0 0 0 0);
	}
	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>
