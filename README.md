# DATROMTool

[![CI](https://github.com/andrebrait/DATROMTool/actions/workflows/maven.yaml/badge.svg?branch=master)](https://github.com/andrebrait/DATROMTool/actions/workflows/maven.yaml)

## What?

DATROMTool, or "_That_ ROM tool", is a platform-independent tool for performing operations on DAT files and ROMs.

It is heavily inspired by [​SabreTools​](https://github.com/SabreTools/SabreTools).

## DAT files?

DAT files are used for ROM management tools like [ClrMAMEPro](https://mamedev.emulab.it/clrmamepro/), [SabreTools](https://github.com/SabreTools/SabreTools), [Romulus](https://romulus.cc/), [RomVault​](http://www.romvault.com), [RomCenter](https://www.romcenter.com) and others.  

Currently, the only DAT format accepted by DATROMTool is the [Logiqx XML format](https://github.com/SabreTools/SabreTools/wiki/DatFile-Formats#logiqx-xml-format).

They contain the files needed with correct names and checksums to allow you to rebuild a working arcade set from other collections (e.g.: [MAME](https://www.mamedev.org/)) you may have.  

## Why?

I wanted to do things other tools didn't support very well, and most of the ones that already existed targeted Windows on x86 only.

I created [1g1r-tomset-generator](https://github.com/andrebrait/1g1r-romset-generator), but I quickly realized it was increasingly hard to expand on it and that I could do much better.

On top of that, DAT files often contain conflicting data, making some operations on existing tools either painful to perform or impossible.

Example taken from _No-Intro Nintendo - Game Boy Advance (Parent-Clone) (20220215-012049)_:

```xml
    <game name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" cloneof="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (En+En,Es,It,Sv,Da)">
		<description>2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)</description>
		<release name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" region="ITA"/>
		<release name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da)" region="SPA"/>
		<rom name="2 Games in 1 - Finding Nemo + Finding Nemo - The Continuing Adventures (Europe) (Es,It+En,Es,It,Sv,Da).gba" size="16777216" crc="73444273" md5="2cede728164ea50342d41a554ef36a02" sha1="a7b8aa7f3cd86b664f314650206d9043806e9b22"/>
	</game>
```

This DAT uses the [No-Intro Naming Convention](https://wiki.no-intro.org/index.php?title=Naming_Convention).

The release entries suggest this game should be considered the regions of Italy and Spain, but the name suggests its region is Europe.

There is also no information on language other than the name.

Other tools often make use of only one source of information. This means the ROM above would _not_ be selected in a query like "select all games with an European release". 

The lack of language information also makes impossible to do something like "select all games with releases from the USA, Europe and Japan, but only if they're in English or German".

Currently, it supports:

- Parsing DAT files
