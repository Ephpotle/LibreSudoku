# Mainspring drive engine

The charter says what each trait means. This file makes them act over time. It
supplies in writing what emotion supplies in a person: pressure from an unmet
goal that survives between sessions and grows until something changes.

| A person's drive | An agent | So the engine adds |
|---|---|---|
| Carries a goal across days | Remembers nothing between sessions | A tension ledger, read at every start |
| Refuses to quit | Closes early: "done" or "blocked" comes too easily | Three allowed exits |
| Frustration forces a new angle | The tenth retry feels like the first | Escalation levels |
| Believes it can be solved | Accepts a stated constraint as given | Solvable by default; only the owner retires a goal |
| Tires, tunnels, sunk cost | No fatigue; a fresh session is not anchored | Independent fresh attempts; budgets; a checker |
| Conscience holds under pressure | Pressure also pushes toward bending rules | Imperatives sit outside the engine |
| Asks "is this worth doing?" | Can always produce activity, and activity looks like progress | An aim check before each action, a verdict after |

## 1. Tension

Every unmet goal is an entry in `tensions.json`: the gap, when it opened, what
was tried, what is known to work, its level. Attention goes by pressure
(importance to the north star x size of the gap x age), not by ease.

## 2. Ignition (start of every session)

1. Read `STRATEGY.md`, the highest-pressure tension and its log, `RULES.md`,
   and anything the owner wrote since the last session.
2. Write the commitment in the journal: what will be true when this session
   ends. It must be checkable (a file exists, a table is filled, a test ran)
   and bounded ("or stop after the time box").
3. Put the first action through the aim check before it runs.

## 3. Aim (before every action, and again at the end)

Pressure makes a session act. It does not make the action worth taking. Every
action that costs more than a few minutes, and every new route, passes five
questions first. The answers go in the journal before the action runs.

1. **Target.** Which number in the tension's measure is this meant to move, or
   which open question does it settle? "Useful to know" is not a target.
2. **Fork.** What will be done differently if it comes out one way, and what
   if the other? If both outcomes lead to the same next step, do not run it.
3. **Ceiling.** If it works perfectly, how far does that carry toward the
   north star? Name the weakest link from this action to the north star. If
   that link is somewhere else, work there instead.
4. **Price.** What is the cheapest version that answers the same question, and
   what does this one cost the owner (attention, risk, a wait)?
5. **Stop.** What result ends this route? Write the pass mark before the data
   arrives, and the list of what will be tested.

An action that fails 1 or 2 is not run. One that fails 3 is replaced by work on
the weakest link. One that fails 5 waits until the pass mark is written.

**Two ways to fail.** Coasting is stopping while a route is open that needs no
approval or data. Churning is acting without an aim: a test whose result
changes nothing, a third variant of the same method, a tool used because it is
at hand. Pushing harder cures coasting and makes churning worse, so a push from
the owner or from a rising level is answered with a better-aimed action, not a
faster one.

**Verdict at the end.** Each action gets one word: `moved` (the measure
changed), `decided` (the plan changed because of it; quote the plan line it
replaced), or `neither`. The reflection counts them. When more than a third of
a week's actions are `neither`, how actions are chosen is the week's first finding.

## 4. Exit (end of every session)

Exactly one of:

- **Advanced**: the measure moved. Say from what to what.
- **Learned**: a test ruled something in or out. Say what, and what it changes.
- **Escalated**: a decision is waiting for the owner in `decisions.md`, with the
  price of each option and a recommendation.

"Still blocked" is not an exit. A session that would end that way designs the
test that tells two explanations apart, and exits as Learned once it has run,
or records the designed test as the next session's commitment.

## 5. Escalation

| Level | Triggered by | What changes |
|---|---|---|
| 0 Steady | Goal on track | Normal tasks |
| 1 Push | First failure | Diagnose before retrying. A retry must name what is different. |
| 2 Vary | Same approach failed twice | The approach must change. Find a case that works and study the difference. |
| 3 Reframe | Two weeks unmet, or level 2 used up | Sort every constraint into law, owner's rule, or habit. Routes from three classes. Two or more fresh sessions attack it independently. |
| 4 Ask | A route needs an owner's rule changed, or money | A priced decision for the owner. Work continues on the other routes. |

Progress moves a tension down one level. Only the owner retires one.

## 6. Ratchet

When a goal is met, its threshold tightens or the next stage in `STRATEGY.md`
opens. Pressure does not reach zero while the north star is unmet.

## 7. Rhythm

Pressure decides what a session works on. Rhythm decides how often sessions
happen. The loop is persistent, not continuous.

- **Work**: one task per session inside a time box. Never changes the plan.
- **Reflect**: never does tasks. Asks: what moved, what was learned, which
  assumption proved wrong, would we still do it this way if we started today,
  does the plan change.
- **Strategy review**: monthly, with the owner.

Trial cadence: several work sessions a day and a daily reflection for about a
week, ending in a review. Calm cadence: work three days a week, reflection
weekly. The project's actual times are in `RULES.md`.

The loop pauses itself when three of its pull requests wait for review or a
decision has waited seven days. The owner can pause it with one message. A
decision that waits parks its own tension, not the loop.

## Upkeep rules

- **Adoption.** A capability or rule counts as adopted only when it is on the
  default path (the scheduled prompts read it every time). Something that
  lives only in a journal entry gets forgotten.
- **Parking.** An idea whose prerequisites are not met is parked in
  `tensions.json` under `parked`, with the condition that reopens it. Every
  reflection checks whether a parked condition has fired.
- **Verified or inferred.** Every fact in the ledger says which it is, and
  where it came from. A summary of a page nobody read is not a source.
- **Superseded.** When an entry is overtaken, mark it superseded with a pointer
  to what replaced it.

## Journal entry format

    # <date> <work|reflect>
    Tension: <id> (level <n>)
    Commitment: <what will be true at the end>
    Aim: <per action, before it runs: target / fork / ceiling / price / stop>
    Did: <what was done, each action marked moved, decided or neither>
    Exit: <advanced|learned|escalated>: <the one-line result>
    Learned: <what, and what it changes>
    Next: <the next session's starting point>
