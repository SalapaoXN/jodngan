# Product Requirements

## 1. Product Goal
JodNgan helps students capture assignments almost as quickly as writing them in a phone note, then turns those entries into useful planning guidance.

The core question the product must answer is:

> What should I work on next, and when should I start it?

## 2. Primary User
A student managing assignments, projects, and exams across multiple courses who wants a faster alternative to manually prioritizing a note or todo list.

## 3. Product Principles
- Notes-speed capture: adding work must stay lightweight.
- Progressive detail: optional planning data should improve recommendations without blocking capture.
- Action over analytics: prioritize "what should I do now?" over charts and dashboards.
- Explainable recommendations: users should understand why an item is urgent or recommended.
- Deterministic core: the MVP scheduling logic must work without AI.
- Mobile-first UX: the primary flow should work comfortably on a phone-sized screen.

## 4. MVP Functional Requirements

### FR-01 Quick Task Capture
The user can create a task using only:
- Task name (required)
- Due date/time (required)
- Course (optional)

The initial flow must not require estimated effort, manually selected priority, tags, subtasks, or scheduling preferences.

### FR-02 Remaining Time
For every open task with a due date, the system displays understandable time remaining, for example:
- Due today
- 1 day left
- 4 days left
- Overdue by 2 days

### FR-03 Task Recommendation
The system recommends which open task the user should work on next.

The first version may use limited information when a task is newly captured. The recommendation must remain usable when estimated effort is unknown.

### FR-04 Progressive Task Enrichment
The user can later add planning information such as:
- Estimated effort
- Importance
- Notes
- Preferred working time (later if needed)

Adding this information should improve planning quality but must not be required for the task to exist.

### FR-05 Explain Recommendation
A recommended task must show a short reason, such as:
- Due soon
- Large task with little time remaining
- Overdue
- Higher importance

The UI should not expose internal scoring formulas unless useful for debugging or documentation.

### FR-06 At-Risk Detection
The system identifies tasks that are at risk of missing their deadline when enough information is available.

If the system lacks enough information for a reliable risk estimate, it should communicate uncertainty rather than claim false precision.

### FR-07 Task Lifecycle
The user can:
- View a task
- Edit a task
- Mark a task complete
- Reopen a completed task

Completed tasks are excluded from active recommendations.

### FR-08 Course Organization
The user can create and manage courses and optionally associate a task with a course.

A course must not be required to create a task.

### FR-09 Daily View
The primary view answers, without requiring navigation through multiple screens:
- What should I work on next?
- What is due soon?
- How much time remains?
- What is currently at risk?

### FR-10 Authentication
The production-oriented MVP supports a user account and authentication so each user's courses and tasks remain isolated.

Authentication must not make the normal post-login task-capture flow slower or more complex.

## 5. MVP UX Requirements
- Quick capture exposes no more than three primary fields before save.
- Optional planning fields are hidden from the initial capture flow.
- Creating a task requires one clear save action.
- The newly created task immediately appears with remaining-time information.
- The primary recommendation is visible from the main view.
- Empty, loading, validation, error, overdue, and completed states must be designed explicitly.
- The interface should remain usable on a phone-sized viewport before desktop polish is added.

## 6. Core User Stories and Acceptance Criteria

### US-01 Quick Add Assignment
**As a student, I want to record an assignment quickly so that capturing homework is not more annoying than writing it in Notes.**

Acceptance Criteria:
1. Task name is required.
2. Due date/time is required.
3. Course is optional.
4. Estimated effort and priority are not required.
5. The user can save from the quick-capture flow with one primary save action.
6. After save, the task appears in the active task list immediately.
7. The task displays remaining time immediately after creation.
8. Missing optional planning data does not cause creation to fail.

### US-02 See What Is Due Soon
**As a student, I want to see how long remains before each deadline so that I do not have to calculate dates myself.**

Acceptance Criteria:
1. Every open task with a deadline displays time remaining.
2. Tasks due today are clearly distinguishable from future tasks.
3. Overdue tasks are clearly identified.
4. Remaining time updates as time passes without requiring the user to edit the task.
5. Completed tasks are not shown as overdue.

