
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
 *       &lt;attribute name="plugin" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="rommode" default="split">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="merged"/>
 *             &lt;enumeration value="split"/>
 *             &lt;enumeration value="unmerged"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="biosmode" default="split">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="merged"/>
 *             &lt;enumeration value="split"/>
 *             &lt;enumeration value="unmerged"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="samplemode" default="merged">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="merged"/>
 *             &lt;enumeration value="unmerged"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="lockrommode" default="no">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="yes"/>
 *             &lt;enumeration value="no"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="lockbiosmode" default="no">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="yes"/>
 *             &lt;enumeration value="no"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="locksamplemode" default="no">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="yes"/>
 *             &lt;enumeration value="no"/>
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
@XmlRootElement(name = "romcenter")
public class Romcenter {

    @XmlAttribute(name = "plugin")
    protected String plugin;
    @XmlAttribute(name = "rommode")
    protected String rommode;
    @XmlAttribute(name = "biosmode")
    protected String biosmode;
    @XmlAttribute(name = "samplemode")
    protected String samplemode;
    @XmlAttribute(name = "lockrommode")
    protected String lockrommode;
    @XmlAttribute(name = "lockbiosmode")
    protected String lockbiosmode;
    @XmlAttribute(name = "locksamplemode")
    protected String locksamplemode;

    /**
     * Gets the value of the plugin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlugin() {
        return plugin;
    }

    /**
     * Sets the value of the plugin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlugin(String value) {
        this.plugin = value;
    }

    /**
     * Gets the value of the rommode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRommode() {
        if (rommode == null) {
            return "split";
        } else {
            return rommode;
        }
    }

    /**
     * Sets the value of the rommode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRommode(String value) {
        this.rommode = value;
    }

    /**
     * Gets the value of the biosmode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBiosmode() {
        if (biosmode == null) {
            return "split";
        } else {
            return biosmode;
        }
    }

    /**
     * Sets the value of the biosmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBiosmode(String value) {
        this.biosmode = value;
    }

    /**
     * Gets the value of the samplemode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSamplemode() {
        if (samplemode == null) {
            return "merged";
        } else {
            return samplemode;
        }
    }

    /**
     * Sets the value of the samplemode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSamplemode(String value) {
        this.samplemode = value;
    }

    /**
     * Gets the value of the lockrommode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLockrommode() {
        if (lockrommode == null) {
            return "no";
        } else {
            return lockrommode;
        }
    }

    /**
     * Sets the value of the lockrommode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLockrommode(String value) {
        this.lockrommode = value;
    }

    /**
     * Gets the value of the lockbiosmode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLockbiosmode() {
        if (lockbiosmode == null) {
            return "no";
        } else {
            return lockbiosmode;
        }
    }

    /**
     * Sets the value of the lockbiosmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLockbiosmode(String value) {
        this.lockbiosmode = value;
    }

    /**
     * Gets the value of the locksamplemode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocksamplemode() {
        if (locksamplemode == null) {
            return "no";
        } else {
            return locksamplemode;
        }
    }

    /**
     * Sets the value of the locksamplemode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocksamplemode(String value) {
        this.locksamplemode = value;
    }

}
