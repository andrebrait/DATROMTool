# CLRMAMEPro dialect: full field inventory and what it expresses that Logiqx cannot

Research ticket [#52](https://github.com/andrebrait/DATROMTool/issues/52), part of the
wayfinder map [#50](https://github.com/andrebrait/DATROMTool/issues/50)
(DATROMTool internal data format).

## Scope and trust ladder

The map fixed the adapter scope at Logiqx **plus CLRMAMEPro**. This document inventories
what a CLRMAMEPro DAT actually carries, so the internal model can hold the union of both
dialects and round-trip either one without silent loss.

Sources, in descending order of trust for the *format definition*:

| Rank | Source | What it is | Revision |
| --- | --- | --- | --- |
| 1 | <https://mamedev.emulab.it/clrmamepro/docs/htm/datfile.htm> | clrmamepro's own "DatFiles" documentation page — the only first-party statement of the format | Retrieved 2026-07-27 |
| 2 | `SabreTools/SabreTools.IO` → `SabreTools.Text.ClrMamePro/Reader.cs`, `Writer.cs` | The tokenizer/serializer that defines the lexical layer (quoting, comments, row kinds) in the most widely used third-party implementation | `c4b41e1` (2026-07-12) |
| 3 | `SabreTools/SabreTools.Serialization` → `SabreTools.Data.Models/ClrMamePro/*.cs`, `SabreTools.Serialization.Readers/ClrMamePro.cs`, `SabreTools.Serialization.Writers/ClrMamePro.cs` | The most complete third-party *model* of the format, including documented vendor extensions | `bcc0a5b` (2026-07-23) |
| 4 | `~/git/retool` (retool v2.4.9, `59f169f`) `modules/dat/parse_dat.py` | An executable specification of what **one consumer** reads. A strict lower bound. | v2.4.9 |
| 5 | `~/git/retool/tests/source/features/Retool - Missing *(CMP).dat` | Real-world syntax samples | v2.4.9 |

> **Evidence-class warning.** The ticket asks this be flagged explicitly, and it matters.
> There is no normative grammar, no DTD, and no XSD for CLRMAMEPro. The first-party page
> (rank 1) documents roughly twenty keys and then says clrmamepro parses "-listinfo syntax",
> which is open-ended by construction — `engine.cfg` even lets a user **rename keywords at
> parse time** via `replace x y` and **suppress them** via `ignore x`. Everything beyond
> rank 1 is therefore a *consumer's* view:
>
> - **Ranks 2–3 (SabreTools)** are the broadest consumer view available, but still a
>   consumer. Fields SabreTools models as "MAME extension", "Aaru extension", "No-Intro
>   extension", "RomVault extension" and "DiscImageCreator extension" are *observed in the
>   wild*, not blessed by clrmamepro.
> - **Rank 4 (retool)** is a deliberately narrow consumer. It reads **only** header
>   `name`/`description`/`version`/`author`/`category`, and per-set `name`/`description`/
>   `rom`/`disk` (`parse_dat.py:263-280`, `:418-491`). Everything else in the file is
>   dropped on the floor. Retool never writes CLRMAMEPro back out — a CMP input is
>   converted to Logiqx XML on output (see `tests/goldens/features/missing-header-data-cmp/`).
>   Retool's coverage is evidence of what *must* be supported, never of what *may* be
>   omitted.
>
> **Consequence for the model:** a field no consumer reads still has to survive a lossless
> round trip. Because the key space is open (`replace`/`ignore` in `engine.cfg`, plus every
> vendor extension listed below arriving without announcement), the CLRMAMEPro adapter
> **cannot** be built on a closed enum of known keys. It needs the map's "escape hatch for
> unknown attributes and unknown elements" (premise 4 of #50) as a load-bearing part of the
> design, not an afterthought.

## 1. Lexical structure

CLRMAMEPro DATs are a plain-text, line-oriented, parenthesis-nested key/value format. UTF-8
in practice; the first-party docs do not specify an encoding, SabreTools reads and writes
UTF-8 (`SabreTools.Serialization.Readers/ClrMamePro.cs:83`,
`SabreTools.Serialization.Writers/ClrMamePro.cs:73`).

The first-party statement of the syntax rules, verbatim:

> The order and the case doesn't matter ! The space between a tagname and its attribute or
> a '(' or ')' is important. The description name has to be in quotation marks. If you're
> using quotation marks within a name, use '\\"'.
>
> — <https://mamedev.emulab.it/clrmamepro/docs/htm/datfile.htm>

And the canonical example, verbatim from the same page:

```
set (
    name pacman
    cloneof pacman
    description "PuckMan (Japan set 1)"
    rom ( name namcopac.6e size 4096 crc fee263b3 md5 3f84d78d59147b9b3c816da72110e55f)
    sample shot.wav
    sampleof galaxian
)
```

### 1.1 Row kinds

SabreTools' tokenizer recognises exactly five row kinds
(`SabreTools.Text.ClrMamePro/Enums.cs`), which is a good description of the grammar:

| Row kind | Regex (`SabreTools.Text.ClrMamePro/Reader.cs:13-18`) | Example |
| --- | --- | --- |
| `TopLevel` | `(^.*?) \($` | `clrmamepro (` / `game (` |
| `EndTopLevel` | `^\s*\)\s*$` | `)` |
| `Standalone` (key + value on its own line) | `^\s*(\S*?) (.*)` | `name "Some Game"` |
| `Internal` (a nested single-line item with attributes) | `(^\S*?) (\(.+\))$` | `rom ( name a.bin size 1024 crc deadbeef )` |
| `Comment` | line starts with `#` | `# generated by ...` |

The tokenizer lowercases both top-level names and internal item names
(`Reader.cs:164`, `:177`), and the CLRMAMEPro reader lowercases every key before dispatch
(`SabreTools.Serialization.Readers/ClrMamePro.cs:210`), matching the first-party
"the case doesn't matter" rule. **Element/key names are case-insensitive; values are not.**

Attribute splitting inside an internal item uses `[^\s""]+|""[^""]*""`
(`Reader.cs:15`) after stripping the outer parentheses (`Reader.cs:300-312`) — i.e.
whitespace-separated tokens, except that a double-quoted run is one token.

### 1.2 Quoting and escaping — and the round-trip hazard

This is the single most important finding for a lossless model.

| Question | Answer | Source |
| --- | --- | --- |
| Which values must be quoted? | Only `description` is stated as mandatory-quoted. In practice writers quote every string value. | mamedev.emulab.it (rank 1); `Writer.cs:212-224` quotes all attribute values when `Quotes = true` (the default). |
| How is an embedded `"` written? | First-party: escaped as `\"`. | mamedev.emulab.it (rank 1) |
| Does the reference third-party implementation honour that escape? | **No.** SabreTools' reader has no unescaping step at all: it strips quote characters with `linegc[i].Replace("\"", string.Empty)` (`Reader.cs:183`, `:214`, `:250`) and `gc[2].Value.Replace("\"", string.Empty)` (`Reader.cs:266`). A value containing `\"` therefore reads back with the backslash retained and the quote gone. | `SabreTools.Text.ClrMamePro/Reader.cs` |
| Does the reference writer emit that escape? | **No.** It *destroys* the character: `name.Replace("\"", "''")` / `value?.Replace("\"", "''")` — a literal `"` inside a value is rewritten to two apostrophes (`Writer.cs:177`, `:218`, `:314-315`, `:386`). | `SabreTools.Text.ClrMamePro/Writer.cs` |
| How does retool unquote? | `re.sub('^"(.*)"$', '\\1', string)` — strips one leading and one trailing quote, no escape handling whatsoever. | `parse_dat.py:757-767` |
| How is a newline inside a value represented? | **It cannot be.** The format is line-oriented; every reader examined reads line by line (`Reader.cs:130`; `parse_dat.py:392`) and there is no continuation or escape syntax. A value containing `\n` is not expressible. | `Reader.cs:130`, `parse_dat.py:392` |

**Model implications.**

1. `"` inside a value is *theoretically* expressible (`\"`) but is corrupted by the two
   most-used implementations in opposite ways. DATROMTool should emit the first-party `\"`
   escape and accept both `\"` and a bare `"` on input, and the design doc should record
   that a CMP→CMP round trip through third-party tools is not quote-safe.
2. `\n`, `\r` inside a value are **not representable at all** in CLRMAMEPro. This is a
   hard, structural loss on the Logiqx→CLRMAMEPro direction: XML text nodes carry newlines
   fine. The "semantically lossless" fidelity contract (#50 premise 4) can only hold
   *within* a dialect, not across the Logiqx→CMP conversion. The adapter must either reject
   or explicitly report such values.
3. A trailing `)` that terminates a `game (` block is only recognised when it is alone on
   its line (`^\s*\)\s*$`), so a writer must never put content on the closing line.

### 1.3 The unquoted-name minefield

If a producer omits quotes around a multi-word `name` inside a `rom ( ... )` item, the token
splitter cannot tell where the name ends. Both major consumers have ad-hoc heuristics for
this:

- SabreTools: with `Quotes = false`, `name` consumes tokens until it hits one of the literal
  words `merge`, `size`, `crc`, `md5`, `sha1` (`Reader.cs:218-231`). The source comments the
  hazard itself: *"This can backfire in a lot of circumstances, so don't disable this unless
  you know what you're doing"* (`Reader.cs:55-62`).
- retool: tries the quoted pattern `name\s".*?"\s` first and falls back to `name\s.*?\s`,
  which silently truncates an unquoted multi-word filename at the first space
  (`parse_dat.py:440-451`).

DATROMTool should always **write** quoted values, and on read should prefer the quoted
interpretation, treating the unquoted-multi-word case as a recoverable parse warning.

### 1.4 Bare-word (valueless) flags

Two positions accept a bare token with no key:

- Dump status inside a `rom`/`disk` item: the bare words `baddump`, `good`, `nodump`,
  `verified` are promoted to `status <word>` (`Reader.cs:236-240`). The first-party docs
  confirm `baddump` and `nodump` as `rom` attributes.
- `sample`: `sample shot.wav` — the token is the name, promoted to `name <token>`
  (`Reader.cs:242-246`).

A model that stores `status` as a plain enum cannot tell `nodump` from `status nodump` on
output. If byte-shape matters for a re-emit, this is a formatting detail worth flagging as
explicitly *not* promised (consistent with #50 premise 4, which does not promise formatting).

## 2. Header block: full key inventory

The header is the first top-level block. Its name is `clrmamepro` in the mainline dialect
(`Readers/ClrMamePro.cs:179`); see §6 for the `romvault` and `doscenter` variants.

None of the header keys are mandatory in any implementation examined — SabreTools models
every one as nullable (`Data.Models/ClrMamePro/ClrMamePro.cs`), and retool's own
`Retool - Missing header data (CMP).dat` fixture omits `name` entirely and is still parsed.
The *practical* requirement is that line 1 of the file is exactly `clrmamepro (`: retool
uses that as its format sniff and bails otherwise (`parse_dat.py:240-249`), and retool's GUI
sniffs `b'clrmamepro' in first_line` (`modules/gui/gui_utils.py:276`).

| Key | Cardinality | Documented by | Read by retool | Meaning | Logiqx equivalent |
| --- | --- | --- | --- | --- | --- |
| `name` | 0..1 | rank 1 (profiler setting) | yes (`:276`) | Name shown in clrmamepro's profiler | `header/name` |
| `description` | 0..1 | rank 1 (profiler setting) | yes (`:271`) | Description shown in profiler | `header/description` |
| `category` | 0..1 | rank 3 | yes (`:266`) | DAT-wide category | `header/category` |
| `version` | 0..1 | rank 1 (profiler setting) | yes (`:279`) | Version string | `header/version` |
| `date` | 0..1 | rank 3 | no | Build date | `header/date` |
| `author` | 0..1 | rank 1 (profiler setting) | yes (`:263`) | Author / copyright / email | `header/author` |
| `comment` | 0..1 | rank 1 (profiler setting) | no | Free comment line | `header/comment` |
| `homepage` | 0..1 | rank 3 | no | Homepage URL | `header/homepage` |
| `url` | 0..1 | rank 3 | no | URL | `header/url` |
| `rootdir` | 0..1 | rank 3 | no | Root directory hint | **none** |
| `header` | 0..1 | rank 1 | no | Name of an XML header-skipper definition, e.g. `header nes.xml` | `header/clrmamepro/@header` |
| `type` | 0..1 | rank 3 | no | DAT type marker | **none** |
| `forcemerging` | 0..1 | rank 1 | no | `none` \| `split` \| `full`. Other values disable the feature. SabreTools also accepts `merged`, `nonmerged`/`unmerged`, `fullmerged`, `device`/`deviceunmerged`/`devicenonmerged`, `fullunmerged`/`fullnonmerged` (`Data.Models/Metadata/Enums.cs:373-398`) | `header/clrmamepro/@forcemerging` (`none\|split\|full` only) |
| `forcezipping` | 0..1 | rank 1 | no | `zip` \| `unzip`. **SabreTools models it as a `yes`/`no` boolean** (`Data.Models/ClrMamePro/ClrMamePro.cs:48`), which disagrees with the first-party docs — see §6.3 | `header/clrmamepro/@forcepacking` |
| `forcepacking` | 0..1 | rank 3 | no | `zip` \| `unzip` \| `partial` \| `flat` \| ... (`Enums.cs:436-467`). Sibling spelling of `forcezipping`, seen in DATs that were converted from Logiqx | `header/clrmamepro/@forcepacking` |
| `forcenodump` | 0..1 | rank 1 | no | `obsolete` (default) \| `required` \| `ignore`. **Not in SabreTools' CMP header model** — see §5 | `header/clrmamepro/@forcenodump` |

### 2.1 The `info (` block

A second top-level block, `info (`, carries repeated `source` standalone lines
(`Readers/ClrMamePro.cs:151`, `:185`, `Data.Models/ClrMamePro/Info.cs`). It is a SabreTools-
recognised extension with no first-party documentation and no Logiqx counterpart. Cardinality
0..1 per file, `source` 0..n.

## 3. Set block: full key inventory

A set block opens with one of four interchangeable top-level names — `game`, `set`,
`machine`, `resource` — all deserialising into the same shape
(`Readers/ClrMamePro.cs:118-121`, `:182-194`; `Data.Models/ClrMamePro/{Game,Set,Machine,Resource}.cs`
all derive from `GameBase`). Retool accepts `game` and `set` only (`parse_dat.py:407`).

The *tag name itself is semantic*: `resource (` marks a non-game support set (BIOS/samples
container), which Logiqx has no way to express except via the boolean `game/@isbios`.

### 3.1 Standalone keys directly on the set

| Key | Cardinality | Documented by | Read by retool | Logiqx equivalent |
| --- | --- | --- | --- | --- |
| `name` | **1** (only required field in the format) | rank 1 | yes (`:422`) | `game/@name` |
| `description` | 0..1 | rank 1 | yes (`:424`) | `game/description` (required in Logiqx) |
| `year` | 0..1 | rank 1 | no | `game/year` |
| `manufacturer` | 0..1 | rank 1 | no | `game/manufacturer` |
| `category` | 0..1 | rank 3 | no (only the *header* category, applied to every set — `:418-419`) | `game/category` (0..n in Logiqx) |
| `cloneof` | 0..1 | rank 1 | no | `game/@cloneof` |
| `romof` | 0..1 | rank 3 | no | `game/@romof` |
| `sampleof` | 0..1 | rank 1 | no | `game/@sampleof` |
| `rebuildto` | 0..1 | rank 1 | no | `game/@rebuildto` (present only in the retool-extended DTD, `datafile.dtd:52`) |
| `driverstatus` | 0..1 | rank 3 (MAME extension) | no | **none** |

`name` is the only field SabreTools marks `[Required]` on a set
(`Data.Models/ClrMamePro/GameBase.cs:10-12`), and the CMP writer throws if it is absent
(`Writers/ClrMamePro.cs:177`). Retool additionally refuses to keep a set with no ROM/disk
entries or an empty name (`parse_dat.py:493`) — a consumer rule, not a format rule; the
retool fixture `Retool - Missing title data (CMP).dat` deliberately contains both a
`name ""` set and a set with no `rom` lines.

### 3.2 `rom ( ... )` — 0..n

`name` and `size` are the only two attributes SabreTools marks `[Required]`
(`Data.Models/ClrMamePro/Rom.cs:11-16`); the writer throws without them
(`Writers/ClrMamePro.cs:263-264`). The first-party docs list `name`, `size`, `baddump`,
`nodump`, `crc`, `crc32`, `md5`, `sha1`.

| Attribute | Documented by | Logiqx `rom` equivalent |
| --- | --- | --- |
| `name` | rank 1 (**required**) | `@name` |
| `size` | rank 1 (**required**) | `@size` |
| `crc` / `crc32` | rank 1 (`crc32` is an alias renameable via `engine.cfg replace crc32 crc`) | `@crc` |
| `md5` | rank 1 | `@md5` |
| `sha1` | rank 1 | `@sha1` |
| `baddump`, `nodump` (bare words) | rank 1 | `@status` |
| `status` | rank 3 — `good` \| `baddump` \| `nodump` \| `verified` \| `deduped` (RomVault) (`Enums.cs:243-262`) | `@status` (no `deduped`) |
| `merge` | rank 3 (in `engine.cfg` keyword list, rank 1) | `@merge` |
| `date` | rank 3 (in `engine.cfg` keyword list, rank 1) | `@date` (retool-extended DTD only) |
| `flags` | rank 3 | **none** |
| `crc16`, `crc64`, `md2`, `md4`, `ripemd128`, `ripemd160`, `sha384`, `sha512`, `spamsum`, `blake3` | rank 3 (hash extensions) | **none** |
| `sha256` | rank 3; also No-Intro. Read by retool (`:482-488`) | `@sha256` (retool-extended DTD only) |
| `xxh3_64`, `xxh3_128` | rank 3 (DiscImageCreator extension) | **none** |
| `region` | rank 3 (MAME extension) | **none** at ROM level (Logiqx has `release/@region` at set level) |
| `offs` | rank 3 (MAME extension) | **none** |
| `serial` | rank 3 (No-Intro extension) | `@serial` (retool-extended DTD only) |
| `header` | rank 3 (No-Intro extension) | `@header` (retool-extended DTD only) |
| `inverted` | rank 3 (RomVault extension), yes/no | **none** |
| `mia` | rank 3 (RomVault extension), yes/no | `@mia` (retool-extended DTD only) |

### 3.3 `disk ( ... )` — 0..n

`name` required. Attributes: `md5`, `sha1`, `merge`, `status`, `flags`
(`Data.Models/ClrMamePro/Disk.cs`; `Writers/ClrMamePro.cs:308-314`). Retool reads `disk`
lines through the exact same code path as `rom`, tagging them `type: 'disk'`
(`parse_dat.py:428-435`). `flags` has no Logiqx counterpart; `sha256` is Logiqx-only for
disks (`datafile.dtd:82`).

### 3.4 `sample`, `archive`, `release`, `biosset`

| Item | Cardinality | Shape | Logiqx equivalent |
| --- | --- | --- | --- |
| `sample` | 0..n | Bare token or `sample ( name X )` (`Reader.cs:242-246`) | `game/sample/@name` |
| `archive` | 0..n | `archive ( name X )` | `game/archive/@name` |
| `release` | 0..n | `name` (req), `region` (req), `language`, `date`, `default` (yes/no) — identical to Logiqx (`Data.Models/ClrMamePro/Release.cs`) | `game/release` |
| `biosset` | 0..n | `name` (req), `description` (req), `default` (yes/no) — identical to Logiqx (`Data.Models/ClrMamePro/BiosSet.cs`) | `game/biosset` |

### 3.5 `media ( ... )` — 0..n (Aaru extension, rank 3)

`name` (req), `md5`, `sha1`, `sha256`, `spamsum` (`Data.Models/ClrMamePro/Media.cs`).
No Logiqx equivalent in the vendored DTD.

### 3.6 MAME hardware-description extensions (rank 3 only, no Logiqx equivalent at all)

These come from MAME's `-listinfo` output, which is exactly what the first-party doc says
clrmamepro parses. None of them exist anywhere in the Logiqx DTD/XSD.

| Item | Cardinality | Attributes |
| --- | --- | --- |
| `chip` | 0..n | `type` (`cpu`\|`audio`), `name`, `flags`, `clock` |
| `video` | 0..n | `screen` (`raster`\|`vector`), `orientation` (`vertical`\|`horizontal`), `x`, `y`, `aspectx`, `aspecty`, `freq` |
| `sound` | 0..1 | `channels` |
| `input` | 0..1 | `players`, `control`, `buttons`, `coins`, `tilt` (yes/no), `service` (yes/no) |
| `dipswitch` | 0..n | `name`, `entry` (repeatable), `default` (yes/no) |
| `driver` | 0..1 | `status`, `color`, `sound` (each `good`\|`imperfect`\|`preliminary`), `palettesize`, `blit` (`plain`\|`dirty`) |

Full key list as dispatched by the reader, verbatim from
`SabreTools.Serialization.Readers/ClrMamePro.cs`:

```
game machine resource set info clrmamepro game info machine resource set
name description rootdir category version date author homepage url comment header type
forcemerging forcezipping forcepacking
name description driverstatus year manufacturer category cloneof romof sampleof sample source
release biosset rom disk media sample archive chip video sound input dipswitch driver
name region language date default
name description default
name size crc crc16 crc64 md2 md4 md5 ripemd128 ripemd160 sha1 sha256 sha384 sha512
spamsum blake3 xxh3_64 xxh3_128 merge status region flags offs serial header date inverted mia
name md5 sha1 merge status flags
name md5 sha1 sha256 spamsum
name
name type
name flags clock
screen orientation x y aspectx aspecty freq
channels
players control buttons coins tilt service
name entry default
status color sound palettesize blit
```

## 4. How CLRMAMEPro expresses what Logiqx expresses structurally

| Concept | Logiqx | CLRMAMEPro | Notes |
| --- | --- | --- | --- |
| Parent/clone | `game/@cloneof` (attribute) | `cloneof <name>` standalone line | 1:1. Both are name references, not IDs. |
| ROM-set parent (merge source) | `game/@romof` | `romof <name>` | 1:1 |
| BIOS set | `game/@isbios="yes"` **plus** `game/biosset` children | `resource (` top-level tag name; `biosset ( name .. description .. default .. )` | **Not 1:1.** CMP encodes "this is a support set" in the *tag name*; Logiqx encodes it in a boolean attribute. `resource` also covers non-BIOS support sets, so the mapping is lossy in the CMP→Logiqx direction. |
| Disks (CHD) | `game/disk` element | `disk ( ... )` internal item | 1:1 except `sha256` (Logiqx-only) and `flags` (CMP-only). |
| Samples | `game/sample/@name` | `sample <name>` bare, or `sample ( name X )`; set-level `sampleof <name>` | 1:1 |
| Category | `game/category` — **repeatable (0..n)** per game, plus optional `header/category` | `category <value>` — **single (0..1)** per set, plus header `category` | **Asymmetric cardinality.** Logiqx can carry multiple categories on one game; CMP cannot. A game with 2+ categories cannot be written to CMP without loss. Retool sidesteps this by projecting the *header* category onto every title (`parse_dat.py:418-419`). |
| Region / language | `game/release/@region` (required) and `@language` | Identical `release ( name .. region .. language .. date .. default .. )` item | 1:1. Note: neither dialect declares region/language on the game itself; both hang them off `release`. Everything else DATROMTool knows about region/language is *derived from the title string* — which is exactly why #50 makes provenance first-class. |
| Merge/packing policy | `header/clrmamepro/@forcemerging`, `@forcenodump`, `@forcepacking` | header `forcemerging`, `forcenodump`, `forcezipping`/`forcepacking` | Same concepts; CMP's value domains are wider (see §5). |
| Header-skipper reference | `header/clrmamepro/@header` | header `header nes.xml` | 1:1 |
| Rebuild destination | `game/@rebuildto` (retool-extended DTD only, not original Logiqx) | `rebuildto <path>` | 1:1 against the vendored DTD. |

## 5. Two-directional gap table

### 5.1 CLRMAMEPro-only — no Logiqx equivalent

Compared against the vendored Logiqx DTD/XSD at
`core/src/test/resources/xsd/datafile/logiqx/datafile.dtd` (rev 2.2, the retool-extended
variant, which is already a superset of the original Logiqx DTD).

| CMP construct | Level | Source rank | Why Logiqx cannot express it |
| --- | --- | --- | --- |
| `resource (` / `machine (` as a set kind | set | 3 (retool: `set`/`game` only) | Logiqx has one element name `game`; set kind collapses to `@isbios` yes/no |
| `rootdir` | header | 3 | No element |
| `type` | header | 3 | No element |
| `info ( source ... )` block | file | 3 | No element |
| `driverstatus` | set | 3 (MAME) | No element |
| `chip`, `video`, `sound`, `input`, `dipswitch`, `driver` (and all their attributes) | set | 3 (MAME) | Entire hardware-description subtree absent from Logiqx |
| `media ( name md5 sha1 sha256 spamsum )` | set | 3 (Aaru) | No element |
| `rom/@flags`, `disk/@flags` | item | 3 | No attribute |
| `rom/@region` | item | 3 (MAME) | Logiqx has region only at `release` level, not per ROM |
| `rom/@offs` | item | 3 (MAME) | No attribute |
| `rom/@inverted` | item | 3 (RomVault) | No attribute |
| `rom/@crc16`, `@crc64`, `@md2`, `@md4`, `@ripemd128`, `@ripemd160`, `@sha384`, `@sha512`, `@spamsum`, `@blake3`, `@xxh3_64`, `@xxh3_128` | item | 3 | Logiqx defines only `crc`, `md5`, `sha1`, `sha256` |
| `status="deduped"` | item | 3 (RomVault) | Logiqx enum is `baddump\|nodump\|good\|verified` |
| `forcemerging` values `merged`, `nonmerged`/`unmerged`, `fullmerged`, `device`/`deviceunmerged`/`devicenonmerged`, `fullunmerged`/`fullnonmerged` | header | 3 | Logiqx enum is `none\|split\|full` |
| `forcepacking` values `partial`, `flat` (and further values) | header | 3 | Logiqx enum is `zip\|unzip` |
| `# ...` comment lines | file | 2 | XML comments exist, but they are a different construct with different placement rules |
| Arbitrary keys introduced by `engine.cfg replace x y` | any | 1 | Open key space by design; nothing to map to |

### 5.2 Logiqx-only — no CLRMAMEPro equivalent

| Logiqx construct | Level | DTD line | Why CMP cannot express it |
| --- | --- | --- | --- |
| `datafile/@build` | file | `datafile.dtd:18` | No file-level attribute concept |
| `datafile/@debug` (`yes`\|`no`) | file | `:19` | Same |
| `header/email` | header | `:27` | No key; would have to be folded into `author` |
| `header/romcenter/@plugin`, `@rommode`, `@biosmode`, `@samplemode`, `@lockrommode`, `@lockbiosmode`, `@locksamplemode` | header | `:36-43` | Entire RomCenter policy sub-element absent. clrmamepro *reads* RomCenter DATs but its own DAT format has no equivalent keys. |
| `header/clrmamepro/@forcenodump` | header | `:34` | Documented as a CMP header key by rank 1, but **not modelled by SabreTools** (`Data.Models/ClrMamePro/ClrMamePro.cs` has `ForceMerging`/`ForceZipping`/`ForcePacking`, no `ForceNodump`) — so in practice a `forcenodump` value survives a Logiqx→CMP→Logiqx trip through SabreTools only if the adapter preserves it explicitly. Not a format gap; an implementation gap worth guarding with a test. |
| `game/comment` (0..n) | set | `:44` | No per-set comment key; `#` comments are file-level lexical trivia, not attached to a set |
| `game/category` **repeated** | set | `:44` | CMP `category` is single-valued |
| `game/game_id` (0..n) | set | `:44,65` | No key. This is a retool DTD extension carrying No-Intro IDs. |
| `game/@sourcefile` | set | `:46` | No key at set level (`sourcefile` appears in `engine.cfg`'s renameable-keyword list, so a producer *could* emit it, but no consumer models it) |
| `game/@isbios` as an explicit boolean | set | `:47` | Expressible only by choosing the `resource (` tag, which is lossy in both directions |
| `game/@board` | set | `:51` | No key |
| `disk/@sha256` | item | `:82` | CMP `disk` models only `md5`/`sha1` |
| Newlines and control characters inside any value | any | XML text nodes | CMP is line-oriented with no continuation syntax — structurally unrepresentable (§1.2) |
| XML namespaces, DTD/XSD validation, `<?xml?>` declaration, DOCTYPE | file | — | No schema layer of any kind exists for CMP |
| No-Intro `header/id` | header | `schema_nointro_datfile_v3.xsd:9` | No key. Present in real No-Intro DATs, so it matters for the corpus even though it is absent from the Logiqx DTD. |
| Retool's `<retool>` header element (seen in retool output, e.g. `tests/goldens/features/missing-header-data-cmp/Unknown (-x).dat:12`) | header | — | No key |

### 5.3 The asymmetry that matters most

The union is **not** "Logiqx plus a few CMP extras". The two gaps are different in kind:

- **CMP-only fields are mostly *additive data*** — more hash algorithms, more hardware
  description, more status values. A model that keeps them as typed optional fields plus an
  unknown-key escape hatch holds them fine.
- **Logiqx-only gaps are mostly *structural capabilities*** — repeatable categories,
  per-set comments, per-file attributes, and above all the ability to hold a value
  containing a newline. These cannot be solved by adding fields to the CMP adapter; they are
  limits of the target format. The internal model must be able to hold them and the
  CLRMAMEPro *writer* must report, not silently drop, values it cannot express.

This is a direct constraint on #50 premise 4 ("semantically lossless"): the contract holds
for `dialect → model → same dialect`. For `Logiqx → model → CLRMAMEPro` it cannot hold in
general, and the spec should say so explicitly rather than leave it implied.

## 6. Dialect variants in the wild

Yes — and they are not cosmetic.

### 6.1 Header-block name variants

The top-level block name identifies the dialect:

| Opening line | Dialect | Modelled at |
| --- | --- | --- |
| `clrmamepro (` | Mainline | `Data.Models/ClrMamePro/ClrMamePro.cs` |
| `romvault (` | RomVault | `Data.Models/ClrMamePro/RomVault.cs` — same 15 keys, but every one typed as a plain `string` including `forcemerging`/`forcezipping`/`forcepacking`, i.e. RomVault writes values outside clrmamepro's enums |
| `doscenter (` | DOSCenter | `Data.Models/DosCenter/` — a genuinely different dialect (see §6.2) |

Retool recognises only `clrmamepro (` and requires it on **line 1 exactly**
(`parse_dat.py:240-249`). A RomVault-flavoured DAT is silently treated as "not CLRMAMEPro"
and then fails the Logiqx sniff too.

### 6.2 DOSCenter

Same parenthesis syntax, different vocabulary and different lexical rules. The tokenizer
carries an explicit `DosCenter` mode (`Reader.cs:48-52`, `:180-200`):

- Header keys: `name`, `description`, `version`, `date`, `author`, `homepage`, `comment`
  (`Data.Models/DosCenter/DosCenter.cs`) — no `category`, no `url`, no force-flags.
- Sets contain `file ( ... )` items, **not** `rom` — with `name`, `size`, `crc`, `sha1`,
  `date` (`Data.Models/DosCenter/File.cs`).
- `Name:` (with a colon) is a recognised standalone form, special-cased before anything else
  (`Reader.cs:144-149`).
- `date` is **two whitespace-separated tokens** (date + time), consumed as a pair
  (`Reader.cs:206-210`) — a lexical rule that does not exist in mainline CMP.
- Unquoted multi-word `name` values are the norm, terminated by the literals `size`, `date`,
  `crc` (`Reader.cs:191-201`).

### 6.3 `forcezipping` — three incompatible readings

| Reading | Value domain | Source |
| --- | --- | --- |
| First-party | `zip` \| `unzip` | mamedev.emulab.it |
| SabreTools CMP model | `yes` \| `no` boolean (`ClrMamePro.cs:47-48`; written via `FromYesNo()` at `Writers/ClrMamePro.cs:116`) | rank 3 |
| SabreTools RomVault model | free-form string (`RomVault.cs:43`) | rank 3 |

The adapter should read `forcezipping` permissively (accepting `zip`/`unzip`/`yes`/`no`) and
retain the original lexeme, since normalising it to a boolean is exactly how the value gets
corrupted on re-emit.

### 6.4 `engine.cfg`-driven renaming

The first-party docs document `replace x y` and `ignore x` directives in `engine.cfg` that
rename or suppress keywords **at parse time**, with a published keyword list of ~50 names
including `rombaddump`, `biosnodump`, `diskimagenodump`, `diskimagechd_md5`, `biossetname`,
`resourcexml`, `resourcexmlvalue`, and so on
(<https://mamedev.emulab.it/clrmamepro/docs/htm/datfile.htm>). This means a producer
targeting a specific clrmamepro profile can legitimately emit key names that appear nowhere
in this inventory. It is the strongest single argument that the CLRMAMEPro adapter must be
built around an open key space.

### 6.5 Case and ordering

Rank 1 states "the order and the case doesn't matter". SabreTools honours both
(`Readers/ClrMamePro.cs:210` lowercases keys; the reader is order-independent). Retool does
**not**: its parsing is `startswith('name ')`, `startswith('description ')` — exact
lowercase, exact single trailing space (`parse_dat.py:422-428`, `:263-280`). A DAT using
`Name "..."` parses correctly in clrmamepro and SabreTools and silently loses every title in
retool. DATROMTool should be case-insensitive on keys, per the first-party rule.

## 7. Recommendations for the #50 spec

1. **Open key space, not a closed enum.** `engine.cfg` renaming (§6.4) and the long tail of
   vendor extensions (§3.2) mean the CLRMAMEPro adapter must retain unrecognised keys
   verbatim, keyed by their original lexeme, at header / set / item level. The map's
   "escape hatch for unknown attributes and unknown elements" carries the whole
   round-trip guarantee here — for Logiqx it is a safety net, for CLRMAMEPro it is the
   primary mechanism.
2. **Model set-kind as an enum, not a boolean.** `game` / `set` / `machine` / `resource`
   must be retained as a value (§3), because Logiqx's `@isbios` cannot round-trip it and
   defaulting to `game` loses the distinction.
3. **Model hashes as an open map, not fixed fields.** Twelve CMP-only digest algorithms
   (§3.2) versus Logiqx's four. A `Map<HashAlgorithm, String>` with an unknown-algorithm
   fallback is the shape that holds both without churn each time a new algorithm appears.
4. **State the cross-dialect fidelity limit in the spec.** Semantically lossless holds
   dialect→model→same-dialect. Logiqx→CMP loses repeated categories, per-set comments,
   file-level attributes, `email`, the RomCenter block, and any value containing a newline
   (§5.2). The CMP writer must report these, not drop them.
5. **Retain original lexemes for enum-valued fields.** `forcezipping` (§6.3), `forcemerging`
   and `forcepacking` all have wider or conflicting domains across dialects. Normalising to
   a Java enum and re-emitting the canonical spelling is a silent data change.
6. **Do not treat retool's coverage as the requirement.** Retool reads 9 of the ~60 keys
   inventoried here and never writes CLRMAMEPro at all. Any test suite derived from retool's
   fixtures will pass while dropping most of the format.
7. **Quote on write, accept both escapes on read** (§1.2), and add a round-trip test for a
   value containing `"` — both reference implementations get it wrong, in different
   directions.

## Appendix: real-world sample

`~/git/retool/tests/source/features/Retool - Missing title data (CMP).dat`, verbatim (retool
v2.4.9, `59f169f`):

```
clrmamepro (
	name "Retool - Missing title data (CMP)"
	description "Missing title data (CMP)"
	category Console
	version 2025-06-29
	author "unexpectedpanda | https://unexpectedpanda.github.io/retool"
)

game (
	name "Test Title (USA)"
	description "Test Title (USA)"
	rom ( name "Test Title (USA).cue" size 1000 crc 10000000 md5 10000000000000000000000000000000 sha1 1000000000000000000000000000000000000000 )
	rom ( name "Test Title (USA).bin" size 1000 crc 10000000 md5 10000000000000000000000000000000 sha1 1000000000000000000000000000000000000000 )
)

game (
	name ""
	description "Test Title (Europe)"
	rom ( name "Test Title (Europe).bin" size 1000 crc 10000000 md5 10000000000000000000000000000000 sha1 1000000000000000000000000000000000000000 )
)

game (
	name "Test Title (Europe) (En,Fr,De,It)"
	rom ( name "Test Title (Europe) (En,Fr,De,It).iso" size 1000 crc 10000000 md5 10000000000000000000000000000000 sha1 1000000000000000000000000000000000000000 )
)

game (
	name "Test Title (Europe) (Da,No,Sv)"
	description "Test Title (Europe) (Da,No,Sv)"
)
```

Note in the sample: `category Console` and `version 2025-06-29` are **unquoted**, while
`name`, `description` and `author` are quoted — confirming that quoting is per-value and
optional except where a value contains whitespace. Tab indentation is conventional but not
required (every reader examined trims the line first: `Reader.cs:130`, `parse_dat.py:392`).
