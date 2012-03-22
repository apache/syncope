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
package org.syncope.core.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
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
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.SyncopeUserCond;
import org.syncope.client.to.UserTO;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.notification.NotificationManager;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.propagation.PropagationException;
import org.syncope.core.propagation.PropagationManager;
import org.syncope.core.rest.controller.InvalidSearchConditionException;
import org.syncope.core.rest.controller.UnauthorizedRoleException;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.core.scheduling.SyncResult.Operation;
import org.syncope.core.util.ConnObjectUtil;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.util.SchemaMappingUtil;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowResult;
import org.syncope.types.ConflictResolutionAction;
import org.syncope.types.SyncPolicySpec;
import org.syncope.types.TraceLevel;

/**
 * Job for executing synchronization tasks.
 *
 * @see org.syncope.core.scheduling.Job
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
        final SyncPolicy policy = syncTask.getResource().getSyncPolicy();
        final SyncPolicySpec policySpec = policy != null
                ? (SyncPolicySpec) policy.getSpecification()
                : null;
        // ---------------------------------

        final List<Long> result = new ArrayList<Long>();

        if (policySpec != null && !policySpec.getAlternativeSearchAttrs().isEmpty()) {

            // search external attribute name/value
            // about each specified name
            final ConnectorObject object = delta.getObject();

            final Map<String, Attribute> extValues = new HashMap<String, Attribute>();

            for (SchemaMapping mapping : syncTask.getResource().getMappings()) {
                extValues.put(SchemaMappingUtil.getIntAttrName(mapping), object.getAttributeByName(SchemaMappingUtil
                        .getExtAttrName(mapping)));
            }

            // search user by attributes specified into the policy
            NodeCond searchCondition = null;

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

                    final SyncopeUserCond cond = new SyncopeUserCond();
                    cond.setSchema(schema);
                    cond.setType(type);
                    cond.setExpression(expression);

                    nodeCond = NodeCond.getLeafCond(cond);

                } else {
                    final AttributeCond cond = new AttributeCond();
                    cond.setSchema(schema);
                    cond.setType(type);
                    cond.setExpression(expression);

                    nodeCond = NodeCond.getLeafCond(cond);
                }

                searchCondition = searchCondition != null
                        ? NodeCond.getAndCond(searchCondition, nodeCond)
                        : nodeCond;
            }

            List<SyncopeUser> users = userSearchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                    searchCondition);
            for (SyncopeUser user : users) {
                result.add(user.getId());
            }
        } else {
            final SyncopeUser found;
            List<SyncopeUser> users;

            final SchemaMapping accountIdMap = SchemaMappingUtil.getAccountIdMapping(syncTask.getResource()
                    .getMappings());

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
        }

        return result;
    }

    private SyncResult createUser(SyncDelta delta, final boolean dryRun) throws JobExecutionException {

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
                        enabled = status != null && status.getValue() != null && !status.getValue().isEmpty()
                                ? (Boolean) status.getValue().get(0)
                                : null;
                    }
                }
                // --------------------------

                WorkflowResult<Map.Entry<Long, Boolean>> created = wfAdapter.create(userTO, true, enabled);

                List<PropagationTask> tasks = propagationManager.getCreateTaskIds(created, userTO.getPassword(), userTO
                        .getVirtualAttributes(), Collections.singleton(((SyncTask) this.task).getResource().getName()));

                propagationManager.execute(tasks);

                notificationManager.createTasks(new WorkflowResult<Long>(created.getResult().getKey(),
                        created.getPropByRes(), created.getPerformedTasks()));

                userTO = userDataBinder.getUserTO(created.getResult().getKey());

                result.setUserId(created.getResult().getKey());
                result.setUsername(userTO.getUsername());
                result.setStatus(Status.SUCCESS);
            } catch (PropagationException e) {
                LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
            } catch (Throwable t) {
                result.setStatus(Status.FAILURE);
                result.setMessage(t.getMessage());
                LOG.error("Could not create user " + delta.getUid().getUidValue(), t);
            }
        }

        delta = actions.after(delta, userTO, result);

        return result;
    }

    private void updateUsers(SyncDelta delta, final List<Long> users, final boolean dryRun,
            final List<SyncResult> results) throws JobExecutionException {

        if (!((SyncTask) task).isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return;
        }

        LOG.debug("About to update {}", users);

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

                        notificationManager.createTasks(new WorkflowResult<Long>(updated.getResult().getKey(),
                                updated.getPropByRes(), updated.getPerformedTasks()));

                        userTO = userDataBinder.getUserTO(updated.getResult().getKey());
                    }
                } catch (PropagationException e) {
                    LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
                } catch (Throwable t) {
                    result.setStatus(Status.FAILURE);
                    result.setMessage(t.getMessage());
                    LOG.error("Could not update user " + delta.getUid().getUidValue(), t);
                }

                delta = actions.after(delta, userTO, result);
                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find user {}", userId, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read user {}", userId, e);
            }
        }
    }

    private void deleteUsers(SyncDelta delta, final List<Long> users, final boolean dryRun,
            final List<SyncResult> results) throws JobExecutionException {

        if (!((SyncTask) task).isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return;
        }

        LOG.debug("About to delete {}", users);

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

                        notificationManager.createTasks(new WorkflowResult<Long>(userId, null, "delete"));

                    } catch (Exception e) {
                        LOG.error("Could not propagate user " + userId, e);
                    }

                    try {
                        wfAdapter.delete(userId);
                    } catch (Throwable t) {
                        result.setStatus(Status.FAILURE);
                        result.setMessage(t.getMessage());
                        LOG.error("Could not delete user " + userId, t);
                    }
                }

                delta = actions.after(delta, userTO, result);
                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find user {}", userId, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read user {}", userId, e);
            }
        }
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

        // Summary, also to be included for FAILURE and ALL, so create it
        // anyway.
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

            final ConflictResolutionAction conflictResolutionAction = syncPolicy != null
                    && syncPolicy.getSpecification() != null
                    ? ((SyncPolicySpec) syncPolicy.getSpecification()).getConflictResolutionAction()
                    : ConflictResolutionAction.IGNORE;

            if (syncTask.isFullReconciliation()) {
                connector.getAllObjects(ObjectClass.ACCOUNT, new SyncResultsHandler() {

                    @Override
                    public boolean handle(final SyncDelta delta) {
                        try {

                            return results.addAll(handleDelta(syncTask, delta, conflictResolutionAction, dryRun));

                        } catch (JobExecutionException e) {
                            LOG.error("Reconciliation failed", e);
                            return false;
                        }
                    }
                }, connector.getOperationOptions(syncTask.getResource()));
            } else {
                connector.sync(syncTask.getResource().getSyncToken(), new SyncResultsHandler() {

                    @Override
                    public boolean handle(final SyncDelta delta) {
                        try {

                            return results.addAll(handleDelta(syncTask, delta, conflictResolutionAction, dryRun));

                        } catch (JobExecutionException e) {
                            LOG.error("Synchronization failed", e);
                            return false;
                        }
                    }
                }, connector.getOperationOptions(syncTask.getResource()));
            }

            if (!dryRun && !syncTask.isFullReconciliation()) {
                try {
                    ExternalResource resource = resourceDAO.find(syncTask.getResource().getName());
                    resource.setSyncToken(connector.getLatestSyncToken());
                    resourceDAO.save(resource);

                } catch (Throwable t) {
                    throw new JobExecutionException("While updating SyncToken", t);
                }
            }
        } catch (Throwable t) {
            throw new JobExecutionException("While syncing on connector", t);
        }

        actions.afterAll(syncTask, results);

        final String result = createReport(results, syncTask.getResource().getSyncTraceLevel(), dryRun);

        LOG.debug("Sync result: {}", result);

        return result.toString();
    }

    /**
     * Handle delatas.
     *
     * @param syncTask sync task.
     * @param delta delta.
     * @param conflictResolutionAction conflict resolution action.
     * @param dryRun dry run.
     * @return list of synchronization results.
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final List<SyncResult> handleDelta(final SyncTask syncTask, final SyncDelta delta,
            final ConflictResolutionAction conflictResolutionAction, final boolean dryRun) throws JobExecutionException {

        final List<SyncResult> results = new ArrayList<SyncResult>();

        LOG.debug("Process '{}' for '{}'", delta.getDeltaType(), delta.getUid().getUidValue());

        final List<Long> users = findExistingUsers(delta);

        switch (delta.getDeltaType()) {
            case CREATE_OR_UPDATE:
                if (users.isEmpty()) {
                    if (syncTask.isPerformCreate()) {
                        results.add(createUser(delta, dryRun));
                    } else {
                        LOG.debug("SyncTask not configured for create");
                    }
                } else if (users.size() == 1) {
                    updateUsers(delta, users.subList(0, 1), dryRun, results);
                } else {
                    switch (conflictResolutionAction) {
                        case IGNORE:
                            LOG.error("More than one match {}", users);
                            break;

                        case FIRSTMATCH:
                            updateUsers(delta, users.subList(0, 1), dryRun, results);
                            break;

                        case LASTMATCH:
                            updateUsers(delta, users.subList(users.size() - 1, users.size()), dryRun, results);
                            break;

                        case ALL:
                            updateUsers(delta, users, dryRun, results);
                            break;

                        default:
                    }
                }
                break;

            case DELETE:
                if (users.isEmpty()) {
                    LOG.debug("No match found for deletion");
                } else if (users.size() == 1) {
                    deleteUsers(delta, users, dryRun, results);
                } else {
                    switch (conflictResolutionAction) {
                        case IGNORE:
                            LOG.error("More than one match {}", users);
                            break;

                        case FIRSTMATCH:
                            deleteUsers(delta, users.subList(0, 1), dryRun, results);
                            break;

                        case LASTMATCH:
                            deleteUsers(delta, users.subList(users.size() - 1, users.size()), dryRun, results);
                            break;

                        case ALL:
                            deleteUsers(delta, users, dryRun, results);
                            break;

                        default:
                    }
                }

                break;

            default:
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        SyncTask syncTask = (SyncTask) task;

        // True if either failed and failures have to be registered, or if ALL
        // has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.FAILURE && syncTask.getResource().getSyncTraceLevel()
                .ordinal() >= TraceLevel.FAILURES.ordinal())
                || syncTask.getResource().getSyncTraceLevel() == TraceLevel.ALL;
    }
}
