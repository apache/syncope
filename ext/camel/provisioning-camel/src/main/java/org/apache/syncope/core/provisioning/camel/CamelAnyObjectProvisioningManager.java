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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CamelAnyObjectProvisioningManager
        extends AbstractCamelProvisioningManager implements AnyObjectProvisioningManager {

    @Override
    public Pair<String, List<PropagationStatus>> create(final AnyObjectTO any, final boolean nullPriorityAsync) {
        return create(any, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectTO anyObjectTO, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:createAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:createAnyObject", anyObjectTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<AnyObjectPatch, List<PropagationStatus>> update(
            final AnyObjectPatch anyPatch, final boolean nullPriorityAsync) {

        return update(anyPatch, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public Pair<AnyObjectPatch, List<PropagationStatus>> update(
            final AnyObjectPatch anyPatch, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:updateAnyObject", anyPatch, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public List<PropagationStatus> delete(final String anyObjectObjectKey, final boolean nullPriorityAsync) {
        return delete(anyObjectObjectKey, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(
            final String key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:deleteAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:deleteAnyObject", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public String unlink(final AnyObjectPatch anyObjectPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkAnyObjectPort");

        sendMessage("direct:unlinkAnyObject", anyObjectPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(AnyObjectPatch.class).getKey();
    }

    @Override
    public String link(final AnyObjectPatch anyObjectPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkAnyObjectPort");

        sendMessage("direct:linkAnyObject", anyObjectPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(AnyObjectPatch.class).getKey();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> provision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:provisionAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

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
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionAnyObjectPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:deprovisionAnyObject", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

}
