# Product Principle: Notes-Speed Capture

## Observation
Some students already use a phone note to record simple homework information such as the task name and due date. The behavior works because capture is immediate and requires almost no setup.

## Product Opportunity
JodNgan should preserve that low-friction behavior instead of replacing it with a long task form.

The product promise is:

> Capture work almost as quickly as Notes; let JodNgan do the planning afterward.

## Core Principle
Creating a task should require only the minimum information needed to be useful.

Initial capture should normally be:
- Task name
- Due date
- Optional course

Information such as estimated effort, importance, or preferred working time should be progressive rather than mandatory at first capture.

## System Responsibility
After capture, JodNgan should calculate or display:
- Days/time remaining until the deadline
- Relative urgency
- Recommended time to start or continue the task
- Potential workload/deadline conflicts
- What the user should work on next

## UX Constraint
If adding a homework item feels meaningfully slower than writing it in a phone note, the capture flow should be reconsidered.

## Engineering Implication
The scheduling engine must tolerate incomplete task metadata. Recommendations should improve as more information becomes available instead of requiring perfect data before a task can exist.
