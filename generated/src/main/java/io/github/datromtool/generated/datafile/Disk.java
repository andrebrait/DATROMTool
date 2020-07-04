
package io.github.datromtool.generated.datafile;

import lombok.EqualsAndHashCode;
import lombok.ToString;

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
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sha1" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="md5" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="merge" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="status" default="good">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="baddump"/>
 *             &lt;enumeration value="nodump"/>
 *             &lt;enumeration value="good"/>
 *             &lt;enumeration value="verified"/>
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
@XmlRootElement(name = "disk")
@ToString
@EqualsAndHashCode
public class Disk {

    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "sha1")
    protected String sha1;
    @XmlAttribute(name = "md5")
    protected String md5;
    @XmlAttribute(name = "merge")
    protected String merge;
    @XmlAttribute(name = "status")
    protected String status;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the sha1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Sets the value of the sha1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSha1(String value) {
        this.sha1 = value;
    }

    /**
     * Gets the value of the md5 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMd5() {
        return md5;
    }

    /**
     * Sets the value of the md5 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMd5(String value) {
        this.md5 = value;
    }

    /**
     * Gets the value of the merge property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMerge() {
        return merge;
    }

    /**
     * Sets the value of the merge property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMerge(String value) {
        this.merge = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        if (status == null) {
            return "good";
        } else {
            return status;
        }
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

}
