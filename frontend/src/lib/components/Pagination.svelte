<script lang="ts">
	// Page controls for the grid (E2-S6, ui-design.md section 4). Renders nothing when everything fits on one page.
	let {
		page,
		size,
		total,
		onchange
	}: {
		page: number;
		size: number;
		total: number;
		onchange: (page: number) => void;
	} = $props();

	const pageCount = $derived(Math.max(1, Math.ceil(total / size)));
</script>

{#if pageCount > 1}
	<nav class="pagination" aria-label="Pages">
		<button
			type="button"
			class="btn btn-secondary"
			disabled={page === 0}
			onclick={() => onchange(page - 1)}
		>
			Previous
		</button>
		<span>Page {page + 1} of {pageCount}</span>
		<button
			type="button"
			class="btn btn-secondary"
			disabled={page >= pageCount - 1}
			onclick={() => onchange(page + 1)}
		>
			Next
		</button>
	</nav>
{/if}

<style>
	.pagination {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 1rem;
		margin-top: 1.5rem;
	}
</style>
