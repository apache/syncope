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
import org.apache.syncope.client.mod.AbstractAttributableMod;
import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.AttributableCond;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.quartz.AbstractTaskJob;
import org.apache.syncope.core.rest.controller.InvalidSearchConditionException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.apache.syncope.types.AttributableType;
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

public class SyncopeSyncResultHandler implements SyncResultsHandler {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeSyncResultHandler.class);

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
     * Role DAO.
     */
    @Autowired
    private RoleDAO roleDAO;

    /**
     * User search DAO.
     */
    @Autowired
    private AttributableSearchDAO searchDAO;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    /**
     * User workflow adapter.
     */
    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    /**
     * Role workflow adapter.
     */
    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

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
     * Role data binder.
     */
    @Autowired
    private RoleDataBinder roleDataBinder;

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

    private List<Long> findByAccountIdItem(final String uid, final AttributableUtil attrUtil) {
        final List<Long> result = new ArrayList<Long>();

        final AbstractMappingItem accountIdItem = attrUtil.getAccountIdItem(syncTask.getResource());
        switch (accountIdItem.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
                final AbstractAttrValue value = attrUtil.newAttrValue();

                AbstractSchema schema = schemaDAO.find(accountIdItem.getIntAttrName(), attrUtil.schemaClass());
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

                List<AbstractAttributable> subjects =
                        userDAO.findByAttrValue(accountIdItem.getIntAttrName(), value, attrUtil);
                for (AbstractAttributable subject : subjects) {
                    result.add(subject.getId());
                }
                break;

            case UserDerivedSchema:
            case RoleDerivedSchema:
                try {
                    subjects = userDAO.findByDerAttrValue(accountIdItem.getIntAttrName(), uid, attrUtil);
                    for (AbstractAttributable subject : subjects) {
                        result.add(subject.getId());
                    }
                } catch (InvalidSearchConditionException e) {
                    LOG.error("Could not search for matching subjects", e);
                }
                break;

            case Username:
                SyncopeUser user = userDAO.find(uid);
                if (user != null) {
                    result.add(user.getId());
                }
                break;

            case UserId:
                user = userDAO.find(Long.parseLong(uid));
                if (user != null) {
                    result.add(user.getId());
                }
                break;

            case RoleName:
                List<SyncopeRole> roles = roleDAO.find(uid);
                for (SyncopeRole role : roles) {
                    result.add(role.getId());
                }
                break;

            case RoleId:
                SyncopeRole role = roleDAO.find(Long.parseLong(uid));
                if (role != null) {
                    result.add(role.getId());
                }
                break;

            default:
                LOG.error("Invalid accountId type '{}'", accountIdItem.getIntMappingType());
        }

        return result;
    }

    private List<Long> findByAttributableSearch(final SyncDelta delta, final SyncPolicySpec policySpec,
            final AttributableUtil attrUtil) {

        final List<Long> result = new ArrayList<Long>();

        // search for external attribute's name/value of each specified name

        final Map<String, Attribute> extValues = new HashMap<String, Attribute>();

        for (AbstractMappingItem item : attrUtil.getMappingItems(syncTask.getResource())) {
            extValues.put(item.getIntAttrName(), delta.getObject().getAttributeByName(item.getExtAttrName()));
        }

        // search for user/role by attribute(s) specified in the policy
        NodeCond searchCond = null;

        for (String schema : attrUtil.getAltSearchSchemas(policySpec)) {
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
            // users: just id or username can be selected to be used
            // roles: just id or name can be selected to be used
            if ("id".equalsIgnoreCase(schema) || "username".equalsIgnoreCase(schema)
                    || "name".equalsIgnoreCase(schema)) {

                AttributableCond cond = new AttributableCond();
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

        final List<AbstractAttributable> subjects = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCond, attrUtil);
        for (AbstractAttributable subject : subjects) {
            result.add(subject.getId());
        }

        return result;
    }

    /**
     * Find users / roles based on mapped uid value (or previous uid value, if updated).
     *
     * @param delta sync delta
     * @param attrUtil attributable util
     * @return list of matching users / roles
     */
    protected List<Long> findExisting(final SyncDelta delta, final AttributableUtil attrUtil) {
        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        SyncPolicySpec policySpec = null;
        if (syncTask.getResource().getSyncPolicy() != null) {
            policySpec = (SyncPolicySpec) syncTask.getResource().getSyncPolicy().getSpecification();
        }

        return policySpec == null || attrUtil.getAltSearchSchemas(policySpec).isEmpty()
                ? findByAccountIdItem(uid, attrUtil)
                : findByAttributableSearch(delta, policySpec, attrUtil);
    }

    protected List<SyncResult> create(SyncDelta delta, final AttributableUtil attrUtil,
            final boolean dryRun) throws JobExecutionException {

        if (!syncTask.isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.EMPTY_LIST;
        }

        final SyncResult result = new SyncResult();
        result.setOperation(SyncResult.Operation.CREATE);

        AbstractAttributableTO subjectTO = connObjectUtil.getAttributableTO(delta.getObject(), syncTask, attrUtil);

        delta = actions.beforeCreate(delta, subjectTO);

        if (dryRun) {
            result.setId(0L);
            if (subjectTO instanceof UserTO) {
                result.setName(((UserTO) subjectTO).getUsername());
            }
            if (subjectTO instanceof RoleTO) {
                result.setName(((RoleTO) subjectTO).getName());
            }
            result.setStatus(AbstractTaskJob.Status.SUCCESS);
        } else {
            try {
                if (AttributableType.USER == attrUtil.getType()) {
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

                    WorkflowResult<Map.Entry<Long, Boolean>> created =
                            uwfAdapter.create((UserTO) subjectTO, true, enabled);

                    List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(created,
                            ((UserTO) subjectTO).getPassword(), subjectTO.getVirtualAttributes(),
                            Collections.singleton(syncTask.getResource().getName()));

                    taskExecutor.execute(tasks);

                    notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

                    subjectTO = userDataBinder.getUserTO(created.getResult().getKey());

                    result.setId(created.getResult().getKey());
                }
                if (AttributableType.ROLE == attrUtil.getType()) {
                    WorkflowResult<Long> created = rwfAdapter.create((RoleTO) subjectTO);

                    EntitlementUtil.extendAuthContext(created.getResult());

                    List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created,
                            subjectTO.getVirtualAttributes(), Collections.singleton(syncTask.getResource().getName()));

                    taskExecutor.execute(tasks);

                    subjectTO = roleDataBinder.getRoleTO(created.getResult());

                    result.setId(created.getResult());
                }

                if (subjectTO instanceof UserTO) {
                    result.setName(((UserTO) subjectTO).getUsername());
                }
                if (subjectTO instanceof RoleTO) {
                    result.setName(((RoleTO) subjectTO).getName());
                }
                result.setStatus(AbstractTaskJob.Status.SUCCESS);
            } catch (PropagationException e) {
                LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
            } catch (Exception e) {
                result.setStatus(AbstractTaskJob.Status.FAILURE);
                result.setMessage(e.getMessage());
                LOG.error("Could not create {} {} ", attrUtil.getType(), delta.getUid().getUidValue(), e);
            }
        }

        actions.after(delta, subjectTO, result);
        return Collections.singletonList(result);
    }

    protected List<SyncResult> update(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil,
            final boolean dryRun) throws JobExecutionException {

        if (!syncTask.isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.EMPTY_LIST;
        }

        LOG.debug("About to update {}", subjects);

        List<SyncResult> updResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            final SyncResult result = new SyncResult();
            result.setOperation(SyncResult.Operation.UPDATE);

            try {
                AbstractAttributableTO subjectTO = AttributableType.USER == attrUtil.getType()
                        ? userDataBinder.getUserTO(id)
                        : roleDataBinder.getRoleTO(id);
                try {
                    final AbstractAttributableMod mod = connObjectUtil.getAttributableMod(
                            id, delta.getObject(), subjectTO, syncTask, attrUtil);
                    delta = actions.beforeUpdate(delta, subjectTO, mod);

                    result.setStatus(AbstractTaskJob.Status.SUCCESS);
                    result.setId(mod.getId());
                    if (mod instanceof UserMod) {
                        result.setName(((UserMod) mod).getUsername());
                    }
                    if (mod instanceof RoleMod) {
                        result.setName(((RoleMod) mod).getName());
                    }

                    if (!dryRun) {
                        if (AttributableType.USER == attrUtil.getType()) {
                            WorkflowResult<Map.Entry<Long, Boolean>> updated = uwfAdapter.update((UserMod) mod);

                            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated,
                                    ((UserMod) mod).getPassword(), mod.getVirtualAttributesToBeRemoved(),
                                    mod.getVirtualAttributesToBeUpdated(),
                                    Collections.singleton(syncTask.getResource().getName()));

                            taskExecutor.execute(tasks);

                            notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

                            subjectTO = userDataBinder.getUserTO(updated.getResult().getKey());
                        }
                        if (AttributableType.ROLE == attrUtil.getType()) {
                            WorkflowResult<Long> updated = rwfAdapter.update((RoleMod) mod);

                            List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                                    mod.getVirtualAttributesToBeRemoved(), mod.getVirtualAttributesToBeUpdated(),
                                    Collections.singleton(syncTask.getResource().getName()));

                            taskExecutor.execute(tasks);

                            subjectTO = roleDataBinder.getRoleTO(updated.getResult());
                        }
                    }
                } catch (PropagationException e) {
                    LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                } catch (Exception e) {
                    result.setStatus(AbstractTaskJob.Status.FAILURE);
                    result.setMessage(e.getMessage());
                    LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                }

                actions.after(delta, subjectTO, result);
                updResults.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", attrUtil.getType(), id, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read {} {}", attrUtil.getType(), id, e);
            }
        }

        return updResults;
    }

    protected List<SyncResult> delete(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil,
            final boolean dryRun) throws JobExecutionException {

        if (!syncTask.isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return Collections.EMPTY_LIST;
        }

        LOG.debug("About to delete {}", subjects);

        List<SyncResult> delResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            try {
                AbstractAttributableTO subjectTO = AttributableType.USER == attrUtil.getType()
                        ? userDataBinder.getUserTO(id)
                        : roleDataBinder.getRoleTO(id);
                delta = actions.beforeDelete(delta, subjectTO);

                final SyncResult result = new SyncResult();
                result.setId(id);
                if (subjectTO instanceof UserTO) {
                    result.setName(((UserTO) subjectTO).getUsername());
                }
                if (subjectTO instanceof RoleTO) {
                    result.setName(((RoleTO) subjectTO).getName());
                }
                result.setOperation(SyncResult.Operation.DELETE);
                result.setStatus(AbstractTaskJob.Status.SUCCESS);

                if (!dryRun) {
                    try {
                        List<PropagationTask> tasks = Collections.EMPTY_LIST;
                        if (AttributableType.USER == attrUtil.getType()) {
                            tasks = propagationManager.getUserDeleteTaskIds(id, syncTask.getResource().getName());
                            notificationManager.createTasks(id, Collections.singleton("delete"));
                        }
                        if (AttributableType.ROLE == attrUtil.getType()) {
                            tasks = propagationManager.getRoleDeleteTaskIds(id, syncTask.getResource().getName());
                        }
                        taskExecutor.execute(tasks);
                    } catch (Exception e) {
                        LOG.error("Could not propagate user " + id, e);
                    }

                    try {
                        if (AttributableType.USER == attrUtil.getType()) {
                            uwfAdapter.delete(id);
                        }
                        if (AttributableType.ROLE == attrUtil.getType()) {
                            rwfAdapter.delete(id);
                        }
                    } catch (Exception e) {
                        result.setStatus(AbstractTaskJob.Status.FAILURE);
                        result.setMessage(e.getMessage());
                        LOG.error("Could not delete {} {}", attrUtil.getType(), id, e);
                    }
                }

                actions.after(delta, subjectTO, result);
                delResults.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", attrUtil.getType(), id, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read {} {}", attrUtil.getType(), id, e);
            }
        }

        return delResults;
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

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        AttributableUtil attrUtil = AttributableUtil.getInstance(delta.getObject().getObjectClass());

        final List<Long> subjects = findExisting(delta, attrUtil);

        if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
            if (subjects.isEmpty()) {
                results.addAll(create(delta, attrUtil, dryRun));
            } else if (subjects.size() == 1) {
                results.addAll(update(delta, subjects.subList(0, 1), attrUtil, dryRun));
            } else {
                switch (resAct) {
                    case IGNORE:
                        LOG.error("More than one match {}", subjects);
                        break;

                    case FIRSTMATCH:
                        results.addAll(update(delta, subjects.subList(0, 1), attrUtil, dryRun));
                        break;

                    case LASTMATCH:
                        results.addAll(update(delta, subjects.subList(subjects.size() - 1, subjects.size()), attrUtil,
                                dryRun));
                        break;

                    case ALL:
                        results.addAll(update(delta, subjects, attrUtil, dryRun));
                        break;

                    default:
                }
            }
        }

        if (SyncDeltaType.DELETE == delta.getDeltaType()) {
            if (subjects.isEmpty()) {
                LOG.debug("No match found for deletion");
            } else if (subjects.size() == 1) {
                results.addAll(delete(delta, subjects, attrUtil, dryRun));
            } else {
                switch (resAct) {
                    case IGNORE:
                        LOG.error("More than one match {}", subjects);
                        break;

                    case FIRSTMATCH:
                        results.addAll(delete(delta, subjects.subList(0, 1), attrUtil, dryRun));
                        break;

                    case LASTMATCH:
                        results.addAll(delete(delta, subjects.subList(subjects.size() - 1, subjects.size()), attrUtil,
                                dryRun));
                        break;

                    case ALL:
                        results.addAll(delete(delta, subjects, attrUtil, dryRun));
                        break;

                    default:
                }
            }
        }

        return results;
    }
}
