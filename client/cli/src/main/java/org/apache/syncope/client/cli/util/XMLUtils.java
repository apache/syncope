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
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

public final class XMLUtils {

    public static void createXMLFile(final InputStream sis, final String filePath) throws Exception {
        DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domImpl = (DOMImplementationLS) reg.getDOMImplementation("LS");
        LSInput lsinput = domImpl.createLSInput();
        lsinput.setByteStream(sis);
        LSParser parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        tf.newTransformer().transform(new DOMSource(parser.parse(lsinput)), new StreamResult(new File(filePath)));
    }

    private XMLUtils() {
        // private constructor for static utility class
    }
}
