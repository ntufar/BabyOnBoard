# Contributing

## Testing requirement

Every new feature must include tests before it is considered complete.

- **Unit tests** are required for all domain logic (use cases, models, engine algorithms).
- **Integration tests** (Room DAO, repository) are required for data-layer changes.
- **ViewModel tests** are required for UI-layer changes that involve business logic.
- Tests must pass before the feature branch is merged.

## Release process

When creating a new release:

1. Update `versionName` and `versionCode` in `app/build.gradle`.
2. Add an entry to `CHANGELOG.md` with the new version, date, and all changes since the last release.
3. Update the version badge in `README.md` (`release-{version}`).
4. Update the version in `web/index.html` (`v{version}`).
5. Commit, tag (`v{version}`), and push. The release workflow will build, create a GitHub release, and upload to Google Play.

Run tests locally:

```bash
./gradlew test
```
