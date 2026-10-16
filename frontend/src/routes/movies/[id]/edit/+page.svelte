<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { getMovie, updateMovie } from '#lib/api/movies.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess } from '#lib/stores/toast.svelte.ts';
	import type { Movie, MovieInput } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import MovieForm from '#lib/components/MovieForm.svelte';

	// E2-S4: load the movie first, then the same form as "add" with the values filled in (like the person edit page).
	// NOT_FOUND on load is its own view with no Retry, because retrying a missing id cannot succeed.
	type Status = 'loading' | 'ready' | 'notfound' | 'error';
	// The route is /movies/[id]/edit, so the param is always there; the type says "maybe" for any route.
	const id = $derived(page.params.id ?? '');
	let status = $state<Status>('loading');
	let movie = $state<Movie | null>(null);
	let errorMessage = $state('');

	async function load() {
		status = 'loading';
		try {
			movie = await getMovie(id);
			status = 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = e instanceof ApiError && e.code === 'NOT_FOUND' ? 'notfound' : 'error';
		}
	}

	// Back to the movie page, where the updated details are shown
	async function save(input: MovieInput) {
		const updated = await updateMovie(id, input);
		toastSuccess(`${updated.title} saved`);
		await goto(`/movies/${id}`);
	}

	onMount(load);
</script>

<svelte:head>
	<title>Edit movie - Movie Hub</title>
</svelte:head>

{#if status !== 'notfound'}
	<a href="/movies/{id}">&larr; Back to the movie</a>
{/if}
<h1>Edit movie</h1>

{#if status === 'loading'}
	<Spinner label="Loading movie" />
{:else if status === 'notfound'}
	<p role="alert">Movie not found</p>
	<a href="/" class="btn btn-secondary">Back to the catalogue</a>
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={load} />
{:else if movie}
	<MovieForm
		initial={{ title: movie.title, releaseYear: movie.releaseYear, synopsis: movie.synopsis }}
		cancelHref="/movies/{id}"
		onsave={save}
	/>
{/if}
