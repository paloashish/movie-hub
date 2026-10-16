<script lang="ts">
	import { onMount } from 'svelte';
	import { listMovies, PAGE_SIZE } from '#lib/api/movies.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { MOVIE_SORTS, type Movie, type MovieSort } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import EmptyState from '#lib/components/EmptyState.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import MovieCard from '#lib/components/MovieCard.svelte';
	import Pagination from '#lib/components/Pagination.svelte';

	// The four-state page pattern (application-architecture 5), same shape as the people list.
	type Status = 'loading' | 'ready' | 'empty' | 'error';
	let status = $state<Status>('loading');
	let movies = $state<Movie[]>([]);
	let page = $state(0);
	let total = $state(0);
	let sort = $state<MovieSort>('TITLE_ASC');
	let errorMessage = $state('');

	// E2-S6: every page change goes through here, so the spinner shows while the next page loads.
	async function load(toPage = page) {
		page = toPage; // set first, so a Retry after a failure asks for the page that failed
		status = 'loading';
		const requested = sort; // if the order changes while this request runs, the newer request owns the grid
		try {
			const result = await listMovies(toPage, sort);
			if (sort !== requested) return;
			const lastPage = Math.max(0, Math.ceil(result.total / PAGE_SIZE) - 1);
			if (result.items.length === 0 && toPage > lastPage) {
				// Past the end (a movie on the last page was deleted elsewhere): show the last page instead
				return load(lastPage);
			}
			movies = result.items;
			total = result.total;
			status = total === 0 ? 'empty' : 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = 'error';
		}
	}

	// E6-S1: a new order starts again from the first page
	function changeSort(event: Event) {
		sort = (event.currentTarget as HTMLSelectElement).value as MovieSort;
		load(0);
	}

	onMount(() => load(0));
</script>

<svelte:head>
	<title>Movies - Movie Hub</title>
</svelte:head>

<div class="page-header">
	<div>
		<h1>Movies</h1>
		<p>Discover great movies, explore cast and creators.</p>
	</div>
	<span class="controls">
		<label for="sort">Sort</label>
		<select id="sort" value={sort} onchange={changeSort}>
			{#each MOVIE_SORTS as option (option.value)}
				<option value={option.value}>{option.label}</option>
			{/each}
		</select>
		<a href="/movies/new" class="btn btn-primary">+ Add movie</a>
	</span>
</div>

{#if status === 'loading'}
	<Spinner label="Loading movies" />
{:else if status === 'empty'}
	<EmptyState message="No movies yet" />
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={() => load()} />
{:else}
	<ul class="grid" style="--grid-min: 10rem">
		{#each movies as movie (movie.id)}
			<MovieCard {movie} />
		{/each}
	</ul>
	<Pagination {page} size={PAGE_SIZE} {total} onchange={(p) => load(p)} />
{/if}

<style>
	.controls {
		display: flex;
		align-items: center;
		gap: 0.5rem 0.75rem;
		flex-wrap: wrap;
	}
	.controls label {
		font-weight: 600;
	}
	.controls select {
		font: inherit;
		padding: 0.45rem 0.75rem;
		border: 1px solid var(--color-border);
		border-radius: 0.375rem;
		background: var(--color-surface);
	}
</style>
