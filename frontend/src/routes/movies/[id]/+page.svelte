<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { getMovieDetail, deleteMovie, posterSrc } from '#lib/api/movies.ts';
	import { removeCredit } from '#lib/api/credits.ts';
	import { roleLabel as labelOf } from '#lib/roles.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';
	import type { MovieDetail, Credit } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import ConfirmDialog from '#lib/components/ConfirmDialog.svelte';
	import PosterUpload from '#lib/components/PosterUpload.svelte';
	import CreditPicker from '#lib/components/CreditPicker.svelte';

	// E2-S2: the movie page (ui-design.md section 4). Edit is E2-S4, Delete is E2-S5, the poster upload E3-S1,
	// the credits that fill Cast and Creators E4-S1, removing one E4-S2.
	// NOT_FOUND is its own view with a link back; retrying a missing id cannot succeed (application-architecture 4.1).
	type Status = 'loading' | 'ready' | 'notfound' | 'error';
	// The route is /movies/[id], so the param is always there; the type says "maybe" for any route.
	const id = $derived(page.params.id ?? '');
	let status = $state<Status>('loading');
	let movie = $state<MovieDetail | null>(null);
	// E4: cast is ACTOR, creators are the other roles (application-architecture section 1)
	const cast = $derived(movie?.credits.filter((c) => c.role === 'ACTOR') ?? []);
	const creators = $derived(movie?.credits.filter((c) => c.role !== 'ACTOR') ?? []);

	const roleLabel = (credit: Credit) => labelOf(credit.role);
	let errorMessage = $state('');

	async function load() {
		status = 'loading';
		try {
			movie = await getMovieDetail(id);
			status = 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = e instanceof ApiError && e.code === 'NOT_FOUND' ? 'notfound' : 'error';
		}
	}

	// E2-S5: Delete opens the dialog; nothing happens until Confirm. The dialog stays busy while the call
	// runs so it cannot be confirmed twice. On success the movie is gone, so the grid is the place to land.
	let confirmingDelete = $state(false);
	let deleting = $state(false);

	async function confirmDelete() {
		if (!movie || deleting) return;
		deleting = true;
		try {
			await deleteMovie(movie.id);
			confirmingDelete = false; // close, toast, leave
			toastSuccess(`${movie.title} deleted`);
			await goto('/');
		} catch (e) {
			confirmingDelete = false;
			if (e instanceof ApiError && e.code === 'NOT_FOUND') {
				status = 'notfound'; // deleted elsewhere already: show that instead of a stale page
			} else {
				toastError(e instanceof ApiError ? e.message : 'Something went wrong');
			}
		} finally {
			deleting = false;
		}
	}

	// E4-S2: Remove on a credit asks first, like every delete. The person stays in the directory; only the link goes.
	let pendingCredit = $state<Credit | null>(null);
	let removing = $state(false);
	const pendingRole = $derived(pendingCredit ? roleLabel(pendingCredit) : '');

	async function confirmRemoveCredit() {
		const credit = pendingCredit;
		if (!credit || !movie || removing) return;
		removing = true;
		try {
			await removeCredit(credit.id);
			movie.credits = movie.credits.filter((c) => c.id !== credit.id);
			toastSuccess(`${credit.person.name} removed`);
		} catch (e) {
			if (e instanceof ApiError && e.code === 'NOT_FOUND') {
				// Already gone (removed elsewhere): the page should not keep showing it
				movie.credits = movie.credits.filter((c) => c.id !== credit.id);
				toastSuccess(`${credit.person.name} was already removed`);
			} else {
				toastError(e instanceof ApiError ? e.message : 'Something went wrong');
			}
		} finally {
			removing = false;
			pendingCredit = null;
		}
	}

	onMount(load);
</script>

<svelte:head>
	<title
		>{movie ? movie.title : status === 'notfound' ? 'Movie not found' : 'Movie'} - Movie Hub</title
	>
</svelte:head>

