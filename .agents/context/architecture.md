# DATROMTool — architecture & domain notes

Scope: the routed home for architecture/domain detail (the thin `CLAUDE.md` no longer holds
it). Load when: working on module structure, the DAT pipeline, filtering/sorting, archive I/O,
or No-Intro name parsing. Reference doc — read the section you need.

DATROMTool ("_That_ ROM tool") is a platform-independent Java CLI for ROM management and DAT
manipulation, successor to `1g1r-romset-generator`, inspired by SabreTools and Retool. It
parses DAT files (primarily Logiqx XML), extracts metadata from ROM names per the No-Intro
naming convention, detects divergences between parsed and declared info, and reorganizes ROMs
across archive formats. It can also consume external metadata sources (e.g. Retool metadata).

## Module map (Maven multi-module)

- **`domain`** — data models + domain logic:
  - `datafile/logiqx/` — Logiqx XML DAT model, Jackson-serialized: `Datafile`, `Game`, `Rom`,
    `Header`.
  - `detector/` — file header detection: AND/OR/XOR logical tests identifying ROM type from
    binary content, used to validate DAT metadata divergences.
  - `serialization/` — custom Jackson serializers/deserializers (hex encoding,
    comma-separated lists).
- **`core`** — business logic:
  - `GameFilterer` — filters games by region, language, type, version via predicates.
  - `GameParser` — parses ROM names with the No-Intro convention → `ParsedGame`.
  - `GameSorter` — sorts games via a chain of `SubComparator` implementations in `sorting/`.
  - `SerializationHelper` — Jackson (de)serialization to/from JSON/YAML/XML.
  - `command/OneGameOneRom` — main 1G1R execution logic (the CLI delegates here).
  - `config/AppConfig` — runtime config (thread counts, paths to external tools).
  - `io/FileScanner` — scans directories/archives, computes hashes, returns scan results.
  - `io/FileCopier` — copies/reorganizes files between archives and directories.
  - `io/ScanResultMatcher` — matches `FileScanner` output against DAT entries by hash priority.
  - `io/compression/` — compression algorithm abstractions.
  - `data/` — `Filter`, `PostFilter`, `SortingPreference`, `ParsedGame`, `OutputMode`.
  - `display/` — `Displayable`/`Addressable` interfaces for progress and logging output.
- **`cli`** — picocli command-line interface:
  - `DatRomCommand` — entry point (`io.github.datromtool.cli.DatRomCommand`), holds
    subcommands.
  - `command/OneGameOneRomCommand` — CLI adapter; collects options, builds `core` config
    objects, delegates to `core/command/OneGameOneRom`.
  - `option/` — grouped option mixins: `InputOptions`, `OutputOptions`, `FilteringOptions`,
    `SortingOptions`, `PostFilteringOptions`, `PerformanceOptions`, `DiagnosticOptions`.
  - `converter/` — picocli `ITypeConverter` implementations for complex arg types.
  - `argument/` — complex argument types (`DatafileArgument`, `PatternsFileArgument`).
- **`logging`** — Logback config with ANSI color via jansi.

Jar: `cli/target/cli-<version>-jar-with-dependencies.jar`.

## Key design patterns

1. **Jackson 3 serialization** — domain models are Jackson-serialized; see `lang-java.md` for
   the `tools.jackson.*` (core/databind/dataformat) vs `com.fasterxml.jackson.annotation.*`
   (annotations) namespace split and the `lombok.jacksonized.jacksonVersion += 3` config.
2. **picocli framework** — annotation-driven commands with option-group mixins
   (`@ArgGroup`/`@Mixin`) for clean separation of concerns.
3. **Strategy pattern** — `GameFilterer` and `GameSorter` take predicates/comparators; sorting
   uses a pluggable `SubComparator` chain.
4. **No-Intro name parsing** — `GameParser` decodes structured metadata from ROM filenames.
5. **Archive abstraction** — `ArchiveSourceSpec`/`ArchiveDestinationSpec` hierarchies with
   factory classes; RAR (v4 and v5) read natively via junrar, no external executables.
6. **CLI → core delegation** — `OneGameOneRomCommand` (CLI) builds config and delegates to
   `OneGameOneRom` (core), keeping CLI concerns out of business logic.

## Hash match priority

ROM matching in `ScanResultMatcher`, best available in the DAT wins:

    SHA-256  >  SHA-1  >  MD5  >  (file size + CRC)

## Archive & compression support

| Format | Read | Write |
| ------ | ---- | ----- |
| Zip    | ✅   | ✅    |
| RAR    | ✅   | ❌    |
| 7z     | ✅   | ✅    |
| TAR    | ✅   | ✅    |

- RAR (v4 and v5) is read natively via **junrar**; no external executables required.
- Compression algorithms (plain, or combined with TAR, both read and write): **GZip, BZip2,
  LZMA, XZ**.

## No-Intro naming convention

`GameParser` decodes structured metadata from ROM filenames per the No-Intro naming
convention: regions, languages, version/revision, pre-release status, and "game" type (BIOS,
Program, etc.). The `detector/` header tests validate the parsed metadata against DAT-declared
metadata and surface divergences (e.g. a name that implies a Europe release while the DAT's
`<release>` entries say ITA/SPA, or a name carrying language info the DAT omits).

## DAT formats

- **Logiqx XML** — primary input format (`domain/datafile/logiqx/`).
- **DATROMTool JSON** and **DATROMTool YAML** — native formats.
- Convert between all three via `SerializationHelper`.

## Common workflows

**Adding a new CLI option:**

1. Add a field with `@Option` to the relevant mixin in `cli/option/` (or to
   `OneGameOneRomCommand` directly).
2. If it's a complex type, implement an `ITypeConverter` in `cli/converter/`.
3. Map the option value to a `core` data class in `OneGameOneRomCommand` before calling
   `OneGameOneRom`.

**Adding a new filter/sorter:**

1. Implement logic in `core` — filters in `GameFilterer`, sorters as a new `SubComparator` in
   `sorting/`.
2. Register the comparator in its provider if applicable.
3. Expose via the `Filter`/`PostFilter`/`SortingPreference` data classes; wire into
   `OneGameOneRom`.

**Parsing/modifying DAT files:**

1. Domain classes live in `domain/datafile/logiqx/` (`Datafile`, `Game`, `Rom`).
2. Load/save via `SerializationHelper`; model changes are automatically Jackson-serializable.

## Testing

JUnit 5 + junit-pioneer + Mockito; test data in `test-data/`. Base classes
`ArchiveContentsDependantTest` / `TestDirDependantTest` for tests needing filesystem fixtures.
Tests run during `mvn verify`. Configuration validation happens at option-parsing time via
picocli converters, not at runtime.
