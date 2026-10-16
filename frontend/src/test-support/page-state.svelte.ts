import { SvelteURL } from 'svelte/reactivity';

// A reactive stand-in for SvelteKit's `page` in component tests. Tests assign `pageState.url` and anything
// derived from it in the component follows, exactly as it does in the browser after Back or a link.
// Used from a vi.mock factory, which may import lazily: `async () => ({ page: (await import(...)).pageState })`.
export const pageState = $state({
	url: new SvelteURL('http://localhost/') as URL,
	params: {} as Record<string, string>
});
