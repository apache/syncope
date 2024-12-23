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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.java.job.AbstractSchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractProvisioningJobDelegate<T extends ProvisioningTask<T>>
        extends AbstractSchedTaskJobDelegate<T> {

    private static final String USER = "USER";

    private static final String GROUP = "GROUP";

    private static final String LINKED_ACCOUNT = "LINKED_ACCOUNT";

    @Autowired
    protected ConnectorManager connectorManager;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected PolicyDAO policyDAO;

    protected Optional<ProvisionSorter> perContextProvisionSorter = Optional.empty();

    protected Connector connector;

    @Override
    protected void init(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context) throws JobExecutionException {

        super.init(taskType, taskKey, context);

        boolean noMapping = true;
        for (Provision provision : task.getResource().getProvisions().stream().
                filter(provision -> provision.getMapping() != null).toList()) {

            noMapping = false;
            if (provision.getMapping().getConnObjectKeyItem() == null) {
                throw new JobExecutionException("Invalid ConnObjectKey mapping for provision " + provision);
            }
        }
        if (noMapping) {
            noMapping = task.getResource().getOrgUnit() == null;
        }
        if (noMapping) {
            throw new JobExecutionException("No provisions nor orgUnit available: aborting...");
        }

        connector = connectorManager.getConnector(task.getResource());
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        // True if either failed and failures have to be registered, or if ALL has to be registered.
        return (TaskJob.Status.valueOf(execution.getStatus()) == TaskJob.Status.FAILURE
                && task.getResource().getProvisioningTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || task.getResource().getProvisioningTraceLevel().ordinal() >= TraceLevel.SUMMARY.ordinal();
    }

    protected ProvisionSorter getProvisionSorter(final T task) {
        if (task.getResource().getProvisionSorter() != null) {
            try {
                return ImplementationManager.build(
                        task.getResource().getProvisionSorter(),
                        () -> perContextProvisionSorter.orElse(null),
                        instance -> perContextProvisionSorter = Optional.of(instance));
            } catch (Exception e) {
                LOG.error("While building {}", task.getResource().getProvisionSorter(), e);
            }
        }

        if (perContextProvisionSorter.isEmpty()) {
            perContextProvisionSorter = Optional.of(new DefaultProvisionSorter());
        }
        return perContextProvisionSorter.get();
    }

    /**
     * Helper method to invoke logging per provisioning result, for the given trace level.
     *
     * @param results provisioning results
     * @param level trace level
     * @return report as string
     */
    protected String generate(final Collection<ProvisioningReport> results, final TraceLevel level) {
        StringBuilder sb = new StringBuilder();

        results.stream().map(result -> {
            if (level == TraceLevel.SUMMARY) {
                // No per entry log in this case.
                return null;
            } else if (level == TraceLevel.FAILURES && result.getStatus() == ProvisioningReport.Status.FAILURE) {
                // only report failures
                return String.format("Failed %s (key/name): %s/%s with message: %s",
                        result.getOperation(), result.getKey(), result.getName(), result.getMessage());
            } else {
                // All
                return String.format("%s %s (key/name): %s/%s %s",
                        result.getOperation(), result.getStatus(), result.getKey(), result.getName(),
                        StringUtils.isBlank(result.getMessage())
                        ? StringUtils.EMPTY
                        : "with message: " + result.getMessage());
            }
        }).filter(Objects::nonNull).forEach(report -> sb.append(report).append('\n'));

        return sb.toString();
    }

    /**
     * Create a textual report of the provisioning operation, based on the trace level.
     *
     * @param provResults Provisioning results
     * @param resource Provisioning resource
     * @param dryRun dry run?
     * @return report as string
     */
    protected String createReport(
            final Collection<ProvisioningReport> provResults,
            final ExternalResource resource,
            final boolean dryRun) {

        TraceLevel traceLevel = resource.getProvisioningTraceLevel();
        if (traceLevel == TraceLevel.NONE) {
            return null;
        }

        StringBuilder report = new StringBuilder();

        if (dryRun) {
            report.append("==> Dry run only, no modifications were made <==\n\n");
        }

        List<ProvisioningReport> rSuccCreate = new ArrayList<>();
        List<ProvisioningReport> rFailCreate = new ArrayList<>();
        List<ProvisioningReport> rSuccUpdate = new ArrayList<>();
        List<ProvisioningReport> rFailUpdate = new ArrayList<>();
        List<ProvisioningReport> rSuccDelete = new ArrayList<>();
        List<ProvisioningReport> rFailDelete = new ArrayList<>();
        List<ProvisioningReport> rSuccNone = new ArrayList<>();
        List<ProvisioningReport> rIgnore = new ArrayList<>();
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
        List<ProvisioningReport> laSuccCreate = new ArrayList<>();
        List<ProvisioningReport> laFailCreate = new ArrayList<>();
        List<ProvisioningReport> laSuccUpdate = new ArrayList<>();
        List<ProvisioningReport> laFailUpdate = new ArrayList<>();
        List<ProvisioningReport> laSuccDelete = new ArrayList<>();
        List<ProvisioningReport> laFailDelete = new ArrayList<>();
        List<ProvisioningReport> laSuccNone = new ArrayList<>();
        List<ProvisioningReport> laIgnore = new ArrayList<>();

        for (ProvisioningReport provResult : provResults) {
            switch (provResult.getStatus()) {
                case SUCCESS -> {
                    switch (provResult.getOperation()) {
                        case CREATE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rSuccCreate.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uSuccCreate.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laSuccCreate.add(provResult);

                                    case GROUP ->
                                        gSuccCreate.add(provResult);

                                    default ->
                                        aSuccCreate.add(provResult);
                                }
                            }
                        }

                        case UPDATE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rSuccUpdate.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uSuccUpdate.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laSuccUpdate.add(provResult);

                                    case GROUP ->
                                        gSuccUpdate.add(provResult);

                                    default ->
                                        aSuccUpdate.add(provResult);
                                }
                            }
                        }

                        case DELETE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rSuccDelete.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uSuccDelete.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laSuccDelete.add(provResult);

                                    case GROUP ->
                                        gSuccDelete.add(provResult);

                                    default ->
                                        aSuccDelete.add(provResult);
                                }
                            }
                        }

                        case NONE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rSuccNone.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uSuccNone.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laSuccNone.add(provResult);

                                    case GROUP ->
                                        gSuccNone.add(provResult);

                                    default ->
                                        aSuccNone.add(provResult);
                                }
                            }
                        }

                        default -> {
                        }
                    }
                }

                case FAILURE -> {
                    switch (provResult.getOperation()) {
                        case CREATE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rFailCreate.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uFailCreate.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laFailCreate.add(provResult);

                                    case GROUP ->
                                        gFailCreate.add(provResult);

                                    default ->
                                        aFailCreate.add(provResult);
                                }
                            }
                        }

                        case UPDATE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rFailUpdate.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uFailUpdate.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laFailUpdate.add(provResult);

                                    case GROUP ->
                                        gFailUpdate.add(provResult);

                                    default ->
                                        aFailUpdate.add(provResult);
                                }
                            }
                        }

                        case DELETE -> {
                            if (StringUtils.isBlank(provResult.getAnyType())) {
                                rFailDelete.add(provResult);
                            } else {
                                switch (provResult.getAnyType()) {
                                    case USER ->
                                        uFailDelete.add(provResult);

                                    case LINKED_ACCOUNT ->
                                        laFailDelete.add(provResult);

                                    case GROUP ->
                                        gFailDelete.add(provResult);

                                    default ->
                                        aFailDelete.add(provResult);
                                }
                            }
                        }

                        default -> {
                        }
                    }
                }

                case IGNORE -> {
                    if (StringUtils.isBlank(provResult.getAnyType())) {
                        rIgnore.add(provResult);
                    } else {
                        switch (provResult.getAnyType()) {
                            case USER ->
                                uIgnore.add(provResult);

                            case LINKED_ACCOUNT ->
                                laIgnore.add(provResult);

                            case GROUP ->
                                gIgnore.add(provResult);

                            default ->
                                aIgnore.add(provResult);
                        }
                    }
                }

                default -> {
                }
            }
        }

        // Summary, also to be included for FAILURE and ALL, so create it anyway.
        boolean includeUser = resource.getProvisionByAnyType(AnyTypeKind.USER.name()).isPresent();
        boolean includeGroup = resource.getProvisionByAnyType(AnyTypeKind.GROUP.name()).isPresent();
        boolean includeAnyObject = resource.getProvisions().stream().anyMatch(
                provision -> !provision.getAnyType().equals(AnyTypeKind.USER.name())
                && !provision.getAnyType().equals(AnyTypeKind.GROUP.name()));
        boolean includeRealm = resource.getOrgUnit() != null;

        if (includeUser) {
            report.append("Users ").
                    append("[created/failures]: ").append(uSuccCreate.size()).append('/').append(uFailCreate.size()).
                    append(' ').
                    append("[updated/failures]: ").append(uSuccUpdate.size()).append('/').append(uFailUpdate.size()).
                    append(' ').
                    append("[deleted/failures]: ").append(uSuccDelete.size()).append('/').append(uFailDelete.size()).
                    append(' ').
                    append("[no operation/ignored]: ").append(uSuccNone.size()).append('/').append(uIgnore.size()).
                    append('\n');

            report.append("Accounts ").
                    append("[created/failures]: ").append(laSuccCreate.size()).append('/').append(laFailCreate.size()).
                    append(' ').
                    append("[updated/failures]: ").append(laSuccUpdate.size()).append('/').append(laFailUpdate.size()).
                    append(' ').
                    append("[deleted/failures]: ").append(laSuccDelete.size()).append('/').append(laFailDelete.size()).
                    append(' ').
                    append("[no operation/ignored]: ").append(laSuccNone.size()).append('/').append(laIgnore.size()).
                    append('\n');
        }
        if (includeGroup) {
            report.append("Groups ").
                    append("[created/failures]: ").append(gSuccCreate.size()).append('/').append(gFailCreate.size()).
                    append(' ').
                    append("[updated/failures]: ").append(gSuccUpdate.size()).append('/').append(gFailUpdate.size()).
                    append(' ').
                    append("[deleted/failures]: ").append(gSuccDelete.size()).append('/').append(gFailDelete.size()).
                    append(' ').
                    append("[no operation/ignored]: ").append(gSuccNone.size()).append('/').append(gIgnore.size()).
                    append('\n');
        }
        if (includeAnyObject) {
            report.append("Any objects ").
                    append("[created/failures]: ").append(aSuccCreate.size()).append('/').append(aFailCreate.size()).
                    append(' ').
                    append("[updated/failures]: ").append(aSuccUpdate.size()).append('/').append(aFailUpdate.size()).
                    append(' ').
                    append("[deleted/failures]: ").append(aSuccDelete.size()).append('/').append(aFailDelete.size()).
                    append(' ').
                    append("[no operation/ignored]: ").append(aSuccNone.size()).append('/').append(aIgnore.size());
        }
        if (includeRealm) {
            report.append("Realms ").
                    append("[created/failures]: ").append(rSuccCreate.size()).append('/').append(rFailCreate.size()).
                    append(' ').
                    append("[updated/failures]: ").append(rSuccUpdate.size()).append('/').append(rFailUpdate.size()).
                    append(' ').
                    append("[deleted/failures]: ").append(rSuccDelete.size()).append('/').append(rFailDelete.size()).
                    append(' ').
                    append("[no operation/ignored]: ").append(rSuccNone.size()).append('/').append(rIgnore.size());
        }

        // Failures
        if (traceLevel == TraceLevel.FAILURES || traceLevel == TraceLevel.ALL) {
            if (includeUser) {
                if (!uFailCreate.isEmpty()) {
                    report.append("\n\nUsers failed to create: ");
                    report.append(generate(uFailCreate, traceLevel));
                }
                if (!uFailUpdate.isEmpty()) {
                    report.append("\nUsers failed to update: ");
                    report.append(generate(uFailUpdate, traceLevel));
                }
                if (!uFailDelete.isEmpty()) {
                    report.append("\nUsers failed to delete: ");
                    report.append(generate(uFailDelete, traceLevel));
                }

                if (!laFailCreate.isEmpty()) {
                    report.append("\n\nAccounts failed to create: ");
                    report.append(generate(laFailCreate, traceLevel));
                }
                if (!laFailUpdate.isEmpty()) {
                    report.append("\nAccounts failed to update: ");
                    report.append(generate(laFailUpdate, traceLevel));
                }
                if (!laFailDelete.isEmpty()) {
                    report.append("\nAccounts failed to delete: ");
                    report.append(generate(laFailDelete, traceLevel));
                }
            }

            if (includeGroup) {
                if (!gFailCreate.isEmpty()) {
                    report.append("\n\nGroups failed to create: ");
                    report.append(generate(gFailCreate, traceLevel));
                }
                if (!gFailUpdate.isEmpty()) {
                    report.append("\nGroups failed to update: ");
                    report.append(generate(gFailUpdate, traceLevel));
                }
                if (!gFailDelete.isEmpty()) {
                    report.append("\nGroups failed to delete: ");
                    report.append(generate(gFailDelete, traceLevel));
                }
            }

            if (includeAnyObject && !aFailCreate.isEmpty()) {
                report.append("\nAny objects failed to create: ");
                report.append(generate(aFailCreate, traceLevel));
            }
            if (includeAnyObject && !aFailUpdate.isEmpty()) {
                report.append("\nAny objects failed to update: ");
                report.append(generate(aFailUpdate, traceLevel));
            }
            if (includeAnyObject && !aFailDelete.isEmpty()) {
                report.append("\nAny objects failed to delete: ");
                report.append(generate(aFailDelete, traceLevel));
            }

            if (includeRealm) {
                if (!rFailCreate.isEmpty()) {
                    report.append("\nRealms failed to create: ");
                    report.append(generate(rFailCreate, traceLevel));
                }
                if (!rFailUpdate.isEmpty()) {
                    report.append("\nRealms failed to update: ");
                    report.append(generate(rFailUpdate, traceLevel));
                }
                if (!rFailDelete.isEmpty()) {
                    report.append("\nRealms failed to delete: ");
                    report.append(generate(rFailDelete, traceLevel));
                }
            }
        }

        // Succeeded, only if on 'ALL' level
        if (traceLevel == TraceLevel.ALL) {
            if (includeUser) {
                if (!uSuccCreate.isEmpty()) {
                    report.append("\n\nUsers created:\n").
                            append(generate(uSuccCreate, traceLevel));
                }
                if (!uSuccUpdate.isEmpty()) {
                    report.append("\nUsers updated:\n").
                            append(generate(uSuccUpdate, traceLevel));
                }
                if (!uSuccDelete.isEmpty()) {
                    report.append("\nUsers deleted:\n").
                            append(generate(uSuccDelete, traceLevel));
                }
                if (!uSuccNone.isEmpty()) {
                    report.append("\nUsers no operation:\n").
                            append(generate(uSuccNone, traceLevel));
                }
                if (!uIgnore.isEmpty()) {
                    report.append("\nUsers ignored:\n").
                            append(generate(uIgnore, traceLevel));
                }

                if (!laSuccCreate.isEmpty()) {
                    report.append("\n\nAccounts created:\n").
                            append(generate(laSuccCreate, traceLevel));
                }
                if (!laSuccUpdate.isEmpty()) {
                    report.append("\nAccounts updated:\n").
                            append(generate(laSuccUpdate, traceLevel));
                }
                if (!laSuccDelete.isEmpty()) {
                    report.append("\nAccounts deleted:\n").
                            append(generate(laSuccDelete, traceLevel));
                }
                if (!laSuccNone.isEmpty()) {
                    report.append("\nAccounts no operation:\n").
                            append(generate(laSuccNone, traceLevel));
                }
                if (!laIgnore.isEmpty()) {
                    report.append("\nAccounts ignored:\n").
                            append(generate(laIgnore, traceLevel));
                }
            }
            if (includeGroup) {
                if (!gSuccCreate.isEmpty()) {
                    report.append("\n\nGroups created:\n").
                            append(generate(gSuccCreate, traceLevel));
                }
                if (!gSuccUpdate.isEmpty()) {
                    report.append("\nGroups updated:\n").
                            append(generate(gSuccUpdate, traceLevel));
                }
                if (!gSuccDelete.isEmpty()) {
                    report.append("\nGroups deleted:\n").
                            append(generate(gSuccDelete, traceLevel));
                }
                if (!gSuccNone.isEmpty()) {
                    report.append("\nGroups no operation:\n").
                            append(generate(gSuccNone, traceLevel));
                }
                if (!gIgnore.isEmpty()) {
                    report.append("\nGroups ignored:\n").
                            append(generate(gIgnore, traceLevel));
                }
            }
            if (includeAnyObject) {
                if (!aSuccCreate.isEmpty()) {
                    report.append("\n\nAny objects created:\n").
                            append(generate(aSuccCreate, traceLevel));
                }
                if (!aSuccUpdate.isEmpty()) {
                    report.append("\nAny objects updated:\n").
                            append(generate(aSuccUpdate, traceLevel));
                }
                if (!aSuccDelete.isEmpty()) {
                    report.append("\nAny objects deleted:\n").
                            append(generate(aSuccDelete, traceLevel));
                }
                if (!aSuccNone.isEmpty()) {
                    report.append("\nAny objects no operation:\n").
                            append(generate(aSuccNone, traceLevel));
                }
                if (!aIgnore.isEmpty()) {
                    report.append("\nAny objects ignored:\n").
                            append(generate(aIgnore, traceLevel));
                }
            }
            if (includeRealm) {
                if (!rSuccCreate.isEmpty()) {
                    report.append("\n\nRealms created:\n").
                            append(generate(rSuccCreate, traceLevel));
                }
                if (!rSuccUpdate.isEmpty()) {
                    report.append("\nRealms updated:\n").
                            append(generate(rSuccUpdate, traceLevel));
                }
                if (!rSuccDelete.isEmpty()) {
                    report.append("\nRealms deleted:\n").
                            append(generate(rSuccDelete, traceLevel));
                }
                if (!rSuccNone.isEmpty()) {
                    report.append("\nRealms no operation:\n").
                            append(generate(rSuccNone, traceLevel));
                }
                if (!rIgnore.isEmpty()) {
                    report.append("\nRealms ignored:\n").
                            append(generate(rIgnore, traceLevel));
                }
            }
        }

        return report.toString();
    }
}
