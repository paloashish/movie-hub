import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import PersonCard from './PersonCard.svelte';

describe('PersonCard', () => {
	it('should show the name and the bio', () => {
		render(PersonCard, { props: { person: { id: '1', name: 'Anna', bio: 'Actor' } } });

		expect(screen.getByRole('heading', { name: 'Anna' })).toBeInTheDocument();
		expect(screen.getByText('Actor')).toBeInTheDocument();
	});

	it('should link the whole card to the person page', () => {
		render(PersonCard, { props: { person: { id: '3', name: 'Anna', bio: null } } });

		expect(screen.getByRole('link', { name: 'Open Anna' })).toHaveAttribute('href', '/people/3');
	});

	it('should show only the name when there is no bio', () => {
		render(PersonCard, { props: { person: { id: '1', name: 'Bob', bio: null } } });

		expect(screen.getByRole('heading', { name: 'Bob' })).toBeInTheDocument();
		expect(screen.queryByRole('paragraph')).not.toBeInTheDocument();
	});
});
