/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.cli.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.cxf.helpers.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XMLUtils {

    public static void createXMLFile(final InputStream sis, final String filePath)
            throws TransformerException, SAXException, IOException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        
        tf.newTransformer().
                transform(new DOMSource(dbf.newDocumentBuilder().
                        parse(new InputSource(new StringReader(IOUtils.toString(sis))))),
                        new StreamResult(new File(filePath)));
    }

    private XMLUtils() {
        // private constructor for static utility class
    }
}
