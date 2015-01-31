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
package org.apache.syncope.server.provisioning.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.camel.model.Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.server.misc.spring.ApplicationContextProvider;
import org.apache.syncope.server.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.server.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component
public class SyncopeCamelContext {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeCamelContext.class);

    @Autowired
    private CamelRouteDAO routeDAO;

    private SpringCamelContext camelContext = null;

    public SpringCamelContext getContext() {
        synchronized (this) {
            if (camelContext == null) {
                camelContext = new SpringCamelContext(ApplicationContextProvider.getApplicationContext());
            }
        }

        if (camelContext.getRouteDefinitions().isEmpty()) {
            List<CamelRoute> routes = routeDAO.findAll();
            LOG.debug("{} route(s) are going to be loaded ", routes.size());
            loadContext(routeDAO, routes);
            try {
                camelContext.start();
            } catch (Exception e) {
                LOG.error("While starting Camel context", e);
            }
        }

        return camelContext;
    }

    public void loadContext(final CamelRouteDAO routeDAO, final List<CamelRoute> routes) {
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            JAXBContext jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<RouteDefinition> routeDefs = new ArrayList<>();
            for (CamelRoute route : routes) {
                InputStream is = null;
                try {
                    is = new ByteArrayInputStream(
                            URLDecoder.decode(route.getContent(), SyncopeConstants.DEFAULT_ENCODING).getBytes());
                    Document doc = dBuilder.parse(is);
                    doc.getDocumentElement().normalize();
                    Node routeEl = doc.getElementsByTagName("route").item(0);
                    JAXBElement<RouteDefinition> obj = unmarshaller.unmarshal(routeEl, RouteDefinition.class);
                    routeDefs.add(obj.getValue());
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            camelContext.addRouteDefinitions(routeDefs);
        } catch (Exception e) {
            LOG.error("While loading Camel context {}", e);
        }
    }

    public void reloadContext() {
        if (camelContext == null) {
            getContext();
        } else {
            if (!camelContext.getRouteDefinitions().isEmpty()) {
                try {
                    camelContext.removeRouteDefinitions(new ArrayList<>(camelContext.getRouteDefinitions()));
                } catch (Exception e) {
                    LOG.error("While clearing Camel context {}", e);
                }
            }

            loadContext(routeDAO, new ArrayList<>(routeDAO.findAll()));
        }
    }

    public void reloadContext(final String routeKey) {
        if (camelContext == null) {
            getContext();
        } else {
            if (!camelContext.getRouteDefinitions().isEmpty()) {
                camelContext.getRouteDefinitions().remove(camelContext.getRouteDefinition(routeKey));
                loadContext(routeDAO, Collections.singletonList(routeDAO.find(routeKey)));
            }
        }
    }

    public List<RouteDefinition> getDefinitions() {
        return camelContext.getRouteDefinitions();
    }
}
