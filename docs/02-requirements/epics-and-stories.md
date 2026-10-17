# Epics and User Stories - Movie Hub

| | |
|---|---|
| Version | 0.1 |
| Status | Reviewed, draft accepted |
| Source | `docs/01-business/brd.md` |
| Personas | Catalogue manager (CM), Visitor (V) |

A tick after an ID means the story is done (definition of done met). Priority uses MoSCoW: Must, Should, Could. Every story cites the BRD feature it serves through its epic. Feedback rules from BRD N2 appear as acceptance criteria on the stories they affect.

## E1 - People (cast and creators)
Maintain the people directory: actors, directors, writers and producers. Covers: F4, F5.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E1-S1 ✓ | Must | As a visitor, I want to see a list of cast and creators, so that I can browse who is in the catalogue. | - Given people exist, when I open the People page, then I see a loading indicator and then the names sorted A to Z<br>- Given no people exist, then I see a "No people yet" message<br>- Given the list cannot be loaded, then I see an error message with a Retry action |
| E1-S2 ✓ | Must | As a catalogue manager, I want to add a person with a name and optional short bio, so that they can be credited on movies. | - Given a valid name, when I save, then I see a saving indicator, a success message, and the person in the list<br>- Given a blank name, when I save, then I see a validation error and nothing is saved |
| E1-S3 ✓ | Must | As a catalogue manager, I want to edit a person, so that mistakes can be corrected. | - Given an existing person, when I change the name and save, then I see a success message and the new name everywhere it appears<br>- Given a blank name, when I save, then I see a validation error<br>- Given the person was deleted meanwhile, when I save, then I see a "not found" error |
| E1-S4 ✓ | Must | As a catalogue manager, I want to delete a person, so that the directory stays accurate. | - When I choose Delete, then I see a confirmation before anything happens<br>- Given I confirm, then the person disappears from the list, I see a success message, and they no longer appear in any movie's credits<br>- Given I cancel, then nothing changes |

## E2 - Movies
Browse and maintain movie records. Covers: F1, F2.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E2-S1 ✓ | Must | As a visitor, I want to see all movies as a grid of posters with title and year, so that I can browse the catalogue. | - Given movies exist, when I open the home page, then I see a loading indicator and then the poster grid<br>- Given a movie has no poster, then a placeholder image is shown in its place<br>- Given no movies exist, then I see a "No movies yet" message; given the list cannot be loaded, then I see an error message with a Retry action |
| E2-S2 ✓ | Must | As a visitor, I want to open a movie and see its poster, details, cast and creators, so that I can learn about it. | - Given an existing movie, when I open it, then I see poster, title, year, synopsis, a Cast list and a Creators list with each person's role<br>- Given a movie with no credits, then each list shows "None yet"<br>- Given a movie that does not exist, then I see a "Movie not found" message with a link back to the catalogue |
| E2-S3 ✓ | Must | As a catalogue manager, I want to add a movie with title, release year and optional synopsis, so that the catalogue grows. | - Given a valid title and year, when I save, then I see a saving indicator, a success message, and the movie in the grid<br>- Given a blank title, when I save, then I see a validation error and nothing is saved<br>- Given a year before 1888 or more than five years in the future, when I save, then I see a validation error |
| E2-S4 ✓ | Must | As a catalogue manager, I want to edit a movie, so that details stay correct. | - Given an existing movie, when I change fields and save, then I see a success message and the updated details<br>- Given invalid values, when I save, then I see the matching validation error<br>- Given the movie was deleted meanwhile, when I save, then I see a "not found" error |
| E2-S5 ✓ | Must | As a catalogue manager, I want to delete a movie, so that removed titles disappear from the catalogue. | - When I choose Delete, then I see a confirmation before anything happens<br>- Given I confirm, then the movie, its poster and its credits are gone and I see a success message<br>- Given I cancel, then nothing changes |
| E2-S6 ✓ | Should | As a visitor, I want the movie grid split into pages of 12, so that large catalogues stay fast to browse. | - Given more than 12 movies, then I see page controls and the current page number<br>- Given I change page, then I see a loading indicator and the next 12 movies |

