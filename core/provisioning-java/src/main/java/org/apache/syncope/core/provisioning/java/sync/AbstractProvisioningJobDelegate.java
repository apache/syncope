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
import javax.annotation.Resource;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningReport;
import org.apache.syncope.core.provisioning.java.job.AbstractSchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractProvisioningJobDelegate<T extends ProvisioningTask>
        extends AbstractSchedTaskJobDelegate {

    @Resource(name = "adminUser")
    protected String adminUser;

    /**
     * ConnInstance loader.
     */
    @Autowired
    protected ConnectorFactory connFactory;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    /**
     * Resource DAO.
     */
    @Autowired
    protected ExternalResourceDAO resourceDAO;

    /**
     * Policy DAO.
     */
    @Autowired
    protected PolicyDAO policyDAO;

    /**
     * Create a textual report of the synchronization, based on the trace level.
     *
     * @param provResults Sync results
     * @param syncTraceLevel Sync trace level
     * @param dryRun dry run?
     * @return report as string
     */
    protected String createReport(final Collection<ProvisioningReport> provResults, final TraceLevel syncTraceLevel,
            final boolean dryRun) {

        if (syncTraceLevel == TraceLevel.NONE) {
            return null;
        }

        StringBuilder report = new StringBuilder();

        if (dryRun) {
            report.append("==>Dry run only, no modifications were made<==\n\n");
        }

        List<ProvisioningReport> uSuccCreate = new ArrayList<>();
        List<ProvisioningReport> uFailCreate = new ArrayList<>();
        List<ProvisioningReport> uSuccUpdate = new ArrayList<>();
        List<ProvisioningReport> uFailUpdate = new ArrayList<>();
        List<ProvisioningReport> uSuccDelete = new ArrayList<>();
        List<ProvisioningReport> uFailDelete = new ArrayList<>();
        List<ProvisioningReport> uSuccNone = new ArrayList<>();
        List<ProvisioningReport> uIgnore = new ArrayList<>();
        List<ProvisioningReport> gSuccCreate = new ArrayList<>();
        List<ProvisioningReport> gFailCreate = new ArrayList<>();
        List<ProvisioningReport> gSuccUpdate = new ArrayList<>();
        List<ProvisioningReport> gFailUpdate = new ArrayList<>();
        List<ProvisioningReport> gSuccDelete = new ArrayList<>();
        List<ProvisioningReport> gFailDelete = new ArrayList<>();
        List<ProvisioningReport> gSuccNone = new ArrayList<>();
        List<ProvisioningReport> gIgnore = new ArrayList<>();
        List<ProvisioningReport> aSuccCreate = new ArrayList<>();
        List<ProvisioningReport> aFailCreate = new ArrayList<>();
        List<ProvisioningReport> aSuccUpdate = new ArrayList<>();
        List<ProvisioningReport> aFailUpdate = new ArrayList<>();
        List<ProvisioningReport> aSuccDelete = new ArrayList<>();
        List<ProvisioningReport> aFailDelete = new ArrayList<>();
        List<ProvisioningReport> aSuccNone = new ArrayList<>();
        List<ProvisioningReport> aIgnore = new ArrayList<>();

        for (ProvisioningReport provResult : provResults) {
            AnyType anyType = anyTypeDAO.find(provResult.getAnyType());

            switch (provResult.getStatus()) {
                case SUCCESS:
                    switch (provResult.getOperation()) {
                        case CREATE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uSuccCreate.add(provResult);
                                    break;

                                case GROUP:
                                    gSuccCreate.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aSuccCreate.add(provResult);
                            }
                            break;

                        case UPDATE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uSuccUpdate.add(provResult);
                                    break;

                                case GROUP:
                                    gSuccUpdate.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aSuccUpdate.add(provResult);
                            }
                            break;

                        case DELETE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uSuccDelete.add(provResult);
                                    break;

                                case GROUP:
                                    gSuccDelete.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aSuccDelete.add(provResult);
                            }
                            break;

                        case NONE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uSuccNone.add(provResult);
                                    break;

                                case GROUP:
                                    gSuccNone.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aSuccNone.add(provResult);
                            }
                            break;

                        default:
                    }
                    break;

                case FAILURE:
                    switch (provResult.getOperation()) {
                        case CREATE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uFailCreate.add(provResult);
                                    break;

                                case GROUP:
                                    gFailCreate.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aFailCreate.add(provResult);
                            }
                            break;

                        case UPDATE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uFailUpdate.add(provResult);
                                    break;

                                case GROUP:
                                    gFailUpdate.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aFailUpdate.add(provResult);
                            }
                            break;

                        case DELETE:
                            switch (anyType.getKind()) {
                                case USER:
                                    uFailDelete.add(provResult);
                                    break;

                                case GROUP:
                                    gFailDelete.add(provResult);
                                    break;

                                case ANY_OBJECT:
                                default:
                                    aFailDelete.add(provResult);
                            }
                            break;

                        default:
                    }
                    break;

                case IGNORE:
                    switch (anyType.getKind()) {
                        case USER:
                            uIgnore.add(provResult);
                            break;

                        case GROUP:
                            gIgnore.add(provResult);
                            break;

                        case ANY_OBJECT:
                        default:
                            aIgnore.add(provResult);
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
                append("[no operation/ignored]: ").append(uSuccNone.size()).append('/').append(uIgnore.size()).
                append('\n');
        report.append("Groups ").
                append("[created/failures]: ").append(gSuccCreate.size()).append('/').append(gFailCreate.size()).
                append(' ').
                append("[updated/failures]: ").append(gSuccUpdate.size()).append('/').append(gFailUpdate.size()).
                append(' ').
                append("[deleted/failures]: ").append(gSuccDelete.size()).append('/').append(gFailDelete.size()).
                append(' ').
                append("[no operation/ignored]: ").append(gSuccNone.size()).append('/').append(gIgnore.size()).
                append('\n');
        report.append("Any objects ").
                append("[created/failures]: ").append(aSuccCreate.size()).append('/').append(aFailCreate.size()).
                append(' ').
                append("[updated/failures]: ").append(aSuccUpdate.size()).append('/').append(aFailUpdate.size()).
                append(' ').
                append("[deleted/failures]: ").append(aSuccDelete.size()).append('/').append(aFailDelete.size()).
                append(' ').
                append("[no operation/ignored]: ").append(aSuccNone.size()).append('/').append(aIgnore.size());

        // Failures
        if (syncTraceLevel == TraceLevel.FAILURES || syncTraceLevel == TraceLevel.ALL) {
            if (!uFailCreate.isEmpty()) {
                report.append("\n\nUsers failed to create: ");
                report.append(ProvisioningReport.produceReport(uFailCreate, syncTraceLevel));
            }
            if (!uFailUpdate.isEmpty()) {
                report.append("\nUsers failed to update: ");
                report.append(ProvisioningReport.produceReport(uFailUpdate, syncTraceLevel));
            }
            if (!uFailDelete.isEmpty()) {
                report.append("\nUsers failed to delete: ");
                report.append(ProvisioningReport.produceReport(uFailDelete, syncTraceLevel));
            }

            if (!gFailCreate.isEmpty()) {
                report.append("\n\nGroups failed to create: ");
                report.append(ProvisioningReport.produceReport(gFailCreate, syncTraceLevel));
            }
            if (!gFailUpdate.isEmpty()) {
                report.append("\nGroups failed to update: ");
                report.append(ProvisioningReport.produceReport(gFailUpdate, syncTraceLevel));
            }
            if (!gFailDelete.isEmpty()) {
                report.append("\nGroups failed to delete: ");
                report.append(ProvisioningReport.produceReport(gFailDelete, syncTraceLevel));
            }

            if (!aFailCreate.isEmpty()) {
                report.append("\nAny objects failed to create: ");
                report.append(ProvisioningReport.produceReport(aFailCreate, syncTraceLevel));
            }
            if (!aFailUpdate.isEmpty()) {
                report.append("\nAny objects failed to update: ");
                report.append(ProvisioningReport.produceReport(aFailUpdate, syncTraceLevel));
            }
            if (!aFailDelete.isEmpty()) {
                report.append("\nAny objects failed to delete: ");
                report.append(ProvisioningReport.produceReport(aFailDelete, syncTraceLevel));
            }
        }

        // Succeeded, only if on 'ALL' level
        if (syncTraceLevel == TraceLevel.ALL) {
            report.append("\n\nUsers created:\n").
                    append(ProvisioningReport.produceReport(uSuccCreate, syncTraceLevel)).
                    append("\nUsers updated:\n").
                    append(ProvisioningReport.produceReport(uSuccUpdate, syncTraceLevel)).
                    append("\nUsers deleted:\n").
                    append(ProvisioningReport.produceReport(uSuccDelete, syncTraceLevel)).
                    append("\nUsers no operation:\n").
                    append(ProvisioningReport.produceReport(uSuccNone, syncTraceLevel)).
                    append("\nUsers ignored:\n").
                    append(ProvisioningReport.produceReport(uIgnore, syncTraceLevel));
            report.append("\n\nGroups created:\n").
                    append(ProvisioningReport.produceReport(gSuccCreate, syncTraceLevel)).
                    append("\nGroups updated:\n").
                    append(ProvisioningReport.produceReport(gSuccUpdate, syncTraceLevel)).
                    append("\nGroups deleted:\n").
                    append(ProvisioningReport.produceReport(gSuccDelete, syncTraceLevel)).
                    append("\nGroups no operation:\n").
                    append(ProvisioningReport.produceReport(gSuccNone, syncTraceLevel)).
                    append("\nGroups ignored:\n").
                    append(ProvisioningReport.produceReport(gSuccNone, syncTraceLevel));
            report.append("\n\nAny objects created:\n").
                    append(ProvisioningReport.produceReport(aSuccCreate, syncTraceLevel)).
                    append("\nAny objects updated:\n").
                    append(ProvisioningReport.produceReport(aSuccUpdate, syncTraceLevel)).
                    append("\nAny objects deleted:\n").
                    append(ProvisioningReport.produceReport(aSuccDelete, syncTraceLevel)).
                    append("\nAny objects no operation:\n").
                    append(ProvisioningReport.produceReport(aSuccNone, syncTraceLevel)).
                    append("\nAny objects ignored:\n").
                    append(ProvisioningReport.produceReport(aSuccNone, syncTraceLevel));
        }

        return report.toString();
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        try {
            Class<T> clazz = getTaskClassReference();
            if (!clazz.isAssignableFrom(task.getClass())) {
                throw new JobExecutionException("Task " + task.getKey() + " isn't a ProvisioningTask");
            }

            T provisioningTask = clazz.cast(task);

            Connector connector;
            try {
                connector = connFactory.getConnector(provisioningTask.getResource());
            } catch (Exception e) {
                String msg = String.format("Connector instance bean for resource %s and connInstance %s not found",
                        provisioningTask.getResource(), provisioningTask.getResource().getConnector());
                throw new JobExecutionException(msg, e);
            }

            boolean noMapping = true;
            for (Provision provision : provisioningTask.getResource().getProvisions()) {
                Mapping mapping = provision.getMapping();
                if (mapping != null) {
                    noMapping = false;
                    if (mapping.getConnObjectKeyItem() == null) {
                        throw new JobExecutionException(
                                "Invalid ConnObjectKey mapping for provision " + provision);
                    }
                }
            }
            if (noMapping) {
                return "No mapping configured for both users and groups: aborting...";
            }

            return doExecuteProvisioning(
                    provisioningTask,
                    connector,
                    dryRun);
        } catch (Throwable t) {
            LOG.error("While executing provisioning job {}", getClass().getName(), t);
            throw t;
        }
    }

    protected abstract String doExecuteProvisioning(
            final T task,
            final Connector connector,
            final boolean dryRun) throws JobExecutionException;

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        final ProvisioningTask provTask = (ProvisioningTask) task;

        // True if either failed and failures have to be registered, or if ALL has to be registered.
        return (TaskJob.Status.valueOf(execution.getStatus()) == TaskJob.Status.FAILURE
                && provTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || provTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.SUMMARY.ordinal();
    }

    @SuppressWarnings("unchecked")
    private Class<T> getTaskClassReference() {
        return (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
