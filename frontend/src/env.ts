import { defineEnvVars } from '@sveltejs/kit/env';

// SvelteKit 3 way: declare variables here, read them from $app/env/public (browser) or $app/env/private (server).
export const variables = defineEnvVars({
	API_URL: {
		public: true,
		description:
			'Base URL of movie-service (GraphQL at /graphql, poster upload at /movies/{id}/poster)',
		schema: (value) => value ?? 'http://localhost:8080'
	}
});
