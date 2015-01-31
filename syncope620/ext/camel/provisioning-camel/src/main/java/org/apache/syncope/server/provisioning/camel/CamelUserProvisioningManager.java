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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.provisioning.api.UserProvisioningManager;
import org.apache.syncope.server.provisioning.api.WorkflowResult;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelUserProvisioningManager extends AbstractCamelProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelUserProvisioningManager.class);

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO) {
        return create(userTO, true, false, null, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, boolean storePassword) {
        return create(userTO, storePassword, false, null, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword,
            boolean disablePwdPolicyCheck, Boolean enabled, Set<String> excludedResources) {

        Map<String, Object> props = new HashMap<>();
        props.put("storePassword", storePassword);
        props.put("disablePwdPolicyCheck", disablePwdPolicyCheck);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createUser", userTO, props);

        String uri = "direct:createPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod) {
        return update(userMod, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> update(UserMod userMod, boolean removeMemberships) {
        Map<String, Object> props = new HashMap<>();
        props.put("removeMemberships", removeMemberships);

        sendMessage("direct:updateUser", userMod, props);

        String uri = "direct:updatePort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    public List<PropagationStatus> delete(final Long userKey) {
        return delete(userKey, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(final Long userKey, final Set<String> excludedResources) {
        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:deleteUser", userKey, props);

        String uri = "direct:deletePort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final UserMod userMod) {
        sendMessage("direct:unlinkUser", userMod);

        String uri = "direct:unlinkPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        exchange.getIn().setBody((exchange.getIn().getBody(UserMod.class).getKey()));
        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> activate(final User user, final StatusMod statusMod) {
        Map<String, Object> props = new HashMap<>();
        props.put("token", statusMod.getToken());
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:activateUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props);
        }

        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> reactivate(final User user, final StatusMod statusMod) {
        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:reactivateUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props);
        }

        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> suspend(final User user, final StatusMod statusMod) {
        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:suspendUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props);
        }

        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Long link(final UserMod subjectMod) {
        sendMessage("direct:linkUser", subjectMod);

        String uri = "direct:linkPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        exchange.getIn().setBody((exchange.getIn().getBody(UserMod.class).getKey()));
        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> deprovision(final Long user, final Collection<String> resources) {
        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionUser", user, props);

        String uri = "direct:deprovisionPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<Long, List<PropagationStatus>> update(
            final UserMod userMod, Long id, final ProvisioningResult result,
            final Boolean enabled, final Set<String> excludedResources) {

        Map<String, Object> props = new HashMap<>();
        props.put("id", id);
        props.put("result", result);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:updateSyncUser", userMod, props);

        String uri = "direct:updateSyncPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        Exception e;
        if ((e = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT)) != null) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", id, e);

            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            WorkflowResult<Map.Entry<UserMod, Boolean>> updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
            sendMessage("direct:syncUserStatus", updated, props);
            exchange = pollingConsumer.receive();
        }

        return exchange.getIn().getBody(Map.Entry.class);
    }

    @Override
    public void innerSuspend(final User user, final boolean suspend) {
        Map<String, Object> props = new HashMap<>();
        props.put("suspend", suspend);

        sendMessage("direct:suspendUserWF", user, props);

        String uri = "direct:suspendWFPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void requestPasswordReset(final Long userKey) {
        sendMessage("direct:requestPwdReset", userKey);

        String uri = "direct:requestPwdResetPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void confirmPasswordReset(final User user, final String token, final String password) {
        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("userId", user.getKey());
        props.put("token", token);
        props.put("password", password);

        sendMessage("direct:confirmPwdReset", user, props);

        String uri = "direct:confirmPwdResetPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

}
