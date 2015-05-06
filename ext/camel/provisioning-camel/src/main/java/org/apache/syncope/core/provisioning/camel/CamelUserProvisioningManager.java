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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelUserProvisioningManager extends AbstractCamelProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelUserProvisioningManager.class);

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO) {
        return create(userTO, true, false, null, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, storePassword, false, null, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword,
            final boolean disablePwdPolicyCheck, final Boolean enabled, final Set<String> excludedResources) {

        PollingConsumer pollingConsumer = getConsumer("direct:createPort");

        Map<String, Object> props = new HashMap<>();
        props.put("storePassword", storePassword);
        props.put("disablePwdPolicyCheck", disablePwdPolicyCheck);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createUser", userTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final UserMod userMod) {
        return update(userMod, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> update(final UserMod userMod, final boolean removeMemberships) {
        PollingConsumer pollingConsumer = getConsumer("direct:updatePort");

        Map<String, Object> props = new HashMap<>();
        props.put("removeMemberships", removeMemberships);

        sendMessage("direct:updateUser", userMod, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public List<PropagationStatus> delete(final Long userKey) {
        return delete(userKey, Collections.<String>emptySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(final Long userKey, final Set<String> excludedResources) {
        PollingConsumer pollingConsumer = getConsumer("direct:deletePort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:deleteUser", userKey, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final UserMod userMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkPort");

        sendMessage("direct:unlinkUser", userMod);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        exchange.getIn().setBody((exchange.getIn().getBody(UserMod.class).getKey()));
        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> activate(final User user, final StatusMod statusMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("token", statusMod.getToken());
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:activateUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:userStatusPropagation", updated, props);
        }

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> reactivate(final User user, final StatusMod statusMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:reactivateUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:userStatusPropagation", updated, props);
        }

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> suspend(final User user, final StatusMod statusMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("statusMod", statusMod);

        if (statusMod.isOnSyncope()) {
            sendMessage("direct:suspendUser", user.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:userStatusPropagation", updated, props);
        }

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Long link(final UserMod subjectMod) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkPort");

        sendMessage("direct:linkUser", subjectMod);

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
        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionUser", user, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> update(
            final UserMod userMod, final Long key, final ProvisioningResult result,
            final Boolean enabled, final Set<String> excludedResources) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateInSyncPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", key);
        props.put("result", result);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:updateUserInSync", userMod, props);

        Exchange exchange = pollingConsumer.receive();

        Exception ex = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        if (ex != null) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", key, ex);

            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + ex.getMessage());

            WorkflowResult<Pair<UserMod, Boolean>> updated = new WorkflowResult<Pair<UserMod, Boolean>>(
                    new ImmutablePair<>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
            sendMessage("direct:userInSync", updated, props);
            exchange = pollingConsumer.receive();
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public void innerSuspend(final User user, final boolean propagate) {
        PollingConsumer pollingConsumer = getConsumer("direct:innerSuspendUserPort");

        Map<String, Object> props = new HashMap<>();
        props.put("propagate", propagate);

        sendMessage("direct:innerSuspendUser", user, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void requestPasswordReset(final Long userKey) {
        PollingConsumer pollingConsumer = getConsumer("direct:requestPwdResetPort");

        sendMessage("direct:requestPwdReset", userKey);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void confirmPasswordReset(final User user, final String token, final String password) {
        PollingConsumer pollingConsumer = getConsumer("direct:confirmPwdResetPort");

        Map<String, Object> props = new HashMap<>();
        props.put("user", user);
        props.put("userKey", user.getKey());
        props.put("token", token);
        props.put("password", password);

        sendMessage("direct:confirmPwdReset", user, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

}
