# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Directives

**Always use `/caveman` skill.** Compress all responses for token efficiency while preserving technical substance.

## Project Overview

**DATROMTool** is a Java-based platform-independent tool for ROM management and DAT file manipulation. It parses DAT files (primarily Logiqx XML format), extracts metadata from ROM names following the No-Intro naming convention, detects divergences between parsed and declared information, and supports operations like game filtering and ROM reorganization across multiple archive formats.

## Build Commands

All commands use Maven 3.9+ and Java 25.

### Standard Build
```bash
mvn clean compile test-compile
```

### Run Tests
```bash
# All tests
mvn verify

# Single test class
mvn test -Dtest=ByteSizeTest

# Single test method
mvn test -Dtest=ByteSizeTest#testMethod
```

### Package the CLI
```bash
# Creates jar-with-dependencies (entry point: io.github.datromtool.cli.DatRomCommand)
mvn package -DskipTests
```

The jar is created at `cli/target/cli-<version>-jar-with-dependencies.jar`.

### Run CLI Locally
```bash
java -jar cli/target/cli-*-jar-with-dependencies.jar --help
```

## Architecture

Maven multi-module build:

- **`domain`** — Data models and domain logic
  - `datafile/logiqx/` — Logiqx XML DAT format model (Jackson-serialized); `Datafile`, `Game`, `Rom`, `Header`
  - `detector/` — File header detection (AND/OR/XOR logical tests for identifying ROM type from binary content)
  - `serialization/` — Custom Jackson serializers/deserializers (hex encoding, comma-separated lists)

- **`core`** — Core business logic
  - `GameFilterer` — Filters games by region, language, type, version using predicates
  - `GameParser` — Parses ROM names using No-Intro naming convention -> `ParsedGame`
  - `GameSorter` — Sorts games using a chain of `SubComparator` implementations in `sorting/`
  - `SerializationHelper` — Jackson serialization to JSON/YAML/XML
  - `command/OneGameOneRom` — Main execution logic for the 1G1R command (CLI delegates here)
  - `config/AppConfig` — Runtime app configuration (thread counts, paths to external tools)
  - `io/FileScanner` — Scans directories/archives, computes hashes, returns scan results
  - `io/FileCopier` — Copies/reorganizes files between archives and directories
  - `io/ScanResultMatcher` — Matches `FileScanner` output against DAT entries using hash priority
  - `io/compression/` — Compression algorithm abstractions (GZip, BZip2, LZMA, LZ4, XZ)
  - `data/` — Data classes: `Filter`, `PostFilter`, `SortingPreference`, `ParsedGame`, `OutputMode`
  - `display/` — `Displayable`/`Addressable` interfaces for progress and logging output

- **`cli`** — Command-line interface
  - `DatRomCommand` — Main entry point with subcommands (uses picocli)
  - `command/OneGameOneRomCommand` — CLI adapter; collects options and calls `core/command/OneGameOneRom`
  - `option/` — Grouped CLI option mixins: `InputOptions`, `OutputOptions`, `FilteringOptions`, `SortingOptions`, `PostFilteringOptions`, `PerformanceOptions`, `DiagnosticOptions`
  - `converter/` — picocli `ITypeConverter` implementations for complex arg types
  - `argument/` — Complex argument types (`DatafileArgument`, `PatternsFileArgument`)

- **`logging`** — Logback configuration with ANSI color support via jansi

## Key Design Patterns

