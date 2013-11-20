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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.AttributableCond;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.ConflictResolutionAction;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.sync.SyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.sync.SyncCorrelationRule;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
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
     * Policy DAO.
     */
    @Autowired
    protected PolicyDAO policyDAO;

    /**
     * Entitlement DAO.
     */
    @Autowired
    protected EntitlementDAO entitlementDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    protected SchemaDAO schemaDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Role DAO.
     */
    @Autowired
    protected RoleDAO roleDAO;

    /**
     * Search DAO.
     */
    @Autowired
    protected AttributableSearchDAO searchDAO;

    /**
     * ConnectorObject util.
     */
    @Autowired
    protected ConnObjectUtil connObjectUtil;

    /**
     * User workflow adapter.
     */
    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    /**
     * Role workflow adapter.
     */
    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    /**
     * Propagation Manager.
     */
    @Autowired
    protected PropagationManager propagationManager;

    /**
     * PropagationTask executor.
     */
    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    /**
     * User data binder.
     */
    @Autowired
    protected UserDataBinder userDataBinder;

    /**
     * Role data binder.
     */
    @Autowired
    protected RoleDataBinder roleDataBinder;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected AttributableTransformer attrTransformer;

    /**
     * Syncing connector.
     */
    protected Connector connector;

    /**
     * SyncJob actions.
     */
    protected SyncActions actions;

    protected Collection<SyncResult> results;

    protected SyncTask syncTask;

    protected ConflictResolutionAction resAct;

    protected boolean dryRun;

    protected Map<Long, String> roleOwnerMap = new HashMap<Long, String>();

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(final Connector connector) {
        this.connector = connector;
    }

    public SyncActions getActions() {
        return actions;
    }

    public void setActions(final SyncActions actions) {
        this.actions = actions;
    }

    public Collection<SyncResult> getResults() {
        return results;
    }

    public void setResults(final Collection<SyncResult> results) {
        this.results = results;
    }

    public SyncTask getSyncTask() {
        return syncTask;
    }

    public void setSyncTask(final SyncTask syncTask) {
        this.syncTask = syncTask;
    }

    public ConflictResolutionAction getResAct() {
        return resAct;
    }

    public void setResAct(final ConflictResolutionAction resAct) {
        this.resAct = resAct;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Map<Long, String> getRoleOwnerMap() {
        return roleOwnerMap;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            doHandle(delta);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    protected List<Long> findByAccountIdItem(final String uid, final AttributableUtil attrUtil) {
        final List<Long> result = new ArrayList<Long>();

        final AbstractMappingItem accountIdItem = attrUtil.getAccountIdItem(syncTask.getResource());
        switch (accountIdItem.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
                final AbstractAttrValue value = attrUtil.newAttrValue();

                AbstractNormalSchema schema = schemaDAO.find(accountIdItem.getIntAttrName(), attrUtil.schemaClass());
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

    protected List<Long> search(final NodeCond searchCond, final AttributableUtil attrUtil) {
        final List<Long> result = new ArrayList<Long>();

        final List<AbstractAttributable> subjects = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCond, attrUtil);
        for (AbstractAttributable subject : subjects) {
            result.add(subject.getId());
        }

        return result;
    }

    protected List<Long> findByCorrelationRule(
            final ConnectorObject connObj, final SyncCorrelationRule rule, final AttributableUtil attrUtil) {

        return search(rule.getSearchCond(connObj), attrUtil);
    }

    protected List<Long> findByAttributableSearch(
            final ConnectorObject connObj, final List<String> altSearchSchemas, final AttributableUtil attrUtil) {

        // search for external attribute's name/value of each specified name
        final Map<String, Attribute> extValues = new HashMap<String, Attribute>();

        for (AbstractMappingItem item
                : attrUtil.getMappingItems(syncTask.getResource(), MappingPurpose.SYNCHRONIZATION)) {

            extValues.put(item.getIntAttrName(), connObj.getAttributeByName(item.getExtAttrName()));
        }

        // search for user/role by attribute(s) specified in the policy
        NodeCond searchCond = null;

        for (String schema : altSearchSchemas) {
            Attribute value = extValues.get(schema);

            AttributeCond.Type type;
            String expression = null;

            if (value == null || value.getValue() == null || value.getValue().isEmpty()
                    || (value.getValue().size() == 1 && value.getValue().get(0) == null)) {
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

        return search(searchCond, attrUtil);
    }

    /**
     * Find users / roles based on mapped uid value (or previous uid value, if updated).
     *
     * @param uid for finding by account id
     * @param connObj for finding by attribute value
     * @param attrUtil attributable util
     * @return list of matching users / roles
     */
    protected List<Long> findExisting(final String uid, final ConnectorObject connObj,
            final AttributableUtil attrUtil) {

        SyncPolicySpec syncPolicySpec = null;
        if (syncTask.getResource().getSyncPolicy() == null) {
            SyncPolicy globalSP = policyDAO.getGlobalSyncPolicy();
            if (globalSP != null) {
                syncPolicySpec = globalSP.<SyncPolicySpec>getSpecification();
            }
        } else {
            syncPolicySpec = syncTask.getResource().getSyncPolicy().<SyncPolicySpec>getSpecification();
        }

        SyncCorrelationRule syncRule = null;
        List<String> altSearchSchemas = null;

        if (syncPolicySpec != null) {
            syncRule = attrUtil.getCorrelationRule(syncPolicySpec);
            altSearchSchemas = attrUtil.getAltSearchSchemas(syncPolicySpec);
        }

        return syncRule == null ? altSearchSchemas == null || altSearchSchemas.isEmpty()
                ? findByAccountIdItem(uid, attrUtil)
                : findByAttributableSearch(connObj, altSearchSchemas, attrUtil)
                : findByCorrelationRule(connObj, syncRule, attrUtil);
    }

    public Long findMatchingAttributableId(final ObjectClass objectClass, final String name) {
        Long result = null;

        final AttributableUtil attrUtil = AttributableUtil.getInstance(objectClass);

        final List<ConnectorObject> found = connector.search(objectClass,
                new EqualsFilter(new Name(name)), connector.getOperationOptions(
                attrUtil.getMappingItems(syncTask.getResource(), MappingPurpose.SYNCHRONIZATION)));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", objectClass, syncTask.getResource(), name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only",
                        objectClass, syncTask.getResource(), name);
            }

            ConnectorObject connObj = found.iterator().next();
            final List<Long> subjectIds = findExisting(connObj.getUid().getUidValue(), connObj, attrUtil);
            if (subjectIds.isEmpty()) {
                LOG.debug("No matching {} found for {}, aborting", attrUtil.getType(), connObj);
            } else {
                if (subjectIds.size() > 1) {
                    LOG.warn("More than one {} found {} - taking first only", attrUtil.getType(), subjectIds);
                }

                result = subjectIds.iterator().next();
            }
        }

        return result;
    }

    protected Boolean readEnabled(final ConnectorObject connectorObject) {
        Boolean enabled = null;
        if (syncTask.isSyncStatus()) {
            Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, connectorObject.getAttributes());
            if (status != null && status.getValue() != null && !status.getValue().isEmpty()) {
                enabled = (Boolean) status.getValue().get(0);
            }
        }

        return enabled;
    }

    protected List<SyncResult> create(SyncDelta delta, final AttributableUtil attrUtil, final boolean dryRun)
            throws JobExecutionException {

        if (!syncTask.isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.<SyncResult>emptyList();
        }

        final SyncResult result = new SyncResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(SyncResult.Status.SUCCESS);

        AbstractAttributableTO subjectTO = connObjectUtil.getAttributableTO(delta.getObject(), syncTask, attrUtil);

        delta = actions.beforeCreate(this, delta, subjectTO);

        // Attributable transformation (if configured)
        AbstractAttributableTO actual = attrTransformer.transform(subjectTO);
        LOG.debug("Transformed: {}", actual);

        if (dryRun) {
            result.setId(0L);
            if (actual instanceof UserTO) {
                result.setName(((UserTO) actual).getUsername());
            }
            if (actual instanceof RoleTO) {
                result.setName(((RoleTO) actual).getName());
            }
        } else {
            Object output = null;
            Result resultStatus;

            try {
                if (AttributableType.USER == attrUtil.getType()) {
                    Boolean enabled = readEnabled(delta.getObject());
                    WorkflowResult<Map.Entry<Long, Boolean>> created =
                            uwfAdapter.create((UserTO) actual, true, enabled);

                    List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(created,
                            ((UserTO) actual).getPassword(), actual.getVirAttrs(),
                            Collections.singleton(syncTask.getResource().getName()));

                    taskExecutor.execute(tasks);

                    actual = userDataBinder.getUserTO(created.getResult().getKey());

                    result.setId(created.getResult().getKey());
                    result.setName(((UserTO) actual).getUsername());
                } else if (AttributableType.ROLE == attrUtil.getType()) {
                    WorkflowResult<Long> created = rwfAdapter.create((RoleTO) actual);
                    AttributeTO roleOwner = actual.getAttrMap().get(StringUtils.EMPTY);
                    if (roleOwner != null) {
                        roleOwnerMap.put(created.getResult(), roleOwner.getValues().iterator().next());
                    }

                    EntitlementUtil.extendAuthContext(created.getResult());

                    List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created,
                            actual.getVirAttrs(), Collections.singleton(syncTask.getResource().getName()));

                    taskExecutor.execute(tasks);

                    actual = roleDataBinder.getRoleTO(created.getResult());

                    result.setId(created.getResult());
                    result.setName(((RoleTO) actual).getName());
                }
                output = actual;
                resultStatus = Result.SUCCESS;

            } catch (PropagationException e) {
                // A propagation failure doesn't imply a synchronization failure.
                // The propagation exception status will be reported into the propagation task execution.
                LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                output = e;
                resultStatus = Result.FAILURE;
            } catch (Exception e) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(e.getMessage());
                LOG.error("Could not create {} {} ", attrUtil.getType(), delta.getUid().getUidValue(), e);
                output = e;
                resultStatus = Result.FAILURE;
            }

            notificationManager.createTasks(
                    AuditElements.EventCategoryType.SYNCHRONIZATION,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    "create",
                    resultStatus,
                    null, // searching for before object is too much expensive ... 
                    output,
                    delta);

            auditManager.audit(
                    AuditElements.EventCategoryType.SYNCHRONIZATION,
                    AttributableType.USER.name().toLowerCase(),
                    syncTask.getResource().getName(),
                    "create",
                    resultStatus,
                    null, // searching for before object is too much expensive ... 
                    output,
                    delta);
        }

        actions.after(this, delta, actual, result);
        return Collections.singletonList(result);
    }

    protected Map.Entry<UserTO, UserTO> updateUser(final Long id, SyncDelta delta, final boolean dryRun,
            final SyncResult result)
            throws Exception {

        final UserTO before = userDataBinder.getUserTO(id);
        UserMod userMod = connObjectUtil.getAttributableMod(
                id, delta.getObject(), before, syncTask, AttributableUtil.getInstance(AttributableType.USER));

        delta = actions.beforeUpdate(this, delta, before, userMod);

        if (dryRun) {
            return new AbstractMap.SimpleEntry<UserTO, UserTO>(before, before);
        }

        // Attribute value transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(actual);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", id, e);

            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
        }

        Boolean enabled = readEnabled(delta.getObject());
        if (enabled != null) {
            SyncopeUser user = userDAO.find(id);

            WorkflowResult<Long> enableUpdate = null;
            if (user.isSuspended() == null) {
                enableUpdate = uwfAdapter.activate(id, null);
            } else if (enabled && user.isSuspended()) {
                enableUpdate = uwfAdapter.reactivate(id);
            } else if (!enabled && !user.isSuspended()) {
                enableUpdate = uwfAdapter.suspend(id);
            }

            if (enableUpdate != null) {
                if (enableUpdate.getPropByRes() != null) {
                    updated.getPropByRes().merge(enableUpdate.getPropByRes());
                    updated.getPropByRes().purge();
                }
                updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
            }
        }

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                updated, Collections.singleton(syncTask.getResource().getName()));

        taskExecutor.execute(tasks);

        final UserTO after = userDataBinder.getUserTO(updated.getResult().getKey().getId());
        actions.after(this, delta, after, result);

        return new AbstractMap.SimpleEntry<UserTO, UserTO>(before, after);
    }

    protected Map.Entry<RoleTO, RoleTO> updateRole(
            final Long id, SyncDelta delta, final boolean dryRun, final SyncResult result)
            throws Exception {

        final RoleTO before = roleDataBinder.getRoleTO(id);
        RoleMod roleMod = connObjectUtil.getAttributableMod(
                id, delta.getObject(), before, syncTask, AttributableUtil.getInstance(AttributableType.ROLE));

        delta = actions.beforeUpdate(this, delta, before, roleMod);

        if (dryRun) {
            return new AbstractMap.SimpleEntry<RoleTO, RoleTO>(before, before);
        }

        // Attribute value transformation (if configured)
        RoleMod actual = attrTransformer.transform(roleMod);
        LOG.debug("Transformed: {}", actual);

        WorkflowResult<Long> updated = rwfAdapter.update(actual);
        String roleOwner = null;
        for (AttributeMod attrMod : actual.getAttrsToUpdate()) {
            if (attrMod.getSchema().isEmpty()) {
                roleOwner = attrMod.getValuesToBeAdded().iterator().next();
            }
        }
        if (roleOwner != null) {
            roleOwnerMap.put(updated.getResult(), roleOwner);
        }

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                actual.getVirAttrsToRemove(),
                actual.getVirAttrsToUpdate(),
                Collections.singleton(syncTask.getResource().getName()));

        taskExecutor.execute(tasks);

        final RoleTO after = roleDataBinder.getRoleTO(updated.getResult());

        actions.after(this, delta, after, result);

        return new AbstractMap.SimpleEntry<RoleTO, RoleTO>(before, after);
    }

    protected List<SyncResult> update(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil,
            final boolean dryRun)
            throws JobExecutionException {

        if (!syncTask.isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        List<SyncResult> updResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            LOG.debug("About to update {}", id);

            Object output = null;
            AbstractAttributableTO before = null;
            Result resultStatus;

            final SyncResult result = new SyncResult();
            result.setOperation(ResourceOperation.UPDATE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(SyncResult.Status.SUCCESS);
            result.setId(id);

            try {
                final AbstractAttributableTO updated;
                if (AttributableType.USER == attrUtil.getType()) {
                    final Map.Entry<UserTO, UserTO> res = updateUser(id, delta, dryRun, result);
                    before = res.getKey();
                    updated = res.getValue();
                    result.setName(((UserTO) updated).getUsername());
                } else if (AttributableType.ROLE == attrUtil.getType()) {
                    final Map.Entry<RoleTO, RoleTO> res = updateRole(id, delta, dryRun, result);
                    before = res.getKey();
                    updated = res.getValue();
                    result.setName(((RoleTO) updated).getName());
                } else {
                    updated = null;
                }
                output = updated;
                resultStatus = Result.SUCCESS;
            } catch (PropagationException e) {
                // A propagation failure doesn't imply a synchronization failure.
                // The propagation exception status will be reported into the propagation task execution.
                LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                output = e;
                resultStatus = Result.FAILURE;
            } catch (Exception e) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(e.getMessage());
                LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                output = e;
                resultStatus = Result.FAILURE;
            }
            updResults.add(result);

            if (!dryRun) {
                notificationManager.createTasks(
                        AuditElements.EventCategoryType.SYNCHRONIZATION,
                        attrUtil.getType().name().toLowerCase(),
                        syncTask.getResource().getName(),
                        "update",
                        resultStatus,
                        before,
                        output,
                        delta);

                auditManager.audit(
                        AuditElements.EventCategoryType.SYNCHRONIZATION,
                        attrUtil.getType().name().toLowerCase(),
                        syncTask.getResource().getName(),
                        "update",
                        resultStatus,
                        before,
                        output,
                        delta);
            }

            LOG.debug("{} {} successfully updated", attrUtil.getType(), id);
        }

        return updResults;
    }

    protected List<SyncResult> delete(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil,
            final boolean dryRun)
            throws JobExecutionException {

        if (!syncTask.isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to delete {}", subjects);

        List<SyncResult> delResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            Object output = null;
            Result resultStatus = Result.FAILURE;

            try {
                AbstractAttributableTO subjectTO = AttributableType.USER == attrUtil.getType()
                        ? userDataBinder.getUserTO(id)
                        : roleDataBinder.getRoleTO(id);
                delta = actions.beforeDelete(this, delta, subjectTO);

                final SyncResult result = new SyncResult();
                result.setId(id);
                if (subjectTO instanceof UserTO) {
                    result.setName(((UserTO) subjectTO).getUsername());
                }
                if (subjectTO instanceof RoleTO) {
                    result.setName(((RoleTO) subjectTO).getName());
                }
                result.setOperation(ResourceOperation.DELETE);
                result.setSubjectType(attrUtil.getType());
                result.setStatus(SyncResult.Status.SUCCESS);

                if (!dryRun) {
                    try {
                        List<PropagationTask> tasks = Collections.<PropagationTask>emptyList();
                        if (AttributableType.USER == attrUtil.getType()) {
                            tasks = propagationManager.getUserDeleteTaskIds(id, syncTask.getResource().getName());
                        } else if (AttributableType.ROLE == attrUtil.getType()) {
                            tasks = propagationManager.getRoleDeleteTaskIds(id, syncTask.getResource().getName());
                        }
                        taskExecutor.execute(tasks);
                    } catch (Exception e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate user " + id, e);
                    }

                    try {
                        if (AttributableType.USER == attrUtil.getType()) {
                            uwfAdapter.delete(id);
                        } else if (AttributableType.ROLE == attrUtil.getType()) {
                            rwfAdapter.delete(id);
                        }
                        output = null;
                        resultStatus = Result.SUCCESS;
                    } catch (Exception e) {
                        result.setStatus(SyncResult.Status.FAILURE);
                        result.setMessage(e.getMessage());
                        LOG.error("Could not delete {} {}", attrUtil.getType(), id, e);
                        output = e;
                    }
                }

                actions.after(this, delta, subjectTO, result);
                delResults.add(result);

            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", attrUtil.getType(), id, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read {} {}", attrUtil.getType(), id, e);
            }

            if (!dryRun) {
                notificationManager.createTasks(
                        AuditElements.EventCategoryType.SYNCHRONIZATION,
                        attrUtil.getType().name().toLowerCase(),
                        syncTask.getResource().getName(),
                        "delete",
                        resultStatus,
                        null, // searching for before object is too much expensive ... 
                        output,
                        delta);

                auditManager.audit(
                        AuditElements.EventCategoryType.SYNCHRONIZATION,
                        attrUtil.getType().name().toLowerCase(),
                        syncTask.getResource().getName(),
                        "delete",
                        resultStatus,
                        null, // searching for before object is too much expensive ... 
                        output,
                        delta);
            }
        }

        return delResults;
    }

    /**
     * Look into SyncDelta and take necessary actions (create / update / delete) on user(s).
     *
     * @param delta returned by the underlying connector
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final void doHandle(final SyncDelta delta)
            throws JobExecutionException {

        if (results == null) {
            results = new ArrayList<SyncResult>();
        }

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        AttributableUtil attrUtil = AttributableUtil.getInstance(delta.getObject().getObjectClass());

        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();
        final List<Long> subjectIds = findExisting(uid, delta.getObject(), attrUtil);

        if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
            if (subjectIds.isEmpty()) {
                results.addAll(create(delta, attrUtil, dryRun));
            } else if (subjectIds.size() == 1) {
                results.addAll(update(delta, subjectIds.subList(0, 1), attrUtil, dryRun));
            } else {
                switch (resAct) {
                    case IGNORE:
                        LOG.error("More than one match {}", subjectIds);
                        break;

                    case FIRSTMATCH:
                        results.addAll(update(delta, subjectIds.subList(0, 1), attrUtil, dryRun));
                        break;

                    case LASTMATCH:
                        results.addAll(update(delta, subjectIds.subList(subjectIds.size() - 1, subjectIds.size()),
                                attrUtil, dryRun));
                        break;

                    case ALL:
                        results.addAll(update(delta, subjectIds, attrUtil, dryRun));
                        break;

                    default:
                }
            }
        } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
            if (subjectIds.isEmpty()) {
                LOG.debug("No match found for deletion");
            } else if (subjectIds.size() == 1) {
                results.addAll(delete(delta, subjectIds, attrUtil, dryRun));
            } else {
                switch (resAct) {
                    case IGNORE:
                        LOG.error("More than one match {}", subjectIds);
                        break;

                    case FIRSTMATCH:
                        results.addAll(delete(delta, subjectIds.subList(0, 1), attrUtil, dryRun));
                        break;

                    case LASTMATCH:
                        results.addAll(delete(delta, subjectIds.subList(subjectIds.size() - 1, subjectIds.size()),
                                attrUtil,
                                dryRun));
                        break;

                    case ALL:
                        results.addAll(delete(delta, subjectIds, attrUtil, dryRun));
                        break;

                    default:
                }
            }
        }
    }
}
