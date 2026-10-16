<script lang="ts">
	import { goto } from '$app/navigation';
	import { createMovie } from '#lib/api/movies.ts';
	import { toastSuccess } from '#lib/stores/toast.svelte.ts';
	import MovieForm from '#lib/components/MovieForm.svelte';
	import type { MovieInput } from '#lib/types/graphql.ts';

	// E2-S3: the form validates and shows errors; this page only says what "save" means.
	async function save(input: MovieInput) {
		const movie = await createMovie(input);
		toastSuccess(`${movie.title} added`);
		await goto('/');
	}
</script>

<svelte:head>
	<title>Add movie - Movie Hub</title>
</svelte:head>

<a href="/">&larr; Back to movies</a>
<h1>Add movie</h1>

<MovieForm onsave={save} />