1. **Jackson 3 Serialization** — Domain models use Jackson 3 annotations; `lombok.config` sets `lombok.jacksonized.jacksonVersion += 3` for `@Jacksonized` compatibility. Jackson 3 split into two package namespaces: `jackson-annotations` keeps `com.fasterxml.jackson.annotation.*` (backward-compatible by design), while `jackson-core`, `jackson-databind`, and `jackson-dataformat-*` moved to `tools.jackson.*`. Imports of `com.fasterxml.jackson.annotation.*` (e.g. `@JsonInclude`, `@JsonProperty`) are therefore correct for Jackson 3.
2. **picocli Framework** — CLI uses annotation-driven commands with option group mixins (`@ArgGroup` / `@Mixin`) for clean separation of concerns.
3. **Strategy Pattern** — `GameFilterer` and `GameSorter` accept predicates/comparators; sorting uses a pluggable `SubComparator` chain.
4. **No-Intro Name Parsing** — `GameParser` decodes structured metadata from ROM filenames (regions, languages, version, pre-release status, type).
5. **Archive Abstraction** — `ArchiveSourceSpec`/`ArchiveDestinationSpec` hierarchies with factory classes; RAR5 delegates to external process (`ProcessArchiveSourceSpec`).
6. **CLI → Core delegation** — `OneGameOneRomCommand` (CLI) builds config objects and delegates to `OneGameOneRom` (core), keeping CLI concerns out of business logic.

## Testing

- JUnit 5 + junit-pioneer; test data in `test-data/`
- `ArchiveContentsDependantTest` / `TestDirDependantTest` — base classes for tests needing filesystem fixtures
- Tests run during `mvn verify`

## Important Implementation Details

1. **No-Intro Naming Convention** — Drives `GameParser` for region, language, version, type metadata; detector validates DAT metadata divergences.
2. **Hash Priority** — ROM matching: SHA-256 > SHA-1 > MD5 > (size + CRC).
3. **Archive Format Support** — ZIP and 7z native; RAR ≤ v4 via junrar; RAR5 requires external UnRAR or 7-Zip executable.
4. **Lombok** — Used extensively (`@Data`, `@Value`, `@With`, `@Jacksonized`). `lombok.config` at root applies project-wide.
5. **Java 25** — Minimum language level; do not downgrade. Use Java 25 idioms: pattern switch (`switch (x) { case Foo f -> ... }`), pattern variables in `instanceof` (`x instanceof Foo f`), and `Stream.toList()` instead of `Collectors.toList()` for read-only results.

## Dependencies

- **Jackson** 3.1.3 (`tools.jackson` groupId) — Serialization (XML, JSON, YAML)
- **picocli** 4.7.7 — CLI framework
- **jline** + **jansi** — Terminal detection and ANSI color output
- **Guava** 33.6.0-jre — Immutable collections and utilities
- **Apache Commons** (compress, codec, lang3) — Archive and utility operations
- **junrar** 7.6.0 — RAR ≤ v4 reading
- **JUnit 5** + **junit-pioneer** + **Mockito** — Testing
- **SLF4J + Logback** — Logging

## Common Workflows

**Adding a new CLI option:**
1. Add field with `@Option` to the relevant option mixin in `cli/option/` (or to `OneGameOneRomCommand` directly)
2. If complex type, implement `ITypeConverter` in `cli/converter/`
3. Map the option value to a `core` data class in `OneGameOneRomCommand` before calling `OneGameOneRom`

**Adding a new filter/sorter:**
1. Implement logic in `core` — filters in `GameFilterer`, sorters as new `SubComparator` in `sorting/`
2. Register comparator in `SubComparatorProvider` if applicable
3. Expose via `Filter`/`PostFilter`/`SortingPreference` data classes; wire into `OneGameOneRom`

**Parsing/modifying DAT files:**
1. Domain classes in `domain/datafile/logiqx/` (`Datafile`, `Game`, `Rom`)
2. Load/save via `SerializationHelper`; changes are automatically Jackson-serializable

## Release Process

Releases are automated via GitHub Actions on version tags (`v*`):
1. Tag commit (e.g., `v1.0.0` or `v1.0.0-rc1`) and push to origin
2. CI builds and tests on Linux/macOS/Windows with Java 25
3. Release job packages all modules, creates GitHub release with zip artifacts
4. Tags containing `-rc` are automatically marked as pre-release

## Notes

- **Master branch** is the main development branch (not `main`)
- **Java 25** is the minimum (set via `<maven.compiler.release>25</maven.compiler.release>` in root pom.xml)
- **Configuration validation** happens at option parsing time via picocli converters, not at runtime
