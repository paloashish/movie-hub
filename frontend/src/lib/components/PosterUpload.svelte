<script lang="ts">
	import { uploadPoster } from '#lib/api/upload.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess } from '#lib/stores/toast.svelte.ts';
	import UploadProgress from '#lib/components/UploadProgress.svelte';

	// E3-S1: pick a file, watch it go up, see the result (ui-design.md section 4, movie detail).
	// The previous poster is untouched until the server answers with the new URL.
	// E3-S2: the same type and size rules as the server run at pick time, so a bad file never starts an upload.
	let { movieId, onuploaded }: { movieId: string; onuploaded: (posterUrl: string) => void } =
		$props();

	const ALLOWED_TYPES = ['image/jpeg', 'image/png'];
	const MAX_MB = 10;
	const MAX_BYTES = MAX_MB * 1024 * 1024;

	/** The reason a file cannot be uploaded, or null when it can. */
	function rejectReason(file: File): string | null {
		// Same words as the server (PosterService), so the message is the same whichever side rejects
		if (!ALLOWED_TYPES.includes(file.type)) return 'file type must be JPG or PNG';
		if (file.size > MAX_BYTES) return `file must be at most ${MAX_MB} MB`;
		return null;
	}

	type Status = 'idle' | 'uploading' | 'error';
	let status = $state<Status>('idle');
	let percent = $state(0);
	let errorMessage = $state('');
	let lastFile = $state<File | null>(null); // kept so Retry can send the same file again
	let input = $state<HTMLInputElement | null>(null);

	async function upload(file: File) {
		lastFile = file;
		status = 'uploading';
		percent = 0;
		try {
			const posterUrl = await uploadPoster(movieId, file, (p) => (percent = p));
			status = 'idle';
			toastSuccess('Poster uploaded');
			onuploaded(posterUrl);
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = 'error';
		} finally {
			if (input) input.value = ''; // so picking the same file again fires a change event
		}
	}

	function onchange(event: Event) {
		const target = event.currentTarget as HTMLInputElement;
		const file = target.files?.[0];
		if (!file) return;
		const reason = rejectReason(file);
		if (reason) {
			// Rejected before upload: no progress bar, no server call, nothing to retry
			errorMessage = reason;
			lastFile = null;
			status = 'error';
			target.value = '';
			return;
		}
		upload(file);
	}
</script>

<div class="poster-upload">
	<label for="poster-file">Poster</label>
	<input
		id="poster-file"
		aria-describedby="poster-hint"
		type="file"
		accept="image/jpeg,image/png"
		bind:this={input}
		disabled={status === 'uploading'}
		{onchange}
	/>
	<p class="hint" id="poster-hint">JPG or PNG, up to {MAX_MB} MB</p>
	{#if status === 'uploading'}
		<UploadProgress {percent} />
	{:else if status === 'error'}
		<div class="error" role="alert">
			<span>{errorMessage}</span>
			{#if lastFile}
				<button type="button" class="btn btn-secondary" onclick={() => lastFile && upload(lastFile)}
					>Retry</button
				>
			{/if}
		</div>
	{/if}
</div>

<style>
	.poster-upload {
		display: grid;
		gap: 0.25rem;
		margin-top: 0.75rem;
	}
	label {
		font-weight: 600;
	}
	.hint {
		margin: 0;
		font-size: 0.8125rem;
		color: var(--color-muted);
	}
	.error {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		color: var(--color-danger);
		font-size: 0.875rem;
	}
</style>
