# Architecture Overview

## Initial Direction
JodNgan starts as a modular monolith with a Java/Spring Boot backend, PostgreSQL database, and React + TypeScript frontend.

## Why Modular Monolith
- Keeps deployment and local development simple.
- Preserves clear domain/module boundaries without distributed-system overhead.
- Makes refactoring easier while requirements are still changing.
- Provides enough architectural depth for a software engineering portfolio.

## Candidate Backend Modules
- identity
- course
- task
- scheduling
- notification (later)

Each module should expose clear application boundaries and avoid reaching directly into another module's persistence details.

## Frontend Direction
Use a small reusable component system and build against an agreed user flow before polishing visual details.

## Data Direction
PostgreSQL is the system of record. Schema changes should be versioned through migrations once backend implementation begins.

## Architecture Rule
Infrastructure such as Redis, queues, microservices, or additional databases must be justified by a concrete requirement or measured problem before adoption.
