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
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelUserProvisioningManager extends AbstractCamelProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelUserProvisioningManager.class);

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean nullPriorityAsync) {
        return create(userTO, true, false, null, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return create(userTO, storePassword, false, null, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(
            final UserTO userTO, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        return create(userTO, false, false, null, excludedResources, nullPriorityAsync);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> create(
            final UserTO userTO,
            final boolean storePassword,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:createPort");

        Map<String, Object> props = new HashMap<>();
        props.put("storePassword", storePassword);
        props.put("disablePwdPolicyCheck", disablePwdPolicyCheck);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:createUser", userTO, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> update(final UserPatch userPatch, final boolean nullPriorityAsync) {
        PollingConsumer pollingConsumer = getConsumer("direct:updatePort");

        Map<String, Object> props = new HashMap<>();
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:updateUser", userPatch, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(
            final UserPatch anyPatch, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        return update(anyPatch, new ProvisioningReport(), null, excludedResources, nullPriorityAsync);
    }

    @Override
    public List<PropagationStatus> delete(final Long key, final boolean nullPriorityAsync) {
        return delete(key, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> delete(
            final Long key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:deletePort");

        Map<String, Object> props = new HashMap<>();
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:deleteUser", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final UserPatch userPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:unlinkPort");

        sendMessage("direct:unlinkUser", userPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        exchange.getIn().setBody((exchange.getIn().getBody(UserPatch.class).getKey()));
        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Long, List<PropagationStatus>> activate(
            final StatusPatch statusPatch, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("token", statusPatch.getToken());
        props.put("key", statusPatch.getKey());
        props.put("statusPatch", statusPatch);
        props.put("nullPriorityAsync", nullPriorityAsync);

        if (statusPatch.isOnSyncope()) {
            sendMessage("direct:activateUser", statusPatch.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(statusPatch.getKey(), null, statusPatch.getType().name().toLowerCase());
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
    public Pair<Long, List<PropagationStatus>> reactivate(
            final StatusPatch statusPatch, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", statusPatch.getKey());
        props.put("statusPatch", statusPatch);
        props.put("nullPriorityAsync", nullPriorityAsync);

        if (statusPatch.isOnSyncope()) {
            sendMessage("direct:reactivateUser", statusPatch.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(statusPatch.getKey(), null, statusPatch.getType().name().toLowerCase());
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
    public Pair<Long, List<PropagationStatus>> suspend(
            final StatusPatch statusPatch, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:statusPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", statusPatch.getKey());
        props.put("statusPatch", statusPatch);
        props.put("nullPriorityAsync", nullPriorityAsync);

        if (statusPatch.isOnSyncope()) {
            sendMessage("direct:suspendUser", statusPatch.getKey(), props);
        } else {
            WorkflowResult<Long> updated =
                    new WorkflowResult<>(statusPatch.getKey(), null, statusPatch.getType().name().toLowerCase());
            sendMessage("direct:userStatusPropagation", updated, props);
        }

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public Long link(final UserPatch anyPatch) {
        PollingConsumer pollingConsumer = getConsumer("direct:linkPort");

        sendMessage("direct:linkUser", anyPatch);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        exchange.getIn().setBody((exchange.getIn().getBody(UserPatch.class).getKey()));
        return exchange.getIn().getBody(Long.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> provision(
            final Long key,
            final boolean changePwd,
            final String password,
            final Collection<String> resources,
            final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:provisionPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", key);
        props.put("changePwd", changePwd);
        props.put("password", password);
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:provisionUser", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return exchange.getIn().getBody(List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropagationStatus> deprovision(
            final Long user, final Collection<String> resources, final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:deprovisionPort");

        Map<String, Object> props = new HashMap<>();
        props.put("resources", resources);
        props.put("nullPriorityAsync", nullPriorityAsync);

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
            final UserPatch userPatch,
            final ProvisioningReport result,
            final Boolean enabled,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync) {

        PollingConsumer pollingConsumer = getConsumer("direct:updateInSyncPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", userPatch.getKey());
        props.put("result", result);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);
        props.put("nullPriorityAsync", nullPriorityAsync);

        sendMessage("direct:updateUserInSync", userPatch, props);

        Exchange exchange = pollingConsumer.receive();

        Exception ex = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        if (ex != null) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)",
                    nullPriorityAsync, ex);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + ex.getMessage());

            WorkflowResult<Pair<UserPatch, Boolean>> updated = new WorkflowResult<Pair<UserPatch, Boolean>>(
                    new ImmutablePair<>(userPatch, false), new PropagationByResource(),
                    new HashSet<String>());
            sendMessage("direct:userInSync", updated, props);
            exchange = pollingConsumer.receive();
        }

        return exchange.getIn().getBody(Pair.class);
    }

    @Override
    public void internalSuspend(final Long key) {
        PollingConsumer pollingConsumer = getConsumer("direct:internalSuspendUserPort");

        sendMessage("direct:internalSuspendUser", key);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void requestPasswordReset(final Long key) {
        PollingConsumer pollingConsumer = getConsumer("direct:requestPwdResetPort");

        sendMessage("direct:requestPwdReset", key);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

    @Override
    public void confirmPasswordReset(final Long key, final String token, final String password) {
        PollingConsumer pollingConsumer = getConsumer("direct:confirmPwdResetPort");

        Map<String, Object> props = new HashMap<>();
        props.put("key", key);
        props.put("token", token);
        props.put("password", password);

        sendMessage("direct:confirmPwdReset", key, props);

        Exchange exchange = pollingConsumer.receive();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
    }

}
