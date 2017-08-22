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
package org.apache.syncope.core.provisioning.camel;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.model.Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

@Component
public class SyncopeCamelContext {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeCamelContext.class);

    @Autowired
    private CamelRouteDAO routeDAO;

    private SpringCamelContext camelContext;

    public SpringCamelContext getContext() {
        synchronized (this) {
            if (camelContext == null) {
                camelContext = new SpringCamelContext(ApplicationContextProvider.getApplicationContext());
                camelContext.setStreamCaching(false);
                camelContext.setAllowUseOriginalMessage(false);
                camelContext.addRoutePolicyFactory(new MetricsRoutePolicyFactory());
            }
        }

        if (camelContext.getRouteDefinitions().isEmpty()) {
            List<CamelRoute> routes = routeDAO.findAll();
            LOG.debug("{} route(s) are going to be loaded ", routes.size());
            loadContext(routes.stream().map(input -> input.getContent()).collect(Collectors.toList()));
            try {
                camelContext.start();
            } catch (Exception e) {
                LOG.error("While starting Camel context", e);
                throw new CamelException(e);
            }
        }

        return camelContext;
    }

    private void loadContext(final Collection<String> routes) {
        try {
            DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
            DOMImplementationLS domImpl = (DOMImplementationLS) reg.getDOMImplementation("LS");
            LSParser parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

            JAXBContext jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<RouteDefinition> routeDefs = new ArrayList<>();
            for (String route : routes) {
                InputStream input = null;
                try {
                    input = IOUtils.toInputStream(route, StandardCharsets.UTF_8);
                    LSInput lsinput = domImpl.createLSInput();
                    lsinput.setByteStream(input);

                    Node routeElement = parser.parse(lsinput).getDocumentElement();
                    routeDefs.add(unmarshaller.unmarshal(routeElement, RouteDefinition.class).getValue());
                } finally {
                    IOUtils.closeQuietly(input);
                }
            }
            camelContext.addRouteDefinitions(routeDefs);
        } catch (Exception e) {
            LOG.error("While loading Camel context {}", e);
            throw new CamelException(e);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateContext(final String routeKey) {
        if (camelContext == null) {
            getContext();
        } else if (!camelContext.getRouteDefinitions().isEmpty()) {
            camelContext.getRouteDefinitions().remove(camelContext.getRouteDefinition(routeKey));
            loadContext(Collections.singletonList(routeDAO.find(routeKey).getContent()));
        }
    }

    public void restoreRoute(final String routeKey, final String routeContent) {
        try {
            camelContext.getRouteDefinitions().remove(camelContext.getRouteDefinition(routeKey));
            loadContext(Collections.singletonList(routeContent));

            camelContext.start();
        } catch (Exception e) {
            LOG.error("While restoring Camel route {}", routeKey, e);
            throw new CamelException(e);
        }
    }

    public void restartContext() {
        try {
            camelContext.stop();
            camelContext.start();
        } catch (Exception e) {
            LOG.error("While restarting Camel context", e);
            throw new CamelException(e);
        }
    }

}
