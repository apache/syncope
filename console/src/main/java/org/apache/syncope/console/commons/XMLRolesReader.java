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
package org.apache.syncope.console.commons;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XMLRolesReader singleton class.
 */
public class XMLRolesReader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(XMLRolesReader.class);

    private String authorizations;

    private Map<Pair<String, String>, String> authMap;

    public void setAuthorizations(final String authorizations) {
        this.authorizations = authorizations;
    }

    private void init() {
        authMap = new HashMap<Pair<String, String>, String>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(getClass().getResource("/" + authorizations).openStream());
            doc.getDocumentElement().normalize();

            Node authNode = null;
            NodeList root = doc.getChildNodes();
            for (int i = 0; i < root.getLength() && authNode == null; i++) {
                if ("auth".equals(root.item(i).getNodeName())) {
                    authNode = root.item(i);
                }
            }
            if (authNode == null) {
                throw new IllegalArgumentException("Could not find root <auth> node");
            }

            NodeList pages = authNode.getChildNodes();
            for (int i = 0; i < pages.getLength(); i++) {
                if ("page".equals(pages.item(i).getNodeName())) {
                    String page = pages.item(i).getAttributes().getNamedItem("id").getTextContent();

                    NodeList actions = pages.item(i).getChildNodes();
                    for (int j = 0; j < actions.getLength(); j++) {
                        if ("action".equals(actions.item(j).getNodeName())) {
                            String action = actions.item(j).getAttributes().getNamedItem("id").getTextContent();

                            NodeList entitlements = actions.item(j).getChildNodes();
                            for (int k = 0; k < entitlements.getLength(); k++) {
                                if ("entitlement".equals(entitlements.item(k).getNodeName())) {
                                    String entitlement = entitlements.item(k).getTextContent();
                                    authMap.put(new ImmutablePair<String, String>(page, action), entitlement);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("While initializing parsing of {}", authorizations, e);
        }
    }

    /**
     * Get entitlement required for page / action.
     *
     * @param pageId page
     * @param actionId action
     * @return entitlement required
     */
    public String getEntitlement(final String pageId, final String actionId) {
        synchronized (this) {
            if (authMap == null) {
                init();
            }
        }

        Pair<String, String> key = new ImmutablePair<String, String>(pageId, actionId);
        return authMap.containsKey(key)
                ? authMap.get(key)
                : StringUtils.EMPTY;
    }
}
