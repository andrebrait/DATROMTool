### Updating the DTD

The base DTD file must be updated and committed alongside the converted XSD and the generated classes.

The XSD is only used for testing purposes, validating the output DAT XML against it.

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

