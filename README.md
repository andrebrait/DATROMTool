# DATROMTool

[![CI](https://github.com/andrebrait/DATROMTool/actions/workflows/maven.yaml/badge.svg?branch=master)](https://github.com/andrebrait/DATROMTool/actions/workflows/maven.yaml)

## What is it?

DATROMTool, or "_That_ ROM tool", is a platform-independent tool for performing operations on DAT files, arcade sets and video game ROMs.
It is the successor to [1g1r-romset-generator](https://github.com/andrebrait/1g1r-romset-generator).

It is heavily inspired by [SabreTools](https://github.com/SabreTools/SabreTools), which you should definitely check out.

## What are DAT files?

DAT files are used for ROM management tools like 
[ClrMAMEPro](https://mamedev.emulab.it/clrmamepro/), 
[SabreTools](https://github.com/SabreTools/SabreTools), 
[Romulus](https://romulus.cc/), 
[RomVault](http://www.romvault.com), 
[RomCenter](https://www.romcenter.com) and others.
They contain the files needed with correct names and checksums to allow you to rebuild a working arcade set from other collections (e.g.: [MAME](https://www.mamedev.org/)) you may have.  

## Why?

Basically, it boils down to:

1. Functionality in other tools was either missing or not up to the task
2. Despite the great effort the community puts into it, DAT files are not perfect. They might have issues such as:
    1. Lack information, such as language
    2. Mismatching region information
3. Existing tools are incapable of detecting, pointing out and correcting these issues
4. Most widely-used XML-based DAT formats make adding information cumbersome
5. Most existing tools target only Windows running on x86 CPUs

See an example of issues in a DAT file below, taken from _No-Intro Nintendo - Game Boy Advance (Parent-Clone) (20220215-012049)_, which uses the [No-Intro Naming Convention](https://wiki.no-intro.org/index.php?title=Naming_Convention).:

```xml
<game name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" cloneof="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (En+En,Es,It,Sv,Da)">
    <description>2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)</description>
    <release name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" region="ITA"/>
    <release name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" region="SPA"/>
    <rom name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da).gba" size="16777216" crc="73444273" md5="2cede728164ea50342d41a554ef36a02" sha1="a7b8aa7f3cd86b664f314650206d9043806e9b22"/>
</game>
```

We can see that:

- The release entries suggest this game should be considered released in the regions of Italy and Spain, but the name suggests it was released in Europe as a whole
- There is also no information on language other than in the name

Therefore, the following issues would arise from using this DAT in existing tools, which rely solely on the data in the DAT and don't parse anything from the game's name:

- The game would _not_ be selected in a query like "select all games with an European release", even though its name suggests it's an European release
- The game would _not_ be selected in a query like "select all games in Spanish", even though it clearly is in Spanish 
- It would never come up in a query like "select all games with releases from the USA, Europe and Japan, but only if they're in English, Spanish or German".

## Capabilities

Currently, DATROMTool supports:

- Parsing DAT files in the Logiqx format
- Parsing additional information from names in the No-Intro naming convention, such as:
    - Regions
    - Languages
    - Version/Revision
    - Pre-release status
    - Type of "game", such as BIOSes, Programs, among others
- Point out divergences and possible issues in the DAT, such as:
    - Diverging Region information between what's parsed and what's in the DAT
    - Diverging Language information between what's parsed and what's in the DAT
- Convert from and to:
    - [Logiqx XML DAT format](https://github.com/SabreTools/SabreTools/wiki/DatFile-Formats#logiqx-xml-format)
    - DATROMTool JSON format
    - DATROMTool YAML format
 - Matching ROMs based on file hashes, in the following order, as long as they're available in the DAT file:
    - SHA-1
    - MD5
    - File size + CRC

DATROMTool can work with the following archive formats:

| Archive format | Read | Write |
|----------------|------|-------|
| Zip            | ✅    | ✅     |
| RAR            | ✅*   | ❌     |
| 7z             | ✅    | ✅     |
| TAR            | ✅    | ✅     |

- RAR up to version 4 is natively supported
- RAR 5 is supported through external executables
    - UnRAR, if present in your `PATH`
    - 7-Zip
    - DATROMTool includes executables for either UnRAR and 7-Zip for the following platforms and architectures, meaning you shouldn't have to install anything in order to use it:
        - Windows (x86, x64 and ARM64)
        - Linux (x86, x64, ARM and ARM64)
        - macOS (x64 and ARM64)
        - BSD (x86 and x64)

The following compression algorithms are supoorted either plain or used in conjunction with TAR archives (both read and write):

- GZip
- BZip2
- LZMA
- XZ
