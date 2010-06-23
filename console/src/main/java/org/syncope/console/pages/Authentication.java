/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.io.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Authentication {

    static public String nodo;
    static public Writer out;
    public Boolean authenticated = false;
    public String xmlUsername;
    public String xmlPassword;

    public boolean authentication(String username, String password, InputStream inputStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                boolean bfname = false;
                boolean blname = false;
                boolean bnname = false;
                boolean bsalary = false;

                @Override
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    if (qName.equalsIgnoreCase("username")) {
                        bfname = true;
                    }
                    if (qName.equalsIgnoreCase("password")) {
                        blname = true;
                    }
                }

                @Override
                public void endElement(String uri, String localName,
                        String qName)
                        throws SAXException {
                }

                @Override
                public void characters(char ch[], int start, int length)
                        throws SAXException {
                    if (bfname) {
                        xmlUsername = new String(ch, start, length);
                        bfname = false;
                    }
                    if (blname) {
                        xmlPassword = new String(ch, start, length);
                        blname = false;
                    }
                }
            };

            
            saxParser.parse(inputStream, handler);
            if (username.equals(xmlUsername) && password.equals(xmlPassword)) {
                authenticated = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return authenticated;
    }
}
