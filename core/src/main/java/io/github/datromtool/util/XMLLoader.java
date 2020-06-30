package io.github.datromtool.util;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class XMLLoader<T> {

    private final Class<T> tClass;
    private final SAXParserFactory saxParserFactory;
    private final Unmarshaller unmarshaller;

    private XMLLoader(
            Class<T> tClass,
            SAXParserFactory saxParserFactory,
            Unmarshaller unmarshaller) {
        this.tClass = tClass;
        this.saxParserFactory = saxParserFactory;
        this.unmarshaller = unmarshaller;
    }

    public static <T> XMLLoader<T> forClass(Class<T> tClass) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(tClass);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        spf.setValidating(false);
        return new XMLLoader<>(tClass, spf, jc.createUnmarshaller());
    }

    public T unmarshal(Path xml) throws Exception {
        XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
        try (BufferedReader bufferedReader = Files.newBufferedReader(xml)) {
            InputSource inputSource = new InputSource(bufferedReader);
            SAXSource source = new SAXSource(xmlReader, inputSource);
            return tClass.cast(unmarshaller.unmarshal(source));
        }
    }

}
