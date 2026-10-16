<script lang="ts">
	// The "nothing here yet" state of the four-state page pattern.
	let { message }: { message: string } = $props();
</script>

<p class="empty" role="status">{message}</p>

<style>
	.empty {
		padding: 2rem 1rem;
		text-align: center;
		color: var(--color-muted);
	}
</style>
