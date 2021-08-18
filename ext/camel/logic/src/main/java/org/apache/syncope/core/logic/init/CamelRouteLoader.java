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
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
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

public class CamelRouteLoader implements SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(CamelRouteLoader.class);

    protected static final boolean IS_JBOSS;

    static {
        IS_JBOSS = isJBoss();
    }

    protected static boolean isJBoss() {
        try {
            Class.forName("org.jboss.vfs.VirtualFile");
            LOG.debug("Running in JBoss AS / Wildfly, disabling {}", DOMImplementationRegistry.class.getName());
            return true;
        } catch (Throwable ex) {
            LOG.debug("Not running in JBoss AS / Wildfly, enabling {}", DOMImplementationRegistry.class.getName());
            return false;
        }
    }

    protected final Resource userRoutes;

    protected final Resource groupRoutes;

    protected final Resource anyObjectRoutes;

    public CamelRouteLoader(final Resource userRoutes, final Resource groupRoutes, final Resource anyObjectRoutes) {
        this.userRoutes = userRoutes;
        this.groupRoutes = groupRoutes;
        this.anyObjectRoutes = anyObjectRoutes;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public void load() {
        EntitlementsHolder.getInstance().addAll(CamelEntitlement.values());
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        loadRoutes(domain, datasource, userRoutes, AnyTypeKind.USER);
        loadRoutes(domain, datasource, groupRoutes, AnyTypeKind.GROUP);
        loadRoutes(domain, datasource, anyObjectRoutes, AnyTypeKind.ANY_OBJECT);
    }

    protected static String nodeToString(final Node content, final DOMImplementationLS domImpl) {
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

    protected static String nodeToString(final Node content, final TransformerFactory tf) {
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

    protected static void loadRoutes(
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
                    tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    try {
                        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                    } catch (IllegalArgumentException ex) {
                        LOG.debug("The JAXP parser does not support the following attribute: ", ex);
                    }
                    tf.setURIResolver((href, base) -> null);

                    Document doc = StaxUtils.read(resource.getInputStream());

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
