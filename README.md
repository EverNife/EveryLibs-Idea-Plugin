# everylibs-idea-plugin

An IntelliJ IDEA plugin that adds **reflection-aware autocomplete, navigation and rename** for the
[EveryLibs reflection](../reflection) library — replicating, for EveryLibs' own lookups, what the
IDE already does for the JDK's `Class.getDeclaredField("…")`.

```java
FieldReflection.INSTANCE.getField(Player.class, "na|");      // ⌃Space → "name", "nameTag", …
FCReflectionUtil.methods().getMethod(Player.class, "set|");  // → "setName", "setHealth", …
ClassReflect.of(Player.class).method("get|");                // → "getHealth", …
```

Ctrl-click on a resolved name navigates to the `PsiField`/`PsiMethod`; Rename rewrites the string.

## Why this is its own build (not a module of EveryLibs)

This project is **not** part of the EveryLibs Gradle reactor. The reactor pins every subproject to a
Java 8 API floor (Jabel + `options.release = 8`); an IntelliJ plugin targets the IDE runtime
(JDK 17+) and cannot live under that constraint. It sits inside the repo for convenience but builds
standalone, with its own `settings.gradle.kts` and wrapper.

## How it works

A single `PsiReferenceContributor` registers one broad provider (any string literal that is a method
argument); all the precise matching happens in code, not in the pattern:

| File | Role |
|---|---|
| `RecognizedLookup` | The registry — the one place that knows the API surface: `(declaring class, method) → {target source, name-arg index, member kind}`. Add a row to grow coverage. |
| `EveryLibsReferenceContributor` | Registers the provider on `psi.referenceContributor` (`language="JAVA"`). |
| `EveryLibsMemberReferenceProvider` | Verifies the call resolves to a registered lookup, the literal is at the name index, and the target resolves; then builds the reference. |
| `TargetClassResolver` | Resolves the target `PsiClass` from `SomeClass.class`, a `Class<X>`-typed expression, or a `ClassReflect<C>` receiver's generic — one operation, like IntelliJ's `getReflectiveClass`. |
| `MemberWalk` | The PSI twin of the library's `TypeHierarchy`: superclass chain (declared members, incl. inherited `private`) then interfaces (constants + un-overridden `default` methods). |
| `EveryLibsMemberReference` | `PsiReferenceBase` with `resolve()` (navigation/rename) and `getVariants()` (completion). Soft, so an unknown name is not a hard error. |

Matching on the **declaring class** of the called method (`FieldReflection`/`MethodReflection`/
`ClassReflect`) is what makes the singleton (`FieldReflection.INSTANCE.getField`) and the facade
(`FCReflectionUtil.fields().getField`) behave the same — the qualifier expression is irrelevant.

`getRangeInElement()` is intentionally not overridden and **no `ElementManipulator` is registered**:
the platform's built-in `StringLiteralManipulator` already gives the inside-the-quotes range and
writes Rename back into the literal.

## Build & run

Requires a JDK 17 and network access (the first build downloads the IntelliJ Platform).

```bash
./gradlew runIde         # launch a sandbox IDE with the plugin
./gradlew buildPlugin    # produce the installable zip in build/distributions
./gradlew verifyPlugin   # IntelliJ Plugin Verifier (binary compatibility)
./gradlew test           # LightJavaCodeInsightFixtureTestCase suite
```

Install the zip via *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

## Scope (v1) and how to extend

**v1** covers:
- **Member-name** lookups whose target is resolvable at edit time: `FieldReflection.getField`,
  `MethodReflection.getMethod`/`getTypedMethod` (target `SomeClass.class`), and the fluent
  `ClassReflect.field`/`method`/`typedMethod` (target = the receiver's generic).
- **Class-name** strings (the `Class.forName("…")` analogue): `ClassReflection.getClass`/
  `getFirstClass`/`getUntypedClass`/`isClassLoaded`, `ClassReflect.of(String)`, and the class-name
  argument of the `getField(String className, …)` / `getMethod(String className, …)` overloads.
  Handled by the platform's `JavaClassReferenceProvider`/`Set` (see `ClassNameSupport`), so it gets
  package/class completion, navigation and rename for free.

Out of scope (gracefully ignored — contribute no reference):
- The **member-name** argument of the `getField(String className, String name, …)` overload — the
  class is a runtime string, so its members are not resolved at edit time (the class name itself IS
  completed; only the dependent member name is not).
- **NMS / obfuscated targets.** Where the dev workspace is Mojang-mapped but the runtime is
  Spigot/obfuscated, the IDE name and the runtime name differ; rather than risk suggesting the wrong
  string, the member reference is left **soft** and the developer owns the call. EveryLibs is a
  general-purpose library — Minecraft NMS is a restricted case, not the default.

To extend: add a row to `RecognizedLookup`. A new target shape (e.g. a typed `Class<?>` local) is a
new branch in `TargetClassResolver`. Both are small, single-responsibility classes.

## Roadmap

- **v2** — an "unresolved member" inspection (`LocalInspectionTool`); richer multi-resolve to all
  overloads of a method name.
- **v3 (optional)** — a portable codegen complement (typed constants/accessors) for non-IntelliJ
  IDEs, generated from the same registry.
