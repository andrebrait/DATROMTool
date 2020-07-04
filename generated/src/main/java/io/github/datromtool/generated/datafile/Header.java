
package io.github.datromtool.generated.datafile;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element ref="{}name"/>
 *         &lt;element ref="{}description"/>
 *         &lt;element ref="{}category" minOccurs="0"/>
 *         &lt;element ref="{}version"/>
 *         &lt;element ref="{}date" minOccurs="0"/>
 *         &lt;element ref="{}author"/>
 *         &lt;element ref="{}email" minOccurs="0"/>
 *         &lt;element ref="{}homepage" minOccurs="0"/>
 *         &lt;element ref="{}url" minOccurs="0"/>
 *         &lt;element ref="{}comment" minOccurs="0"/>
 *         &lt;element ref="{}clrmamepro" minOccurs="0"/>
 *         &lt;element ref="{}romcenter" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "description",
    "category",
    "version",
    "date",
    "author",
    "email",
    "homepage",
    "url",
    "comment",
    "clrmamepro",
    "romcenter"
})
@XmlRootElement(name = "header")
@ToString
@EqualsAndHashCode
public class Header {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(required = true)
    protected String description;
    protected String category;
    @XmlElement(required = true)
    protected String version;
    protected String date;
    @XmlElement(required = true)
    protected String author;
    protected String email;
    protected String homepage;
    protected String url;
    protected String comment;
    protected Clrmamepro clrmamepro;
    protected Romcenter romcenter;

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
     * Gets the value of the category property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCategory(String value) {
        this.category = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDate(String value) {
        this.date = value;
    }

    /**
     * Gets the value of the author property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the value of the author property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuthor(String value) {
        this.author = value;
    }

    /**
     * Gets the value of the email property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the value of the email property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmail(String value) {
        this.email = value;
    }

    /**
     * Gets the value of the homepage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the value of the homepage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHomepage(String value) {
        this.homepage = value;
    }

    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

    /**
     * Gets the value of the clrmamepro property.
     * 
     * @return
     *     possible object is
     *     {@link Clrmamepro }
     *     
     */
    public Clrmamepro getClrmamepro() {
        return clrmamepro;
    }

    /**
     * Sets the value of the clrmamepro property.
     * 
     * @param value
     *     allowed object is
     *     {@link Clrmamepro }
     *     
     */
    public void setClrmamepro(Clrmamepro value) {
        this.clrmamepro = value;
    }

    /**
     * Gets the value of the romcenter property.
     * 
     * @return
     *     possible object is
     *     {@link Romcenter }
     *     
     */
    public Romcenter getRomcenter() {
        return romcenter;
    }

    /**
     * Sets the value of the romcenter property.
     * 
     * @param value
     *     allowed object is
     *     {@link Romcenter }
     *     
     */
    public void setRomcenter(Romcenter value) {
        this.romcenter = value;
    }

}
