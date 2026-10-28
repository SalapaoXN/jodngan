# ADR 0001: Start with a Modular Monolith

## Status
Accepted for initial development.

## Context
JodNgan is a single-developer portfolio project with evolving requirements. The main goal is to demonstrate sound software engineering, not distributed-system complexity.

## Decision
Implement the backend as a modular monolith. Keep domain boundaries explicit inside one deployable Spring Boot application.

## Consequences
### Positive
- Simpler development, testing, deployment, and debugging.
- Easier cross-module refactoring while the product is still changing.
- Clear path to extracting a service later if a real scaling or ownership need appears.

### Trade-offs
- Module boundaries are enforced by code structure and tests rather than network isolation.
- A future extraction may require additional work if a module becomes independently deployable.

## Revisit When
A module has a concrete need for independent scaling, deployment cadence, failure isolation, or ownership.
