### Updating XML schemas

The base DTD files must be updated and committed alongside the converted XSD.

The XSD files are only used for testing purposes, validating the output DAT XML against them.

### How to convert DTD to XSD

Using IntelliJ IDEA, open the DTD in the editor and:

1. Go to `Tools` -> `XML Actions` -> `Convert Schema`
2. Select `W3C XML Schema`
3. Go to `Advanced` and check `inline-attlist`
4. Click the `Convert` button and allow it to replace the existing XSD file
5. Check the diff for the new XSD file
6. Run `mvn clean test`

Commit the generated XSD.

_Note: IntelliJ uses [trang](https://relaxng.org/jclark/trang.html) to perform the conversion, so using it directly is also an option._

### Changing the code

Despite solutions for automatically compiling either the DTD or the XSD to Java classes,
this process does not result in the cleanest code out there. We therefore prefer combining
manual changes to automated testing to ensure DATROMTool can read and create proper XML files
in all of its supported formats.