<script lang="ts">
	// The error state of the four-state page pattern: a message and a Retry action (BRD N2).
	let { message, onretry }: { message: string; onretry: () => void } = $props();
</script>

<div class="error" role="alert">
	<p>{message}</p>
	<button type="button" class="btn btn-danger" onclick={onretry}>Retry</button>
</div>

<style>
	.error {
		padding: 1.5rem 1rem;
		text-align: center;
		border: 1px solid var(--color-danger);
		background: var(--color-danger-bg);
		border-radius: var(--radius);
		color: var(--color-danger);
	}
	button {
		margin-top: 0.5rem;
	}
</style>
