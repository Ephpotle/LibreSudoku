# Backlog

Planned improvements for this killer sudoku fork, in the order they should be built.
The goal is an ad-free, offline killer sudoku app with puzzles hard enough that you
win fewer than 40% of them.

Already shipped: zero-given killer generation with killer-technique grading and a
win rate for each level (#1), clearer cage outlines (#2, #6), Play internal testing
on every merge to main (#3), launcher icon (#4), Android 16 target (#5),
"Try again" at the mistake limit (#7), win celebration (#8).

Each item says why it matters, what to change, and roughly how big it is
(S = an evening, M = a couple of sessions, L = a larger project).

---

## Phase 1: Make killer play feel right

These are the gaps you notice in every killer game today.

### 1. Killer-aware auto notes (S)
**Why:** "Compute notes" ignores cages. `SudokuUtils.computeNotes` only removes digits
seen in the row, column and box, so a 3-cage of 2 still gets 3-9 as notes.
**What:** When the board has cages, intersect each cell's candidates with the digits
that can appear in some valid combination of its cage (`CageCombinations` already
computes this), minus digits already placed in the cage. Apply the same rule in
auto-erase notes, so placing a digit removes it from the rest of its cage.
**Done when:** Notes on a fresh killer show only cage-legal digits, and unit tests cover
a 2-cage, a 3-cage and a cage with one digit placed.

### 2. Cage combination panel (M)
**Why:** Looking up "which digit sets make 17 in 3 cells" is the core killer chore. Most
killer apps show it.
**What:** When a cell is selected, show a small strip above the keyboard with the
selected cage's sum, its size and every combination that still fits (for example
`17/3: 289 379 469 478 568`). Grey out combinations that clash with placed digits or
with digits that are already fixed in the row, column or box. Add a setting to turn it
off for purists.
**Done when:** The strip updates as digits are placed and never lists an impossible set.

### 3. Highlight the selected cage and its remaining sum (S)
**Why:** With cage tints it can still be hard to see where one cage ends, and mental
arithmetic on partly filled cages is error-prone.
**What:** Draw the selected cell's cage with a stronger outline or fill. Optionally
show the remaining sum ("17 → 8 left") in the cage label once a digit is placed.
**Done when:** The selected cage stands out in light and dark themes, checked with a
screenshot test or a manual check on the internal track.

### 4. Cage rule mistakes (S)
**Why:** Mistake highlighting compares against the solution. A rule-based mode is more
useful when you play without "highlight mistakes": flag a full cage whose sum is
wrong, or a cage with a repeated digit.
**What:** Add a "Rules only" option to mistake highlighting that marks rule violations
(duplicates in a row, column, box or cage, and wrong totals in full cages) without
revealing the solution.
**Done when:** A cage with a repeat or a wrong total is marked, and a correct but
not-yet-solution-matching state is not.

---

## Phase 2: Hard puzzles and an honest win rate

This is the core of the "under 40%" goal. Order matters: the win rate has to be honest
before it can be used to tune difficulty.

### 5. Record assistance, and count only strict wins (M)
**Why:** A win with hints, auto notes or solution-based mistake highlighting counts the
same as a clean solve. `SavedGame` stores mistakes but not hints used, so the win rate
is inflated and can't guide difficulty.
**What:** Add columns to `SavedGame` (a Room migration): hints used, advanced hints
used, whether auto notes or solution-based mistake highlighting were on. A game is
"strict" if none of these were used. On the statistics screen, show the win rate for
each level over strict games, with the assisted count beside it.
**Done when:** A game where you took one hint shows as assisted, and old games still
load (migration test).

### 6. Strict mode preset (S)
**Why:** One switch is easier than five settings, and it makes item 5 meaningful.
**What:** A "Strict" toggle on the new game sheet, or a setting, that turns off hints,
auto notes and solution-based mistake highlighting, and sets the mistake limit to 3.
The cage combination panel (item 2) stays allowed, since it is what a paper player
would look up.
**Done when:** Starting a strict game locks those settings for that game only.

### 7. Pre-generated puzzle stock (M)
**Why:** Challenge generation runs on demand with a 20 second budget in
`KillerController` and can fail or keep you waiting. Harder levels (item 8) will be
slower still.
**What:** Keep a small stock (for example 3) of puzzles per size and level in the
database, filled by a `WorkManager` job while charging or idle, and refilled after each
new game. Starting a game takes from the stock and falls back to live generation when
it is empty.
**Done when:** Starting a 9x9 Challenge is instant after the app has been idle once.

### 8. An "Extreme" level above Challenge (M)
**Why:** Challenge is currently "chains, or score above 2800". To reliably reach a win
rate under 40% you need a level that is clearly harder.
**What:** Add `GameDifficulty.Extreme` (append it, so stored values stay valid) with its
own generation plan in `KillerController`: bigger cages (sizes 4-6), more merges, and
a minimum requirement such as multi-unit innies/outies or chains *and* a high
effort score. Bump `KillerRatingPolicy.VERSION`. Depends on item 7, since generation
will take longer.
**Done when:** A batch of 50 generated Extreme puzzles all solve logically, and their
average score is clearly above Challenge (report the numbers in the PR).

### 9. Adaptive difficulty toward 40% (M)
**Why:** Fixed score thresholds are a guess. Your own strict win rate is the real signal.
**What:** On the new game sheet, mark the level whose strict win rate over your last
20 games is closest to (but under) 40% as "Recommended". Optionally add a "Personal"
level that nudges the target effort score up after a win and down after a loss,
within the range of Hard to Extreme, and stores the current target in preferences.
**Done when:** After a run of wins the recommendation moves up a level, shown with a
unit test on the selection logic.

### 10. Show what made a puzzle hard (S)
**Why:** After a loss or a win it is satisfying, and useful for calibrating, to see the
puzzle's score and which techniques it needed. The rating metadata is already saved.
**What:** On the end-of-game sheet and the saved game screen, show the effort score and
the hardest techniques used (for example "Multi-unit innie/outie × 2, X-Wing × 1").
**Done when:** Every generated killer shows its score and techniques after the game.

---

## Phase 3: Help when stuck, without spoiling

### 11. Killer techniques in advanced hints (L)
**Why:** Advanced hints only know classic techniques, so on a killer they either miss
the next step or suggest something far harder than the cage logic that applies.
**What:** Reuse `KillerLogicalRater`'s step search to find the next killer step
(cage combination, innie/outie, cage unit lock, multi-unit innie/outie) and explain it
in the existing hint panel, highlighting the cages and cells involved.
**Done when:** On a puzzle whose next step is an innie, the hint names it and highlights
the right cells. Hints still count as assistance (item 5).

### 12. "Check my board" without revealing (S)
**Why:** A gentler aid than a hint: tells you whether you have gone wrong, not where.
**What:** A menu action that says "No mistakes so far" or "There is at least one wrong
digit". Counts as assistance.

---

## Phase 4: Make it your app

### 13. Rename and tidy branding (S)
**Why:** The app is still called "LibreSudoku" (`app_name`), version 2.0.2, and the About
screen shows the upstream author's donation addresses.
**What:** Rename to "Killer Sudoku", set your own version name, and keep a clear credit
and link to upstream LibreSudoku in About (required in spirit by GPL-3.0, and fair).
Replace the donation section with the credit. Update the Play listing text to match.

### 14. Killer first home screen (S)
**Why:** The home screen is built around classic sudoku types with killer as an option.
**What:** Default to 9x9 Killer, put the level picker with win rates up front, and move
classic and 12x12 types under "More types" (or hide them with a setting).

### 15. Daily killer (M)
**Why:** A fixed daily puzzle gives a reason to open the app and a clean streak metric.
**What:** Generate one Hard or Challenge puzzle per day from a date-based seed (offline,
deterministic), with its own streak and history on the statistics screen.

### 16. Learn: killer techniques (M)
**Why:** The Learn section only covers classic basics. Harder puzzles need the rule of
45, innies/outies and cage combinations.
**What:** Add three short tutorials in the existing tutorial format: cage combinations,
the rule of 45 and innies/outies, and cages that lock a digit in a unit.

---

## Phase 5: Upkeep

### 17. Killer 12x12 with zero givens, or drop it (M)
12x12 killer still uses the old format with given digits, because the new generator was
too slow. With the stock from item 7 it could run in the background. If 12x12 is not
something you play, hide it instead (item 14) and skip this.

### 18. Pull in upstream fixes regularly (S, recurring)
Upstream LibreSudoku is still active. Every month or two, merge `kaajjo/LibreSudoku`
main into a branch, resolve conflicts, and merge it if CI passes. Note the upstream
commit in the PR.

### 19. UI tests for the game screen (M)
Unit tests run in PR CI, but nothing tests the game screen. Add Robolectric Compose
tests for the killer flows above (notes, combination panel, strict mode), so the
auto-deploy on every merge to main does not ship a broken game screen.

### 20. Board export with cages (S)
"Export board" copies only digits, which loses the cages of a killer. Include cages in
the exported string (the parser already writes them for the database) so a puzzle can be
shared or reimported.
