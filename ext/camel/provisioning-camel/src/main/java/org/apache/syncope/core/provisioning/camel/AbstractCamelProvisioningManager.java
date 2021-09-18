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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCamelProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCamelProvisioningManager.class);

    protected final CamelRouteDAO routeDAO;

    protected final SyncopeCamelContext contextFactory;

    protected RoutesDefinition routes;

    protected final Map<String, PollingConsumer> consumerMap = new HashMap<>();

    protected final List<String> knownURIs = new ArrayList<>();

    protected AbstractCamelProvisioningManager(final CamelRouteDAO routeDAO, final SyncopeCamelContext contextFactory) {
        this.routeDAO = routeDAO;
        this.contextFactory = contextFactory;
    }

    protected void sendMessage(final String uri, final Object obj) {
        Exchange exchange = new DefaultExchange(contextFactory.getCamelContext());

        DefaultMessage message = new DefaultMessage(contextFactory.getCamelContext());
        message.setBody(obj);
        exchange.setIn(message);

        ProducerTemplate template = contextFactory.getCamelContext().createProducerTemplate();
        template.send(uri, exchange);
    }

    protected void sendMessage(final String uri, final Object body, final Map<String, Object> properties) {
        DefaultExchange exchange = new DefaultExchange(contextFactory.getCamelContext());
        // exclude properties with null value, otherwise DefaultExchange#properties, being a ConcurrentHashMap,
        // will raise NPE; no problems for later usage, as Map#get will return null for missing keys anyway
        exchange.setProperties(properties.keySet().stream().
                filter(key -> properties.get(key) != null).
                collect(Collectors.toMap(Function.identity(), properties::get)));

        DefaultMessage message = new DefaultMessage(contextFactory.getCamelContext());
        message.setBody(body);
        exchange.setIn(message);
        ProducerTemplate template = contextFactory.getCamelContext().createProducerTemplate();
        template.send(uri, exchange);
    }

    protected PollingConsumer getConsumer(final String uri) {
        if (!knownURIs.contains(uri)) {
            knownURIs.add(uri);
            Endpoint endpoint = contextFactory.getCamelContext().getEndpoint(uri);
            PollingConsumer pollingConsumer = null;
            try {
                pollingConsumer = endpoint.createPollingConsumer();
                consumerMap.put(uri, pollingConsumer);
                pollingConsumer.start();
            } catch (Exception ex) {
                LOG.error("Unexpected error in Consumer creation ", ex);
            }
            return pollingConsumer;
        } else {
            return consumerMap.get(uri);
        }
    }
}
