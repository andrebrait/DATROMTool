# Java — language context

Scope: writing or changing `*.java`. Load when: any touched Java file.

- **Java 25 minimum** — set via `<maven.compiler.release>25</maven.compiler.release>` in the
  root `pom.xml`. Never downgrade the language level. Use modern idioms where they fit the
  surrounding code:
  - pattern `switch` — `case Foo f -> …` (with `null`/`default` arms as needed);
  - pattern variables in `instanceof` — `if (x instanceof Foo f) …`;
  - `Stream.toList()` for read-only results instead of `Collectors.toList()`;
  - `records`/`sealed` where they genuinely fit (don't retrofit Lombok `@Value` classes into
    records for its own sake);
  - `var` for obvious locals, not for public API or where the type isn't clear at a glance.
- **Lombok** (annotation processor, applied via `maven-compiler-plugin`) is used extensively:
  `@Data`, `@Value`, `@With`, `@Builder`, `@Jacksonized`. The root `lombok.config` applies
  project-wide — including `lombok.jacksonized.jacksonVersion += 3`, which makes `@Jacksonized`
  emit Jackson 3-compatible builders. Do **not** hand-write getters/setters/equals/hashCode/
  toString/builders that Lombok already generates; add the annotation instead.
- **Jackson 3 namespaces** — Jackson 3 split its packages:
  - `tools.jackson.*` — `jackson-core`, `jackson-databind`, `jackson-dataformat-xml`/`-yaml`
    (mappers, `JsonNode`, serializers/deserializers, format modules);
  - `com.fasterxml.jackson.annotation.*` — annotations (`@JsonProperty`, `@JsonInclude`,
    `@JsonCreator`, …), kept backward-compatible by design.
  These annotation imports are **correct** for Jackson 3 — do NOT "fix" them to `tools.jackson`.
  Custom serdes live in `domain/serialization/`.
- **picocli** — CLI is annotation-driven: `@Command`, `@Option`, `@Parameters`, option mixins
  via `@Mixin`/`@ArgGroup`, and `ITypeConverter` for complex arg types. Follow the existing
  mixin split under `cli/option/`; don't inline option parsing into business logic.
- **No `System.exit()` deep in library code.** `core`/`domain` return or throw; picocli and
  `DatRomCommand` own process exit codes.
- **Collections** — prefer Guava immutable collections (`ImmutableList`/`ImmutableSet`/
  `ImmutableMap`) consistent with the codebase for returned/stored data.
- **No arrays as fields of a value type.** A `record`'s generated `equals`/`hashCode`/
  `toString` compare an array component **by identity**, so two otherwise-identical values
  are unequal, hash differently, and print `[B@1f2e3d`. Store the data as an immutable
  collection instead — `ImmutableList<Byte>` for a byte payload (`Rom.header`) — so the
  generated methods are correct by construction. Do **not** hand-write
  `equals`/`hashCode`/`toString` around an array field: that is the same maintenance burden
  Lombok and records exist to remove, and it silently rots as components are added. Lombok
  `@Value` classes do generate `Arrays.equals`/`Arrays.hashCode`, so an array field there is
  not broken, but new value types still use the collection. Arrays remain the right choice
  for **buffers and parameters** on the scanning/detector hot path (`FileScanner`'s read
  buffer, `Rule.apply(byte[], …)`) — those are not value components, and boxing them per
  byte would be paid on every file scanned.
- **Style** — match the indentation and conventions of the surrounding file. No linter or
  formatter is configured yet (Checkstyle/Spotless are a future addition), so there is no
  auto-fix to lean on — `mvn verify` is the gate.
