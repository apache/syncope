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
package org.apache.syncope.core.sync;

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
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.UserSearchDAO;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.quartz.AbstractTaskJob;
import org.apache.syncope.core.rest.controller.InvalidSearchConditionException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.util.SchemaMappingUtil;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.types.ConflictResolutionAction;
import org.apache.syncope.types.SyncPolicySpec;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeSyncResultHanlder implements SyncResultsHandler {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeSyncResultHanlder.class);

    /**
     * Entitlement DAO.
     */
    @Autowired
    private EntitlementDAO entitlementDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * User search DAO.
     */
    @Autowired
    private UserSearchDAO userSearchDAO;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

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
     * PropagationTask executor.
     */
    @Autowired
    private PropagationTaskExecutor taskExecutor;

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * Notification Manager.
     */
    @Autowired
    private NotificationManager notificationManager;

    /**
     * SyncJob actions.
     */
    private SyncActions actions;

    private Collection<SyncResult> results;

    private SyncTask syncTask;

    private ConflictResolutionAction resAct;

    private boolean dryRun;

    public void setActions(final SyncActions actions) {
        this.actions = actions;
    }

    public void setResults(final Collection<SyncResult> results) {
        this.results = results;
    }

    public void setSyncTask(final SyncTask syncTask) {
        this.syncTask = syncTask;
    }

    public void setResAct(final ConflictResolutionAction resAct) {
        this.resAct = resAct;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            results.addAll(doHandle(delta));
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    /**
     * Find users based on mapped uid value (or previous uid value, if updated).
     *
     * @param delta sync delta
     * @return list of matching users
     */
    private List<Long> findExistingUsers(final SyncDelta delta) {
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

                    USchema schema = schemaDAO.find(accountIdMap.getIntAttrName(), USchema.class);
                    if (schema == null) {
                        value.setStringValue(uid);
                    } else {
                        try {
                            value.parseValue(schema, uid);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided __UID__ {}", uid, e);
                            value.setStringValue(uid);
                        }
                    }

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
        if (!syncTask.isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.EMPTY_LIST;
        }

        final SyncResult result = new SyncResult();
        result.setOperation(SyncResult.Operation.CREATE);

        UserTO userTO = connObjectUtil.getUserTO(delta.getObject(), syncTask);

        delta = actions.beforeCreate(delta, userTO);

        if (dryRun) {
            result.setUserId(0L);
            result.setUsername(userTO.getUsername());
            result.setStatus(AbstractTaskJob.Status.SUCCESS);
        } else {
            try {
                // --------------------------
                // Check for status synchronization ...
                // --------------------------
                Boolean enabled = null;
                if (syncTask.isSyncStatus()) {
                    Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME,
                            delta.getObject().getAttributes());
                    if (status != null && status.getValue() != null && !status.getValue().isEmpty()) {
                        enabled = (Boolean) status.getValue().get(0);
                    }
                }
                // --------------------------

                WorkflowResult<Map.Entry<Long, Boolean>> created = wfAdapter.create(userTO, true, enabled);

                List<PropagationTask> tasks = propagationManager.getCreateTaskIds(created, userTO.getPassword(),
                        userTO.getVirtualAttributes(), Collections.singleton(syncTask.getResource().getName()));

                taskExecutor.execute(tasks);

                notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

                userTO = userDataBinder.getUserTO(created.getResult().getKey());

                result.setUserId(created.getResult().getKey());
                result.setUsername(userTO.getUsername());
                result.setStatus(AbstractTaskJob.Status.SUCCESS);
            } catch (PropagationException e) {
                LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
            } catch (Exception e) {
                result.setStatus(AbstractTaskJob.Status.FAILURE);
                result.setMessage(e.getMessage());
                LOG.error("Could not create user " + delta.getUid().getUidValue(), e);
            }
        }

        actions.after(delta, userTO, result);
        return Collections.singletonList(result);
    }

    private List<SyncResult> updateUsers(SyncDelta delta, final List<Long> users, final boolean dryRun)
            throws JobExecutionException {

        if (!syncTask.isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.EMPTY_LIST;
        }

        LOG.debug("About to update {}", users);

        List<SyncResult> results = new ArrayList<SyncResult>();

        for (Long userId : users) {
            final SyncResult result = new SyncResult();
            result.setOperation(SyncResult.Operation.UPDATE);

            try {
                UserTO userTO = userDataBinder.getUserTO(userId);
                try {

                    final UserMod userMod = connObjectUtil.getUserMod(userId, delta.getObject(), syncTask);
                    delta = actions.beforeUpdate(delta, userTO, userMod);

                    result.setStatus(AbstractTaskJob.Status.SUCCESS);
                    result.setUserId(userMod.getId());
                    result.setUsername(userMod.getUsername());

                    if (!dryRun) {
                        WorkflowResult<Map.Entry<Long, Boolean>> updated = wfAdapter.update(userMod);

                        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(updated,
                                userMod.getPassword(), userMod.getVirtualAttributesToBeRemoved(),
                                userMod.getVirtualAttributesToBeUpdated(),
                                Collections.singleton(syncTask.getResource().getName()));

                        taskExecutor.execute(tasks);

                        notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

                        userTO = userDataBinder.getUserTO(updated.getResult().getKey());
                    }
                } catch (PropagationException e) {
                    LOG.error("Could not propagate user " + delta.getUid().getUidValue(), e);
                } catch (Exception e) {
                    result.setStatus(AbstractTaskJob.Status.FAILURE);
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

        if (!syncTask.isPerformDelete()) {
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
                result.setOperation(SyncResult.Operation.DELETE);
                result.setStatus(AbstractTaskJob.Status.SUCCESS);

                if (!dryRun) {
                    try {
                        List<PropagationTask> tasks = propagationManager.getDeleteTaskIds(userId,
                                syncTask.getResource().getName());
                        taskExecutor.execute(tasks);

                        notificationManager.createTasks(userId, Collections.singleton("delete"));
                    } catch (Exception e) {
                        LOG.error("Could not propagate user " + userId, e);
                    }

                    try {
                        wfAdapter.delete(userId);
                    } catch (Exception e) {
                        result.setStatus(AbstractTaskJob.Status.FAILURE);
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
     * Look into SyncDelta and take necessary actions (create / update / delete) on user(s).
     *
     * @param delta returned by the underlying connector
     * @return list of synchronization results
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final List<SyncResult> doHandle(final SyncDelta delta) throws JobExecutionException {
        final List<SyncResult> results = new ArrayList<SyncResult>();

        LOG.debug("Process '{}' for '{}'", delta.getDeltaType(), delta.getUid().getUidValue());

        final List<Long> users = findExistingUsers(delta);

        if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
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
        }

        if (SyncDeltaType.DELETE == delta.getDeltaType()) {
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
        }

        return results;
    }
}
