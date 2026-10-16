<script lang="ts">
	import { onMount } from 'svelte';
	import { listPeople } from '#lib/api/people.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import type { Person } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import EmptyState from '#lib/components/EmptyState.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import PersonCard from '#lib/components/PersonCard.svelte';

	// The four-state page pattern (application-architecture 5). Every list page looks like this.
	// Edit and Delete live on the person page (E4-S3); this list only browses.
	type Status = 'loading' | 'ready' | 'empty' | 'error';
	let status = $state<Status>('loading');
	let people = $state<Person[]>([]);
	let errorMessage = $state('');

	async function load() {
		status = 'loading';
		try {
			people = await listPeople();
			status = people.length === 0 ? 'empty' : 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = 'error';
		}
	}

	onMount(load);
</script>

<svelte:head>
	<title>Cast & Creators - Movie Hub</title>
</svelte:head>

<div class="page-header">
	<div>
		<h1>Cast & Creators</h1>
		<p>The people behind the movies. Their roles live on each movie's credits.</p>
	</div>
	<a href="/people/new" class="btn btn-primary">+ Add person</a>
</div>

{#if status === 'loading'}
	<Spinner label="Loading people" />
{:else if status === 'empty'}
	<EmptyState message="No people yet" />
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={load} />
{:else}
	<ul class="grid" style="--grid-min: 16rem">
		{#each people as person (person.id)}
			<PersonCard {person} />
		{/each}
	</ul>
{/if}
