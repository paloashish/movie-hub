<script lang="ts">
	import type { Movie } from '#lib/types/graphql.ts';
	import { posterSrc } from '#lib/api/movies.ts';

	// One tile in the movie grid (ui-design.md section 4); the whole card links to the movie page (E2-S2).
	let { movie }: { movie: Movie } = $props();
</script>

<li class="card movie">
	<a href="/movies/{movie.id}" aria-label="Open {movie.title}">
		{#if movie.posterUrl}
			<img class="poster" src={posterSrc(movie.posterUrl)} alt="Poster of {movie.title}" />
		{:else}
			<!-- A placeholder keeps the grid aligned; the title is read out instead of an image -->
			<div class="poster placeholder" role="img" aria-label="No poster for {movie.title}">
				No poster
			</div>
		{/if}
		<div class="body">
			<h2>{movie.title}</h2>
			<p>{movie.releaseYear}</p>
		</div>
	</a>
</li>

<style>
	.movie {
		overflow: hidden; /* the poster's corners follow the card's radius */
	}
	a {
		display: block;
		color: inherit;
		text-decoration: none;
	}
	a:hover h2,
	a:focus-visible h2 {
		color: var(--color-accent);
	}
	.poster {
		display: block;
		width: 100%;
		aspect-ratio: 2 / 3; /* every poster box is the same shape, whatever the image size */
		object-fit: cover;
		background: var(--color-border);
	}
	.placeholder {
		display: grid;
		place-items: center;
		color: var(--color-muted);
		font-size: 0.875rem;
	}
	.body {
		padding: 0.625rem 0.75rem 0.75rem;
	}
	h2 {
		margin: 0;
		font-size: 0.95rem;
		line-height: 1.3;
		overflow-wrap: anywhere;
	}
	p {
		margin: 0.125rem 0 0;
		color: var(--color-muted);
		font-size: 0.8125rem;
	}
</style>
