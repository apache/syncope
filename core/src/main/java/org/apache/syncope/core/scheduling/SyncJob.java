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
package org.apache.syncope.core.scheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.search.SyncopeUserCond;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.UserSearchDAO;
import org.apache.syncope.core.propagation.ConnectorFacadeProxy;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.rest.controller.InvalidSearchConditionException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.scheduling.SyncResult.Operation;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.util.SchemaMappingUtil;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.types.ConflictResolutionAction;
import org.apache.syncope.types.SyncPolicySpec;
import org.apache.syncope.types.TraceLevel;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
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
 * @see org.apache.syncope.core.scheduling.Job
 * @see SyncTask
 */
public class SyncJob extends AbstractTaskJob {

    /**
     * ConnInstance loader.
     */
    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    /**
     * Resource DAO.
     */
    @Autowired
    private ResourceDAO resourceDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserSearchDAO userSearchDAO;

    /**
     * Entitlement DAO.
     */
    @Autowired
    private EntitlementDAO entitlementDAO;

    /**
     * User workflow adapter.
     */
    @Autowired
    private UserWorkflowAdapter wfAdapter;

    /**
     * Propagation Manager.
     */
    @Autowired
    private PropagationManager propagationManager;

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * SyncJob actions.
     */
    private SyncJobActions actions;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    /**
     * Notification Manager.
     */
    @Autowired
    private NotificationManager notificationManager;

    public void setActions(final SyncJobActions actions) {
        this.actions = actions;
    }

    /**
     * Find users based on mapped uid value (or previous uid value, if updated).
     *
     * @param delta sync delta
     * @return list of matching users
     */
    private List<Long> findExistingUsers(final SyncDelta delta) {
        final SyncTask syncTask = (SyncTask) this.task;

        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        // ---------------------------------
        // Get sync policy specification
        // ---------------------------------
        SyncPolicySpec policySpec = null;
        if (syncTask.getResource().getSyncPolicy() != null) {
            policySpec = (SyncPolicySpec) syncTask.getResource().getSyncPolicy().getSpecification();
        }
        // ---------------------------------

        final List<Long> result = new ArrayList<Long>();

        if (policySpec == null || policySpec.getAlternativeSearchAttrs().isEmpty()) {
            SyncopeUser found;
            List<SyncopeUser> users;

            final SchemaMapping accountIdMap =
                    SchemaMappingUtil.getAccountIdMapping(syncTask.getResource().getMappings());
            switch (accountIdMap.getIntMappingType()) {
                case Username:
                    found = userDAO.find(uid);
                    if (found != null) {
                        result.add(found.getId());
                    }
                    break;

                case SyncopeUserId:
                    found = userDAO.find(Long.parseLong(uid));
                    if (found != null) {
                        result.add(found.getId());
                    }
                    break;

                case UserSchema:
                    final UAttrValue value = new UAttrValue();
                    value.setStringValue(uid);
                    users = userDAO.findByAttrValue(accountIdMap.getIntAttrName(), value);
                    for (SyncopeUser user : users) {
                        result.add(user.getId());
                    }
                    break;

                case UserDerivedSchema:
                    try {
                        users = userDAO.findByDerAttrValue(accountIdMap.getIntAttrName(), uid);
                        for (SyncopeUser user : users) {
                            result.add(user.getId());
                        }
                    } catch (InvalidSearchConditionException e) {
                        LOG.error("Could not search for matching users", e);
                    }
                    break;

                default:
                    LOG.error("Invalid accountId type '{}'", accountIdMap.getIntMappingType());
            }
        } else {
            // search for external attribute's name/value of each specified name

            final Map<String, Attribute> extValues = new HashMap<String, Attribute>();

            for (SchemaMapping mapping : syncTask.getResource().getMappings()) {
                extValues.put(SchemaMappingUtil.getIntAttrName(mapping),
                        delta.getObject().getAttributeByName(SchemaMappingUtil.getExtAttrName(mapping)));
            }

            // search for user by attribute(s) specified in the policy
            NodeCond searchCond = null;

            for (String schema : policySpec.getAlternativeSearchAttrs()) {
                Attribute value = extValues.get(schema);

                AttributeCond.Type type;
                String expression = null;

                if (value == null || value.getValue() == null || value.getValue().isEmpty()) {
                    type = AttributeCond.Type.ISNULL;
                } else {
                    type = AttributeCond.Type.EQ;
                    expression = value.getValue().size() > 1
                            ? value.getValue().toString()
                            : value.getValue().get(0).toString();
                }

                NodeCond nodeCond;
                // just Username or SyncopeUserId can be selected to be used
                if ("id".equalsIgnoreCase(schema) || "username".equalsIgnoreCase(schema)) {
                    SyncopeUserCond cond = new SyncopeUserCond();
                    cond.setSchema(schema);
                    cond.setType(type);
                    cond.setExpression(expression);

                    nodeCond = NodeCond.getLeafCond(cond);
                } else {
                    AttributeCond cond = new AttributeCond();
                    cond.setSchema(schema);
                    cond.setType(type);
                    cond.setExpression(expression);

                    nodeCond = NodeCond.getLeafCond(cond);
                }

                searchCond = searchCond == null
                        ? nodeCond
                        : NodeCond.getAndCond(searchCond, nodeCond);
            }

            final List<SyncopeUser> users =
                    userSearchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCond);
            for (SyncopeUser user : users) {
                result.add(user.getId());
            }
        }

