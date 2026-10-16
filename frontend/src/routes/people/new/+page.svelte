<script lang="ts">
	import { goto } from '$app/navigation';
	import { createPerson } from '#lib/api/people.ts';
	import { toastSuccess } from '#lib/stores/toast.svelte.ts';
	import PersonForm from '#lib/components/PersonForm.svelte';
	import type { PersonInput } from '#lib/types/graphql.ts';

	// E1-S2: the form does the validating and error display; this page only says what "save" means.
	async function save(input: PersonInput) {
		const person = await createPerson(input);
		toastSuccess(`${person.name} added`);
		await goto('/people');
	}
</script>

<svelte:head>
	<title>Add person - Movie Hub</title>
</svelte:head>

<a href="/people">&larr; Back to people</a>
<h1>Add person</h1>

<PersonForm onsave={save} />
