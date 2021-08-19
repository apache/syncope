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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.xml.jaxb.JaxbHelper;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SyncopeCamelContext {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeCamelContext.class);

    protected final CamelRouteDAO routeDAO;

    public SyncopeCamelContext(final CamelRouteDAO routeDAO) {
        this.routeDAO = routeDAO;
    }

    protected SpringCamelContext camelContext;

    public SpringCamelContext getCamelContext() {
        synchronized (this) {
            if (camelContext == null) {
                camelContext = ApplicationContextProvider.getBeanFactory().getBean(SpringCamelContext.class);
                camelContext.addRoutePolicyFactory(new MetricsRoutePolicyFactory());
            }

            if (camelContext.getRoutes().isEmpty()) {
                List<CamelRoute> routes = routeDAO.findAll();
                LOG.debug("{} route(s) are going to be loaded ", routes.size());

                loadRouteDefinitions(routes.stream().map(CamelRoute::getContent).collect(Collectors.toList()));
            }
        }

        return camelContext;
    }

    protected void loadRouteDefinitions(final List<String> routes) {
        try {
            RoutesDefinition routeDefs = JaxbHelper.loadRoutesDefinition(
                    camelContext,
                    new ByteArrayInputStream(("<routes xmlns=\"http://camel.apache.org/schema/spring\">"
                            + routes.stream().collect(Collectors.joining())
                            + "</routes>").getBytes(StandardCharsets.UTF_8)));
            camelContext.addRouteDefinitions(routeDefs.getRoutes());
        } catch (Exception e) {
            LOG.error("While adding route definitions into Camel Context {}", camelContext, e);
            throw new CamelException(e);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateContext(final String routeKey) {
        if (!getCamelContext().getRouteDefinitions().isEmpty()) {
            getCamelContext().getRouteDefinitions().remove(getCamelContext().getRouteDefinition(routeKey));
            loadRouteDefinitions(List.of(routeDAO.find(routeKey).getContent()));
        }
    }

    public void restoreRoute(final String routeKey, final String routeContent) {
        try {
            getCamelContext().getRouteDefinitions().remove(getCamelContext().getRouteDefinition(routeKey));
            loadRouteDefinitions(List.of(routeContent));
        } catch (Exception e) {
            LOG.error("While restoring Camel route {}", routeKey, e);
            throw new CamelException(e);
        }
    }

    public void restartContext() {
        try {
            getCamelContext().stop();
            getCamelContext().start();
        } catch (Exception e) {
            LOG.error("While restarting Camel context", e);
            throw new CamelException(e);
        }
    }
}
