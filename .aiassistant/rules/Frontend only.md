---
apply: always
---

# Frontend-Only Development Rules

## Scope

This project is focused strictly on frontend development using React. Do not create, modify, or suggest backend logic unless explicitly requested.

The backend for this project uses Java Spring Boot. You should be aware of and integrate with existing backend endpoints, request/response structures, and authentication flows when building frontend features.

## Allowed Work

You may:

* Build and modify React components
* Create pages, layouts, and UI flows
* Manage frontend state
* Implement frontend routing
* Build reusable UI components
* Handle API calls from the frontend only
* Integrate with existing Spring Boot REST endpoints
* Consume backend APIs and properly map DTO responses
* Implement frontend authentication handling using existing backend auth flows
* Work with styling systems such as Tailwind, CSS Modules, or styled-components
* Implement frontend validation and user interactions
* Create mock data or temporary frontend-only data structures when needed
* Improve accessibility, responsiveness, and UX

## Do NOT Do

Do not:

* Create backend services
* Create or modify databases
* Write server-side business logic
* Create Spring Boot controllers, repositories, or services unless explicitly requested
* Implement APIs or API servers
* Create backend authentication systems
* Modify backend architecture
* Create backend file structures
* Suggest infrastructure or deployment architecture unrelated to the frontend

## Tech Stack

The frontend stack should use:

* React
* TypeScript preferred
* Modern React patterns and hooks
* Functional components only
* Tailwind CSS preferred for styling
* Component-driven architecture

The backend stack assumptions are:

* Java Spring Boot
* REST APIs
* JSON request/response payloads
* JWT or Bearer token authentication unless specified otherwise

## API Assumptions

Assume backend APIs already exist unless told otherwise.

Frontend code should:

* Integrate with existing Spring Boot endpoints
* Respect existing endpoint routes and DTO structures
* Avoid inventing backend functionality that was not specified
* Use fetch or axios for API communication
* Handle loading, error, and authentication states gracefully

If backend details are missing:

* Ask for the endpoint contract
* Or use clearly labeled mock API responses without inventing backend implementation details

## Coding Style

* Keep components modular and reusable
* Prefer clean and maintainable code
* Avoid overengineering
* Focus on user experience and frontend performance
* Keep logic client-side only
* Separate UI logic from API service logic

## When Unsure

If a task appears backend-related:

* Stop and ask for clarification
* Do not implement backend logic automatically
* Default to frontend-only solutions
* Assume backend changes are handled separately in Spring Boot
