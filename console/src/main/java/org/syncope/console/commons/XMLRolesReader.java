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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * XMLRolesReader singleton class.
 */
public class XMLRolesReader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            XMLRolesReader.class);

    @Autowired
    private String authorizations;

    private Document doc;

    public void init() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(getClass().getResource("/" + authorizations).
                    openStream());
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            LOG.error("While initializing parsing of {}", authorizations, e);
            doc = null;
        }
    }

    /**
     * Get all roles allowed for specific page and actio requested.
     *
     * @param pageId
     * @param actionId
     * @return roles list comma separated
     */
    public String getAllAllowedRoles(final String pageId,
            final String actionId) {

        if (doc == null) {
            init();
        }
        if (doc == null) {
            return "";
        }

        final StringBuilder roles = new StringBuilder();
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(
                    "//page[@id='" + pageId + "']/"
                    + "action[@id='" + actionId + "']/"
                    + "entitlement/text()");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            NodeList nodes = (NodeList) result;

            for (int i = 0; i < nodes.getLength(); i++) {
                if (i > 0) {
                    roles.append(",");
                }
                roles.append(nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            LOG.error("While parsing authorizations file", e);
        }

        LOG.debug("Authorizations found: {}", roles);

        return roles.toString();
    }
}
