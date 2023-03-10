XML driven header support
=========================

>This is a Markdown version of [the original document](https://mamedev.emulab.it/clrmamepro/docs/xmlheaders.txt).

So...what's the main problem with headers anyway? They have an impact on the
file's hash values, like crc32, sha1 or md5. A used datfile usually lists only
the hash value of the pure rom data while the header isn't included. Before
clrmamepro 3.90, the program calculated checksums for a full file only, so if
a header was present, it was included in the calculation and you ended in a
different hash value.

With clrmamepro 3.90 onwards, the program can do additional checks to detect
headers, skip them and calculated the hashes over the pure data.

Now how are such checks defined? Do I have to code a plugin for clrmamepro?

While thinking about a way to support headers, I quickly dropped the idea of a
plugin system (not to mention the security risks which generally arise with
plugins), Mike (Logiqx) came up with the idea of defining header detection
rules in XML. I liked the idea right away from his mail and thought about what
is needed to detect headers and how to describe these operations in XML.

Generally you need to support some test(s) which return you a start and end
offset of the real data block of a given file. Maybe additionally operations
which do byte or wordswaps on that resulting block. clrmamepro will then only
calculate the hash values on that 'real data' block.



How to use the header support:
==============================


What do I need?
---------------

If you got a datfile for a system which commonly comes with headers in its
files, you need a XML file which describes that system, what to skip, what to
keep, etc. For the syntax of such files, look at the end of this little
textfile.


Where to place such header definitions?
---------------------------------------

If you use clrmamepro 3.90 for the first time, you'll see that a new subfolder
was created in your clrmamepro folder. That folder is named `headers`. That's
the place where clrmamepro looks for `*.XML` files at startup. So if anyone
provides XML header detection files, put them in there and restart clrmamepro
to get them loaded.


Ok I got XML files in there, how do I use them?
-----------------------------------------------

If you start clrmamepro and go to the settings window (after loading a profile
), you'll find a new entry `headers` in the combo box you usually use for
rompath, etc. selection. If you select that entry, you'll see all currently
available header formats listed. You can enable/disable them there as well.
This is a per-profile option and your enable/disable selection is remembered
in the currently loaded profile.

If you don't see one of your files in there, it probably didn't make it through
a validation check. Contact the author of that XML file and ask what's wrong
with it.

Every activated header support will then be used when a checksum of a file is
calculated. This includes Scanner's checksum check, scanner's name check, the
rebuilder, the merger and several other stuff.

Of course usually you should know which header support should be enabled for
which profile/datfile. And of course you should only enable exactly THAT used
format, like a NES header detector for a NES datfile.


Some additional information:
----------------------------

Header detection returns a file size value which defines the size of the 'real
data' in the file. So generally this differs from the original file size. So
a datfile which uses files with headers should list either no size information
at all (size `-`) or should use the size value of that data block (without the
header).


In which parts does the header detector play a role?
-----------------------------------------------------

Currently the variable hash calculation is active in: Scanner, Rebuilder,
Merger and Dir2Dat. Keep in mind, it will have an impact on file sizes too,
since if a header was detected the file size is set to the size of the 'real'
data block.


Performance:
------------

Generally header detection doesn't come for free. If you work on unzipped
files, it's just some additional checks but if you scan zipped files, internal
data decompressing data is needed which takes some time.



That's it basically for the common user. Now header-gurus should read on. The
interesting part is coming. How do I write such a header XML file?



XML format
==========

the general look of such a XML file:

```xml
<?xml version="1.0"?>

<detector>

    <name>...</name>
    <author>...</author>
    <version>...</version>

    <rule attrib="...">

        <test attrib="..."/>
        <test attrib="..."/>
        ...

    </rule>

    <rule attrib="...">...</rule>
    <rule attrib="...">...</rule>
    ...

</detector>
```


So you got -besides some metainformation- a list of rules which include a list
of tests.

A rule is 'fulfilled' if all (logical `AND`) tests of that rule succeed. Single
rules are connected with a logical `OR`. If a rule is fulfilled, the rule
attributes define the real datablock, ie skip the header/footer part.

As soon as a rule is fulfilled, no more rules are tested. If no rule can be
applied successfully to the current file, the default values (start = `0`, end =
`EOF`) are used.


Detector:
---------

* `<name>` (required)

  an unique name for the system. Like 'NES'. This name is shown in the settings
  window where you list the headers. Since it's a unique name, it shouldn't get
  altered without a reason.

* `<author>` (optional)

  author specific information, maybe also a contact address to report issues.

* `<version>` (optional)

  some information about the version, the status of the XML file.

* `<rule>` (required)

  at least one rule has to be specified. The rule element holds information
  about the 'real data' start/end and a possible operation on the data.


Rules:
------

Rules can contain tests (we come to that later...). If a rule is fulfilled,
the given start and end offset values are used for later hash calculation.

Example:

```xml
<rule start_offset="80" end_offset="-800" operation="byteswap">...</rule>
```

  - start_offset (optional, default = 0)
    hexadecimal value (max 64bit) which gives the real data start

  - end_offset (optional, default = `EOF`)
    hexadecimal value (max 64bit) which gives the real data end

  - you can use `EOF` to specify the EndOfFile. For example:
    ```xml
    <rule start_offset="0" end_offset="EOF">...</rule>
    ```

  - you can use negative offsets to indicate an offset relatively taken to the
    end of the file. You have to use `-` for that, e.g. `end_offset="-800"` means
    `0x800` = 2048 bytes from the end.

  - operation: `none`, `bitswap`, `byteswap`, `wordswap`, `wordbyteswap` (optional, default = none)
    `none` does nothing to the data before the hash is calculated.

    `byteswap` performs a byte swap (or 16bit word swap if you like that term
    better). Bytesequence `01|02` becomes `02|01`. An even filesize is required.

    `wordswap` performs a 32bit wordswap: Bytes `01|02|03|04` become
    `04|03|02|01`. Filesize mod 4 = 0 is required.

    `bitswap` swaps higher with lower bits: 7 -> 0, 6 -> 1 etc.

    `wordbyteswap` performs a 32bit wordswap and byteswap: Bytes `01|02|03|04` become
    `03|04|01|02`. Filesize mod 4 = 0 is required.


So with that basic knowledge about rules we can already define a constant
header skip:

Example:
```xml
<rule start_offset="80" end_offset="EOF"/>
```

  Always skips the first `0x80` bytes and the hash is calculated over the rest.
  A rule without any tests is always 'fulfilled'.


Tests:
------

Rules can contain 0, 1 or more test statements which actually 'test' things.
For one rule all tests have to signal `true` to fulfill the rule.

There are 3 groups of possible tests: data tests, boolean tests and file tests

  - data tests:

    Examples:
    
    ```xml
    <rule>
        <data offset="1" value="415441524937383030" result="true"/>
        <data offset="-200" value="08AF" result="false"/>
    </rule>
    ```

    offset (optional, hex value (max 64bit), default = 0)
    value  (required, hex value, something != `""`)
    result (optional, true|false, default = true)

    offset can be negative (to be relative to the end of the file) and it can
    be set to `EOF` to specify the end of file.

    Pretty easy syntax here. Offset attribute gives a hexadecimal offset to
    test for a given byte sequence which is given in the value attribute.

    In the first upper example we seek offset 1 in the file, read 9 bytes (byte
    size of value attribute) and compare the read bytes with the value given in
    the value attribute. If they match, the test signals `true`.

    The result attribute can be used to invert the result of the test.
    So if you like to test for e.g. a `value != 12`, you set `value="12"` and
    `result="false"`.


  - boolean tests:

    Examples:
    ```xml
    <rule>
        <or offset="10" mask="1f54" value="4154" result="true"/>
        <xor offset="10" mask="1f54" value="4154" result="true"/>
        <and offset="10" mask="1f54" value="4154" result="true"/>
    </rule>
    ```

    The value, offset and result attributes are used in the same way as in data
    tests (including `EOF` and negative offsets). The mask attribute however
    defines a bitmask which is applied (byte by byte) to the read data before
    it's compared to value bytesquence. Depending on the used test, either a
    bitwise `OR`, `XOR` or `AND` operation is performed. The byte size of the mask
    has to be identical to the byte size of the value bytesequence.


  - file tests:

    Examples:
    
    ```xml
    <rule>
        <file size="1000" result="true" operator="less"/>
        <file size="PO2" result="false"/>
    </rule>
    ```

    size (required, `PO2` or a hexvalue)
    result (as mentioned above)
    operator (optional, equal|less|greater, default = equal)

    A file test can test a file for a given file size. The size attribute holds
    the hexvalue (in the first example 4096 bytes = `0x1000`) which should be
    tested. You can also use `PO2` instead of a numeric file size which means
    you test the file against a PowerOf2 file size (1, 2, 4,..., 1024, 2048..).

    With the operators you can define to use equality, a lesser or greater
    filesize check. Operators don't play a role if you test on `PO2`.


Things to remember:
-------------------

- offsets are always positive hexadecimal values (max. 64bit)
- a `-` in front of the offset indicates an offset relatively used to `EOF`
- offsets set to `EOF` define the physical end of a file
- mask and value attributes have to be equally sized
- result attribute can be used to invert the test result
- a rule is not fulfilled as soon as a test fails
- a detector is fulfilled as soon as a rule succeeds
- if no rule is fulfilled or illegal seeks/reads are performed, the default
  (start = 0, end = `EOF`) is used for hash calculation



Examples:
=========

Well...now you know the basics, let's continue with some simple examples:


Example 1:
----------
```xml
<rule start_offset="80" end_offset="EOF">
    <data offset="64" value="41435455414C2043" result="true"/>
    <file size="PO2" result="false"/>
</rule>
```

```xml
<rule start_offset="0" end_offset="EOF">
    <file size="PO2" result="true"/>
</rule>
```

These two rules check

(offset `0x64` for bytesequence `41435455414C2043`
  AND filesize is NOT a power of 2)
OR (filesize IS power of two)

Depending on which rule applies, the start offset is set to `0x80` or `0x0` and
the end is always the end of file.


Example 2:
----------

```xml
<rule start_offset="10" end_offset="-40">
    <and offset="-2" mask="f0" value="20" result="false"/>
</rule>
```

We look at offset (`EOF` - `0x2`), read one byte, do a bitwise AND with `0xf0` and
compare the result against `0x20`. If that's NOT true, the real datablock of
the file starts at offset `0x10` and ends at `EOF-0x40`.


Example 3:
----------

```xml
<rule start_offset="0" end_offset="EOF" operation="wordswap">
    <data offset="0" value="504b0304" result="true"/>
</rule>
```

If we find a byte sequence of `0x504b0304` at offset 0, we use the full file for
hash calculation but we perform a 32bit word swap before the actual calculation
is done.


Example 4:
----------

```xml
<detector>
    <rule start_offset="80" end_offset="EOF">
        <data offset="64" value="41" result="true"/>
    </rule>
    <rule start_offset="80" end_offset="EOF">
        <data offset="64" value="42" result="true"/>
    </rule>
    <rule start_offset="80" end_offset="EOF">
        <data offset="64" value="43" result="true"/>
    </rule>
    <rule start_offset="80" end_offset="EOF">
        <data offset="64" value="44" result="true"/>
    </rule>
</detector>
```

If the byte at offset `0x64` is either 41, 42, 43 or 44, we set the start to `0x80`.


Example 5:
----------

```xml
<rule start_offset="0" end_offset="EOF">
    <file size="PO2" result="false"/>
    <file size="1000" result="true" operator="greater"/>
</rule>
```

If the file size isn't a power of two and it is greater than `0x1000` bytes then
we take the whole file.


Example 6:
----------

```xml
<rule start_offset="80" end_offset="-80" operation="byteswap">
    <data offset="64" value="41435455414C2043" result="true"/>
    <xor offset="20" mask="f0f0f0f0f0" value="2020202020" result="false"/>
    <file size="PO2" result="false"/>
</rule>
```

Well...pretty much nonsense...but possible ;) Any useful and not so useful
combination can be used and combined. Feel free to play with it.


Forcing datfiles to use headers:
--------------------------------

By specifying the xml file name in the datfile header (with tag `header`), you
can bind a datfile to this definition file. If the xml file isn't available or
not enabled, you'll see a warning after loading the datfile.
Example:

```
header nes.xml
```


Hints:
------

Make use of the fact that rules and tests are tested in the order how they
are written. As soon as a rule is fulfilled the others are skipped and as
soon as a test fails, the others are skipped, too.

If you try to write a description to work on so called overdumps you may try
to add several rules which test for different filesize (decreasing order) and
set a correct as start/end offsets. Like if test for > 64k succeeds, limit it
to 64k, otherwise test for >32k,...>16k and so on.



Credits
=======

- Many thanks to Loqigx who came up with the XML idea and had some other nice
  ideas what's needed in it and Cowering for telling me what he needs and how
  some header detectors actually work.
