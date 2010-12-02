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

package org.syncope.console.commons;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XMLRolesReader singleton class.
 */
public class XMLRolesReader {
    private String authFilename;

    public XMLRolesReader() {
    }

    /**
     * Class constructor.
     * @param authFilename - name of the authorizations file placed in
     * "/WEB-INF/classes"
     * @throws SAXException
     * @throws IOException
     */
    public XMLRolesReader(String authFilename) throws SAXException, IOException {

        this.authFilename = authFilename;

    }

    /**
     * Get all roles allowed for specific page and actio requested.
     *
     * @param pageId
     * @param actionId
     * @return roles list comma separated
     */
    public String getAllAllowedRoles(String pageId,String actionId) {
    String roles = "";

    Document doc = null;
    
        try {
            doc = getDocumentBuilder();
        } catch (SAXException ex) {
            Logger.getLogger(XMLRolesReader.class.getName()).log(Level.SEVERE,
                    null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(XMLRolesReader.class.getName()).log(Level.SEVERE,
                    null, ex);
            return null;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XMLRolesReader.class.getName()).log(Level.SEVERE,
                    null, ex);
            return null;
        }

    doc.getDocumentElement().normalize();

    try {

    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    XPathExpression expr
     = xpath.compile("//page[@id='"+pageId+"']/action[@id='"+actionId+"']/" +
                     "entitlement/text()");
    Object result = expr.evaluate(doc, XPathConstants.NODESET);

    NodeList nodes = (NodeList) result;

    for (int i = 0; i < nodes.getLength(); i++) {
        if(i > 0) {
        roles += ",";
        roles += nodes.item(i).getNodeValue();     
        }
        else
            roles += nodes.item(i).getNodeValue();
    }

    }
    catch(XPathExpressionException ex) {
         Logger.getLogger(XMLRolesReader.class.getName()).log(Level.SEVERE,
                    null, ex);
    }
    
    //System.out.println("Roles : " + roles);

    return roles;
    }

    public Document getDocumentBuilder() throws SAXException, IOException,
            ParserConfigurationException {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(getClass().getResource("/"+authFilename)
            .openStream());

    return doc;
    }

}
