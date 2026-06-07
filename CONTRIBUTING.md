# Contributing

## Testing requirement

Every new feature must include tests before it is considered complete.

- **Unit tests** are required for all domain logic (use cases, models, engine algorithms).
- **Integration tests** (Room DAO, repository) are required for data-layer changes.
- **ViewModel tests** are required for UI-layer changes that involve business logic.
- Tests must pass before the feature branch is merged.

Run tests locally:

```bash
./gradlew test
```
