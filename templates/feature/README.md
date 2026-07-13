# Feature template

Copy this directory into `app/src/main/java/<package>/feature/<feature>/`, replace `__PACKAGE__`, `__FEATURE__`, and `Feature`, then register the ViewModel in DI and the Route in the app navigation graph.

Keep domain/data directories feature-specific. Delete unused template actions and effects instead of creating empty abstractions. The Screen layout is intentionally unconstrained.

Acceptance checklist:

- Screen depends only on state and callbacks.
- Route is the only Compose layer that obtains the ViewModel or performs platform effects.
- Reducer tests cover phase guards, success, failure, and one-time effects.
- ViewModel tests use fake constructor dependencies; no `GlobalContext` or Preview fallback.
- New ad opportunities use an existing declared area key or update canonical policy and its contract test together.
