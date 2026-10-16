import type { Role } from '#lib/types/graphql.ts';

/** "DIRECTOR" -> "Director": the one place the role enum becomes a word on screen. */
export function roleLabel(role: Role): string {
	return role.charAt(0) + role.slice(1).toLowerCase();
}
