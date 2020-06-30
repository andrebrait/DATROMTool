
package io.github.datromtool.generated.headers;

import java.util.ArrayList;
import java.util.List;
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
 *       &lt;sequence>
 *         &lt;element ref="{}data" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}or" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}xor" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}and" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}file" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="start_offset" default="0">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="-?[a-fA-F0-9]+"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="end_offset" default="EOF">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="(EOF)|(-?[a-fA-F0-9]+)"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="operation" default="none">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="none"/>
 *             &lt;enumeration value="bitswap"/>
 *             &lt;enumeration value="byteswap"/>
 *             &lt;enumeration value="wordswap"/>
 *             &lt;enumeration value="wordbyteswap"/>
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
@XmlType(name = "", propOrder = {
    "data",
    "or",
    "xor",
    "and",
    "file"
})
@XmlRootElement(name = "rule")
public class Rule {

    protected List<Data> data;
    protected List<Or> or;
    protected List<Xor> xor;
    protected List<And> and;
    protected List<File> file;
    @XmlAttribute(name = "start_offset")
    protected String startOffset;
    @XmlAttribute(name = "end_offset")
    protected String endOffset;
    @XmlAttribute(name = "operation")
    protected String operation;

    /**
     * Gets the value of the data property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the data property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Data }
     * 
     * 
     */
    public List<Data> getData() {
        if (data == null) {
            data = new ArrayList<Data>();
        }
        return this.data;
    }

    /**
     * Gets the value of the or property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the or property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOr().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Or }
     * 
     * 
     */
    public List<Or> getOr() {
        if (or == null) {
            or = new ArrayList<Or>();
        }
        return this.or;
    }

    /**
     * Gets the value of the xor property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xor property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Xor }
     * 
     * 
     */
    public List<Xor> getXor() {
        if (xor == null) {
            xor = new ArrayList<Xor>();
        }
        return this.xor;
    }

    /**
     * Gets the value of the and property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the and property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnd().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link And }
     * 
     * 
     */
    public List<And> getAnd() {
        if (and == null) {
            and = new ArrayList<And>();
        }
        return this.and;
    }

    /**
     * Gets the value of the file property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the file property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link File }
     * 
     * 
     */
    public List<File> getFile() {
        if (file == null) {
            file = new ArrayList<File>();
        }
        return this.file;
    }

    /**
     * Gets the value of the startOffset property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStartOffset() {
        if (startOffset == null) {
            return "0";
        } else {
            return startOffset;
        }
    }

    /**
     * Sets the value of the startOffset property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStartOffset(String value) {
        this.startOffset = value;
    }

    /**
     * Gets the value of the endOffset property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEndOffset() {
        if (endOffset == null) {
            return "EOF";
        } else {
            return endOffset;
        }
    }

    /**
     * Sets the value of the endOffset property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEndOffset(String value) {
        this.endOffset = value;
    }

    /**
     * Gets the value of the operation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperation() {
        if (operation == null) {
            return "none";
        } else {
            return operation;
        }
    }

    /**
     * Sets the value of the operation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperation(String value) {
        this.operation = value;
    }

}
