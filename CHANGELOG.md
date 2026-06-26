<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# everylibs-idea-plugin Changelog

## [Unreleased]

### Added

- Reflection-aware autocomplete, navigation and rename for the EveryLibs reflection library —
  the analogue of the IDE's built-in support for `Class.getDeclaredField("…")`.
- Member-name completion inside recognized lookups against a target given as a `SomeClass.class`
  token, a `Class<X>`-typed expression, a `ClassReflect<C>` receiver's generic, or a class-name
  string. Inherited `private` members are offered; `java.lang.Object` members are filtered.
- Class-name string completion/navigation/rename (the `Class.forName("…")` analogue) for the
  lookups that take a fully-qualified class name as a `String`, delegated to the platform's
  `JavaClassReferenceProvider`.

### Changed

- Track the renamed fluent `ClassReflect` accessors (`getField`/`getMethod`/`getTypedMethod`),
  which were `get`-prefixed in the reflection library.
- Restructure the project onto the official
  [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template):
  repository configuration moved to `settings.gradle.kts`, metadata-driven `gradle.properties`,
  Gradle Changelog Plugin integration, predefined Run/Debug configurations, and a CI workflow.

[Unreleased]: https://github.com/EverNife/everylibs-idea-plugin/commits/main
