<script lang="ts">
	import type { MovieInput } from '#lib/types/graphql.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastError } from '#lib/stores/toast.svelte.ts';
	import Spinner from '#lib/components/Spinner.svelte';

	// The movie form from ui-design.md section 4, shared by /movies/new (E2-S3) and /movies/[id]/edit (E2-S4).
	// Same split as PersonForm: the page decides what "save" means, the form owns fields, instant rules and errors.
	let {
		initial = { title: '', releaseYear: new Date().getFullYear(), synopsis: null },
		cancelHref = '/',
		onsave
	}: {
		initial?: MovieInput;
		/** Where Cancel goes: the grid for a new movie, the movie page when editing, same as the Back link */
		cancelHref?: string;
		onsave: (input: MovieInput) => Promise<void>;
	} = $props();

	// 1888 is the first film ever; five years ahead allows announced titles (application-architecture 4.2)
	const MIN_YEAR = 1888;
	const MAX_YEAR = new Date().getFullYear() + 5;

	// The form copies the initial values once on purpose: the user edits the copy, not the prop.
	// svelte-ignore state_referenced_locally
	let title = $state(initial.title);
	// svelte-ignore state_referenced_locally
	// A number input binds as a number, or null while the box is empty
	let year = $state<number | null>(initial.releaseYear);
	// svelte-ignore state_referenced_locally
	let synopsis = $state(initial.synopsis ?? '');
	let saving = $state(false);
	let titleError = $state('');
	let yearError = $state('');
	let formError = $state('');

	// The same rules as the server, for instant feedback; the server stays the truth.
	function validate(): boolean {
		titleError = title.trim() === '' ? 'Title is required' : '';
		yearError =
			year === null
				? 'Release year is required'
				: !Number.isInteger(year) || year < MIN_YEAR || year > MAX_YEAR
					? `Release year must be a whole year between ${MIN_YEAR} and ${MAX_YEAR}`
					: '';
		return titleError === '' && yearError === '';
	}

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		formError = '';
		if (!validate() || year === null) return;
		saving = true;
		try {
			await onsave({
				title: title.trim(),
				releaseYear: year,
				synopsis: synopsis.trim() || null
			});
		} catch (e) {
			if (e instanceof ApiError && (e.code === 'VALIDATION' || e.code === 'NOT_FOUND')) {
				formError = e.message;
			} else {
				toastError(e instanceof ApiError ? e.message : 'Something went wrong');
			}
		} finally {
			saving = false;
		}
	}
</script>

<form onsubmit={submit} novalidate>
	<div class="field">
		<label for="title">Title</label>
		<input
			id="title"
			type="text"
			bind:value={title}
			maxlength="200"
			aria-invalid={titleError !== ''}
			required
		/>
		{#if titleError}
			<p class="field-error" role="alert">{titleError}</p>
		{/if}
	</div>
	<div class="field">
		<label for="year">Release year</label>
		<input
			id="year"
			type="number"
			bind:value={year}
			min={MIN_YEAR}
			max={MAX_YEAR}
			aria-invalid={yearError !== ''}
			required
		/>
		{#if yearError}
			<p class="field-error" role="alert">{yearError}</p>
		{/if}
	</div>
	<div class="field">
		<label for="synopsis">Synopsis (optional)</label>
		<textarea id="synopsis" bind:value={synopsis} maxlength="2000" rows="5"></textarea>
	</div>
	{#if formError}
		<p class="field-error" role="alert">{formError}</p>
	{/if}
	<div class="form-actions">
		<a href={cancelHref} class="btn btn-secondary">Cancel</a>
		<button type="submit" class="btn btn-primary" disabled={saving}>
			{#if saving}
				<!-- the Spinner's label is the button's accessible name; visible text would be read twice -->
				<Spinner label="Saving" />
			{:else}
				Save
			{/if}
		</button>
	</div>
</form>
