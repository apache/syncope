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
package org.apache.syncope.core.sync.impl;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.PushTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.quartz.AbstractTaskJob;
import org.apache.syncope.core.sync.AbstractSyncActions;
import org.apache.syncope.core.sync.SyncResult;
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
public abstract class AbstractSyncJob<H extends AbstractSyncopeResultHandler, A extends AbstractSyncActions<?>>
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
    protected ResourceDAO resourceDAO;

    /**
     * Entitlement DAO.
     */
    @Autowired
    protected EntitlementDAO entitlementDAO;

    /**
     * SyncJob actions.
     */
    protected A actions;

    public void setActions(final A actions) {
        this.actions = actions;
    }

    /**
     * Create a textual report of the synchronization, based on the trace level.
     *
     * @param syncResults Sync results
     * @param syncTraceLevel Sync trace level
     * @param dryRun dry run?
     * @return report as string
     */
    protected String createReport(final List<SyncResult> syncResults, final TraceLevel syncTraceLevel,
            final boolean dryRun) {

        if (syncTraceLevel == TraceLevel.NONE) {
            return null;
        }

        StringBuilder report = new StringBuilder();

        if (dryRun) {
            report.append("==>Dry run only, no modifications were made<==\n\n");
        }

        List<SyncResult> uSuccCreate = new ArrayList<SyncResult>();
        List<SyncResult> uFailCreate = new ArrayList<SyncResult>();
        List<SyncResult> uSuccUpdate = new ArrayList<SyncResult>();
        List<SyncResult> uFailUpdate = new ArrayList<SyncResult>();
        List<SyncResult> uSuccDelete = new ArrayList<SyncResult>();
        List<SyncResult> uFailDelete = new ArrayList<SyncResult>();
        List<SyncResult> rSuccCreate = new ArrayList<SyncResult>();
        List<SyncResult> rFailCreate = new ArrayList<SyncResult>();
        List<SyncResult> rSuccUpdate = new ArrayList<SyncResult>();
        List<SyncResult> rFailUpdate = new ArrayList<SyncResult>();
        List<SyncResult> rSuccDelete = new ArrayList<SyncResult>();
        List<SyncResult> rFailDelete = new ArrayList<SyncResult>();

        for (SyncResult syncResult : syncResults) {
            switch (syncResult.getStatus()) {
                case SUCCESS:
                    switch (syncResult.getOperation()) {
                        case CREATE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uSuccCreate.add(syncResult);
                                    break;

                                case ROLE:
                                    rSuccCreate.add(syncResult);
                                    break;

                                default:
                            }
                            break;

                        case UPDATE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uSuccUpdate.add(syncResult);
                                    break;

                                case ROLE:
                                    rSuccUpdate.add(syncResult);
                                    break;

                                default:
                            }
                            break;

                        case DELETE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uSuccDelete.add(syncResult);
                                    break;

                                case ROLE:
                                    rSuccDelete.add(syncResult);
                                    break;

                                default:
                            }
                            break;

                        default:
                    }
                    break;

                case FAILURE:
                    switch (syncResult.getOperation()) {
                        case CREATE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uFailCreate.add(syncResult);
                                    break;

                                case ROLE:
                                    rFailCreate.add(syncResult);
                                    break;

                                default:
                            }
                            break;

                        case UPDATE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uFailUpdate.add(syncResult);
                                    break;

                                case ROLE:
                                    rFailUpdate.add(syncResult);
                                    break;

                                default:
                            }
                            break;

                        case DELETE:
                            switch (syncResult.getSubjectType()) {
                                case USER:
                                    uFailDelete.add(syncResult);
                                    break;

                                case ROLE:
                                    rFailDelete.add(syncResult);
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
                append('\n');
        report.append("Roles ").
                append("[created/failures]: ").append(rSuccCreate.size()).append('/').append(rFailCreate.size()).
                append(' ').
                append("[updated/failures]: ").append(rSuccUpdate.size()).append('/').append(rFailUpdate.size()).
                append(' ').
                append("[deleted/failures]: ").append(rSuccDelete.size()).append('/').append(rFailDelete.size());

        // Failures
        if (syncTraceLevel == TraceLevel.FAILURES || syncTraceLevel == TraceLevel.ALL) {
            if (!uFailCreate.isEmpty()) {
                report.append("\n\nUsers failed to create: ");
                report.append(SyncResult.produceReport(uFailCreate, syncTraceLevel));
            }
            if (!uFailUpdate.isEmpty()) {
                report.append("\nUsers failed to update: ");
                report.append(SyncResult.produceReport(uFailUpdate, syncTraceLevel));
            }
            if (!uFailDelete.isEmpty()) {
                report.append("\nUsers failed to delete: ");
                report.append(SyncResult.produceReport(uFailDelete, syncTraceLevel));
            }

            if (!rFailCreate.isEmpty()) {
                report.append("\n\nRoles failed to create: ");
                report.append(SyncResult.produceReport(rFailCreate, syncTraceLevel));
            }
            if (!rFailUpdate.isEmpty()) {
                report.append("\nRoles failed to update: ");
                report.append(SyncResult.produceReport(rFailUpdate, syncTraceLevel));
            }
            if (!rFailDelete.isEmpty()) {
                report.append("\nRoles failed to delete: ");
                report.append(SyncResult.produceReport(rFailDelete, syncTraceLevel));
            }
        }

        // Succeeded, only if on 'ALL' level
        if (syncTraceLevel == TraceLevel.ALL) {
            report.append("\n\nUsers created:\n")
                    .append(SyncResult.produceReport(uSuccCreate, syncTraceLevel))
                    .append("\nUsers updated:\n")
                    .append(SyncResult.produceReport(uSuccUpdate, syncTraceLevel))
                    .append("\nUsers deleted:\n")
                    .append(SyncResult.produceReport(uSuccDelete, syncTraceLevel));
            report.append("\n\nRoles created:\n")
                    .append(SyncResult.produceReport(rSuccCreate, syncTraceLevel))
                    .append("\nRoles updated:\n")
                    .append(SyncResult.produceReport(rSuccUpdate, syncTraceLevel))
                    .append("\nRoles deleted:\n")
                    .append(SyncResult.produceReport(rSuccDelete, syncTraceLevel));
        }

        return report.toString();
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        // PRE: grant all authorities (i.e. setup the SecurityContextHolder)
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
        }

        final UserDetails userDetails = new User("admin", "FAKE_PASSWORD", true, true, true, true, authorities);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities));

        try {
            return executeWithSecurityContext(dryRun);
        } finally {
            // POST: clean up the SecurityContextHolder
            SecurityContextHolder.clearContext();
        }
    }

    protected abstract String executeWithSecurityContext(final boolean dryRun) throws JobExecutionException;

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        final AbstractSyncTask syncTask = (AbstractSyncTask) task;

        // True if either failed and failures have to be registered, or if ALL has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.FAILURE
                && syncTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || syncTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.SUMMARY.ordinal();
    }
}
