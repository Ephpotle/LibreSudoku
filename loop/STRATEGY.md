# Strategy

**Status: draft, not approved.** Waiting for Markus to confirm the north star and its
terms (asked 2026-11-22 in the Mainspring thread).

## North star (draft)

> "An ad-free killer sudoku app on my phone where I can always start a fair puzzle
> as hard as the ones fewer than 40% of players solve."

Redrafted 2026-11-22 after his correction: "what i meant was that i only ever attempted
a puzzle that had less than 40% success rate by others. this was an attempt to reach a
higher difficulty level." The first draft (his own win rate under 40%) was wrong.

## Terms (proposed readings, not yet confirmed)

| Term | Reading | Source |
|---|---|---|
| Ad-free, my own app | The Ephpotle/LibreSudoku fork on Play internal testing, offline, no ads or tracking | First thread, 2026-10-25 |
| Below 40% success rate | A filter for difficulty, not a target for his own win rate: puzzles as hard as those that fewer than 40% of other players solved in his old app. His own win rate is only a side signal (a level he nearly always wins is too easy) | His correction, 2026-11-22 |
| Fair, "for a challenge" | One solution, reachable by logic; no feature does the thinking | 2026-11-08 rule |
| Always | A puzzle at that level starts at once, offline | Inferred; to confirm |

Open: how to tell, offline, that a puzzle is as hard as the <40% puzzles of his old app.
Best anchor: rate some of those puzzles with our own rater and compare scores.

## Stages (draft)

1. **A difficulty yardstick.** Our rater scores a set of reference puzzles of known
   difficulty (ideally ones from his old app with <40% success), so "as hard as" is a number.
2. **A level hard enough.** At least one level whose puzzles score at or above the
   reference, all logically solvable, and they start instantly.
3. **Confirmed by him.** He plays that level and agrees it feels as hard as the old
   app's <40% puzzles; his win rate and times back it up.
4. **His app.** Name, home screen and upkeep (backlog phase 3 and 4).

## Current bottleneck (draft)

There is no yardstick for "as hard as". Our rater gives each puzzle a score, but no
one has checked that score against puzzles whose difficulty is known from real players.
Until it is, work on harder levels (Extreme, bigger cages) is aimed blind.

## Out of scope

Solving aids of any kind. Online features, accounts or crowd statistics (the app
stays offline). Classic sudoku polish beyond keeping it working.
