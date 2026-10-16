import { defineConfig } from 'vitest/config';
import adapter from '@sveltejs/adapter-auto';
import { sveltekit } from '@sveltejs/kit/vite';
import { svelteTesting } from '@testing-library/svelte/vite';

export default defineConfig({
	plugins: [
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},

			// adapter-auto only supports some environments, see https://svelte.dev/docs/kit/adapter-auto for a list.
			adapter: adapter()
		})
	],
	test: {
		expect: { requireAssertions: true },
		projects: [
			{
				// Plain TypeScript (api modules, stores, helpers) runs in node.
				extends: './vite.config.ts',
				test: {
					name: 'server',
					environment: 'node',
					include: ['src/**/*.{test,spec}.{js,ts}'],
					exclude: ['src/**/*.svelte.{test,spec}.{js,ts}']
				}
			},
			{
				// Svelte components run in jsdom with Testing Library. Name the file X.svelte.test.ts.
				extends: './vite.config.ts',
				plugins: [svelteTesting()],
				test: {
					name: 'client',
					environment: 'jsdom',
					include: ['src/**/*.svelte.{test,spec}.{js,ts}'],
					setupFiles: ['./src/vitest-setup-client.ts']
				}
			}
		]
	}
});
