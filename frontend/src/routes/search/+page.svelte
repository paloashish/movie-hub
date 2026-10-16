<script lang="ts">
	import { page } from '$app/state';
	import { search } from '#lib/api/search.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import type { SearchResult } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import EmptyState from '#lib/components/EmptyState.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import MovieCard from '#lib/components/MovieCard.svelte';
	import PersonCard from '#lib/components/PersonCard.svelte';

	// E5-S1: the results page (ui-design.md section 4). The header box owns the text and puts it in the URL,
	// so this page re-searches whenever ?q= changes and a result can be bookmarked or shared.
	const text = $derived(page.url.searchParams.get('q')?.trim() ?? '');

	type Status = 'idle' | 'loading' | 'ready' | 'empty' | 'error';
	let status = $state<Status>('idle');
	let result = $state<SearchResult>({ movies: [], people: [] });
	let errorMessage = $state('');

	async function load(query: string) {
		if (!query) {
			status = 'idle';
			return;
		}
		status = 'loading';
		try {
			const found = await search(query);
			if (query !== text) return; // the URL moved on while this search ran: a newer one owns the page
			result = found;
			status = result.movies.length + result.people.length === 0 ? 'empty' : 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = 'error';
		}
	}

	$effect(() => {
		load(text);
	});
</script>

<svelte:head>
	<title>{text ? `Results for "${text}"` : 'Search'} - Movie Hub</title>
</svelte:head>

{#if status === 'idle'}
	<h1>Search</h1>
	<p class="hint">Type a movie title or a person's name in the box above.</p>
{:else}
	<h1>Results for "{text}"</h1>
	{#if status === 'loading'}
		<Spinner label="Searching" />
	{:else if status === 'empty'}
		<EmptyState message="No results for '{text}'" />
	{:else if status === 'error'}
		<ErrorState message={errorMessage} onretry={() => load(text)} />
	{:else}
		<div class="groups">
			<section aria-labelledby="movies-heading">
				<h2 id="movies-heading">Movies ({result.movies.length})</h2>
				{#if result.movies.length === 0}
					<p class="none">No movies match</p>
				{:else}
					<ul class="grid" style="--grid-min: 10rem">
						{#each result.movies as movie (movie.id)}
							<MovieCard {movie} />
						{/each}
					</ul>
				{/if}
			</section>
			<section aria-labelledby="people-heading">
				<h2 id="people-heading">People ({result.people.length})</h2>
				{#if result.people.length === 0}
					<p class="none">No people match</p>
				{:else}
					<ul class="grid" style="--grid-min: 16rem">
						{#each result.people as person (person.id)}
							<PersonCard {person} />
						{/each}
					</ul>
				{/if}
			</section>
		</div>
	{/if}
{/if}

<style>
	.hint,
	.none {
		margin: 0;
		color: var(--color-muted);
	}
	.groups {
		display: grid;
		gap: 1.5rem;
	}
	h2 {
		font-size: 1.125rem;
		margin: 0 0 0.5rem;
	}
</style>
