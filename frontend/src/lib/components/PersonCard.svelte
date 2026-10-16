<script lang="ts">
	import type { Person } from '#lib/types/graphql.ts';

	// One row in the people list; the whole card links to the person page (E4-S3), where Edit and Delete live.
	let { person }: { person: Person } = $props();

	// No photos in scope, so the first letter stands in for the avatar the reference design shows.
	// Names are never blank: both services reject them.
	const initial = $derived(person.name.trim().charAt(0).toUpperCase());
</script>

<li class="card person">
	<a href="/people/{person.id}" aria-label="Open {person.name}">
		<span class="avatar" aria-hidden="true">{initial}</span>
		<div class="body">
			<h2>{person.name}</h2>
			{#if person.bio}
				<p>{person.bio}</p>
			{/if}
		</div>
	</a>
</li>

<style>
	.person a {
		display: flex;
		gap: 0.75rem;
		padding: 0.875rem 1rem;
		color: inherit;
		text-decoration: none;
	}
	.person a:hover h2,
	.person a:focus-visible h2 {
		color: var(--color-accent);
	}
	.avatar {
		flex: none;
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		display: grid;
		place-items: center;
		background: var(--color-accent-soft);
		color: var(--color-accent);
		font-weight: 700;
	}
	.body {
		min-width: 0;
		flex: 1;
	}
	h2 {
		margin: 0;
		font-size: 1rem;
	}
	p {
		margin: 0.25rem 0 0;
		color: var(--color-muted);
		font-size: 0.875rem;
		overflow-wrap: anywhere;
	}
</style>
