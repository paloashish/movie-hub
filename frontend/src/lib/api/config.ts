import { API_URL } from '$app/env/public';

// Kept in its own module so tests can mock it without touching SvelteKit's env machinery.
export const apiBaseUrl: string = API_URL;
