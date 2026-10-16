<script lang="ts">
	// The percentage bar for a running upload (BRD N2: "upload progress with percentage").
	let { percent, label = 'Uploading' }: { percent: number; label?: string } = $props();
</script>

<div class="upload-progress">
	<progress value={percent} max="100" aria-label={label}></progress>
	<span>{percent} %</span>
</div>

<style>
	.upload-progress {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}
	progress {
		flex: 1;
		height: 0.5rem;
		accent-color: var(--color-accent);
	}
	span {
		min-width: 3.5rem;
		text-align: right;
		font-variant-numeric: tabular-nums;
		color: var(--color-muted);
	}
</style>
