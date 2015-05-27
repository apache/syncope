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
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;

public class CamelGroupProvisioningManager
        extends AbstractCamelProvisioningManager implements GroupProvisioningManager {

    @Override
    public Pair<Long, List<PropagationStatus>> create(final GroupTO any) {
        return create(any, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> create(final GroupTO groupTO, final Set<String> excludedResources) {
        PollingConsumer pollingConsumer = getConsumer("direct:createGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createGroup", groupTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> create(final GroupTO groupTO, final Map<Long, String> groupOwnerMap,
            final Set<String> excludedResources) {

        PollingConsumer pollingConsumer = getConsumer("direct:createGroupInSyncPort");

        Map<String, Object> props = new HashMap<>();
        props.put("groupOwnerMap", groupOwnerMap);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createGroupInSync", groupTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final GroupMod anyMod) {
        return update(anyMod, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> update(
            final GroupMod anyMod, final Set<String> excludedResources) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:updateGroup", anyMod, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public List<PropagationStatus> delete(final Long groupObjectKey) {
        return delete(groupObjectKey, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(final Long groupKey, final Set<String> excludedResources) {
        PollingConsumer pollingConsumer = getConsumer("direct:deleteGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:deleteGroup", groupKey, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final GroupMod groupMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkGroupPort");

        sendMessage("direct:unlinkGroup", groupMod);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Long.class);
    }

    @Override
    public Long link(final GroupMod groupMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkGroupPort");

        sendMessage("direct:linkGroup", groupMod);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> deprovision(final Long groupKey, final Collection<String> resources) {
        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionGroupPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionGroup", groupKey, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

}