{#if status !== 'notfound'}
	<a href="/">&larr; Back to movies</a>
{/if}

{#if status === 'loading'}
	<Spinner label="Loading movie" />
{:else if status === 'notfound'}
	<div class="notfound">
		<p role="alert">Movie not found</p>
		<a href="/" class="btn btn-secondary">Back to the catalogue</a>
	</div>
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={load} />
{:else if movie}
	<article class="movie">
		<div class="poster-column">
			{#if movie.posterUrl}
				<img class="poster" src={posterSrc(movie.posterUrl)} alt="Poster of {movie.title}" />
			{:else}
				<div class="poster placeholder" role="img" aria-label="No poster for {movie.title}">
					No poster
				</div>
			{/if}
			<!-- The page only learns the new URL when the server has it; until then the old poster stays (E3-S1) -->
			<PosterUpload movieId={movie.id} onuploaded={(url) => movie && (movie.posterUrl = url)} />
		</div>
		<div class="details">
			<div class="title-row">
				<h1>{movie.title} <span class="year">({movie.releaseYear})</span></h1>
				<span class="actions">
					<a href="/movies/{movie.id}/edit" class="btn btn-secondary">Edit</a>
					<button type="button" class="btn btn-danger" onclick={() => (confirmingDelete = true)}>
						Delete
					</button>
				</span>
			</div>
			{#if movie.synopsis}
				<p class="synopsis">{movie.synopsis}</p>
			{/if}
			<section aria-labelledby="cast-heading">
				<h2 id="cast-heading">Cast</h2>
				{#if cast.length === 0}
					<p class="none">None yet</p>
				{:else}
					<ul class="credits">
						{#each cast as credit (credit.id)}
							<li>
								{credit.person.name} <span class="role">{roleLabel(credit)}</span>
								<button
									type="button"
									class="link"
									aria-label="Remove {credit.person.name} as {roleLabel(credit)}"
									onclick={() => (pendingCredit = credit)}>Remove</button
								>
							</li>
						{/each}
					</ul>
				{/if}
			</section>
			<section aria-labelledby="creators-heading">
				<h2 id="creators-heading">Creators</h2>
				{#if creators.length === 0}
					<p class="none">None yet</p>
				{:else}
					<ul class="credits">
						{#each creators as credit (credit.id)}
							<li>
								{credit.person.name} <span class="role">{roleLabel(credit)}</span>
								<button
									type="button"
									class="link"
									aria-label="Remove {credit.person.name} as {roleLabel(credit)}"
									onclick={() => (pendingCredit = credit)}>Remove</button
								>
							</li>
						{/each}
					</ul>
				{/if}
			</section>
			<CreditPicker movieId={movie.id} onadded={(credit) => movie && movie.credits.push(credit)} />
		</div>
	</article>
{/if}

{#if movie}
	<ConfirmDialog
		open={pendingCredit !== null}
		title="Remove {pendingCredit?.person.name}?"
		message="They will no longer be credited as {pendingRole} on this movie. They stay in the directory."
		confirmLabel="Remove"
		busyLabel="Removing..."
		busy={removing}
		onconfirm={confirmRemoveCredit}
		oncancel={() => (pendingCredit = null)}
	/>
	<ConfirmDialog
		open={confirmingDelete}
		title="Delete {movie.title}?"
		message="The movie, its poster and its credits will be removed. This cannot be undone."
		busy={deleting}
		onconfirm={confirmDelete}
		oncancel={() => (confirmingDelete = false)}
	/>
{/if}

<style>
	.notfound {
		padding: 2rem 0;
		display: grid;
		gap: 1rem;
		justify-items: start;
	}
	.movie {
		display: grid;
		grid-template-columns: minmax(10rem, 16rem) 1fr; /* poster beside the text above 640 px */
		gap: 1.5rem;
		margin-top: 1rem;
	}
	.poster-column {
		min-width: 0;
	}
	.poster {
		width: 100%;
		aspect-ratio: 2 / 3;
		object-fit: cover;
		border-radius: var(--radius);
		background: var(--color-border);
		box-shadow: var(--shadow);
	}
	.placeholder {
		display: grid;
		place-items: center;
		color: var(--color-muted);
	}
	.title-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		flex-wrap: wrap;
	}
	.actions {
		display: flex;
		gap: 0.5rem;
	}
	h1 {
		margin: 0 0 0.5rem;
	}
	.year {
		font-weight: 400;
		color: var(--color-muted);
	}
	.synopsis {
		margin: 0 0 1.5rem;
		max-width: 60ch;
	}
	h2 {
		font-size: 1.125rem;
		margin: 1rem 0 0.25rem;
	}
	.none {
		margin: 0;
		color: var(--color-muted);
	}
	.credits {
		list-style: none;
		margin: 0;
		padding: 0;
		display: grid;
		gap: 0.25rem;
	}
	.role {
		color: var(--color-muted);
		font-size: 0.875rem;
		margin-left: 0.5rem;
	}
	.link {
		margin-left: 0.75rem;
		background: none;
		border: 0;
		padding: 0;
		font: inherit;
		font-size: 0.875rem;
		color: var(--color-danger);
		text-decoration: underline;
		cursor: pointer;
	}
	@media (max-width: 640px) {
		.movie {
			grid-template-columns: 1fr; /* poster stacks above the text on a phone (ui-design section 5) */
		}
		.poster {
			max-width: 14rem;
		}
	}
</style>
