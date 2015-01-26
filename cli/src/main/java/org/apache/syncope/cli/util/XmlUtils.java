package org.apache.syncope.cli.util;

import java.io.File;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.cxf.helpers.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtils {

    public static void createXMLFile(SequenceInputStream sis, String filePath)
            throws TransformerConfigurationException, TransformerException, SAXException, IOException,
            ParserConfigurationException {
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                                        new InputSource(new StringReader(IOUtils.toString(sis))))),
                        new StreamResult(new File(filePath)));
    }
}
