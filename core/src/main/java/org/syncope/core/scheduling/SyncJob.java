/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.SyncopeUserCond;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.Entitlement;
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
import org.syncope.core.propagation.PropagationByResource;
import org.syncope.core.propagation.PropagationException;
import org.syncope.core.propagation.PropagationManager;
import org.syncope.core.rest.controller.InvalidSearchConditionException;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.core.scheduling.SyncResult.Operation;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowResult;
import org.syncope.types.ConflictResolutionAction;
import org.syncope.types.SyncPolicySpec;
import org.syncope.types.TraceLevel;

/**
 * Job for executing synchronization tasks.
 * @see org.syncope.core.scheduling.Job
 * @see SyncTask
 */
public class SyncJob extends AbstractJob {

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

    public void setActions(final SyncJobActions actions) {
        this.actions = actions;
    }

    /**
     * Extract password value from passed values (if instance of GuardedString
     * or GuardedByteArray).
     *
     * @param values list of values received from the underlying connector.
     * @return password value
     */
    private String getPassword(final List<Object> values) {
        final StringBuilder result = new StringBuilder();

        Object pwd;
        if (values == null || values.isEmpty()) {
            pwd = "password";
        } else {
            pwd = values.iterator().next();
        }

        if (pwd instanceof GuardedString) {
            ((GuardedString) pwd).access(new GuardedString.Accessor() {

                @Override
                public void access(final char[] clearChars) {
                    result.append(clearChars);
                }
            });
        } else if (pwd instanceof GuardedByteArray) {
            ((GuardedByteArray) pwd).access(new GuardedByteArray.Accessor() {

                @Override
                public void access(final byte[] clearBytes) {
                    result.append(new String(clearBytes));
                }
            });
        } else if (pwd instanceof String) {
            result.append((String) pwd);
        } else {
            result.append(pwd.toString());
        }

        return result.toString();
    }

    /**
     * Build an UserTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @return UserTO for the user to be created
     */
    private UserTO getUserTO(final ConnectorObject obj) {
        final SyncTask syncTask = (SyncTask) this.task;

        final UserTO userTO = new UserTO();
        userTO.setResources(syncTask.getDefaultResourceNames());
        MembershipTO membershipTO;
        for (Long roleId : syncTask.getDefaultRoleIds()) {
            membershipTO = new MembershipTO();
            membershipTO.setRoleId(roleId);
            userTO.addMembership(membershipTO);
        }

        for (SchemaMapping mapping : syncTask.getResource().getMappings()) {
            Attribute attribute;
            if (mapping.isAccountid()) {
                attribute = obj.getAttributeByName(Name.NAME);
            } else if (mapping.isPassword()) {
                attribute = obj.getAttributeByName(
                        OperationalAttributes.PASSWORD_NAME);
            } else {
                attribute = obj.getAttributeByName(mapping.getExtAttrName());
            }

            AttributeTO attributeTO;
            switch (mapping.getIntMappingType()) {
                case SyncopeUserId:
                    break;

                case Password:
                    userTO.setPassword(getPassword(attribute == null
                            ? Collections.EMPTY_LIST : attribute.getValue()));
                    break;

                case Username:
                    userTO.setUsername(
                            attribute == null || attribute.getValue().isEmpty()
                            ? null : attribute.getValue().get(0).toString());
                    break;

                case UserSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getIntAttrName());
                    for (Object value : attribute == null
                            ? Collections.EMPTY_LIST : attribute.getValue()) {

                        attributeTO.addValue(value.toString());
                    }
                    userTO.addAttribute(attributeTO);
                    break;

                case UserDerivedSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getIntAttrName());
                    userTO.addDerivedAttribute(attributeTO);
                    break;

                case UserVirtualSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getIntAttrName());
                    userTO.addVirtualAttribute(attributeTO);
                    break;

