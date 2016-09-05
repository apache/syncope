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
package org.apache.syncope.core.logic.init;

import java.io.StringWriter;
import java.util.Map;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

@Component
public class CamelRouteLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteLoader.class);

    private static final boolean IS_JBOSS;

    static {
        IS_JBOSS = isJBoss();
    }

    private static boolean isJBoss() {
        try {
            Class.forName("org.jboss.vfs.VirtualFile");
            LOG.debug("Running in JBoss AS / Wildfly, disabling {}", DOMImplementationRegistry.class.getName());
            return true;
        } catch (Throwable ex) {
            LOG.debug("Not running in JBoss AS / Wildfly, enabling {}", DOMImplementationRegistry.class.getName());
            return false;
        }
    }

    @javax.annotation.Resource(name = "userRoutes")
    private ResourceWithFallbackLoader userRoutesLoader;

    @javax.annotation.Resource(name = "groupRoutes")
    private ResourceWithFallbackLoader groupRoutesLoader;

    @javax.annotation.Resource(name = "anyObjectRoutes")
    private ResourceWithFallbackLoader anyObjectRoutesLoader;

    @Autowired
    private DomainsHolder domainsHolder;

    @Override
    public Integer getPriority() {
        return 1000;
    }

    @Override
    public void load() {
        for (Map.Entry<String, DataSource> entry : domainsHolder.getDomains().entrySet()) {
            loadRoutes(entry.getKey(), entry.getValue(),
                    userRoutesLoader.getResource(), AnyTypeKind.USER);
            loadRoutes(entry.getKey(), entry.getValue(),
                    groupRoutesLoader.getResource(), AnyTypeKind.GROUP);
            loadRoutes(entry.getKey(), entry.getValue(),
                    anyObjectRoutesLoader.getResource(), AnyTypeKind.ANY_OBJECT);
        }

        EntitlementsHolder.getInstance().init(CamelEntitlement.values());
    }

    private String nodeToString(final Node content, final DOMImplementationLS domImpl) {
        StringWriter writer = new StringWriter();
        try {
            LSSerializer serializer = domImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", false);
            LSOutput lso = domImpl.createLSOutput();
            lso.setCharacterStream(writer);
            serializer.write(content, lso);
        } catch (Exception e) {
            LOG.debug("While serializing route node", e);
        }
        return writer.toString();
    }

    private String nodeToString(final Node content, final TransformerFactory tf) {
        String output = StringUtils.EMPTY;

        try {
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(content), new StreamResult(writer));
            output = writer.getBuffer().toString();
        } catch (TransformerException e) {
            LOG.debug("While serializing route node", e);
        }

        return output;
    }

    private void loadRoutes(
            final String domain, final DataSource dataSource, final Resource resource, final AnyTypeKind anyTypeKind) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        boolean shouldLoadRoutes = jdbcTemplate.queryForList(
                String.format("SELECT * FROM %s WHERE ANYTYPEKIND = ?", CamelRoute.class.getSimpleName()),
                new Object[] { anyTypeKind.name() }).
                isEmpty();

        if (shouldLoadRoutes) {
            try {
                TransformerFactory tf = null;
                DOMImplementationLS domImpl = null;
                NodeList routeNodes;
                if (IS_JBOSS) {
                    tf = TransformerFactory.newInstance();
                    tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    dbFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(resource.getInputStream());

                    routeNodes = doc.getDocumentElement().getElementsByTagName("route");
                } else {
                    DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
                    domImpl = (DOMImplementationLS) reg.getDOMImplementation("LS");
                    LSInput lsinput = domImpl.createLSInput();
                    lsinput.setByteStream(resource.getInputStream());

                    LSParser parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

                    routeNodes = parser.parse(lsinput).getDocumentElement().getElementsByTagName("route");
                }

                for (int s = 0; s < routeNodes.getLength(); s++) {
                    Node routeElement = routeNodes.item(s);
                    String routeContent = IS_JBOSS
                            ? nodeToString(routeNodes.item(s), tf)
                            : nodeToString(routeNodes.item(s), domImpl);
                    String routeId = ((Element) routeElement).getAttribute("id");

                    jdbcTemplate.update(
                            String.format("INSERT INTO %s(ID, ANYTYPEKIND, CONTENT) VALUES (?, ?, ?)",
                                    CamelRoute.class.getSimpleName()),
                            new Object[] { routeId, anyTypeKind.name(), routeContent });
                    LOG.info("[{}] Route successfully loaded: {}", domain, routeId);
                }
            } catch (Exception e) {
                LOG.error("[{}] Route load failed", domain, e);
            }
        }
    }
}
