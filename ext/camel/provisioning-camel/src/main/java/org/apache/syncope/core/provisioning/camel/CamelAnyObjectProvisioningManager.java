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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CamelAnyObjectProvisioningManager
        extends AbstractCamelProvisioningManager implements AnyObjectProvisioningManager {

    public CamelAnyObjectProvisioningManager(final CamelRouteDAO routeDAO, final SyncopeCamelContext contextFactory) {
        super(routeDAO, contextFactory);
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectCR req, final boolean nullPriorityAsync, final String creator, final String context) {

        return create(req, Set.of(), nullPriorityAsync, creator, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SuppressWarnings("unchecked")
    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectCR req,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String creator,
            final String context) {

        PollingConsumer pollingConsumer = getConsumer("direct:createAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);
        props.put("creator", creator);
        props.put("context", context);

        sendMessage("direct:createAnyObject", req, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<AnyObjectUR, List<PropagationStatus>> update(
            final AnyObjectUR anyUR, final boolean nullPriorityAsync, final String updater, final String context) {

        return update(anyUR, Set.of(), nullPriorityAsync, updater, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public Pair<AnyObjectUR, List<PropagationStatus>> update(
            final AnyObjectUR anyUR,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);
        props.put("updater", updater);
        props.put("context", context);

        sendMessage("direct:updateAnyObject", anyUR, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public List<PropagationStatus> delete(
            final String anyObjectObjectKey,
            final boolean nullPriorityAsync,
            final String eraser,
            final String context) {

        return delete(anyObjectObjectKey, Set.of(), nullPriorityAsync, eraser, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(
            final String key,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String eraser,
            final String context) {

        PollingConsumer pollingConsumer = getConsumer("direct:deleteAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);
        props.put("eraser", eraser);
        props.put("context", context);

        sendMessage("direct:deleteAnyObject", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public String unlink(final AnyObjectUR anyObjectUR, final String updater, final String context) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("updater", updater);
        props.put("context", context);

        sendMessage("direct:unlinkAnyObject", anyObjectUR, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(AnyObjectUR.class).getKey();
    }

    @Override
    public String link(final AnyObjectUR anyObjectUR, final String updater, final String context) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("updater", updater);
        props.put("context", context);

        sendMessage("direct:linkAnyObject", anyObjectUR, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(AnyObjectUR.class).getKey();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> provision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        PollingConsumer pollingConsumer = getConsumer("direct:provisionAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);
        props.put("updater", updater);
        props.put("context", context);

        sendMessage("direct:provisionAnyObject", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> deprovision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);
        props.put("updater", updater);
        props.put("context", context);

        sendMessage("direct:deprovisionAnyObject", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }
}
