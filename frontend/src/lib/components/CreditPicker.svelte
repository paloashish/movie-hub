<script lang="ts">
	import { searchPeople } from '#lib/api/people.ts';
	import { addCredit } from '#lib/api/credits.ts';
	import { ApiError } from '#lib/api/errors.ts';
	import { toastSuccess, toastError } from '#lib/stores/toast.svelte.ts';
	import { ROLES, type Credit, type Person, type Role } from '#lib/types/graphql.ts';
	import Spinner from '#lib/components/Spinner.svelte';
	import { roleLabel } from '#lib/roles.ts';

	// E4-S1: find a person by name, pick a role, add (ui-design.md section 4, movie detail).
	// "already credited" and "No people found" belong to this box; outages go to the toast.
	let { movieId, onadded }: { movieId: string; onadded: (credit: Credit) => void } = $props();

	let text = $state('');
	let results = $state<Person[]>([]);
	let searched = $state(false); // true once a search has answered, so "No people found" never shows before typing
	let searching = $state(false);
	let selected = $state<Person | null>(null);
	let role = $state<Role>('ACTOR');
	let saving = $state(false);
	let errorMessage = $state('');
	let searchTimer: ReturnType<typeof setTimeout> | undefined;

	// Search a moment after the last keystroke, so typing "nolan" is one call, not five
	function oninput() {
		selected = null;
		errorMessage = '';
		clearTimeout(searchTimer);
		const query = text.trim();
		if (!query) {
			results = [];
			searched = false;
			return;
		}
		searchTimer = setTimeout(() => search(query), 300);
	}

	async function search(query: string) {
		searching = true;
		try {
			const found = await searchPeople(query);
			if (query !== text.trim()) return; // the user kept typing: a newer search owns the results
			results = found;
			searched = true;
		} catch (e) {
			toastError(e instanceof ApiError ? e.message : 'Something went wrong');
		} finally {
			searching = false;
		}
	}

	function choose(person: Person) {
		selected = person;
		text = person.name;
		results = [];
		searched = false;
	}

	async function add(event: SubmitEvent) {
		event.preventDefault();
		if (!selected || saving) return;
		saving = true;
		errorMessage = '';
		try {
			const credit = await addCredit(movieId, selected.id, role);
			toastSuccess(`${credit.person.name} added as ${credit.role.toLowerCase()}`);
			onadded(credit);
			selected = null;
			text = '';
		} catch (e) {
			if (e instanceof ApiError && (e.code === 'CONFLICT' || e.code === 'NOT_FOUND')) {
				errorMessage = e.message;
			} else {
				toastError(e instanceof ApiError ? e.message : 'Something went wrong');
			}
		} finally {
			saving = false;
		}
	}
</script>

<form class="credit-picker" onsubmit={add}>
	<div class="field">
		<label for="credit-person">Add a credit</label>
		<input
			id="credit-person"
			type="search"
			placeholder="Find a person by name"
			bind:value={text}
			{oninput}
			autocomplete="off"
		/>
		{#if searching}
			<Spinner label="Searching people" />
		{:else if results.length > 0}
			<ul class="results" aria-label="Matching people">
				{#each results as person (person.id)}
					<li>
						<button type="button" onclick={() => choose(person)}>{person.name}</button>
					</li>
				{/each}
			</ul>
		{:else if searched}
			<p class="none" role="status">
				No people found. <a href="/people/new">Add a person</a>
			</p>
		{/if}
	</div>
	<div class="row">
		<div class="field">
			<label for="credit-role">Role</label>
			<select id="credit-role" bind:value={role}>
				{#each ROLES as r (r)}
					<option value={r}>{roleLabel(r)}</option>
				{/each}
			</select>
		</div>
		<button type="submit" class="btn btn-primary" disabled={!selected || saving}>
			{#if saving}
				<Spinner label="Adding" />
			{:else}
				Add
			{/if}
		</button>
	</div>
	{#if errorMessage}
		<p class="field-error" role="alert">{errorMessage}</p>
	{/if}
</form>

<style>
	.credit-picker {
		margin-top: 1rem;
		padding: 1rem;
		border: 1px dashed var(--color-border);
		border-radius: var(--radius);
		max-width: 28rem;
	}
	.results {
		list-style: none;
		margin: 0.25rem 0 0;
		padding: 0;
		border: 1px solid var(--color-border);
		border-radius: 0.375rem;
		background: var(--color-surface);
		max-height: 12rem;
		overflow: auto;
	}
	.results button {
		width: 100%;
		text-align: left;
		padding: 0.5rem 0.75rem;
		background: none;
		border: 0;
		font: inherit;
		cursor: pointer;
	}
	.results button:hover,
	.results button:focus-visible {
		background: var(--color-accent-soft);
	}
	.none {
		margin: 0.25rem 0 0;
		color: var(--color-muted);
		font-size: 0.875rem;
	}
	.row {
		display: flex;
		align-items: flex-end;
		gap: 0.75rem;
	}
	.row .field {
		flex: 1;
		margin-bottom: 0;
	}
</style>
