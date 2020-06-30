
package io.github.datromtool.generated.datafile;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element ref="{}comment" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}description"/>
 *         &lt;element ref="{}year" minOccurs="0"/>
 *         &lt;element ref="{}manufacturer" minOccurs="0"/>
 *         &lt;element ref="{}release" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}biosset" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}rom" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}disk" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}sample" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}archive" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sourcefile" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="isbios" default="no">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="yes"/>
 *             &lt;enumeration value="no"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="cloneof" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="romof" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sampleof" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="board" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="rebuildto" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "comment",
    "description",
    "year",
    "manufacturer",
    "release",
    "biosset",
    "rom",
    "disk",
    "sample",
    "archive"
})
@XmlRootElement(name = "game")
public class Game {

    protected List<String> comment;
    @XmlElement(required = true)
    protected String description;
    protected String year;
    protected String manufacturer;
    protected List<Release> release;
    protected List<Biosset> biosset;
    protected List<Rom> rom;
    protected List<Disk> disk;
    protected List<Sample> sample;
    protected List<Archive> archive;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "sourcefile")
    protected String sourcefile;
    @XmlAttribute(name = "isbios")
    protected String isbios;
    @XmlAttribute(name = "cloneof")
    protected String cloneof;
    @XmlAttribute(name = "romof")
    protected String romof;
    @XmlAttribute(name = "sampleof")
    protected String sampleof;
    @XmlAttribute(name = "board")
    protected String board;
    @XmlAttribute(name = "rebuildto")
    protected String rebuildto;

    /**
     * Gets the value of the comment property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the comment property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getComment().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getComment() {
        if (comment == null) {
            comment = new ArrayList<String>();
        }
        return this.comment;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the year property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getYear() {
        return year;
    }

    /**
     * Sets the value of the year property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setYear(String value) {
        this.year = value;
    }

    /**
     * Gets the value of the manufacturer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Sets the value of the manufacturer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManufacturer(String value) {
        this.manufacturer = value;
    }

    /**
     * Gets the value of the release property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the release property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelease().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Release }
     * 
     * 
     */
    public List<Release> getRelease() {
        if (release == null) {
            release = new ArrayList<Release>();
        }
        return this.release;
    }

    /**
     * Gets the value of the biosset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the biosset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBiosset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Biosset }
     * 
     * 
     */
    public List<Biosset> getBiosset() {
        if (biosset == null) {
            biosset = new ArrayList<Biosset>();
        }
        return this.biosset;
    }

    /**
     * Gets the value of the rom property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rom property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRom().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Rom }
     * 
     * 
     */
    public List<Rom> getRom() {
        if (rom == null) {
            rom = new ArrayList<Rom>();
        }
        return this.rom;
    }

    /**
     * Gets the value of the disk property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the disk property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisk().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Disk }
     * 
     * 
     */
    public List<Disk> getDisk() {
        if (disk == null) {
            disk = new ArrayList<Disk>();
        }
        return this.disk;
    }

    /**
     * Gets the value of the sample property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sample property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSample().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sample }
     * 
     * 
     */
    public List<Sample> getSample() {
        if (sample == null) {
            sample = new ArrayList<Sample>();
        }
        return this.sample;
    }

    /**
     * Gets the value of the archive property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the archive property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArchive().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Archive }
     * 
     * 
     */
    public List<Archive> getArchive() {
        if (archive == null) {
            archive = new ArrayList<Archive>();
        }
        return this.archive;
    }

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
     * Gets the value of the sourcefile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourcefile() {
        return sourcefile;
    }

    /**
     * Sets the value of the sourcefile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourcefile(String value) {
        this.sourcefile = value;
    }

    /**
     * Gets the value of the isbios property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsbios() {
        if (isbios == null) {
            return "no";
        } else {
            return isbios;
        }
    }

    /**
     * Sets the value of the isbios property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsbios(String value) {
        this.isbios = value;
    }

    /**
     * Gets the value of the cloneof property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCloneof() {
        return cloneof;
    }

    /**
     * Sets the value of the cloneof property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCloneof(String value) {
        this.cloneof = value;
    }

    /**
     * Gets the value of the romof property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRomof() {
        return romof;
    }

    /**
     * Sets the value of the romof property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRomof(String value) {
        this.romof = value;
    }

    /**
     * Gets the value of the sampleof property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSampleof() {
        return sampleof;
    }

    /**
     * Sets the value of the sampleof property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSampleof(String value) {
        this.sampleof = value;
    }

    /**
     * Gets the value of the board property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBoard() {
        return board;
    }

    /**
     * Sets the value of the board property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBoard(String value) {
        this.board = value;
    }

    /**
     * Gets the value of the rebuildto property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRebuildto() {
        return rebuildto;
    }

    /**
     * Sets the value of the rebuildto property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRebuildto(String value) {
        this.rebuildto = value;
    }

}
