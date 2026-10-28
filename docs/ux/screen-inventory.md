# MVP Screen Inventory

## Status
Frozen for the first MVP implementation.

Primary navigation:
- Today
- Tasks

Not in the first MVP navigation:
- Calendar
- Analytics
- Settings-heavy planning screens

Quick Add and Course Management are lightweight interaction surfaces, not primary routes.

---

# 1. Sign In

## Purpose
Authenticate an existing user and enter their private workspace.

## Entry Points
- App launch when no authenticated session exists
- Sign In link from Register

## Visible Data
- Email
- Password

## User Actions
- Sign in
- Navigate to Register

## Validation
- Email is required and must be valid
- Password is required
- Invalid credentials show a generic authentication error

## States
- Default
- Submitting
- Validation error
- Authentication error

## Navigation
- Success -> Today
- Register -> Register

---

# 2. Register

## Purpose
Create a minimal user account for the MVP.

## Entry Points
- Register link from Sign In

## Visible Data
- Email
- Password
- Password confirmation if required by the selected auth implementation

## User Actions
- Create account
- Return to Sign In

## Validation
- Valid email required
- Password must satisfy the selected security policy
- Confirmation must match when present
- Duplicate email returns a clear non-sensitive error

## States
- Default
- Submitting
- Validation error
- Account creation error

## Navigation
- Success -> Today or Sign In, depending on auth implementation
- Sign In -> Sign In

---

# 3. Today

## Purpose
Answer the core question immediately:

> What should I work on next?

## Entry Points
- Successful sign in
- Primary navigation: Today
- Return after Quick Add

## Visible Data
- Quick Add entry point
- Primary recommended task
- Recommendation reason
- Remaining time
- Risk indicator when supported
- Short list of upcoming active tasks
- Course name when available

## User Actions
- Open Quick Add
- Open recommended task
- Open an upcoming task
- Mark a task complete where the interaction remains clear
- Navigate to Tasks

## States
### First-time / Empty
- No active tasks
- Clear prompt to add the first task

### Normal
- Recommendation available
- Upcoming tasks visible

### Due Today
- Clear due-today status

### Overdue
- Clear overdue status

### At Risk
- Risk indicator plus short reason

### Limited Recommendation
Used when estimated effort is missing.
- Recommendation still works
- UI avoids pretending the risk estimate is precise

### Loading
- Preserve layout structure while data loads

### Error
- Explain that Today data could not be loaded
- Provide retry action

## Navigation
- Quick Add -> Quick Add surface
- Task -> Task Detail
- Tasks -> Tasks

---

# 4. Quick Add

## Purpose
Capture an assignment almost as quickly as writing it in Notes.

## Form Fields
Required:
- Task name
- Due date/time

Optional:
- Course

Not shown in initial capture:
- Estimated effort
- Importance
- Notes
- Tags
- Subtasks
- Scheduling preferences

## Entry Points
- Today
- Tasks

## User Actions
- Enter task name
- Set due date/time
- Select optional course
- Save
- Cancel/close

## Validation
- Task name is required
- Due date/time is required
- Invalid input is shown inline
- Missing optional course never blocks save

## States
- Empty
- Partially completed
- Validation error
- Saving
- Save error
- Success

## Success Behavior
- Close Quick Add
- Newly created task becomes immediately visible
- Remaining time is immediately available
- Recommendation can be recalculated without requiring effort data

## Navigation
- Success -> return to originating useful view, normally Today
- Cancel -> return to origin

## Implementation Note
Treat Quick Add as a transient surface rather than a dedicated primary route. On mobile it may be a full-screen sheet; on larger screens it may become a modal or inline panel without changing the product flow.

---

# 5. Tasks

## Purpose
Provide a lightweight overview of the user's workload.

## Entry Points
- Primary navigation: Tasks
- Today

## Visible Data Per Task
- Task name
- Course when available
- Remaining time or overdue state
- Simple urgency/risk cue
- Completion state/control

## User Actions
- Open Quick Add
- Open Task Detail
- Mark active task complete
- Reopen completed task
- Switch Active / Completed
- Filter by Course when useful

## Filters
MVP:
- Active
- Completed
- Course

No advanced filter builder in MVP.

## States
- Active tasks available
- No active tasks
- Completed list available
- No completed tasks
- Due today
- Overdue
- At risk
- Loading
- Error

## Navigation
- Task -> Task Detail
- Quick Add -> Quick Add surface
- Today -> Today

---

# 6. Task Detail

## Purpose
View a task and progressively add planning information without making initial capture harder.

## Visible Data
- Task name
- Due date/time
- Course
- Remaining time
- Completion status
- Estimated effort when available
- Importance when available
- Notes when available
- Recommendation explanation when applicable
- Risk explanation when applicable

## Editable Data
- Task name
- Due date/time
- Course
- Estimated effort
- Importance
- Notes

## User Actions
- Edit and save task
- Mark complete
- Reopen completed task
- Change/remove course association
- Add/remove optional planning data
- Return to previous screen

## Validation
- Task name cannot become empty
- Due date/time is required
- Optional planning data may be removed
- Invalid estimated effort is rejected

## States
- Active
- Completed
- Due today
- Overdue
- At risk
- Limited risk confidence because effort is unknown
- Saving
- Validation error
- Save error
- Not found / inaccessible

## Navigation
- Back -> Today or Tasks depending on origin

## MVP Decision
Hard deletion is not part of the first MVP. Complete/reopen covers the required task lifecycle and avoids adding destructive behavior before it is needed.

---

# 7. Course Management Surface

## Purpose
Allow courses to be created and managed without adding another primary navigation destination.

## Entry Points
- Course selector in Quick Add
- Course field in Task Detail
- Course filter in Tasks when management access is useful

## Visible Data
- User's courses
- Course name

## User Actions
- Create course
- Rename course
- Remove course
- Select course for a task when opened from a selector

## Rules
- Course is always optional for Task
- Removing a course must not delete its tasks
- Tasks previously linked to a removed course become unassigned; removing a course never deletes its tasks

## States
- No courses
- Course list
- Validation error
- Save error

## Implementation Note
This should be a lightweight modal/sheet or embedded management interaction, not a dedicated primary route in the first MVP.

---

# MVP Navigation Map

```text
Unauthenticated
  Sign In <-> Register
      |
      v
Authenticated
  Today <--------------------> Tasks
    |                           |
    +------ Quick Add ----------+
    |                           |
    +------> Task Detail <------+ 
                 |
                 +--> Course Management (secondary surface)
```

---

# User Story Coverage

| User Story | Screen / Surface |
| --- | --- |
| US-01 Quick Add Assignment | Quick Add, Today, Tasks |
| US-02 See What Is Due Soon | Today, Tasks, Task Detail |
| US-03 Know What to Work on Next | Today, Task Detail |
| US-04 Improve a Task Later | Task Detail |
| US-05 Identify Work at Risk | Today, Tasks, Task Detail |
| US-06 Complete and Reopen Work | Today, Tasks, Task Detail |
| US-07 Organize Tasks by Course | Quick Add, Task Detail, Course Management |
| US-08 Use My Own Workspace Securely | Sign In, Register, all authenticated screens |

---

# Frozen MVP UX Decisions

1. Primary navigation is `Today` and `Tasks` only.
2. Calendar is deferred until after the core MVP proves useful.
3. Quick Add is a transient surface, not a primary route.
4. Course Management is secondary and does not need a dedicated primary page.
5. Task hard deletion is deferred; edit, complete, and reopen are the MVP lifecycle operations.
6. Scheduling and risk logic remains a backend/domain responsibility; React displays results and explanations rather than reproducing the rules.