## E3 - Artwork
Upload and manage poster images. Covers: F3.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E3-S1 ✓ | Must | As a catalogue manager, I want to upload a poster for a movie and watch its progress, so that I know the upload is working. | - Given a JPG or PNG up to 10 MB, when I upload it, then I see a progress bar with a percentage from 0 to 100, then the poster appears with a success message<br>- Given the upload fails part way, then I see an error message with a Retry action and the previous poster, if any, is unchanged |
| E3-S2 ✓ | Must | As a catalogue manager, I want invalid files rejected before upload, so that I do not waste time. | - Given a file that is not JPG or PNG, when I pick it, then I see an error and no upload starts<br>- Given a file larger than 10 MB, when I pick it, then I see an error naming the limit and no upload starts |
| E3-S3 ✓ | Should | As a catalogue manager, I want to replace an existing poster, so that artwork can be updated. (Note from E3-S1 review: the file keeps its name on replace, so the image URL needs a cache-buster or the browser shows the old picture.) | - Given a movie with a poster, when I upload a new one, then the new poster is shown on the grid and the detail page<br>- Given the replacement fails, then the old poster stays |

## E4 - Credits
Link people to movies with a role. Covers: F7.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E4-S1 ✓ | Must | As a catalogue manager, I want to add a person to a movie with a role (actor, director, writer or producer), so that credits are shown. | - Given I find a person by name and pick a role, when I save, then I see a success message and the person appears under Cast (actor) or Creators (other roles) on the movie page<br>- Given the same person and role already exist on the movie, when I save, then I see a "already credited" error<br>- Given no person matches my search, then I see "No people found" with a link to add one |
| E4-S2 ✓ | Must | As a catalogue manager, I want to remove a credit, so that wrong credits can be fixed. | - When I choose Remove on a credit, then I see a confirmation<br>- Given I confirm, then the credit disappears from the movie page with a success message; the person still exists in the directory<br>- Given I cancel, then nothing changes |
| E4-S3 ✓ | Must | As a visitor, I want to see a person's movies and roles on their page, so that I can follow their work. (Also: move Edit and Delete from the PersonCard to this page, per ui-design.) | - Given a person with credits, when I open them, then I see their movies with poster, title, year and role, newest first<br>- Given a person with no credits, then I see "No credits yet" |

## E5 - Search, responsive layout and consistent look
Find anything quickly on any device, and every screen looks like the design. Covers: F6, N1, N2, N5.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E5-S1 ✓ | Must | As a visitor, I want one search box that finds movies by title and people by name, so that I can find anything fast. | - Given I type at least one character, when I search, then I see a loading indicator and then results grouped under Movies and People, each opening its detail page<br>- Given "incep", then "Inception" matches: matching is case-insensitive and partial<br>- Given no match, then I see "No results for '<text>'" |
| E5-S2 ✓ | Must | As a visitor, I want the app to work on my phone, tablet and desktop, so that I can use it anywhere. | - Given a phone width (about 375 px), then the people list shows one column and the poster grid two (as in the reference mobile screen), there is no horizontal scrolling, forms can be completed without zooming, and search, navigation and feedback messages stay visible<br>- Given a tablet width, then the grid shows two to four columns; given a desktop width, four or more<br>- Given any screen, then every form field has a visible label, every action is reachable by keyboard, and every poster has alt text (N5) |
| E5-S3 ✓ | Should | As a visitor, I want every screen to share one consistent look, so that the app feels like one product. | - Given any screen, when I compare it with its wireframe in `ui-design.md` section 4 and the reference `moviehub-screen-collection.png`, then the header, page title and primary action, cards, forms, buttons, Back links and footer sit where the design puts them and use the shared styles only<br>- Given the design's visual choices (section 7), then no screen has a one-off colour, font or button style<br>- Given a list, detail or form page, then its feedback states match section 6 of the design (done last, once every screen exists, so the pass happens once) |

## E6 - Extras (stretch)
Nice-to-haves if time allows. Covers: F8.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E6-S1 ✓ | Could | As a visitor, I want to sort movies by title or year, so that I can find recent or alphabetical titles. | - Given I pick a sort order, then the grid reorders and the chosen order is shown<br>- Given the sort order is changed, then the page resets to the first page |
| E6-S2 ✓ | Could | As a catalogue manager, I want sample data loaded on first run, so that the app is not empty when demonstrated. | - Given an empty catalogue on first start, then exactly 10 movies and 20 people with credits are present<br>- Given data already exists, then nothing is added again |

## Coverage check

| BRD item | Epic |
|----------|------|
| F1 List movies with artwork, in pages | E2 (E2-S1, E2-S6) |
| F2 Movie CRUD | E2 |
| F3 Upload artwork | E3 |
| F4 List cast and creators | E1 |
| F5 People CRUD | E1 |
| F6 Search | E5 |
| F7 Credits | E4 |
| F8 Sorting, seed data | E6 |
| N1 Responsive | E5-S2 |
| N2 Feedback | Acceptance criteria throughout |
| N3 Unit tests with edge cases | Not a story; see `docs/04-engineering/testing-strategy.md` |
| N4 One-command local run | Not a story; see the root README |
| N5 Basic accessibility | E5-S2 |
