
package io.github.datromtool.generated.datafile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="header" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="forcemerging" default="split">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="none"/>
 *             &lt;enumeration value="split"/>
 *             &lt;enumeration value="full"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="forcenodump" default="obsolete">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="obsolete"/>
 *             &lt;enumeration value="required"/>
 *             &lt;enumeration value="ignore"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="forcepacking" default="zip">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="zip"/>
 *             &lt;enumeration value="unzip"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "clrmamepro")
public class Clrmamepro {

    @XmlAttribute(name = "header")
    protected String header;
    @XmlAttribute(name = "forcemerging")
    protected String forcemerging;
    @XmlAttribute(name = "forcenodump")
    protected String forcenodump;
    @XmlAttribute(name = "forcepacking")
    protected String forcepacking;

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHeader(String value) {
        this.header = value;
    }

    /**
     * Gets the value of the forcemerging property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForcemerging() {
        if (forcemerging == null) {
            return "split";
        } else {
            return forcemerging;
        }
    }

    /**
     * Sets the value of the forcemerging property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForcemerging(String value) {
        this.forcemerging = value;
    }

    /**
     * Gets the value of the forcenodump property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForcenodump() {
        if (forcenodump == null) {
            return "obsolete";
        } else {
            return forcenodump;
        }
    }

    /**
     * Sets the value of the forcenodump property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForcenodump(String value) {
        this.forcenodump = value;
    }

    /**
     * Gets the value of the forcepacking property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForcepacking() {
        if (forcepacking == null) {
            return "zip";
        } else {
            return forcepacking;
        }
    }

    /**
     * Sets the value of the forcepacking property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForcepacking(String value) {
        this.forcepacking = value;
    }

}
