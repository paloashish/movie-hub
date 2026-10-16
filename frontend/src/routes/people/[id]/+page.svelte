<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { getPersonDetail, deletePerson } from '#lib/api/people.ts';
	import { posterSrc } from '#lib/api/movies.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';
	import type { PersonDetail } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import ErrorState from '#lib/components/ErrorState.svelte';
	import ConfirmDialog from '#lib/components/ConfirmDialog.svelte';
	import { roleLabel } from '#lib/roles.ts';

	// E4-S3: the person page (ui-design.md section 4): name, bio, Edit, Delete, then their movies newest first.
	// Delete moved here from the card (E1-S4); it removes their credits first, then the person (system-design 3.3).
	type Status = 'loading' | 'ready' | 'notfound' | 'error';
	// The route is /people/[id], so the param is always there; the type says "maybe" for any route.
	const id = $derived(page.params.id ?? '');
	let status = $state<Status>('loading');
	let person = $state<PersonDetail | null>(null);
	let errorMessage = $state('');

	async function load() {
		status = 'loading';
		try {
			person = await getPersonDetail(id);
			status = 'ready';
		} catch (e) {
			errorMessage = e instanceof ApiError ? e.message : 'Something went wrong';
			status = e instanceof ApiError && e.code === 'NOT_FOUND' ? 'notfound' : 'error';
		}
	}

	let confirmingDelete = $state(false);
	let deleting = $state(false);

	async function confirmDelete() {
		if (!person || deleting) return;
		deleting = true;
		try {
			await deletePerson(person.id);
			confirmingDelete = false;
			toastSuccess(`${person.name} deleted`);
			await goto('/people');
		} catch (e) {
			confirmingDelete = false;
			if (e instanceof ApiError && e.code === 'NOT_FOUND') {
				status = 'notfound';
			} else {
				toastError(e instanceof ApiError ? e.message : 'Something went wrong');
			}
		} finally {
			deleting = false;
		}
	}

	onMount(load);
</script>

<svelte:head>
	<title
		>{person ? person.name : status === 'notfound' ? 'Person not found' : 'Person'} - Movie Hub</title
	>
</svelte:head>

{#if status !== 'notfound'}
	<a href="/people">&larr; Back to people</a>
{/if}

{#if status === 'loading'}
	<Spinner label="Loading person" />
{:else if status === 'notfound'}
	<div class="notfound">
		<p role="alert">Person not found</p>
		<a href="/people" class="btn btn-secondary">Back to people</a>
	</div>
{:else if status === 'error'}
	<ErrorState message={errorMessage} onretry={load} />
{:else if person}
	<article>
		<div class="title-row">
			<h1>{person.name}</h1>
			<span class="actions">
				<a href="/people/{person.id}/edit" class="btn btn-secondary">Edit</a>
				<button type="button" class="btn btn-danger" onclick={() => (confirmingDelete = true)}
					>Delete</button
				>
			</span>
		</div>
		{#if person.bio}
			<p class="bio">{person.bio}</p>
		{/if}
		<section aria-labelledby="movies-heading">
			<h2 id="movies-heading">Movies</h2>
			{#if person.credits.length === 0}
				<p class="none">No credits yet</p>
			{:else}
				<ul class="credits">
					{#each person.credits as credit (credit.id)}
						<li class="card">
							<a href="/movies/{credit.movie.id}" aria-label="Open {credit.movie.title}">
								{#if credit.movie.posterUrl}
									<img
										src={posterSrc(credit.movie.posterUrl)}
										alt="Poster of {credit.movie.title}"
									/>
								{:else}
									<span class="placeholder" aria-hidden="true"></span>
								{/if}
								<span class="text">
									<strong>{credit.movie.title}</strong>
									<span class="meta"
										>{credit.movie.releaseYear} &middot; {roleLabel(credit.role)}</span
									>
								</span>
							</a>
						</li>
					{/each}
				</ul>
			{/if}
		</section>
	</article>

	<ConfirmDialog
		open={confirmingDelete}
		title="Delete {person.name}?"
		message="They will be removed from the directory and from any movie credits. This cannot be undone."
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
	.title-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		flex-wrap: wrap;
		margin-top: 1rem;
	}
	.actions {
		display: flex;
		gap: 0.5rem;
	}
	h1 {
		margin: 0 0 0.5rem;
	}
	.bio {
		margin: 0 0 1.5rem;
		max-width: 60ch;
	}
	h2 {
		font-size: 1.125rem;
		margin: 1rem 0 0.5rem;
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
		gap: 0.5rem;
		max-width: 32rem;
	}
	.credits a {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.5rem 0.75rem;
		color: inherit;
		text-decoration: none;
	}
	.credits img,
	.placeholder {
		flex: none;
		width: 2.5rem;
		aspect-ratio: 2 / 3;
		object-fit: cover;
		border-radius: 0.25rem;
		background: var(--color-border);
	}
	.text {
		display: grid;
	}
	.meta {
		color: var(--color-muted);
		font-size: 0.875rem;
	}
</style>
