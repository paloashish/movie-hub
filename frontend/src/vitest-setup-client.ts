// Adds matchers such as toBeInTheDocument() to every component test.
import '@testing-library/jest-dom/vitest';
// Unmount rendered components after each test. Testing Library only does this by itself when Vitest globals are on.
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/svelte';

afterEach(() => cleanup());
