# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Directives

**Always use `/caveman` skill.** Compress all responses for token efficiency while preserving technical substance.

## Project Overview

**DATROMTool** is a Java-based platform-independent tool for ROM management and DAT file manipulation. It parses DAT files (primarily Logiqx XML format), extracts metadata from ROM names following the No-Intro naming convention, detects divergences between parsed and declared information, and supports operations like game filtering and ROM reorganization across multiple archive formats.

## Build Commands

All commands use Maven 3.9+ and Java 17+.

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

The project is organized as a Maven multi-module build:

- **`domain`** — Data models and domain logic
  - `datafile/logiqx/` — Logiqx XML DAT format model (Jackson-serialized)
  - `detector/` — File matching detector system (identifies ROM type/region/language from filenames)
  - `serialization/` — Format-specific serialization adapters (JSON, YAML)

- **`core`** — Core business logic
  - `GameFilterer` — Filters games by criteria (region, language, type, version)
  - `GameParser` — Parses ROM names using No-Intro naming convention
  - `GameSorter` — Sorts games by user-defined preferences
  - `SerializationHelper` — Handles Jackson serialization to JSON/YAML/XML
  - `io/` — Archive handling (ZIP, 7z, TAR, RAR) and file copying
  - `data/` — Data classes for options and configuration
  - `sorting/` — Sorting strategies (game/rom/region ordering)

- **`cli`** — Command-line interface
  - Uses **picocli** for argument parsing and command structure
  - `DatRomCommand` — Main entry point with subcommands
  - `OneGameOneRomCommand` — Core command implementation
  - `option/` — CLI option definitions (input, output, diagnostic)
  - `converter/` — Custom picocli argument converters
  - `argument/` — Complex argument types (datafile, patterns file)

- **`logging`** — Logging configuration (Logback with custom formatting)

## Key Design Patterns

1. **Jackson Serialization** — Domain models use Jackson annotations for flexible serialization (XML input, JSON/YAML output)
2. **picocli Framework** — CLI uses picocli's annotation-driven command/option definitions with custom converters for complex types
3. **Strategy Pattern** — `GameFilterer` and `GameSorter` accept predicates and comparators for flexible filtering/sorting
4. **No-Intro Name Parsing** — `GameParser` decodes structured metadata from ROM filenames (regions, languages, version, pre-release status, type)
5. **Archive Abstraction** — Pluggable archive handlers support multiple formats with consistent interface

## Testing Approach

- **Unit tests** use JUnit 5 (Jupiter)
- **Test data** stored in `test-data/` directory (DAT files, config files, ROM samples)
- Key test classes: `ByteSizeTest`, `GameFiltererTest` (more coverage in respective modules)
- Tests run during `mvn verify` phase

## Important Implementation Details

1. **No-Intro Naming Convention** — Drives the parser for extracting region, language, version, and type metadata. The detector uses this to validate and flag divergences in DAT metadata.
2. **Multiple Hash Support** — ROM matching prioritizes SHA-256 > SHA-1 > MD5 > (size + CRC)
3. **Archive Format Support** — ZIP and 7z natively supported; RAR up to v4 is native, RAR5 requires external tool (UnRAR/7-Zip). Cross-platform executables included.
4. **Lombok** — Used extensively for boilerplate reduction (`@Data`, `@AllArgsConstructor`, etc.)
5. **Java 17+ Features** — Uses records and newer APIs; do not downgrade language level.

## Dependencies to Know

- **Jackson** 2.17.1 — Serialization (XML, JSON, YAML)
- **picocli** 4.7.6 — CLI framework
- **Guava** 33.2.0 — Utilities
- **Apache Commons** (compress, codec, lang3) — Archive and utility operations
- **JUnit 5** + **Mockito** — Testing
- **SLF4J + Logback** — Logging

## Common Workflows

**Adding a new CLI option:**
1. Add field to `DatRomCommand` or `OneGameOneRomCommand` with `@Option` annotation
2. If complex type, implement a picocli `ITypeConverter` in `cli/converter/`
3. Document in the description field of the `@Option` annotation

**Adding a new filter/sorter:**
1. Implement filtering logic in `core` module
2. Expose via `GameFilterer` or `GameSorter`
3. Wire into `OneGameOneRomCommand`

**Parsing/modifying DAT files:**
1. Domain classes are in `domain/datafile/logiqx/` (e.g., `Datafile`, `Game`, `Rom`)
2. Use `SerializationHelper` to load/save
3. Changes are automatically Jackson-serializable

## Release Process

Releases are automated via GitHub Actions on version tags (`v*`):
1. Tag commit with version (e.g., `v1.0.0` or `v1.0.0-rc1`)
2. Push tag to origin
3. CI builds, tests on Linux/macOS/Windows with Java 17
4. Release job packages all modules and creates GitHub release with zip artifacts
5. Automatically marks release as pre-release if tag contains `-rc`

## Notes

- **Master branch** is the main development branch (not `main`)
- **Java 17** is the minimum (configured in pom.xml)
- **No IDE-specific files committed** (IntelliJ, VS Code config is local)
- **Configuration validation** happens at option parsing time via picocli converters, not at runtime
