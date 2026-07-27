# Prior art: how SabreTools models DATs format-agnostically

Research note for [issue #51](https://github.com/andrebrait/DATROMTool/issues/51), part of the
wayfinder map [#50](https://github.com/andrebrait/DATROMTool/issues/50) (DATROMTool internal
data format).

> **Note on this file's location.** The repository had no `docs/` convention before this note.
> This establishes one: `docs/research/NNNN-<slug>.md`, numbered by the GitHub issue that
> commissioned the research. If a later ADR/design-doc convention lands, this directory should
> be folded into it rather than kept in parallel.

## Scope and method

Everything below was read from source. SabreTools is not vendored into this repository, so the
upstream repositories were cloned locally and read at pinned commits. Every claim is cited to a
file and line at one of these commits, to a commit's own diff, or to a GitHub issue/PR body.
Claims I could not ground in a primary source are labelled **UNVERIFIED** and collected at the
end.

| Repository | Commit read | Date of commit |
| --- | --- | --- |
| [`SabreTools/SabreTools`](https://github.com/SabreTools/SabreTools) (the application) | `bf72679616c57d1a363635e2a3352f317ddf7e1f` | 2026-07-23 |
| [`SabreTools/SabreTools.Serialization`](https://github.com/SabreTools/SabreTools.Serialization) (models, readers, writers, DAT object model) | `bcc0a5bd1e691fd8c060367a9e212f23a8de72eb` | 2026-07-23 |
| [`SabreTools/SabreTools.Models`](https://github.com/SabreTools/SabreTools.Models) (binary-format models; historical home of the DAT models) | `729a2ca0a27d939654414ccc0e5146cb722b0b4e` | 2025-09-29 |
| [`RomVault/RVWorld`](https://github.com/RomVault/RVWorld) (RomVault) | `9f6b3a476feee7836c2fbf9cbfc57ebeb97bd847` | 2026-04-25 |
| `~/git/retool` (Retool v2.4.9, local clone) | working tree | — |

Unqualified paths below are relative to the repository named in the surrounding text; where a
path could be ambiguous it is given with its repository prefix.

One structural note before anything else: the DAT models moved twice during the period studied.
They lived in `SabreTools.Models/` until commit `c6cba88` ("Migrate metadata models to
Serialization"), then in `SabreTools.Serialization/SabreTools.Serialization/Models/`
(commit `3daec985`, "Migrate metadata models from Models", 2025-09-26), then were renamed into
`SabreTools.Serialization/SabreTools.Data.Models/` (commit `37d5b79b`, "Rename directories to
visually help determine real namespace"). Historical citations therefore use the path that was
live at the commit being quoted.

---

## 1. The architecture, as it actually is

SabreTools converts a DAT dialect into its runtime object model through **four** distinct model
layers, each with hand-written mapping code in both directions:

1. **Dialect models** — faithful, per-format POCOs, one namespace per dialect:
   `SabreTools.Data.Models/Logiqx/`, `.../ClrMamePro/`, `.../Listxml/`, `.../OfflineList/`,
   `.../RomCenter/`, `.../DosCenter/`, `.../ArchiveDotOrg/`, `.../AttractMode/`,
   `.../EverdriveSMDB/`, `.../Hashfile/`, `.../Listrom/`, `.../OpenMSX/`, and more.
2. **Readers / Writers** — file or stream ⇄ dialect model
   (`SabreTools.Serialization.Readers/Logiqx.cs`, 977 lines;
   `SabreTools.Serialization.Writers/Logiqx.cs`, 608 lines).
3. **Cross-model serializers** — dialect model ⇄ the "format-agnostic" model
   (`SabreTools.Serialization.CrossModel/Logiqx.Serializer.cs` 376 lines and
   `Logiqx.Deserializer.cs` 397 lines, both implementing
   `ICrossModel<TSource, TDest>` from `SabreTools.Serialization.CrossModel/ICrossModel.cs:6`).
4. **The DAT object model** — `SabreTools.Metadata.DatFiles` / `SabreTools.Metadata.DatItems`,
   built from and rendered back to layer 3 by
   `SabreTools.Metadata.DatFiles/DatFile.FromMetadata.cs` (804 lines) and
   `DatFile.ToMetadata.cs` (899 lines).

The neutral layer is `SabreTools.Data.Models/Metadata/`, whose root type is deliberately thin:

```csharp
// SabreTools.Data.Models/Metadata/MetadataFile.cs:5-16
/// <summary>
/// Format-agnostic representation of a full metadata file
/// </summary>
public class MetadataFile
{
    public Header? Header { get; set; }
    public InfoSource? InfoSource { get; set; }
    public Machine[]? Machine { get; set; }
}
```

Machines hold items; items are subclasses of an abstract
`DatItem` carrying only an `ItemType` discriminator
(`SabreTools.Data.Models/Metadata/DatItem.cs:9-16`).

So the *shape* is right, and it is the same shape #50 has settled on: dialect adapters on the
edges, one neutral model in the middle, a separate runtime model over it. The interesting part
is what happened to that neutral model under load.

---

## 2. The neutral model is a union of every dialect, not an abstraction over them

`Metadata.Machine` has 91 properties; `Metadata.Rom` has 97; `Metadata.Header` has 48 (counted
by `grep -c "{ get; set; }"` at commit `bcc0a5b`). They are not 91/97/48 concepts. They are the
set-union of every field any supported dialect ever puts on a game or a file, flattened into one
class with no namespacing, and each foreign field is annotated in a comment with the dialect it
came from:

```csharp
// SabreTools.Data.Models/Metadata/Rom.cs:13-32
/// <remarks>ArchiveDotOrg.File</remarks>
public string? Album { get; set; }

/// <remarks>AttractMode.Row</remarks>
public string? AltRomname { get; set; }
...
/// <remarks>ArchiveDotOrg.File</remarks>
public string? ASRDetectedLang { get; set; }
```

`Metadata.Rom` carries `hOCRCharToWordhOCRVersion`, `hOCRPageIndexModuleVersion`,
`BitTorrentMagnetHash`, `ClothCoverDetectionModuleVersion` and a dozen more archive.org-only
fields (`Rom.cs:88-104`, `:40`, `:45`) alongside `CRC32`, `SHA1` and `Name`. A Logiqx `<rom>`
and an archive.org file entry are the *same class*.

The seam is not merely thin — in the header it is broken outright. The "format-agnostic" header
holds five properties whose *types* are OfflineList dialect model classes:

```csharp
// SabreTools.Data.Models/Metadata/Header.cs:21,65,70,86,111
public OfflineList.CanOpen? CanOpen { get; set; }
/// TODO: This needs an internal model OR mapping to fields
public OfflineList.Images? Images { get; set; }
/// TODO: This needs an internal model OR mapping to fields
public OfflineList.Infos? Infos { get; set; }
public OfflineList.NewDat? NewDat { get; set; }
public OfflineList.Search? Search { get; set; }
```

The neutral model has a compile-time dependency on a specific dialect's model. The maintainer's
own `TODO` on two of them ("This needs an internal model OR mapping to fields") is an admission
that the abstraction did not hold and the dialect type was passed through instead.

**Which dialect is privileged?** No single one, and that is arguably worse than privileging one.
Logiqx is not the model (unlike DATROMTool today, where `domain/.../datafile/logiqx/` *is* the
model), but nothing else is either. The neutral layer is a lowest-common-denominator bag with
every dialect's private vocabulary pushed into it. There is no notion of "this field belongs to
namespace X", no per-dialect extension container, and no way to ask the model which fields are
meaningful for the format you are about to write.

---

## 3. The escape hatch existed, was never a fidelity feature, and was deleted

This is the sharpest lesson in the whole survey, because it is a decision that was made,
documented, lived with for a year, and then reversed.

Every completed dialect model used to carry a per-node overflow bucket:

```csharp
// SabreTools.Models/Logiqx/GameBase.cs, as of commit 33217a7^ (2024-11-13)
#region DO NOT USE IN PRODUCTION

/// <remarks>Should be empty</remarks>
[XmlAnyAttribute]
public XmlAttribute[]? ADDITIONAL_ATTRIBUTES { get; set; }

/// <remarks>Should be empty</remarks>
[XmlAnyElement]
public object[]? ADDITIONAL_ELEMENTS { get; set; }

#endregion
```

The `SabreTools.Models` README stated the intent explicitly, and the statement was deleted in the
same commit that deleted the fields:

> This code should be removed before the models are used. This is only included during debugging
> and implementation as to ensure that there are no notable holes in the models that would
> disallow 1:1 replication of inputs.
>
> — `README.MD`, removed in `SabreTools.Models` commit `33217a7efed52020c2bc3f6ee04c93d09554cbbe`,
> "Remove \"overflow\" fields finally", 2024-11-13

So the overflow bucket was never an escape hatch for user data. It was a **coverage assertion
harness**: a device for proving during development that the exhaustively-enumerated model had no
holes, to be removed once it did not. The fidelity strategy is total enumeration, not graceful
degradation. Removal cost 1267 deletions across 118 files
(`git show 33217a7 --stat`, `SabreTools.Models`). The companion commit in the serialization
repo — `94d6556e0491815d4632597390b15c5833f69873`, "Ignore additional elements", 2024-11-12 —
removed the population code and, notably, deleted 832 lines of tests that had been asserting
`Assert.Empty(dat.ADDITIONAL_ELEMENTS)` per node, i.e. the losslessness guard went with it.

**What replaced it: nothing.** Unknown data is now silently discarded at parse time.

- Logiqx: every unknown element hits `default: if (Debug) Console.Error.WriteLine(...); reader.Skip();`
  — `SabreTools.Serialization.Readers/Logiqx.cs:55, 127, 302, 446, 707, 966`. Without `--debug`
  there is not even a log line.
- ClrMamePro retains a vestige of the old hatch that is now dead code. In
  `SabreTools.Serialization.Readers/ClrMamePro.cs`, `CreateRelease` declares
  `var itemAdditional = new List<string>();` at line 448, appends every unrecognised key to it at
  line 470, and then returns without ever reading it (`return release;`, line 475). The variable
  is written and never consumed; a whole-repository grep for `itemAdditional` returns exactly
  those two lines.

**Design lesson.** An overflow bucket used as a development assertion and an overflow bucket used
as a runtime fidelity guarantee are different features, and SabreTools built the first while
naming it like the second. For map premise 4 (semantically lossless), DATROMTool needs the
second: a bucket that survives parse, round-trips through the internal model, serializes into
artifacts A and B, and re-emits on output. SabreTools' `[XmlAnyAttribute]`/`[XmlAnyElement]`
shape is a reasonable starting point for the *capture* side — but it must be reachable from the
neutral model, not only from the dialect model, or it dies at the cross-model hop. And the tests
that assert it is empty for known-good corpora are worth keeping permanently, not deleting: they
are exactly the "did our model grow a hole?" alarm, and #48's silent-field-loss finding is the
symptom they exist to catch.

---

## 4. The open/closed pendulum: dictionary → POCO, and what each end cost

The neutral model was originally **not** a set of POCOs. It was a typed dictionary:

```csharp
// SabreTools.Serialization/Models/Metadata/DatItem.cs @ 3daec985 (2025-09-26)
public class DatItem : DictionaryBase
{
    public const string TypeKey = "_type";
    ...
}

// SabreTools.Serialization/Models/Metadata/DictionaryBase.cs @ 3daec985
public abstract class DictionaryBase : Dictionary<string, object?>
{
    public T? Read<T>(string key) { ... }
    public bool? ReadBool(string key) { ... }   // accepts true/yes/false/no
    public double? ReadDouble(string key) { ... }
    public long? ReadLong(string key) { ... }
}
```

Fields were `const string` keys with the expected runtime type recorded **in a doc comment**:

```csharp
// SabreTools.Serialization/Models/Metadata/Machine.cs @ 3daec985
/// <remarks>Adjuster[]</remarks>
[NoFilter]
public const string AdjusterKey = "adjuster";

/// <remarks>string, string[]</remarks>
public const string CategoryKey = "category";
```

Note `/// <remarks>string, string[]</remarks>` — the type system was prose, and some fields were
genuinely two types depending on the dialect.

Between 2026-04-03 and 2026-04-08 this was torn out. The migration is legible commit by commit
in `SabreTools.Serialization`: "Convert Archive fully over to properties" (`845a0cb9`), "Convert
Port fully over to properties" (`4bb670ac`), "Convert Disk fully over to properties"
(`9ffa2a5e`), "String Machine keys to properties" (`b55a932b`), "Convert last long key to
property" (`5ccfde54`), "DictionaryBase is no more, bon voyage" (`bf852ef0`), "ModelBackedItem
had no actual utility anymore" (`1a10e10f`). It landed as
[PR #79, "Attempt metadata overhaul"](https://github.com/SabreTools/SabreTools.Serialization/pull/79),
merged 2026-04-08, whose body states the reasons:

> - Concrete types and enums where appropriate to avoid frequent and repeated conversion
> - Removal of `DictionaryBase`, `ModelBackedItem`, and `ModelBackedItem<T>` types
> - Consolidation of extensions and enums to appropriate namespaces
> - Near-complete removal of Reflection-based methods
> - Major fixes to the filtering framework, including allowing alternate field names again

Both ends of this pendulum have a bill, and the bills are visible:

**The dictionary end cost** conversion at every read site ("frequent and repeated conversion"),
reflection to reach fields, and no compile-time checking of field names or value types.

**The POCO end cost** field-name addressability, and SabreTools had to rebuild it by hand.
`SabreTools.Metadata.Filter/FilterObject.cs` is now 2532 lines of per-type
`private static bool GetCheckValue(<Type> obj, string fieldName, out string? checkValue)` with a
literal `switch (fieldName)` over string cases (see `:505`, `:538`, `:653`, `:677`, `:710`), plus
`SabreTools.Metadata.Filter/Constants.cs` at 800 lines listing valid field names per item type.
In the application repository, `SabreTools.DatTools/Setter.cs` contains 497 `case "` labels and
`SabreTools.DatTools/Remover.cs` contains 499 — hand-written string→property tables that must be
extended for every new field. And PR #79's own bullet ("allowing alternate field names *again*")
records that the migration broke field aliasing, which then had to be restored.

There is a third cost that neither end removes: SabreTools runs the neutral model and the runtime
object model as **two parallel classes with 1:1 mirrored properties**.
`SabreTools.Metadata.DatItems/Formats/Rom.cs` is 939 lines wrapping the 389-line
`SabreTools.Data.Models/Metadata/Rom.cs`, and essentially every line is:

```csharp
// SabreTools.Metadata.DatItems/Formats/Rom.cs:17-21
public string? Album
{
    get => _internal.Album;
    set => _internal.Album = value;
}
```

**Design lesson.** The choice is not "open map" versus "typed record"; it is *what you pay to
address a field by name at runtime*. DATROMTool will need name-addressed fields for filtering,
for `dat check --divergence`, and for provenance bookkeeping. Java has an option SabreTools' C#
codebase did not lean on: a sealed interface of field descriptors (a per-item-type enum or record
implementing a `Field<T>` accessor pair) generated or written once, giving typed records *and* a
first-class, exhaustively-checked field vocabulary — without reflection, without a 2500-line
`switch`, and without a mirrored wrapper class. Whatever is chosen, the requirement to state
explicitly is: **the model must expose its own field vocabulary as data**, or every downstream
feature that addresses fields by name will grow its own copy of that vocabulary and they will
drift.

---

## 5. Hashes: no hash-set abstraction, but the matching semantics are worth stealing

SabreTools supports 14 digests on a `Rom` — CRC16, CRC32, CRC64, MD2, MD4, MD5, RIPEMD128,
RIPEMD160, SHA-1, SHA-256, SHA-384, SHA-512, SpamSum, BLAKE3 — and they are **14 flat `string?`
properties** on `Metadata.Rom`, not a map or a set. `Disk` gets a different subset (MD5, SHA-1);
`Media` a third (MD5, SHA-1, SHA-256, SpamSum). Every operation over "the hashes" is therefore
hand-unrolled per algorithm per item type, in `SabreTools.Data.Extensions/MetadataExtensions.cs`:

- `HasHashes` — `:322` (Disk), `:334` (Media), `:350` (Rom, 14 null checks OR-ed).
- `HasZeroHash` — `:386`, `:401`, `:424`; compares against `HashType.<X>.ZeroString`, i.e. the
  digest of the empty input, treating "hash of nothing" as equivalent to absent.
- `HasCommonHash` — `:491`, `:506`, `:529`.
- `HashMatch` — `:190`, `:211`, `:236`.
- `FillMissingHashes` — `:596`, `:615`, `:644`.

The *semantics*, though, are the best answer I found to the brief's question about dialects that
disagree on which hashes are mandatory. SabreTools does not define a mandatory set at all. It
defines three-valued comparison plus a commonality guard:

```csharp
// SabreTools.Data.Extensions/MetadataExtensions.cs:173-182
public static bool ConditionalHashEquals(string? firstHash, string? secondHash)
{
    // If either hash is empty, we say they're equal for merging
    if (string.IsNullOrEmpty(firstHash) || string.IsNullOrEmpty(secondHash))
        return true;

    // If they're different sizes, they can't match
    if (firstHash!.Length != secondHash!.Length)
        return false;

    // Otherwise, they need to match exactly
    return string.Equals(firstHash, secondHash, StringComparison.OrdinalIgnoreCase);
}
```

Absent is *compatible with anything*, so a CRC-only ClrMamePro `<rom>` and an SHA-256-bearing
No-Intro `<rom>` do not falsely contradict each other. On its own that would make everything
match everything, so `HashMatch` guards it:

```csharp
// SabreTools.Data.Extensions/MetadataExtensions.cs:236-246 (abridged)
public static bool HashMatch(this Rom self, Rom other)
{
    // If either have no hashes, we return false, otherwise this would be a false positive
    if (!self.HasHashes() || !other.HasHashes())
        return false;

    // If neither have hashes in common, we return false, otherwise this would be a false positive
    if (!self.HasCommonHash(other))
        return false;
    ...
}
```

`HasCommonHash` is a per-algorithm XOR of the two null-ness flags OR-ed together (`:529-587`) —
true when at least one algorithm is populated on both sides. So the rule is: **at least one
shared algorithm must agree, and no shared algorithm may disagree.** Identity is
"agreement on the intersection, non-empty intersection required", not "these mandatory fields are
equal".

Size participates but is allowed to be unknown: `PartialEquals` (`:114-145`) falls back to hashes
alone when either side has a null size, and there is a special case where two `nodump` items with
matching names and no hashes at all are considered duplicates.

`FillMissingHashes` (`:644-731`) is the merge direction, and it is strictly additive — copy a
value only when the destination is empty, never overwrite:

```csharp
string? selfSha256 = self.SHA256;
string? otherSha256 = other.SHA256;
if (string.IsNullOrEmpty(selfSha256) && !string.IsNullOrEmpty(otherSha256))
    self.SHA256 = otherSha256;
```

**Design lesson.** Take the semantics, reject the representation. A `HashSet` value object keyed
by algorithm (an `EnumMap<HashAlgorithm, byte[]>` or equivalent) collapses `HasHashes`,
`HasZeroHash`, `HasCommonHash`, `HashMatch` and `FillMissingHashes` from 14 unrolled branches
each into one loop apiece, and makes adding an algorithm a one-line change instead of the
66-file change measured in §8 below. Model absence as absence (no key), define agreement on the
intersection, require the intersection to be non-empty, and treat "digest of empty input" as
absence — that last one is a real, cheap trap-avoidance that we would otherwise have to
rediscover. RomVault, incidentally, stores hashes as `byte[]` rather than hex strings
(`RVWorld/DATReader/DatStore/DatFile.cs:10-13`), which sidesteps SabreTools' case-insensitive
string comparisons and length-as-algorithm-proxy checks entirely.

---

## 6. There is no provenance. Not partial provenance — none

A case-insensitive grep for `provenance` across the whole of `SabreTools.Serialization` at
`bcc0a5b` returns zero hits. The closest constructs are:

```csharp
// SabreTools.Metadata.DatItems/Source.cs:9-30
public class Source : ICloneable
{
    public readonly int Index;
    public readonly string? Name;
    public Source(int id, string? source = null) { Index = id; Name = source; }
}
```

attached to items as `DatItem.Source` / `DatItem.SourceIndex`
(`SabreTools.Metadata.DatItems/DatItem.cs:78-87`, alongside `Machine` / `MachineIndex`). That is
**file-level** provenance — which input DAT an item was read from, used for bucketing and
renaming (`DatItem.cs:183-215`, `ItemKey.Machine` prefixes a padded source index). It says
nothing about where any individual *value* came from.

Where multiple sources supply the same field, the distinction is destroyed at the point of merge,
in three separate places:

- **Derived-from-file overwrites declared.** `SabreTools.DatTools/DatFromDir.cs:386` and
  `SabreTools.DatTools/Rebuilder.cs:348,543,619,679` call `FileTypeTool.GetInfo(...)` and
  `.ConvertToRom()` to build a `Rom` from a scanned file. It is the same `Rom` class with the
  same properties as the parsed one; nothing distinguishes a hash that was computed from one that
  was declared.
- **Cross-DAT fill is additive-only and unlabelled.** `FillMissingHashes` writes the other item's
  value into the same property (`MetadataExtensions.cs:644-731`).
- **Externally-supplied values are written straight into model fields.**
  `SabreTools.Metadata.Filter/ExtraIniItem.cs` loads a MAME-extras INI into a
  `Dictionary<string, string>` of machine name → value, keyed by an
  `<itemtype>.<fieldname>` `FilterKey` (`ExtraIniItem.cs:16-39`), and
  `SabreTools.DatTools/ExtraIni.cs:130-131` applies it via `setter.SetFields(datItem.Machine)` /
  `setter.SetFields(datItem)`. This is SabreTools' analogue of Retool clonelist/metadata
  enrichment, and after it runs the value is indistinguishable from a declared one.

Provenance is instead **externalised to the operator's command line**:
`SabreTools.DatTools/Replacer.cs` takes explicit `machineFieldNames` and `itemFieldNames` lists
naming which fields to overwrite from which DAT (`Replacer.cs:22-29`). The human decides the
precedence, once, per invocation; the model never records what was decided.

**Design lesson.** This is the single largest gap between SabreTools and #50's premise 6, and it
is the one place where DATROMTool has no prior art to lean on — nobody in this space has built
it. The consequences visible in SabreTools are exactly the ones premise 6 predicts: `dir2dat`
cannot tell you which of its hashes it computed versus inherited; `ExtraIni` enrichment is
irreversible; and issue-report triage repeatedly turns on "where did this value come from?" with
no way to answer from the data. Two concrete constraints follow for our spec. First, provenance
has to sit on the *value*, not on the item — a per-item `Source` is genuinely useful for
bucketing (SabreTools proves that) but it does not answer premise 6's question. Second, if
enrichment (Retool metadata, scanned hashes) writes into the same fields as declared data
without a discriminator, artifact A's "multi-origin" property is lost the moment enrichment runs,
and the A → resolve → B pipeline collapses into a single mutable stage. Keep the origins
side-by-side in A; elect in the resolve step; only B is single-valued.

---

## 7. Round-trip failures, and why they were structural

These are not incidental bugs; each traces to a representation choice.

**Sentinel enum defaults leak into output.**
[Issue #115](https://github.com/SabreTools/SabreTools/issues/115) (2025-06-27, "dir2dat outputting
unexpected status field per rom"): `status="none"` was emitted on every `<rom>`, which broke
No-Intro Dat-o-Matic ingestion. Cause: an enum whose zero value is a sentinel
(`ItemStatus.None` at `SabreTools.Data.Models/Metadata/Enums.cs:243-246`) in a non-nullable
field cannot express "absent". The fix was to make the property nullable and write conditionally
(`SabreTools.Data.Models/Logiqx/Rom.cs:83-85` `public ItemStatus? Status`;
`SabreTools.Serialization.Writers/Logiqx.cs:311` `WriteOptionalAttributeString("status", obj.Status?.AsStringValue())`).

But the same problem is still live elsewhere with a *different* workaround.
`MergingFlag`/`NodumpFlag`/`PackingFlag` (`Enums.cs:373`, `:403`, `:436`) each keep a `None = 0`
member and are held **non-nullable** on `Metadata.Header`
(`Header.cs:45,48,51`), so emission is guarded by ad-hoc comparisons at each call site:

```csharp
// SabreTools.Serialization.CrossModel/Logiqx.Deserializer.cs:60-73 (abridged)
if (item.HeaderSkipper is not null
    || item.ForceMerging is not Data.Models.Metadata.MergingFlag.None
    || item.ForceNodump is not Data.Models.Metadata.NodumpFlag.None
    || item.ForcePacking is not Data.Models.Metadata.PackingFlag.None)
{
    header.RomVault = new RomVault();
    ...
}
```

Two mechanisms for one problem, applied inconsistently, is the shape of a design that leaks.
Note that DATROMTool has the identical hazard today: `Rom`'s compact constructor coerces
`mia == null` to `YesNo.NO`
(`domain/src/main/java/io/github/datromtool/domain/datafile/logiqx/Rom.java:67-69`), which is
precisely the "default injection" #48 observed.

**Collapsing distinct dialect sub-elements makes output non-round-trip.** The Logiqx header can
carry `<clrmamepro>`, `<romvault>` and `<romcenter>` sub-elements
(`SabreTools.Data.Models/Logiqx/Header.cs:59-66`). `<clrmamepro>` and `<romvault>` share four
attributes and differ by one (`dir`, RomVault-only) — see
`SabreTools.Data.Models/Logiqx/ClrMamePro.cs` and `.../Logiqx/RomVault.cs`. The neutral model
collapses both into one flat set (`HeaderSkipper`, `ForceMerging`, `ForceNodump`, `ForcePacking`,
`DirHandling`). On output, the cross-model deserializer therefore **writes both elements**
whenever any of those fields is set (`Logiqx.Deserializer.cs:65` and `:77`). A DAT that came in
with only `<clrmamepro>` goes out with `<clrmamepro>` *and* `<romvault>`. That is not semantic
losslessness; it is invention, and it is the direct consequence of merging two provenance-bearing
containers into one anonymous field group.

**Fields the model does not know are simply gone.**
[Issue #88](https://github.com/SabreTools/SabreTools/issues/88) (2023-01, "`--update` - Doesnt
preserve MIA tag") is the canonical instance: `mia="yes"` present on input, absent on output,
because the model had no `mia`. Resolved only by adding the field
([PR #89](https://github.com/SabreTools/SabreTools/issues/89)). With no overflow bucket, every
such field is a silent data-loss bug until someone reports it, and the report is the only
detection mechanism. Compare
[issue #119](https://github.com/SabreTools/SabreTools/issues/119) (RetroAchievements Logiqx
fields, 2025-09) and
[issue #125](https://github.com/SabreTools/SabreTools/issues/125) (RomVault DATs where
directories contain ROMs without a `<game>` wrapper) — same pattern, ongoing.

**Merge modes discard structural fields.**
[Issue #111](https://github.com/SabreTools/SabreTools/issues/111) (2025-01): `cloneof` and `romof`
are dropped when any merge mode is used, which the reporter notes makes the output unusable for
switching modes in other tools. Structural relationships are consumed by the operation and not
restored.

---

## 8. The per-field tax, measured

Because the model is an enumerated union and every layer has hand-written mapping, adding one
field touches every layer. This is measurable from single commits.

**One optional Logiqx attribute** — `type` on `<game>`, a RomVault extension —
commit `bcc0a5bd1e691fd8c060367a9e212f23a8de72eb` ("Add set type RomVault game extension field",
2026-07-23), **14 files, +40 lines**:

```
SabreTools.Data.Models/Logiqx/GameBase.cs                      | 5 +++++
SabreTools.Data.Models/Metadata/Machine.cs                     | 9 +++++++++
SabreTools.Metadata.DatFiles.Test/DatFileTests.FromMetadata.cs | 3 +++
SabreTools.Metadata.DatFiles.Test/DatFileTests.ToMetadata.cs   | 1 +
SabreTools.Metadata.DatItems/Machine.cs                        | 6 ++++++
SabreTools.Metadata.Filter.Test/FilterObjectTests.cs           | 3 +++
SabreTools.Metadata.Filter/Constants.cs                        | 2 ++
SabreTools.Metadata.Filter/FilterObject.cs                     | 3 +++
SabreTools.Serialization.CrossModel.Test/LogiqxTests.cs        | 2 ++
SabreTools.Serialization.CrossModel/Logiqx.Deserializer.cs     | 1 +
SabreTools.Serialization.CrossModel/Logiqx.Serializer.cs       | 1 +
SabreTools.Serialization.Readers.Test/LogiqxTests.cs           | 2 ++
SabreTools.Serialization.Readers/Logiqx.cs                     | 1 +
SabreTools.Serialization.Writers/Logiqx.cs                     | 1 +
```

plus 6 more files in the application repository (commit `bf72679`: `Remover.cs`, `Replacer.cs`,
`Setter.cs`, `BaseFeature.cs`, `README.1ST`, submodule bump). **20 files for one optional
attribute.**

**One hash algorithm** — BLAKE3 — commit `05c86e4` in `SabreTools.Serialization`
(**49 files, +779/−45**) plus commit `3bc83f6` in `SabreTools` (**17 files**). **66 files.**

**Design lesson.** Both numbers are dominated by the same two causes: a union model with one
property per dialect field, and field-name vocabularies duplicated into filter/setter/remover
tables. A hash-set value object removes most of the second number; a per-dialect extension
container plus a data-driven field vocabulary removes most of the first. This is the concrete
argument for why premise 2's "Logiqx becomes one adapter among several" must be paired with an
explicit answer to "where do dialect-private fields live", or DATROMTool inherits the same tax.

---

## 9. An unfinished second model, sitting in the tree

`DatFile` maintains **two** item stores simultaneously:

```csharp
// SabreTools.Metadata.DatFiles/DatFile.cs:40,46,52-53
public ItemDictionary Items { get; private set; } = new ItemDictionary();
public ItemDatabase ItemsDB { get; private set; } = new ItemDatabase();
public DatStatistics DatStatistics => Items.DatStatistics;
//public DatStatistics DatStatistics => ItemsDB.DatStatistics;
```

with the reason recorded in-source:

```
/*
 * Planning Notes:
 *
 * In order for this in-memory "database" design to work, there need to be a few things:
 * - Feature parity with all existing item dictionary operations
 * - A way to transition between the two item dictionaries (a flag?)
 * - Helper methods that target the "database" version instead of assuming the standard dictionary
 *
 * Notable changes include:
 * - Separation of Machine from DatItem, leading to a mapping instead
 * - Adding machines to the dictionary distinctly from the items
 * - Having a separate "bucketing" system that only reorders indicies and not full items; quicker?
 * - Non-key-based add/remove of values; use explicit methods instead of dictionary-style accessors
*/
// SabreTools.Metadata.DatFiles/ItemDatabase.cs:22-35
```

The cost shows up as a doubled API surface: every operation has a `…DB` twin
(`DatItem.PassesFilter` / `PassesFilterDB` at `SabreTools.Metadata.DatItems/DatItem.cs:161,168`;
`ExtraIni.ApplyExtras` / `ApplyExtrasDB` at `SabreTools.DatTools/ExtraIni.cs:95,154`), with
`DatFile.Filtering.cs` and `DatFile.Splitting.cs` containing 31 and 63 `DB(` call sites
respectively.

The specific thing being retrofitted is telling: *"Separation of Machine from DatItem, leading to
a mapping instead."* The original model **embedded** a `Machine` inside every `DatItem`
(`DatItem.Machine`, `DatItem.cs:65`, with `CopyMachineInformation` deep-cloning the machine onto
each item, `:119-139`). Normalising that after the fact requires a parallel store and a
transition flag, and the migration has been unfinished long enough for the statistics accessor to
sit commented out.

**Design lesson.** Decide the item↔machine ownership relation *in the spec*, before writing code
— it is the hardest thing to change later, and #50's "Not yet specified / Memory and throughput"
row is precisely this decision. Denormalising the machine onto every item is what makes
per-item cloning cheap and per-machine editing expensive; SabreTools chose it, then spent years
trying to reverse it. Related: [issue #103](https://github.com/SabreTools/SabreTools/issues/103)
(2024-10, "[ANNOUNCEMENT] Old Bugs, New Bugs") is the maintainer telling users to re-test
everything, because *"The entire codebase has been ripped apart and reassembled multiple times
since the last stable release."* Between 1.1.0 (2021-02) and 1.2.0 (2025-04) there was no stable
release at all
([releases](https://github.com/SabreTools/SabreTools/releases)). That is the schedule cost of
getting the core model shape wrong and having to redo it under a shipping product.

---

## 10. Neighbours, briefly

**RomVault** (`RVWorld`, commit `9f6b3a4`) is the useful contrast because it is a *consumption*
model, not a *conversion* model, and its model is correspondingly small:

- `DATReader/DatStore/DatFile.cs:9-22` — a ROM has a `ulong? Size` and exactly four digests,
  `CRC`, `SHA1`, `MD5`, `SHA256`, each typed `byte[]` rather than a hex string, plus `Merge`,
  `Status`, `Region`, `MIA`, `isDisk`, `MIAStatus`.
- `DATReader/DatStore/DatGame.cs` — 30-odd string fields, with the EmuArc/Trurip extension block
  flattened behind a single `bool IsEmuArc` discriminator plus its fields
  (`Publisher`, `Developer`, `Genre`, `SubGenre`, `Ratings`, `Score`, `Players`, `Enabled`,
  `CRC`, `Source`, `RelatedTo`) rather than a nested object.
- `DATReader/DatStore/DatBase.cs` — a `Name` + `FileType` discriminator over a `DatDir` tree, so
  directories can hold files directly. That is the structure behind
  [SabreTools issue #125](https://github.com/SabreTools/SabreTools/issues/125), and it confirms
  the RomVault DAT dialect is genuinely tree-shaped rather than flat game→rom.
- `DATReader/DatReader/DatXMLReader.cs:140,143,166,252-253` reads the `type`, `subset` and
  `dir` extensions that SabreTools added in July 2026 — the RomVault header/game extensions the
  brief flags as unmodelled by DATROMTool.
- No overflow bucket, no provenance. Unknown data is discarded, which is defensible for a scanner
  that never re-emits the DAT it read.

**Retool** (v2.4.9) takes the third strategy, and it is the cheapest one that actually works.
Header sub-elements it does not model are captured as **raw serialized XML strings** and
re-emitted verbatim:

```python
# ~/git/retool/modules/dat/parse_dat.py:306-308
if pattern2string(re.compile('(?:clrmamepro|romcenter|romvault)'), element.tag):
    element = clean_namespaces(element)
    input_dat.dat_manager_directives.append(html_.tostring(element).decode('utf-8'))
```

```python
# ~/git/retool/modules/output.py:490-491
for directive in input_dat.dat_manager_directives:
    rom_header.append(f'\t\t{directive.strip()}\n')
```

Retool never parses these directives; it copies them through. That preserves them *byte*-exactly
without modelling them at all — a strictly weaker guarantee than #50's premise 4 (it cannot
merge, filter, or reason about them), but a strictly stronger one than SabreTools' silent
`reader.Skip()`. The list is `list[str]` at `modules/dat/process_dat.py:95`. Note also that
Retool recognises `romvault` as a manager directive alongside `clrmamepro` and `romcenter`;
DATROMTool models only the latter two
(`domain/src/main/java/io/github/datromtool/domain/datafile/logiqx/`: `Clrmamepro.java`,
`RomCenter.java`, no `RomVault.java`).

---

## 11. Design lessons for map #50

1. **A neutral model that is a union of dialect fields is not format-agnostic.** SabreTools'
   `Metadata.Machine`/`Rom`/`Header` (91/97/48 properties, foreign fields tagged by comment,
   OfflineList types embedded outright) is what the seam degrades into if dialect-private fields
   have nowhere else to go. The spec must name the place they go — a per-dialect namespaced
   extension container on machine, item and header — before adapter #2 lands.
2. **Build the escape hatch as a runtime guarantee, not a development assertion, and never delete
   it.** SabreTools built `ADDITIONAL_ATTRIBUTES`/`ADDITIONAL_ELEMENTS` explicitly to be removed
   once the models were "complete", removed them, and has been paying in silent-field-loss bugs
   ever since. Ours must survive parse → neutral model → artifact A → artifact B → output, and
   the "is it empty for known-good corpora?" test suite is a permanent regression alarm, not
   scaffolding.
3. **Make the model's field vocabulary first-class data.** Filtering, divergence checking, setting
   and removing all need to address fields by name. If the model does not publish that
   vocabulary, each feature grows its own copy: SabreTools carries a 2532-line `FilterObject`,
   an 800-line `Constants`, 497 `case` labels in `Setter` and 499 in `Remover`, all
   hand-maintained.
4. **Model the hash set as a set.** Adopt SabreTools' semantics — absent is compatible with
   anything, require a non-empty intersection of populated algorithms, no populated algorithm may
   disagree, treat the empty-input digest as absence, fill only into empty slots — and reject its
   representation of 14 parallel nullable strings. Prefer `byte[]`/binary over hex strings, as
   RomVault does. This is what answers "dialects disagree on which hashes are mandatory" without
   a mandatory set.
5. **Absence must be representable in the type.** Enum sentinels (`ItemStatus.None`,
   `MergingFlag.None`) in non-nullable fields force ad-hoc `!= None` guards at every write site,
   and those guards drift — SabreTools now has two inconsistent mechanisms for one problem, one
   of which was a user-visible bug (#115). DATROMTool's own `mia` coercion is the same hazard
   already present.
6. **Never collapse two distinct dialect containers into one anonymous field group.** Merging
   Logiqx's `<clrmamepro>` and `<romvault>` header elements is why SabreTools now emits both
   whenever either was present. Semantic losslessness constrains *invention* as much as loss.
7. **Provenance must be on the value, not the item, and enrichment must not overwrite in place.**
   SabreTools has no provenance concept at all; scanned hashes, cross-DAT fills and INI-sourced
   enrichment all write into the same properties as declared data, and the precedence decision
   lives only in the operator's command line. If artifact A's multi-origin candidates are not
   kept side-by-side, the A → resolve → B pipeline degenerates into one mutable stage.
8. **Fix the item↔machine ownership relation in the spec.** SabreTools embedded `Machine` in every
   `DatItem` and is still mid-flight reversing it, carrying two parallel stores
   (`Items`/`ItemsDB`) and a doubled API (`PassesFilter`/`PassesFilterDB`) with the migration
   notes sitting in a source comment. This is the decision #50 currently defers under "Memory and
   throughput"; it is the one that cannot be deferred cheaply.
9. **Budget the per-field tax explicitly.** One optional Logiqx attribute cost SabreTools 20
   files; one hash algorithm cost 66. If our design does not get those numbers into single
   digits, adding the next dialect extension will be a project rather than a change.

## 12. What SabreTools supports that #50 has decided not to

Recorded for completeness, since the brief asks. SabreTools' adapter set is far wider than the
Logiqx + ClrMamePro scope of premise 5: ListXML, Listrom, SoftwareList, OfflineList, RomCenter,
DosCenter, AttractMode, ArchiveDotOrg, Everdrive SMDB, OpenMSX, M1, MESS, plain hashfiles
(SFV/MD2/MD4/MD5/SHA1/SHA256/SHA384/SHA512/SpamSum/BLAKE3), separated-value formats
(CSV/SSV/TSV), Missfile, and its own SabreJSON/SabreXML native projections — all listed under
`SabreTools.Metadata.DatFiles/Formats/`. It also carries MAME's full arcade vocabulary as
first-class item types (`Adjuster`, `Chip`, `Configuration`, `Control`, `Device`, `DipSwitch`,
`Display`, `Driver`, `Input`, `Port`, `RamOption`, `Slot`, `Sound`, `Video`), which is the bulk
of `Metadata.Machine`'s 91 properties and none of which DATROMTool needs. The union-model tax
described above is largely a tax on *that* breadth — a narrower adapter scope makes a union
model far less painful, which is worth weighing honestly against the extension-container design
before committing to the latter.

## 13. UNVERIFIED / not established

- **DatVault.** Named in the brief as worth a look. No public source repository was located; a
  GitHub repository search for `datvault` returned only unrelated projects (checked at
  `bcc0a5b`-era, 2026-07-27). No primary source was read, so nothing about DatVault's model is
  asserted here.
- **Why the dictionary→POCO migration was chosen over alternatives.** PR #79's body states the
  benefits claimed, and the commit series shows the work. No design discussion, RFC or issue
  thread explaining the decision was found; the PR has zero comments. The reading in §4 that the
  trade was "conversion/reflection cost" for "field-addressability cost" is my inference from the
  code that replaced it, not a maintainer statement.
- **Whether SabreTools ever wrote `ADDITIONAL_ELEMENTS` back out on serialization.** The
  populating code and the model fields are both established, as is the README's stated intent
  that they exist only for development. I did not find writer code that re-emitted them, but I
  did not exhaustively search every pre-2024 writer, so "they were capture-only" is inference
  from the README statement rather than a proven absence.
- **Performance characteristics of any of these designs.** Nothing here is benchmarked. The
  memory/throughput question in #50's "Not yet specified" is untouched by this note.
