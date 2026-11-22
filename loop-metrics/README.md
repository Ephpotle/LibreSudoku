# loop-metrics/

State for the third Mainspring loop on this killer sudoku fork: **metrics and
feedback**, the data foundation the other loops improve the app from. Set up
2026-11-22 in the thread "Gather gameplay metrics for future improvement".

Sibling loops (coordinate, never duplicate):
- `loop/` (game loop, branch `claude/project-thread-i288ap`, PR #10): hardness scale,
  Extreme, leaderboards, solving style. It is this loop's main customer: its T1 needs
  real solve data, its T3 (leaderboards) is the most likely first online channel.
- `loop-marketing/` (branch `claude/project-thread-0z4sec`): reach players without
  annoying them. More players means more data, and it needs to know what keeps them.

| File | What it holds | Who changes it |
|---|---|---|
| `CHARTER.md` | How every session thinks and behaves (copied from the Mainspring skill, never edited here) | Replaced when the skill changes |
| `DRIVE.md` | Pressure, aim, exits, escalation, rhythm (copied from the skill) | Replaced when the skill changes |
| `STRATEGY.md` | North star, stages, current bottleneck | Markus approves; sessions only propose |
| `RULES.md` | Project imperatives and Markus's decisions | Every time he decides something |
| `decisions.md` | Questions waiting for Markus | Every session |
| `tensions.json` | Unmet goals, measures, what was tried | Every session |
| `journal/` | One entry per session (`<date>-<HHMM>.md`, `<date>-reflect.md`) | Every session |
| `research/` | Notes, probes, measurements, schemas | As needed |

Sessions work from these files, not from memory of the chat.
