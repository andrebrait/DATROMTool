
package io.github.datromtool.generated.headers;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for logicaltest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="logicaltest">
 *   &lt;complexContent>
 *     &lt;extension base="{}binarytest">
 *       &lt;attribute name="mask">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="[a-fA-F0-9]+"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "logicaltest")
@XmlSeeAlso({
    Xor.class,
    And.class,
    Or.class
})
@ToString
@EqualsAndHashCode
public class Logicaltest
    extends Binarytest
{

    @XmlAttribute(name = "mask")
    protected String mask;

    /**
     * Gets the value of the mask property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMask() {
        return mask;
    }

    /**
     * Sets the value of the mask property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMask(String value) {
        this.mask = value;
    }

}