                default:
            }
        }

        return userTO;
    }

    /**
     * Build an UserMod out of connector object attributes and schema mapping.
     *
     * @param user user to be updated
     * @param obj connector object
     * @return UserMod for the user to be updated
     */
    private UserMod getUserMod(final SyncopeUser user,
            final ConnectorObject obj) {

        final SyncTask syncTask = (SyncTask) this.task;

        final UserMod userMod = new UserMod();
        userMod.setId(user.getId());
        userMod.setResourcesToBeAdded(syncTask.getDefaultResourceNames());
        MembershipMod membershipMod;
        for (Long roleId : syncTask.getDefaultRoleIds()) {
            membershipMod = new MembershipMod();
            membershipMod.setRole(roleId);
            userMod.addMembershipToBeAdded(membershipMod);
        }

        for (SchemaMapping mapping : syncTask.getResource().getMappings()) {
            Attribute attribute = obj.getAttributeByName(
                    mapping.getExtAttrName());
            List<Object> values = attribute == null
                    ? Collections.EMPTY_LIST : attribute.getValue();

            AttributeMod attributeMod;
            switch (mapping.getIntMappingType()) {
                case SyncopeUserId:
                    break;

                case Password:
                    attribute = obj.getAttributeByName(
                            OperationalAttributes.PASSWORD_NAME);

                    final String password = getPassword(attribute == null
                            ? Collections.EMPTY_LIST : attribute.getValue());

                    final SyncopeUser passwordUser = new SyncopeUser();
                    passwordUser.setPassword(
                            password, user.getCipherAlgoritm(), 0);

                    // update password if and only if password is really changed
                    if (!user.getPassword().equals(
                            passwordUser.getPassword())) {

                        userMod.setPassword(password);
                    }
                    break;

                case Username:
                    if (values != null && !values.isEmpty()) {
                        userMod.setUsername(values.get(0).toString());
                    }
                    break;

                case UserSchema:
                    userMod.addAttributeToBeRemoved(
                            mapping.getIntAttrName());

                    attributeMod = new AttributeMod();
                    attributeMod.setSchema(mapping.getIntAttrName());
                    for (Object value : values) {
                        attributeMod.addValueToBeAdded(value.toString());
                    }
                    userMod.addAttributeToBeUpdated(attributeMod);
                    break;

                case UserDerivedSchema:
                    userMod.addDerivedAttributeToBeAdded(
                            mapping.getIntAttrName());
                    break;

                case UserVirtualSchema:
                    userMod.addVirtualAttributeToBeRemoved(
                            mapping.getIntAttrName());

                    attributeMod = new AttributeMod();
                    attributeMod.setSchema(mapping.getIntAttrName());
                    for (Object value : values) {
                        attributeMod.addValueToBeAdded(value.toString());
                    }
                    userMod.addVirtualAttributeToBeUpdated(attributeMod);
                    break;

                default:
            }
        }

        return userMod;
    }

    /**
     * Find users based on mapped uid value (or previous uid value, if updated).
     *
     * @param delta sync delta
     * @return list of matching users
     */
    private List<SyncopeUser> findExistingUsers(final SyncDelta delta) {
        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        // ---------------------------------
        // Get sync policy specification
        // ---------------------------------
        final SyncPolicy policy =
                ((SyncTask) this.task).getResource().getSyncPolicy();

        final SyncPolicySpec policySpec = policy != null
                ? (SyncPolicySpec) policy.getSpecification() : null;
        // ---------------------------------

        final List<SyncopeUser> result = new ArrayList<SyncopeUser>();

        if (policySpec != null
                && !policySpec.getAlternativeSearchAttrs().isEmpty()) {

            // search external attribute name/value 
            // about each specified name
            final ConnectorObject object = delta.getObject();

            final Map<String, Attribute> extValues =
                    new HashMap<String, Attribute>();

            for (SchemaMapping mapping :
                    ((SyncTask) this.task).getResource().getMappings()) {

                String key;
                switch (mapping.getIntMappingType()) {
                    case SyncopeUserId:
                        key = "id";
                        break;

                    case Username:
                        key = "username";
                        break;

                    case Password:
                        key = "password";
                        break;

                    default:
                        key = mapping.getIntAttrName();
                }

                extValues.put(key, object.getAttributeByName(
                        mapping.getExtAttrName()));
            }

            // search user by attributes specified into the policy
            NodeCond searchCondition = null;

            for (String schema : policySpec.getAlternativeSearchAttrs()) {
                Attribute value = extValues.get(schema);

                AttributeCond.Type type;
                String expression = null;

                if (value == null
                        || value.getValue() == null
                        || value.getValue().isEmpty()) {

                    type = AttributeCond.Type.ISNULL;
                } else {
                    type = AttributeCond.Type.EQ;
                    expression = value.getValue().size() > 1
                            ? value.getValue().toString()
                            : value.getValue().get(0).toString();
                }

                NodeCond nodeCond;

                // just Username or SyncopeUserId can be selected to be used
                if ("id".equalsIgnoreCase(schema)
                        || "username".equalsIgnoreCase(schema)) {

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

            result.addAll(userSearchDAO.search(
                    EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                    searchCondition));
        } else {
            final SyncopeUser user;

            final SchemaMapping accountIdMap = ((SyncTask) this.task).
                    getResource().getAccountIdMapping();
            switch (accountIdMap.getIntMappingType()) {
                case Username:
                    user = userDAO.find(uid);
                    if (user != null) {
                        result.add(user);
                    }
                    break;

                case SyncopeUserId:
                    user = userDAO.find(Long.parseLong(uid));
                    if (user != null) {
                        result.add(user);
                    }
                    break;

                case UserSchema:
                    final UAttrValue value = new UAttrValue();
                    value.setStringValue(uid);
                    result.addAll(userDAO.findByAttrValue(
                            accountIdMap.getIntAttrName(), value));
                    break;

                case UserDerivedSchema:
                    try {
                        result.addAll(userDAO.findByDerAttrValue(
                                accountIdMap.getIntAttrName(), uid));
                    } catch (InvalidSearchConditionException e) {
                        LOG.error("Could not search for matching users", e);
                    }
                    break;

                default:
                    LOG.error("Invalid accountId type '{}'",
                            accountIdMap.getIntMappingType());
            }
        }

        return result;
    }

    private SyncResult createUser(final SyncDelta delta, final boolean dryRun)
            throws JobExecutionException {

        final SyncResult result = new SyncResult();
        result.setOperation(Operation.CREATE);

        UserTO userTO = getUserTO(delta.getObject());
        actions.beforeCreate(delta, userTO);

        if (dryRun) {
            result.setUserId(0L);
            result.setUsername(userTO.getUsername());
            result.setStatus(SyncResult.Status.SUCCESS);
        } else {
            try {
                WorkflowResult<Map.Entry<Long, Boolean>> created =
                        wfAdapter.create(userTO);
                List<PropagationTask> tasks =
                        propagationManager.getCreateTaskIds(
                        created.getResult().getKey(), userTO.getPassword(),
                        null, created.getResult().getValue(),
                        ((SyncTask) this.task).getResource().getName());
                propagationManager.execute(tasks);

                userTO = userDataBinder.getUserTO(
                        created.getResult().getKey());

                result.setUserId(created.getResult().getKey());
                result.setUsername(userTO.getUsername());
                result.setStatus(SyncResult.Status.SUCCESS);
            } catch (PropagationException e) {
                LOG.error("Could not propagate user "
                        + delta.getUid().getUidValue(), e);
            } catch (Throwable t) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(t.getMessage());
                LOG.error("Could not create user "
                        + delta.getUid().getUidValue(), t);
            }
        }

        actions.after(delta, userTO, result);

        return result;
    }

    private void updateUsers(final SyncDelta delta,
            final List<SyncopeUser> users, final boolean dryRun,
            final List<SyncResult> results)
            throws JobExecutionException {

        if (!((SyncTask) task).isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return;
        }

        LOG.debug("About to update {}", users);

        for (SyncopeUser user : users) {
            final SyncResult result = new SyncResult();
            result.setOperation(Operation.UPDATE);

            UserTO userTO = userDataBinder.getUserTO(user.getId());


            try {
                final UserMod userMod = getUserMod(user, delta.getObject());
                actions.beforeUpdate(delta, userTO, userMod);

                result.setStatus(SyncResult.Status.SUCCESS);
                result.setUserId(userMod.getId());
                result.setUsername(userMod.getUsername());

                if (!dryRun) {
                    WorkflowResult<Map.Entry<Long, PropagationByResource>> upd =
                            wfAdapter.update(userMod);
                    List<PropagationTask> tasks =
                            propagationManager.getUpdateTaskIds(
                            upd.getResult().getKey(), userMod.getPassword(),
                            null, null, null, upd.getResult().getValue(),
                            ((SyncTask) this.task).getResource().getName());
                    propagationManager.execute(tasks);

                    userTO = userDataBinder.getUserTO(
                            upd.getResult().getKey());
                }
            } catch (PropagationException e) {
                LOG.error("Could not propagate user "
                        + delta.getUid().getUidValue(), e);
            } catch (Throwable t) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(t.getMessage());
                LOG.error("Could not update user "
                        + delta.getUid().getUidValue(), t);
            }

            actions.after(delta, userTO, result);
            results.add(result);
        }
    }

    private void deleteUsers(final SyncDelta delta,
            final List<SyncopeUser> users, final boolean dryRun,
            final List<SyncResult> results)
            throws JobExecutionException {

        if (!((SyncTask) task).isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return;
        }

        LOG.debug("About to delete {}", users);

        for (SyncopeUser user : users) {
            Long userId = user.getId();

            UserTO userTO = userDataBinder.getUserTO(userId);
            actions.beforeDelete(delta, userTO);

            final SyncResult result = new SyncResult();
            result.setUserId(userId);
            result.setUsername(user.getUsername());
            result.setOperation(Operation.DELETE);
            result.setStatus(SyncResult.Status.SUCCESS);

            if (!dryRun) {
                try {
                    List<PropagationTask> tasks =
                            propagationManager.getDeleteTaskIds(userId,
                            ((SyncTask) this.task).getResource().getName());
                    propagationManager.execute(tasks);
                } catch (Exception e) {
                    LOG.error("Could not propagate user " + userId, e);
                }

                try {
                    wfAdapter.delete(userId);
                } catch (Throwable t) {
                    result.setStatus(SyncResult.Status.FAILURE);
                    result.setMessage(t.getMessage());
                    LOG.error("Could not delete user " + userId, t);
                }
            }

            actions.after(delta, userTO, result);
            results.add(result);

        }
    }

    /**
     * Create a textual report of the synchronization, based on the trace level.
     * @param syncResults Sync results
     * @param syncTraceLevel Sync trace level
     * @param dryRun dry run?
     * @return report as string
     */
    private String createReport(final List<SyncResult> syncResults,
            final TraceLevel syncTraceLevel, final boolean dryRun) {

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
        report.append("Users [created/failures]: ").
                append(created.size()).append('/').append(createdFailed.size()).
                append(' ').
                append("[updated/failures]: ").
                append(updated.size()).append('/').append(updatedFailed.size()).
                append(' ').
                append("[deleted/ failures]: ").
                append(deleted.size()).append('/').append(deletedFailed.size());

        // Failures
        if (syncTraceLevel == TraceLevel.FAILURES
                || syncTraceLevel == TraceLevel.ALL) {

            if (!createdFailed.isEmpty()) {
                report.append("\n\nFailed to create: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(
                        createdFailed,
                        syncTraceLevel));
            }
            if (!updatedFailed.isEmpty()) {
                report.append("\nFailed to update: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(
                        updatedFailed,
                        syncTraceLevel));
            }
            if (!deletedFailed.isEmpty()) {
                report.append("\nFailed to delete: ");
                report.append(SyncResult.reportSetOfSynchronizationResult(
                        deletedFailed,
                        syncTraceLevel));
            }
        }

        // Succeeded, only if on 'ALL' level
        if (syncTraceLevel == TraceLevel.ALL) {
            report.append("\n\nCreated:\n").
                    append(SyncResult.reportSetOfSynchronizationResult(created,
                    syncTraceLevel)).
                    append("\nUpdated:\n").append(SyncResult.
                    reportSetOfSynchronizationResult(updated, syncTraceLevel)).
                    append("\nDeleted:\n").append(SyncResult.
                    reportSetOfSynchronizationResult(deleted, syncTraceLevel));
        }

        return report.toString();
    }

    /**
     * Used to simulate authentication in order to perform updates through
     * AbstractUserWorkflowAdapter.
     */
    private void setupSecurity() {
        final List<GrantedAuthority> authorities =
                new ArrayList<GrantedAuthority>();

        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new GrantedAuthorityImpl(entitlement.getName()));
        }

        final UserDetails userDetails = new User(
                "admin", "FAKE_PASSWORD", true, true, true, true, authorities);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                userDetails, "FAKE_PASSWORD", authorities));
    }

    @Override
    protected String doExecute(final boolean dryRun)
            throws JobExecutionException {

        // get all entitlements to perform updates
        if (EntitlementUtil.getOwnedEntitlementNames().isEmpty()) {
            setupSecurity();
        }

        if (!(task instanceof SyncTask)) {
            throw new JobExecutionException(
                    "Task " + taskId + " isn't a SyncTask");
        }

        final SyncTask syncTask = (SyncTask) this.task;

        ConnectorFacadeProxy connector;
        try {
            connector = connInstanceLoader.getConnector(syncTask.getResource());
        } catch (BeansException e) {
            final String msg = String.format(
                    "Connector instance bean for resource %s "
                    + "and connInstance %s not found",
                    syncTask.getResource(),
                    syncTask.getResource().getConnector());

            throw new JobExecutionException(msg, e);
        }

        final List<SyncDelta> deltas;
        try {
            deltas = connector.sync(
                    syncTask.getResource().getSyncToken());
        } catch (Throwable t) {
            throw new JobExecutionException("While syncing on connector", t);
        }

        final SchemaMapping accountIdMap =
                syncTask.getResource().getAccountIdMapping();
        if (accountIdMap == null) {
            throw new JobExecutionException(
                    "Invalid account id mapping for resource "
                    + syncTask.getResource());
        }

        final SyncPolicy syncPolicy = syncTask.getResource().getSyncPolicy();
        final ConflictResolutionAction conflictResolutionAction =
                syncPolicy != null && syncPolicy.getSpecification() != null
                ? ((SyncPolicySpec) syncPolicy.getSpecification()).
                getConflictResolutionAction()
                : ConflictResolutionAction.IGNORE;

        final List<SyncResult> results = new ArrayList<SyncResult>();

        actions.beforeAll(deltas);

        for (SyncDelta delta : deltas) {
            List<SyncopeUser> users = findExistingUsers(delta);

            switch (delta.getDeltaType()) {
                case CREATE_OR_UPDATE:
                    if (users.isEmpty()) {
                        if (syncTask.isPerformCreate()) {
                            results.add(createUser(delta, dryRun));
                        } else {
                            LOG.debug("SyncTask not configured for create");
                        }
                    } else if (users.size() == 1) {
                        updateUsers(delta, users.subList(0, 1),
                                dryRun, results);
                    } else {
                        switch (conflictResolutionAction) {
                            case IGNORE:
                                LOG.error("More than one match {}", users);
                                break;

                            case FIRSTMATCH:
                                updateUsers(delta, users.subList(0, 1),
                                        dryRun, results);
                                break;

                            case LASTMATCH:
                                updateUsers(delta, users.subList(users.size()
                                        - 1, users.size()), dryRun, results);
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
                                deleteUsers(delta, users.subList(0, 1),
                                        dryRun, results);
                                break;

                            case LASTMATCH:
                                deleteUsers(delta, users.subList(
                                        users.size() - 1, users.size()),
                                        dryRun, results);
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
        }

        actions.afterAll(deltas, results);

        final String result = createReport(results, syncTask.getResource().
                getSyncTraceLevel(), dryRun);
        LOG.debug("Sync result: {}", result);

        if (!dryRun) {
            try {
                syncTask.getResource().setSyncToken(
                        connector.getLatestSyncToken());
                resourceDAO.save(syncTask.getResource());
            } catch (Throwable t) {
                throw new JobExecutionException("While updating SyncToken", t);
            }
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        SyncTask syncTask = (SyncTask) task;

        // True if either failed and failures have to be registered, or if ALL
        // has to be registered.
        return (Status.valueOf(execution.getStatus()) == Status.FAILURE
                && syncTask.getResource().getSyncTraceLevel().ordinal()
                >= TraceLevel.FAILURES.ordinal())
                || syncTask.getResource().getSyncTraceLevel() == TraceLevel.ALL;
    }
}
