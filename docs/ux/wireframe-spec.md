# Low-Fidelity Wireframe Spec

## Goal
Design JodNgan so that adding an assignment feels almost as fast as writing it in a phone note, while the system immediately turns that entry into useful planning guidance.

## Primary Navigation
Keep MVP navigation minimal:
- Today
- Tasks
- Calendar (only if it proves useful in wireframe testing)

Avoid adding separate screens for analytics, settings-heavy workflows, or advanced planning in the MVP.

## Screen 1: Today
Purpose: answer "What should I work on now?" immediately.

Must show:
- Quick Add entry point near the top
- Primary recommended task
- Short explanation for recommendation
- Remaining time until deadline
- At-risk indicator when supported by available data
- Short list of upcoming tasks

Do not prioritize charts or productivity statistics.

## Screen 2: Quick Add
Purpose: capture a task with minimal friction.

Initial fields:
- Task name — required
- Due date/time — required
- Course — optional

Rules:
- Estimated effort is not required here
- Importance is not required here
- Notes, tags, subtasks, and scheduling preferences stay hidden
- One obvious Save action
- After save, return to a useful view and show remaining time immediately

Target interaction: a normal assignment should be addable in roughly 5–10 seconds once the user is familiar with the flow.

## Screen 3: Tasks
Purpose: provide a lightweight overview of unfinished work.

Each task row/card should communicate:
- Task name
- Course when available
- Remaining time / overdue state
- Simple urgency or risk cue
- Completion control

Useful filters may include:
- Active
- Completed
- Course

Avoid complex filtering in the first wireframe.

## Screen 4: Task Detail
Purpose: let the user improve planning accuracy without making initial capture harder.

Editable information:
- Task name
- Due date/time
- Course
- Estimated effort
- Importance
- Notes
- Completion status

Show:
- Current recommendation/risk explanation when applicable
- Remaining time

Optional metadata should improve recommendations but never be required for a valid task.

## Key States to Wireframe
At minimum, design these states:
- First-time / no tasks
- Normal active tasks
- Task due today
- Overdue task
- At-risk task
- Recommendation with incomplete task metadata
- Completed task
- Validation error during Quick Add

## Mobile-First Constraints
- Primary actions should be reachable without dense menus
- Avoid horizontal scrolling
- Important task information should be readable at a glance
- Do not require opening Task Detail just to understand urgency
- Quick Add should work comfortably with one hand where practical

## UX Test Questions
Before high-fidelity design, the wireframe should answer:
1. Can a new assignment be captured with very little friction?
2. Is it immediately obvious what should be worked on next?
3. Can the user understand why a task is recommended?
4. Does optional planning detail stay out of the way until needed?
5. Does the product feel simpler than maintaining the same workload manually in Notes?

## Next Step After Wireframes
Once the low-fidelity flow is stable, define the domain model, database schema, and API contract from the actual screens and user actions rather than guessing them in advance.
