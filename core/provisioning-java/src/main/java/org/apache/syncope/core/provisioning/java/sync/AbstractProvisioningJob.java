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
package org.apache.syncope.core.provisioning.java.sync;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.group.GMapping;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.UMapping;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.provisioning.java.job.AbstractTaskJob;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Job for executing synchronization tasks.
 *
 * @see AbstractTaskJob
 * @see SyncTask
 * @see PushTask
 */
public abstract class AbstractProvisioningJob<T extends ProvisioningTask, A extends ProvisioningActions>
        extends AbstractTaskJob {

    /**
     * ConnInstance loader.
     */
    @Autowired
    protected ConnectorFactory connFactory;

    /**
     * Resource DAO.
     */
    @Autowired
    protected ExternalResourceDAO resourceDAO;

    /**
     * Entitlement DAO.
     */
    @Autowired
    protected EntitlementDAO entitlementDAO;

    /**
     * Policy DAO.
     */
    @Autowired
    protected PolicyDAO policyDAO;

    /**
     * SyncJob actions.
     */
    protected List<A> actions;

    public void setActions(final List<A> actions) {
        this.actions = actions;
    }

    /**
     * Create a textual report of the synchronization, based on the trace level.
     *
     * @param provResults Sync results
     * @param syncTraceLevel Sync trace level
     * @param dryRun dry run?
     * @return report as string
     */
    protected String createReport(final Collection<ProvisioningResult> provResults, final TraceLevel syncTraceLevel,
            final boolean dryRun) {

        if (syncTraceLevel == TraceLevel.NONE) {
            return null;
        }

        StringBuilder report = new StringBuilder();

        if (dryRun) {
            report.append("==>Dry run only, no modifications were made<==\n\n");
        }

        List<ProvisioningResult> uSuccCreate = new ArrayList<>();
        List<ProvisioningResult> uFailCreate = new ArrayList<>();
        List<ProvisioningResult> uSuccUpdate = new ArrayList<>();
        List<ProvisioningResult> uFailUpdate = new ArrayList<>();
        List<ProvisioningResult> uSuccDelete = new ArrayList<>();
        List<ProvisioningResult> uFailDelete = new ArrayList<>();
        List<ProvisioningResult> uSuccNone = new ArrayList<>();
        List<ProvisioningResult> rSuccCreate = new ArrayList<>();
        List<ProvisioningResult> rFailCreate = new ArrayList<>();
        List<ProvisioningResult> rSuccUpdate = new ArrayList<>();
        List<ProvisioningResult> rFailUpdate = new ArrayList<>();
        List<ProvisioningResult> rSuccDelete = new ArrayList<>();
        List<ProvisioningResult> rFailDelete = new ArrayList<>();
        List<ProvisioningResult> rSuccNone = new ArrayList<>();

        for (ProvisioningResult provResult : provResults) {
            switch (provResult.getStatus()) {
                case SUCCESS:
                    switch (provResult.getOperation()) {
                        case CREATE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uSuccCreate.add(provResult);
                                    break;

                                case GROUP:
                                    rSuccCreate.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        case UPDATE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uSuccUpdate.add(provResult);
                                    break;

                                case GROUP:
                                    rSuccUpdate.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        case DELETE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uSuccDelete.add(provResult);
                                    break;

                                case GROUP:
                                    rSuccDelete.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        case NONE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uSuccNone.add(provResult);
                                    break;

                                case GROUP:
                                    rSuccNone.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        default:
                    }
                    break;

                case FAILURE:
                    switch (provResult.getOperation()) {
                        case CREATE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uFailCreate.add(provResult);
                                    break;

                                case GROUP:
                                    rFailCreate.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        case UPDATE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uFailUpdate.add(provResult);
                                    break;

                                case GROUP:
                                    rFailUpdate.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        case DELETE:
                            switch (provResult.getSubjectType()) {
                                case USER:
                                    uFailDelete.add(provResult);
                                    break;

                                case GROUP:
                                    rFailDelete.add(provResult);
                                    break;

                                default:
                            }
                            break;

                        default:
                    }
                    break;

                default:
            }
        }

        // Summary, also to be included for FAILURE and ALL, so create it anyway.
        report.append("Users ").
                append("[created/failures]: ").append(uSuccCreate.size()).append('/').append(uFailCreate.size()).
                append(' ').
                append("[updated/failures]: ").append(uSuccUpdate.size()).append('/').append(uFailUpdate.size()).
                append(' ').
                append("[deleted/failures]: ").append(uSuccDelete.size()).append('/').append(uFailDelete.size()).
                append(' ').
                append("[ignored]: ").append(uSuccNone.size()).append('\n');
        report.append("Groups ").
                append("[created/failures]: ").append(rSuccCreate.size()).append('/').append(rFailCreate.size()).
                append(' ').
                append("[updated/failures]: ").append(rSuccUpdate.size()).append('/').append(rFailUpdate.size()).
                append(' ').
                append("[deleted/failures]: ").append(rSuccDelete.size()).append('/').append(rFailDelete.size()).
                append(' ').
                append("[ignored]: ").append(rSuccNone.size());

        // Failures
        if (syncTraceLevel == TraceLevel.FAILURES || syncTraceLevel == TraceLevel.ALL) {
            if (!uFailCreate.isEmpty()) {
                report.append("\n\nUsers failed to create: ");
                report.append(ProvisioningResult.produceReport(uFailCreate, syncTraceLevel));
            }
            if (!uFailUpdate.isEmpty()) {
                report.append("\nUsers failed to update: ");
                report.append(ProvisioningResult.produceReport(uFailUpdate, syncTraceLevel));
            }
            if (!uFailDelete.isEmpty()) {
                report.append("\nUsers failed to delete: ");
                report.append(ProvisioningResult.produceReport(uFailDelete, syncTraceLevel));
            }

            if (!rFailCreate.isEmpty()) {
                report.append("\n\nGroups failed to create: ");
                report.append(ProvisioningResult.produceReport(rFailCreate, syncTraceLevel));
            }
            if (!rFailUpdate.isEmpty()) {
                report.append("\nGroups failed to update: ");
                report.append(ProvisioningResult.produceReport(rFailUpdate, syncTraceLevel));
            }
            if (!rFailDelete.isEmpty()) {
                report.append("\nGroups failed to delete: ");
                report.append(ProvisioningResult.produceReport(rFailDelete, syncTraceLevel));
            }
        }

        // Succeeded, only if on 'ALL' level
        if (syncTraceLevel == TraceLevel.ALL) {
            report.append("\n\nUsers created:\n")
                    .append(ProvisioningResult.produceReport(uSuccCreate, syncTraceLevel))
                    .append("\nUsers updated:\n")
                    .append(ProvisioningResult.produceReport(uSuccUpdate, syncTraceLevel))
                    .append("\nUsers deleted:\n")
                    .append(ProvisioningResult.produceReport(uSuccDelete, syncTraceLevel))
                    .append("\nUsers ignored:\n")
                    .append(ProvisioningResult.produceReport(uSuccNone, syncTraceLevel));
            report.append("\n\nGroups created:\n")
                    .append(ProvisioningResult.produceReport(rSuccCreate, syncTraceLevel))
                    .append("\nGroups updated:\n")
                    .append(ProvisioningResult.produceReport(rSuccUpdate, syncTraceLevel))
                    .append("\nGroups deleted:\n")
                    .append(ProvisioningResult.produceReport(rSuccDelete, syncTraceLevel))
                    .append("\nGroups ignored:\n")
                    .append(ProvisioningResult.produceReport(rSuccNone, syncTraceLevel));
        }

        return report.toString();
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        // PRE: grant all authorities (i.e. setup the SecurityContextHolder)
        List<GrantedAuthority> authorities = new ArrayList<>();
        CollectionUtils.collect(entitlementDAO.findAll(), new Transformer<Entitlement, GrantedAuthority>() {

            @Override
            public GrantedAuthority transform(final Entitlement entitlement) {
                return new SimpleGrantedAuthority(entitlement.getKey());
            }
        }, authorities);

        final UserDetails userDetails = new User("admin", "FAKE_PASSWORD", true, true, true, true, authorities);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities));

        try {
            final Class<T> clazz = getTaskClassReference();
            if (!clazz.isAssignableFrom(task.getClass())) {
                throw new JobExecutionException("Task " + taskId + " isn't a SyncTask");
            }

            final T syncTask = clazz.cast(this.task);

            final Connector connector;
            try {
                connector = connFactory.getConnector(syncTask.getResource());
            } catch (Exception e) {
                final String msg = String.
                        format("Connector instance bean for resource %s and connInstance %s not found",
                                syncTask.getResource(), syncTask.getResource().getConnector());

                throw new JobExecutionException(msg, e);
            }

            final UMapping uMapping = syncTask.getResource().getUmapping();
            if (uMapping != null && uMapping.getAccountIdItem() == null) {
                throw new JobExecutionException(
                        "Invalid user account id mapping for resource " + syncTask.getResource());
            }
            final GMapping rMapping = syncTask.getResource().getGmapping();
            if (rMapping != null && rMapping.getAccountIdItem() == null) {
                throw new JobExecutionException(
                        "Invalid group account id mapping for resource " + syncTask.getResource());
            }
            if (uMapping == null && rMapping == null) {
                return "No mapping configured for both users and groups: aborting...";
            }

            return executeWithSecurityContext(
                    syncTask,
                    connector,
                    uMapping,
                    rMapping,
                    dryRun);
        } catch (Throwable t) {
            LOG.error("While executing provisioning job {}", getClass().getName(), t);
            throw t;
        } finally {
            // POST: clean up the SecurityContextHolder
            SecurityContextHolder.clearContext();
        }
    }

    protected abstract String executeWithSecurityContext(
            final T task,
            final Connector connector,
            final UMapping uMapping,
            final GMapping rMapping,
            final boolean dryRun) throws JobExecutionException;

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        final ProvisioningTask provTask = (ProvisioningTask) task;

        // True if either failed and failures have to be registered, or if ALL has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.FAILURE
                && provTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || provTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.SUMMARY.ordinal();
    }

    @SuppressWarnings("unchecked")
    private Class<T> getTaskClassReference() {
        return (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
