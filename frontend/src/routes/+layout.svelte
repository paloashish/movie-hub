<script lang="ts">
	import '../app.css';
	import favicon from '#lib/assets/favicon.svg';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import Toast from '#lib/components/Toast.svelte';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();

	// The app shell from ui-design.md section 2: brand, two sections, a search box, the page, a one-line footer.
	const links = [
		{ href: '/', label: 'Movies' },
		{ href: '/people', label: 'Cast & Creators' }
	];

	// A link is "current" when the path matches exactly (home) or starts with it (sections).
	function isCurrent(href: string): boolean {
		return href === '/' ? page.url.pathname === '/' : page.url.pathname.startsWith(href);
	}

	// The box only carries the text to the results page; E5-S1 makes that page search.
	// Follows ?q= so the box shows what the results page is showing, also after Back; still editable (ui-design section 4)
	let query = $derived(page.url.searchParams.get('q') ?? '');
	function search(event: SubmitEvent) {
		event.preventDefault();
		const text = query.trim();
		if (text) goto(`/search?q=${encodeURIComponent(text)}`);
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Movie Hub</title>
</svelte:head>

<header>
	<a class="brand" href="/">
		<span class="mark" aria-hidden="true"></span>
		Movie Hub
	</a>
	<nav aria-label="Main">
		{#each links as link (link.href)}
			<a href={link.href} aria-current={isCurrent(link.href) ? 'page' : undefined}>{link.label}</a>
		{/each}
	</nav>
	<form class="search" role="search" onsubmit={search}>
		<label class="sr-only" for="site-search">Search</label>
		<input
			id="site-search"
			type="search"
			bind:value={query}
			placeholder="Search movies, cast or creators..."
		/>
	</form>
</header>

<main>
	{@render children()}
</main>

<footer>
	<span class="footer-brand">Movie Hub</span>
	<span>Your movie collection, cast and creators in one place.</span>
</footer>

<Toast />

<style>
	header {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.5rem 1.5rem;
		padding: 0.75rem 1rem;
		background: var(--color-header);
		color: #fff;
	}
	.brand {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		font-weight: 700;
		font-size: 1.125rem;
		color: #fff;
		text-decoration: none;
	}
	/* The brand mark: a small film frame drawn with borders, no icon library (ui-design section 7) */
	.mark {
		width: 1.1rem;
		height: 0.9rem;
		border: 2px solid #fff;
		border-radius: 2px;
		border-left-width: 5px;
		border-right-width: 5px;
		border-left-style: dotted;
		border-right-style: dotted;
	}
	nav {
		display: flex;
		flex-wrap: wrap; /* on a phone the links drop under the brand instead of pushing the page wider (E5-S2) */
		gap: 0.25rem;
	}
	nav a {
		color: var(--color-header-text);
		text-decoration: none;
		padding: 0.5rem 0.75rem;
		border-radius: 0.375rem;
		font-weight: 500;
	}
	nav a:hover,
	nav a:focus-visible {
		background: var(--color-header-hover);
		color: #fff;
	}
	nav a[aria-current='page'] {
		background: var(--color-accent);
		color: #fff;
	}
	.search {
		margin-left: auto;
		flex: 1 1 14rem;
		max-width: 22rem;
	}
	.search input {
		width: 100%;
		padding: 0.45rem 0.75rem;
		border: 1px solid var(--color-header-border);
		border-radius: 0.375rem;
		background: var(--color-header-hover);
		color: #fff;
		font: inherit;
	}
	.search input::placeholder {
		color: var(--color-header-placeholder);
	}
	.search input:focus-visible {
		outline: 2px solid var(--color-accent);
		outline-offset: 1px;
	}
	main {
		flex: 1; /* takes the space the header and footer leave, so the footer stays at the bottom */
		width: 100%;
		max-width: var(--content-max);
		margin: 0 auto;
		padding: 1.5rem 1rem; /* 16px gutter at phone width (E5-S2) */
	}
	footer {
		display: flex;
		flex-wrap: wrap;
		gap: 0.25rem 1rem;
		padding: 1.25rem 1rem;
		font-size: 0.875rem;
		color: var(--color-header-text);
		background: var(--color-header);
	}
	.footer-brand {
		font-weight: 700;
		color: #fff;
	}
</style>
