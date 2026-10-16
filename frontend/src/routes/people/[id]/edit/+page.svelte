<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { getPerson, updatePerson } from '#lib/api/people.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess } from '#lib/stores/toast.svelte.ts';
	import type { Person, PersonInput } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import PersonForm from '#lib/components/PersonForm.svelte';

	// E1-S3: load the person first, then show the same form as "add" with the values filled in.
	// Three states instead of four: an edit page has no "empty". NOT_FOUND is its own view with no Retry,
	// because retrying a missing id cannot succeed (application-architecture 4.1); the Back link is the way out.
	type Status = 'loading' | 'ready' | 'notfound' | 'error';
	// The route is /people/[id]/edit, so the param is always there; the type says "maybe" for any route.
	const id = $derived(page.params.id ?? '');
	let status = $state<Status>('loading');
	let person = $state<Person | null>(null);
	let errorMessage = $state('');

	async function load() {
		status = 'loading';
		try {
			person = await getPerson(id);
			status = 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = e instanceof ApiError && e.code === 'NOT_FOUND' ? 'notfound' : 'error';
		}
	}

	async function save(input: PersonInput) {
		const updated = await updatePerson(id, input);
		toastSuccess(`${updated.name} saved`);
		await goto(`/people/${id}`);
	}

	onMount(load);
</script>

<svelte:head>
	<title>Edit person - Movie Hub</title>
</svelte:head>

{#if status !== 'notfound'}
	<a href="/people/{id}">&larr; Back to the person</a>
{/if}
<h1>Edit person</h1>

{#if status === 'loading'}
	<Spinner label="Loading person" />
{:else if status === 'notfound'}
	<p role="alert">{errorMessage}</p>
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={load} />
{:else if person}
	<PersonForm
		initial={{ name: person.name, bio: person.bio }}
		cancelHref="/people/{id}"
		onsave={save}
	/>
{/if}
