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
package org.apache.syncope.server.logic.init;

import java.io.StringWriter;
import java.util.List;
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
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.server.persistence.api.SyncopeLoader;
import org.apache.syncope.server.persistence.api.entity.CamelEntityFactory;
import org.apache.syncope.server.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class CamelRouteLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteLoader.class);

    private static final DocumentBuilderFactory DOC_FACTORY = DocumentBuilderFactory.newInstance();

    private static final TransformerFactory T_FACTORY = TransformerFactory.newInstance();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CamelEntityFactory entityFactory;

    private int size = 0;

    private boolean loaded = false;

    @Override
    public Integer getPriority() {
        return 1000;
    }

    @Transactional
    public void load() {
        synchronized (this) {
            if (!loaded) {
                loadRoutes("/userRoute.xml", SubjectType.USER);
                loadRoutes("/roleRoute.xml", SubjectType.ROLE);
                loadEntitlements();
                loaded = true;
            }
        }
    }

    private boolean routesAvailable(final SubjectType subject) {
        final String sql = String.format("SELECT * FROM %s WHERE SUBJECT = ?", CamelRoute.class.getSimpleName());
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { subject.name() });
        return !rows.isEmpty();
    }

    private String nodeToString(final Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer transformer = T_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            LOG.debug("nodeToString Transformer Exception", te);
        }
        return sw.toString();
    }

    private void loadRoutes(final String path, final SubjectType subjectType) {
        if (routesAvailable(subjectType)) {
            final String query = String.format("INSERT INTO %s(ID, NAME, SUBJECT, ROUTECONTENT) VALUES (?, ?, ?, ?)",
                    CamelRoute.class.getSimpleName());
            try {
                final DocumentBuilder dBuilder = DOC_FACTORY.newDocumentBuilder();
                final Document doc = dBuilder.parse(getClass().getResourceAsStream(path));
                doc.getDocumentElement().normalize();
                final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                final NodeList listOfRoutes = doc.getElementsByTagName("route");
                for (int s = 0; s < listOfRoutes.getLength(); s++) {
                    //getting the route node element
                    Node routeEl = listOfRoutes.item(s);
                    //crate an instance of CamelRoute Entity
                    CamelRoute route = entityFactory.newCamelRoute();
                    route.setSubjectType(subjectType);
                    route.setKey(((Element) routeEl).getAttribute("id"));
                    route.setContent(nodeToString(listOfRoutes.item(s)));

                    jdbcTemplate.update(query, new Object[] { size++, ((Element) routeEl).getAttribute("id"),
                        subjectType.name(), nodeToString(listOfRoutes.item(s)) });
                    LOG.debug("Route {} successfully registered", ((Element) routeEl).getAttribute("id"));
                }
            } catch (DataAccessException e) {
                LOG.error("While trying to store queries {}", e);
            } catch (Exception e) {
                LOG.error("Route Registration failed {}", e.getMessage());
            }
        }
    }

    private void loadEntitlements() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO Entitlement VALUES('ROUTE_READ')");
        jdbcTemplate.update("INSERT INTO Entitlement VALUES('ROUTE_LIST')");
        jdbcTemplate.update("INSERT INTO Entitlement VALUES('ROUTE_UPDATE')");
    }
}
