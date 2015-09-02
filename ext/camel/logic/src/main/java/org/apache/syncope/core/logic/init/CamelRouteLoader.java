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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.misc.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
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

    @javax.annotation.Resource(name = "userRoutes")
    private ResourceWithFallbackLoader userRoutesLoader;

    @javax.annotation.Resource(name = "groupRoutes")
    private ResourceWithFallbackLoader groupRoutesLoader;

    @javax.annotation.Resource(name = "anyObjectRoutes")
    private ResourceWithFallbackLoader anyObjectRoutesLoader;

    @Autowired
    private DomainsHolder domainsHolder;

    private boolean loaded = false;

    @Override
    public Integer getPriority() {
        return 1000;
    }

    @Override
    public void load() {
        synchronized (this) {
            if (!loaded) {
                for (Map.Entry<String, DataSource> entry : domainsHolder.getDomains().entrySet()) {
                    loadRoutes(entry.getKey(), entry.getValue(),
                            userRoutesLoader.getResource(), AnyTypeKind.USER);
                    loadRoutes(entry.getKey(), entry.getValue(),
                            groupRoutesLoader.getResource(), AnyTypeKind.GROUP);
                    loadRoutes(entry.getKey(), entry.getValue(),
                            anyObjectRoutesLoader.getResource(), AnyTypeKind.ANY_OBJECT);
                }
                loaded = true;
            }
        }
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

    private void loadRoutes(
            final String domain, final DataSource dataSource, final Resource resource, final AnyTypeKind anyTypeKind) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        boolean shouldLoadRoutes = jdbcTemplate.queryForList(
                String.format("SELECT * FROM %s WHERE ANYTYPEKIND = ?", CamelRoute.class.getSimpleName()),
                new Object[] { anyTypeKind.name() }).
                isEmpty();

        if (shouldLoadRoutes) {
            try {
                DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
                DOMImplementationLS domImpl = (DOMImplementationLS) reg.getDOMImplementation("LS");
                LSInput lsinput = domImpl.createLSInput();
                lsinput.setByteStream(resource.getInputStream());

                LSParser parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

                NodeList routeNodes = parser.parse(lsinput).getDocumentElement().getElementsByTagName("route");
                for (int s = 0; s < routeNodes.getLength(); s++) {
                    Node routeElement = routeNodes.item(s);
                    String routeContent = nodeToString(routeNodes.item(s), domImpl);
                    String routeId = ((Element) routeElement).getAttribute("id");

                    jdbcTemplate.update(
                            String.format("INSERT INTO %s(NAME, ANYTYPEKIND, CONTENT) VALUES (?, ?, ?)",
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
