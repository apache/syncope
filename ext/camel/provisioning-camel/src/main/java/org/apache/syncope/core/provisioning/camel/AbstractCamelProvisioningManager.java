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
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractCamelProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCamelProvisioningManager.class);

    @Autowired
    protected CamelRouteDAO routeDAO;

    @Autowired
    protected SyncopeCamelContext contextFactory;

    protected SpringCamelContext camelContext;

    protected RoutesDefinition routes;

    protected final Map<String, PollingConsumer> consumerMap = new HashMap<>();

    protected final List<String> knownURIs = new ArrayList<>();

    protected void sendMessage(final String uri, final Object obj) {
        Exchange exchange = new DefaultExchange(contextFactory.getContext());

        DefaultMessage message = new DefaultMessage();
        message.setBody(obj);
        exchange.setIn(message);

        ProducerTemplate template = contextFactory.getContext().createProducerTemplate();
        template.send(uri, exchange);
    }

    protected void sendMessage(final String uri, final Object body, final Map<String, Object> properties) {
        Exchange exchange = new DefaultExchange(contextFactory.getContext());

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            exchange.setProperty(property.getKey(), property.getValue());
            LOG.debug("Added property {}", property.getKey());
        }

        DefaultMessage message = new DefaultMessage();
        message.setBody(body);
        exchange.setIn(message);
        ProducerTemplate template = contextFactory.getContext().createProducerTemplate();
        template.send(uri, exchange);
    }

    protected PollingConsumer getConsumer(final String uri) {
        if (!knownURIs.contains(uri)) {
            knownURIs.add(uri);
            Endpoint endpoint = contextFactory.getContext().getEndpoint(uri);
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