        return result;
    }

    private List<SyncResult> createUser(SyncDelta delta, final boolean dryRun) throws JobExecutionException {
        if (!((SyncTask) task).isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.EMPTY_LIST;
        }

        final SyncResult result = new SyncResult();
        result.setOperation(Operation.CREATE);

        UserTO userTO = connObjectUtil.getUserTO(delta.getObject(), (SyncTask) task);

        delta = actions.beforeCreate(delta, userTO);

        if (dryRun) {
            result.setUserId(0L);
            result.setUsername(userTO.getUsername());
            result.setStatus(Status.SUCCESS);
        } else {
            try {
                Boolean enabled = null;

                // --------------------------
                // Check for status synchronization ...
                // --------------------------
                if (((SyncTask) this.task).isSyncStatus()) {
                    Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, delta.getObject()
                            .getAttributes());

                    if (status != null) {
                        enabled = status.getValue() != null && !status.getValue().isEmpty()
                                ? (Boolean) status.getValue().get(0)
                                : null;
                    }
                }
                // --------------------------

                WorkflowResult<Map.Entry<Long, Boolean>> created = wfAdapter.create(userTO, true, enabled);

                List<PropagationTask> tasks = propagationManager.getCreateTaskIds(created, userTO.getPassword(), userTO
                        .getVirtualAttributes(), Collections.singleton(((SyncTask) this.task).getResource().getName()));

                propagationManager.execute(tasks);

                notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

                userTO = userDataBinder.getUserTO(created.getResult().getKey());

                result.setUserId(created.getResult().getKey());
                result.setUsername(userTO.getUsername());
                result.setStatus(Status.SUCCESS);
            } catch (PropagationException e) {
                LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
            } catch (Exception e) {
                result.setStatus(Status.FAILURE);
                result.setMessage(e.getMessage());
                LOG.error("Could not create user " + delta.getUid().getUidValue(), e);
            }
        }

        actions.after(delta, userTO, result);
        return Collections.singletonList(result);
    }

    private List<SyncResult> updateUsers(SyncDelta delta, final List<Long> users, final boolean dryRun)
            throws JobExecutionException {

        if (!((SyncTask) task).isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.EMPTY_LIST;
        }

        LOG.debug("About to update {}", users);

        List<SyncResult> results = new ArrayList<SyncResult>();

        for (Long userId : users) {
            final SyncResult result = new SyncResult();
            result.setOperation(Operation.UPDATE);

            try {
                UserTO userTO = userDataBinder.getUserTO(userId);
                try {

                    final UserMod userMod = connObjectUtil.getUserMod(userId, delta.getObject(), (SyncTask) task);
                    delta = actions.beforeUpdate(delta, userTO, userMod);

                    result.setStatus(Status.SUCCESS);
                    result.setUserId(userMod.getId());
                    result.setUsername(userMod.getUsername());

                    if (!dryRun) {
                        WorkflowResult<Map.Entry<Long, Boolean>> updated = wfAdapter.update(userMod);

                        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(updated, userMod
                                .getPassword(), userMod.getVirtualAttributesToBeRemoved(), userMod
                                .getVirtualAttributesToBeUpdated(), Collections.singleton(((SyncTask) this.task)
                                .getResource().getName()));

                        propagationManager.execute(tasks);

                        notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

                        userTO = userDataBinder.getUserTO(updated.getResult().getKey());
                    }
                } catch (PropagationException e) {
                    LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
                } catch (Exception e) {
                    result.setStatus(Status.FAILURE);
                    result.setMessage(e.getMessage());
                    LOG.error("Could not update user " + delta.getUid().getUidValue(), e);
                }

                actions.after(delta, userTO, result);
                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find user {}", userId, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read user {}", userId, e);
            }
        }

        return results;
    }

    private List<SyncResult> deleteUsers(SyncDelta delta, final List<Long> users, final boolean dryRun)
            throws JobExecutionException {

        if (!((SyncTask) task).isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return Collections.EMPTY_LIST;
        }

        LOG.debug("About to delete {}", users);

        List<SyncResult> results = new ArrayList<SyncResult>();

        for (Long userId : users) {
            try {
                UserTO userTO = userDataBinder.getUserTO(userId);
                delta = actions.beforeDelete(delta, userTO);

                final SyncResult result = new SyncResult();
                result.setUserId(userId);
                result.setUsername(userTO.getUsername());
                result.setOperation(Operation.DELETE);
                result.setStatus(Status.SUCCESS);

                if (!dryRun) {
                    try {
                        List<PropagationTask> tasks = propagationManager.getDeleteTaskIds(userId,
                                ((SyncTask) this.task).getResource().getName());
                        propagationManager.execute(tasks);

                        notificationManager.createTasks(userId, Collections.singleton("delete"));

                    } catch (Exception e) {
                        LOG.error("Could not propagate user " + userId, e);
                    }

                    try {
                        wfAdapter.delete(userId);
                    } catch (Exception e) {
                        result.setStatus(Status.FAILURE);
                        result.setMessage(e.getMessage());
                        LOG.error("Could not delete user " + userId, e);
                    }
                }

                actions.after(delta, userTO, result);
                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find user {}", userId, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read user {}", userId, e);
            }
        }

        return results;
    }

    /**
     * Create a textual report of the synchronization, based on the trace level.
     *
     * @param syncResults Sync results
     * @param syncTraceLevel Sync trace level
     * @param dryRun dry run?
     * @return report as string
     */
    private String createReport(final List<SyncResult> syncResults, final TraceLevel syncTraceLevel,
            final boolean dryRun) {

        if (syncTraceLevel == TraceLevel.NONE) {
            return null;
        }

        StringBuilder report = new StringBuilder();

        if (dryRun) {
            report.append("==>Dry run only, no modifications were made<==\n\n");
        }

        List<SyncResult> created = new ArrayList<SyncResult>();
        List<SyncResult> createdFailed = new ArrayList<SyncResult>();
        List<SyncResult> updated = new ArrayList<SyncResult>();
        List<SyncResult> updatedFailed = new ArrayList<SyncResult>();
        List<SyncResult> deleted = new ArrayList<SyncResult>();
        List<SyncResult> deletedFailed = new ArrayList<SyncResult>();

        for (SyncResult syncResult : syncResults) {
            switch (syncResult.getStatus()) {
                case SUCCESS:
                    switch (syncResult.getOperation()) {
                        case CREATE:
                            created.add(syncResult);
                            break;

                        case UPDATE:
                            updated.add(syncResult);
                            break;

                        case DELETE:
                            deleted.add(syncResult);
                            break;

                        default:
                    }
                    break;

                case FAILURE:
                    switch (syncResult.getOperation()) {
                        case CREATE:
                            createdFailed.add(syncResult);
                            break;

                        case UPDATE:
                            updatedFailed.add(syncResult);
                            break;

                        case DELETE:
                            deletedFailed.add(syncResult);
                            break;

                        default:
                    }
                    break;

                default:
            }
        }

        // Summary, also to be included for FAILURE and ALL, so create it anyway.
        report.append("Users [created/failures]: ").append(created.size()).append('/').append(createdFailed.size())
                .append(' ').append("[updated/failures]: ").append(updated.size()).append('/').append(
                updatedFailed.size()).append(' ').append("[deleted/ failures]: ").append(deleted.size())
                .append('/').append(deletedFailed.size());

        // Failures
        if (syncTraceLevel == TraceLevel.FAILURES || syncTraceLevel == TraceLevel.ALL) {

            if (!createdFailed.isEmpty()) {
                report.append("\n\nFailed to create: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(createdFailed, syncTraceLevel));
            }
            if (!updatedFailed.isEmpty()) {
                report.append("\nFailed to update: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(updatedFailed, syncTraceLevel));
            }
            if (!deletedFailed.isEmpty()) {
                report.append("\nFailed to delete: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(deletedFailed, syncTraceLevel));
            }
        }

        // Succeeded, only if on 'ALL' level
        if (syncTraceLevel == TraceLevel.ALL) {
            report.append("\n\nCreated:\n")
                    .append(SyncResult.reportSetOfSynchronizationResult(created, syncTraceLevel))
                    .append("\nUpdated:\n")
                    .append(SyncResult.reportSetOfSynchronizationResult(updated, syncTraceLevel))
                    .append("\nDeleted:\n")
                    .append(SyncResult.reportSetOfSynchronizationResult(deleted, syncTraceLevel));
        }

        return report.toString();
    }

    /**
     * Used to simulate authentication in order to perform updates through AbstractUserWorkflowAdapter.
     */
    private void setupSecurity() {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
        }

        final UserDetails userDetails = new User("admin", "FAKE_PASSWORD", true, true, true, true, authorities);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities));
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {

        // get all entitlements to perform updates
        if (EntitlementUtil.getOwnedEntitlementNames().isEmpty()) {
            setupSecurity();
        }

        if (!(task instanceof SyncTask)) {
            throw new JobExecutionException("Task " + taskId + " isn't a SyncTask");
        }

        final SyncTask syncTask = (SyncTask) this.task;

        ConnectorFacadeProxy connector;
        try {
            connector = connInstanceLoader.getConnector(syncTask.getResource());
        } catch (Exception e) {
            final String msg = String.format("Connector instance bean for resource %s and connInstance %s not found",
                    syncTask.getResource(), syncTask.getResource().getConnector());

            throw new JobExecutionException(msg, e);
        }

        final SchemaMapping accountIdMap = SchemaMappingUtil.getAccountIdMapping(syncTask.getResource().getMappings());

        if (accountIdMap == null) {
            throw new JobExecutionException("Invalid account id mapping for resource " + syncTask.getResource());
        }

        LOG.debug("Execute synchronization with token {}", syncTask.getResource().getSyncToken() != null
                ? syncTask.getResource().getSyncToken().getValue()
                : null);

        final List<SyncResult> results = new ArrayList<SyncResult>();

        actions.beforeAll(syncTask);

        try {
            final SyncPolicy syncPolicy = syncTask.getResource().getSyncPolicy();

            final ConflictResolutionAction resAct = syncPolicy == null || syncPolicy.getSpecification() == null
                    ? ConflictResolutionAction.IGNORE
                    : ((SyncPolicySpec) syncPolicy.getSpecification()).getConflictResolutionAction();

            final SyncJobResultsHandler handler = new SyncJobResultsHandler(results, syncTask, resAct, dryRun);
            if (syncTask.isFullReconciliation()) {
                connector.getAllObjects(ObjectClass.ACCOUNT, handler,
                        connector.getOperationOptions(syncTask.getResource()));
            } else {
                connector.sync(ObjectClass.ACCOUNT, syncTask.getResource().getSyncToken(), handler,
                        connector.getOperationOptions(syncTask.getResource()));
            }

            if (!dryRun && !syncTask.isFullReconciliation()) {
                try {
                    ExternalResource resource = resourceDAO.find(syncTask.getResource().getName());
                    resource.setSyncToken(connector.getLatestSyncToken(ObjectClass.ACCOUNT));
                    resourceDAO.save(resource);
                } catch (Exception e) {
                    throw new JobExecutionException("While updating SyncToken", e);
                }
            }
        } catch (Exception e) {
            throw new JobExecutionException("While syncing on connector", e);
        }

        actions.afterAll(syncTask, results);

        final String result = createReport(results, syncTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result.toString();
    }

    /**
     * Handle deltas.
     *
     * @param syncTask sync task.
     * @param delta delta.
     * @param resAct conflict resolution action.
     * @param dryRun dry run.
     * @return list of synchronization results.
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final List<SyncResult> handleDelta(final SyncTask syncTask, final SyncDelta delta,
            final ConflictResolutionAction resAct, final boolean dryRun) throws JobExecutionException {

        final List<SyncResult> results = new ArrayList<SyncResult>();

        LOG.debug("Process '{}' for '{}'", delta.getDeltaType(), delta.getUid().getUidValue());

        final List<Long> users = findExistingUsers(delta);

        switch (delta.getDeltaType()) {
            case CREATE_OR_UPDATE:
                if (users.isEmpty()) {
                    results.addAll(createUser(delta, dryRun));
                } else if (users.size() == 1) {
                    results.addAll(updateUsers(delta, users.subList(0, 1), dryRun));
                } else {
                    switch (resAct) {
                        case IGNORE:
                            LOG.error("More than one match {}", users);
                            break;

                        case FIRSTMATCH:
                            results.addAll(updateUsers(delta, users.subList(0, 1), dryRun));
                            break;

                        case LASTMATCH:
                            results.addAll(updateUsers(delta, users.subList(users.size() - 1, users.size()), dryRun));
                            break;

                        case ALL:
                            results.addAll(updateUsers(delta, users, dryRun));
                            break;

                        default:
                    }
                }
                break;

            case DELETE:
                if (users.isEmpty()) {
                    LOG.debug("No match found for deletion");
                } else if (users.size() == 1) {
                    results.addAll(deleteUsers(delta, users, dryRun));
                } else {
                    switch (resAct) {
                        case IGNORE:
                            LOG.error("More than one match {}", users);
                            break;

                        case FIRSTMATCH:
                            results.addAll(deleteUsers(delta, users.subList(0, 1), dryRun));
                            break;

                        case LASTMATCH:
                            results.addAll(deleteUsers(delta, users.subList(users.size() - 1, users.size()), dryRun));
                            break;

                        case ALL:
                            results.addAll(deleteUsers(delta, users, dryRun));
                            break;

                        default:
                    }
                }

                break;

            default:
        }

        return results;
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        SyncTask syncTask = (SyncTask) task;

        // True if either failed and failures have to be registered, or if ALL has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.FAILURE
                && syncTask.getResource().getSyncTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                || syncTask.getResource().getSyncTraceLevel() == TraceLevel.ALL;
    }

    private class SyncJobResultsHandler implements SyncResultsHandler {

        private final Collection<SyncResult> results;

        private final SyncTask syncTask;

        private final ConflictResolutionAction resAct;

        private final boolean dryRun;

        public SyncJobResultsHandler(final Collection<SyncResult> results, final SyncTask syncTask,
                final ConflictResolutionAction resAct, final boolean dryRun) {

            this.results = results;
            this.syncTask = syncTask;
            this.resAct = resAct;
            this.dryRun = dryRun;
        }

        @Override
        public boolean handle(final SyncDelta delta) {
            try {
                results.addAll(handleDelta(syncTask, delta, resAct, dryRun));
                return true;
            } catch (JobExecutionException e) {
                LOG.error("Synchronization failed", e);
                return false;
            }
        }
    }
}
