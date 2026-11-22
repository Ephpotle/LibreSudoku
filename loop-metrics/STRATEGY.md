# Strategy

**Status: draft, not approved.** North star and term readings sent to Markus
2026-11-22; data stance decided the same day (on the phone first, online later).

## His words (2026-11-22)

> "and another mainspring to figure out how to gather metrics feedback to allow for
> data gathering as a foundation for improvement later"

## North star (draft 1)

> "Every killer played in the app, starting with mine, leaves trustworthy data about
> how the puzzle went and how it felt, gathered only in ways players agree to, so later
> improvements rest on evidence instead of guesses."

## Terms (proposed readings, not yet confirmed)

| Term | Reading | Source |
|---|---|---|
| Metrics | Numbers recorded per game: puzzle id and rater score, outcome (solved / gave up / abandoned), time, mistakes, pauses, aid use | His "<40% of others solved" filter is a crowd metric of this kind (2026-10-25, 2026-11-22) |
| Feedback | What a player says: one tap after a game (harder/easier than labelled, fair?) or a short note. Never shown during play as help | His "feels different from the old app" (2026-11-22) |
| Data gathering | Starts on his phone, the only install today. Testers and leaderboard players later | Play internal track (2026-10-27) |
| Foundation for improvement later | This loop produces data; the game, design and marketing loops consume it. It does not change the game itself | 2026-11-22 |
| Ways players agree to | Offline, no ads, no tracking as pitched; anything leaving the phone is opt-in and his decision | Coordinator relay 2026-11-22, card 2026-11-22: "On the phone first, online later" |

## Stages (draft)

1. **Inventory and schema.** What the app records today, what each consumer loop
   needs, the gap. A versioned event schema for a finished game.
2. **On-device capture.** The missing fields recorded locally, with tests; an export
   that a script can read; his own games analysed.
3. **Feedback.** A one-tap post-game question that does not nag and does no thinking.
4. **Beyond one phone.** Only after his approval: opt-in, anonymous sharing,
   probably riding on the leaderboard backend of the game loop's T3.
5. **Used.** At least one change in another loop that cites this data as its evidence.

## Current bottleneck (draft)

Nobody has written down what the other loops need from the data, so there is no
target to record toward. The app already stores time, mistakes, give-up and dates per
game (verified: `SavedGame.kt`, `Record.kt`), and the backup screen exports them to a
file (verified: `data/backup/BackupData.kt`). It does not store abandonment, aid use,
pauses, or any player feedback (inferred from the model files; to verify).

## Out of scope

Collecting anything without consent. Ads, ad IDs, third-party analytics SDKs
(Firebase Analytics, Amplitude and the like) unless Markus decides otherwise.
Changing gameplay or puzzles: those belong to the game loop.
