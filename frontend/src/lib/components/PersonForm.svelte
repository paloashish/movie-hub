<script lang="ts">
	import type { PersonInput } from '#lib/types/graphql.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastError } from '#lib/stores/toast.svelte.ts';
	import Spinner from '#lib/components/Spinner.svelte';

	// The person form from ui-design.md section 4, shared by /people/new (E1-S2) and /people/[id]/edit (E1-S3).
	// The page decides what "save" means (create or update) and where to go afterwards; the form owns
	// the fields, the instant validation and the error display.
	let {
		initial = { name: '', bio: null },
		cancelHref = '/people',
		onsave
	}: {
		initial?: PersonInput;
		/** Where Cancel goes: the list for a new person, the person page when editing, same as the Back link */
		cancelHref?: string;
		onsave: (input: PersonInput) => Promise<void>;
	} = $props();

	// The form copies the initial values once on purpose: the user edits the copy, not the prop.
	// svelte-ignore state_referenced_locally
	let name = $state(initial.name);
	// svelte-ignore state_referenced_locally
	let bio = $state(initial.bio ?? '');
	let saving = $state(false);
	let nameError = $state('');
	let formError = $state('');

	// The same rule as the server, for instant feedback; the server stays the truth (application-architecture 4.2).
	function validate(): boolean {
		nameError = name.trim() === '' ? 'Name is required' : '';
		return nameError === '';
	}

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		formError = '';
		if (!validate()) return;
		saving = true;
		try {
			await onsave({ name: name.trim(), bio: bio.trim() || null });
		} catch (e) {
			// VALIDATION and NOT_FOUND belong to this form ("name: must not be blank", "Person 3 not found");
			// anything else is an outage the toast reports.
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
		<label for="name">Name</label>
		<input
			id="name"
			type="text"
			bind:value={name}
			maxlength="200"
			aria-invalid={nameError !== ''}
			required
		/>
		{#if nameError}
			<p class="field-error" role="alert">{nameError}</p>
		{/if}
	</div>
	<div class="field">
		<label for="bio">Bio (optional)</label>
		<textarea id="bio" bind:value={bio} maxlength="2000" rows="4"></textarea>
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