### US-03 Know What to Work on Next
**As a student, I want JodNgan to recommend my next task so that I do not manually compare every deadline.**

Acceptance Criteria:
1. At least one actionable open task can be surfaced as the current recommendation when tasks exist.
2. The engine works even when estimated effort is unknown.
3. When estimated effort is known, it can affect the recommendation.
4. Completed tasks are never recommended.
5. Overdue tasks are considered by the recommendation engine.
6. The recommendation includes a human-readable reason.
7. The same input state produces the same recommendation in the deterministic MVP.

### US-04 Improve a Task Later
**As a student, I want to add more detail later so that quick capture stays fast but planning can become more accurate.**

Acceptance Criteria:
1. The user can add estimated effort after task creation.
2. The user can add or change importance after task creation.
3. The user can add notes without affecting required task fields.
4. Saving additional planning information updates future recommendations.
5. Removing optional planning information does not invalidate the task.

### US-05 Identify Work at Risk
**As a student, I want to know which work may not be finished in time so that I can react before the deadline.**

Acceptance Criteria:
1. The system can mark a task as at risk when available planning data supports that conclusion.
2. At-risk status is visually distinct from ordinary urgency.
3. The user can see a short explanation for the risk.
4. If required information is missing, the system communicates that the estimate is limited instead of fabricating certainty.
5. Completing the task removes it from active risk warnings.

### US-06 Complete and Reopen Work
**As a student, I want to mark work complete so that my recommendations only contain unfinished work.**

Acceptance Criteria:
1. An open task can be marked complete.
2. Completed tasks disappear from active recommendations.
3. Completion status persists after refresh or sign-in again.
4. A completed task can be reopened.
5. Reopening returns it to recommendation consideration.

### US-07 Organize Tasks by Course
**As a student, I want to associate work with a course so that I can understand where each assignment belongs without making course setup mandatory.**

Acceptance Criteria:
1. A user can create a course.
2. A task can optionally reference one course.
3. A task can exist without a course.
4. Course association can be changed later.
5. Removing a course must not silently delete its tasks.

### US-08 Use My Own Workspace Securely
**As a student, I want my tasks to belong to my account so that another user cannot access or modify them.**

Acceptance Criteria:
1. A user can authenticate using the selected authentication approach.
2. Authenticated users can access only their own task and course data.
3. Unauthorized access to another user's resources is rejected by the backend.
4. Passwords, if stored by the application, are never stored in plaintext.
5. Authentication secrets are not committed to the repository.

## 7. Non-Functional Requirements

### NFR-01 Maintainability
- Keep the backend a modular monolith for the MVP.
- Business rules remain outside controllers.
- Important business rules are covered by automated tests.

### NFR-02 Reliability
- Core recommendation logic is deterministic and independently testable.
- Invalid external input is validated at system boundaries.
- Errors return useful responses without leaking internal secrets.

### NFR-03 Security
- Enforce authorization on user-owned resources.
- Store secrets outside source control.
- Use established authentication and password-hashing mechanisms rather than custom cryptography.

### NFR-04 Accessibility and Responsive UX
- Main flows must be keyboard accessible on desktop.
- Interactive controls need understandable labels and states.
- The main task workflow must remain usable on a phone-sized viewport.

### NFR-05 Observability
- Important backend failures should be logged with enough context to diagnose them without logging secrets.

## 8. Explicit MVP Non-Goals
- Microservices
- Kubernetes
- Real-time collaboration
- Team project management
- Advanced productivity analytics
- AI-controlled scheduling
- Calendar synchronization
- Push notifications
- Complex recurring-task rules

These can be reconsidered only after the core quick-capture and recommendation flow is useful.

## 9. MVP Definition of Done
The MVP is considered functionally complete when a user can sign in, quickly create assignments, see remaining time, receive an explainable recommendation, progressively add effort information, identify supported at-risk work, and complete tasks through a responsive end-to-end flow with automated coverage for core business rules.
