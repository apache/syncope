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
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CamelGroupProvisioningManager
        extends AbstractCamelProvisioningManager implements GroupProvisioningManager {

    @Override
    @SuppressWarnings("unchecked")
    public Pair<String, List<PropagationStatus>> create(final GroupTO groupTO, final boolean nullPriorityAsync) {
        PollingConsumer pollingConsumer = getConsumer("direct:createGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", Collections.<String>emptySet());
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:createGroup", groupTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public Pair<String, List<PropagationStatus>> create(
            final GroupTO groupTO,
            final Map<String, String> groupOwnerMap,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:createGroupInPullPort");

        Map<String, Object> props = new HashMap<>();
        props.put("groupOwnerMap", groupOwnerMap);
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:createGroupInPull", groupTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<GroupPatch, List<PropagationStatus>> update(
            final GroupPatch anyPatch, final boolean nullPriorityAsync) {

        return update(anyPatch, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public Pair<GroupPatch, List<PropagationStatus>> update(
            final GroupPatch anyPatch, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:updateGroup", anyPatch, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public List<PropagationStatus> delete(final String key, final boolean nullPriorityAsync) {
        return delete(key, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(
            final String key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:deleteGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:deleteGroup", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public String unlink(final GroupPatch groupPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkGroupPort");

        sendMessage("direct:unlinkGroup", groupPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(GroupPatch.class).getKey();
    }

    @Override
    public String link(final GroupPatch groupPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkGroupPort");

        sendMessage("direct:linkGroup", groupPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(GroupPatch.class).getKey();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> provision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:provisionGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:provisionGroup", key, props);

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

        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:deprovisionGroup", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

}
