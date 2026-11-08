# Backlog

Planned improvements for this killer sudoku fork, in the order they should be built.
The goal is an ad-free, offline killer sudoku app with puzzles hard enough that you
win fewer than 40% of them.

**Ground rule: nothing may do the puzzle's thinking for the player.** Working out cage
combinations, sums and candidates is the game. Features may make the board easier to
read, make puzzles harder, measure results honestly, or make the app nicer to use.
They may not suggest, filter or check digits. Ideas rejected for this reason: a cage
combination panel, cage-aware auto notes, remaining-sum labels, rule-violation flags,
killer hints and "check my board".

Already shipped: zero-given killer generation with killer-technique grading and a
win rate for each level (#1), clearer cage outlines (#2, #6), Play internal testing
on every merge to main (#3), launcher icon (#4), Android 16 target (#5),
"Try again" at the mistake limit (#7), win celebration (#8).

Sizes: S = an evening, M = a couple of sessions, L = a larger project.

---

## Phase 1: Unassisted play and an honest win rate

### 1. Purist defaults (S)
**Why:** The app inherits aids from upstream LibreSudoku that do part of the work: hints,
advanced hints, "compute notes", auto-erasing notes when a digit is placed, mistake
highlighting against the solution, and the remaining-uses counter on the keyboard.
**What:** Turn all of these off by default, and on killer boards hide the hint and
"compute notes" buttons entirely. Keep them only as opt-in settings (or remove them, if
you never want them). Keep the mistake limit, since it is a rule and not an aid.
**Done when:** A fresh install starts a killer game with no hint button, no computed notes
and no solution-based mistake marking.

### 2. Count only clean wins (S)
**Why:** If an aid is ever switched on, a win with it counts the same as a clean solve.
`SavedGame` stores mistakes but not hints, so the win rate can't tell them apart.
**What:** Add a column to `SavedGame` (with a Room migration) that records whether any aid
was used during the game. The per-level win rate counts only clean games, and shows the
number of assisted games beside it.
**Done when:** Old games still load (migration test), and a game with an aid turned on
is left out of the win rate.

### 3. Highlight the selected cage (S)
**Why:** With cage tints it can still be hard to see where one cage ends. This only makes
the board easier to read. It shows no sums or digits.
**What:** Draw the selected cell's cage with a stronger outline or fill.
**Done when:** The selected cage stands out in light and dark themes.

---

## Phase 2: Harder puzzles

### 4. Pre-generated puzzle stock (M)
**Why:** Challenge puzzles are generated on demand with a 20 second budget in
`KillerController`, so generation can fail or keep you waiting. A harder level (item 5)
will be slower still.
**What:** Keep a small stock (for example 3) of puzzles for each size and level in the
database. A `WorkManager` job fills it while the phone is charging or idle, and refills it
after each new game. Starting a game takes a puzzle from the stock, and falls back to live
generation when the stock is empty.
**Done when:** A 9x9 Challenge starts instantly after the app has been idle once.

### 5. An "Extreme" level above Challenge (M)
**Why:** A puzzle is Challenge when it needs chains or scores above 2800. To get your win
rate reliably under 40% you need a level that is clearly harder than that.
**What:** Add `GameDifficulty.Extreme`, appended so stored values stay valid. Give it its own
generation plan in `KillerController`: bigger cages (4 to 6 cells), more merges, and a
minimum such as multi-unit innies/outies or chains together with a high effort score.
Bump `KillerRatingPolicy.VERSION`. This depends on item 4.
**Done when:** 50 generated Extreme puzzles all solve logically, and their average score is
clearly above Challenge's (numbers reported in the PR).

### 6. Recommended level toward 40% (M)
**Why:** The score thresholds are guesses. Your own clean win rate is the real measure.
**What:** On the new game sheet, mark the level whose clean win rate over your last 20 games
is closest to 40% as "Recommended". Optionally add a "Personal" level that raises the
target effort score after a win and lowers it after a loss, kept between Hard and Extreme.
**Done when:** After a run of wins the recommendation moves up a level (unit tested).

### 7. Show what made a puzzle hard, after the game (S)
**Why:** Seeing the score and techniques afterwards helps calibrate difficulty, and it is
satisfying. It is shown only once the game is over, so it gives nothing away. The
rating metadata is already saved.
**What:** On the end-of-game sheet and the saved game screen, show the effort score and the
hardest techniques the rater needed, for example "Multi-unit innie/outie ×2".
**Done when:** Every finished killer game shows its score and techniques.

---

## Phase 3: Make it your app

### 8. Rename and tidy branding (S)
**Why:** The app is still called "LibreSudoku" (`app_name`), version 2.0.2, and the About
screen shows the upstream author's donation addresses.
**What:** Rename the app to "Killer Sudoku" and set your own version name. Keep a clear credit
and link to upstream LibreSudoku in About, which is fair and fits GPL-3.0. Replace the
donation section with that credit. Update the Play listing text to match.

### 9. Killer-first home screen (S)
**Why:** The home screen is built around classic sudoku, with killer as one option.
**What:** Default to 9x9 Killer and put the level picker with win rates up front. Move classic
and 12x12 types under "More types", or hide them with a setting.

### 10. Daily killer (M)
**Why:** A fixed daily puzzle gives a reason to open the app and a clean streak to track.
**What:** Generate one Challenge or Extreme puzzle each day from a seed based on the date, so it
works offline. Give it its own streak and history on the statistics screen.

---

## Phase 4: Upkeep

### 11. 12x12 killer without givens, or drop it (M)
12x12 killer still uses the old format with given digits, because the new generator was too
slow for it. With the stock from item 4, generation could run in the background. If you don't
play 12x12, hide it instead (item 9) and skip this.

### 12. Pull in upstream fixes regularly (S, recurring)
Upstream LibreSudoku is still active. Every month or two, merge upstream `kaajjo/LibreSudoku`
main into a branch, resolve conflicts, and merge if CI passes. Upstream changes that add
solving aids are skipped or kept off.

### 13. UI tests for the game screen (M)
Unit tests run in PR CI, but nothing tests the game screen. Add Robolectric Compose tests for
the killer flows (purist defaults, cage highlight, clean-win recording). Every merge to
main ships to your phone automatically, so these tests catch a broken game screen first.

### 14. Board export with cages (S)
"Export board" copies only the digits, which loses a killer's cages. Include the cages in the
exported string so a puzzle can be shared or imported again. The parser already writes cages
for the database.
