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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.controller.InvalidSearchConditionException;
import org.syncope.core.rest.controller.UserController;

/**
 * Job for executing synchronization tasks.
 * @see Job
 * @see SyncTask
 */
public class SyncJob extends AbstractJob {

    /**
     * ConnInstance loader.
     */
    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    /**
     * Connector instance DAO.
     */
    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * Derived schema DAO.
     */
    @Autowired
    private DerSchemaDAO derSchemaDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * User REST controller.
     */
    @Autowired
    private UserController userController;

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
     * @param mappings schema mappings
     * @param roles default roles to be assigned
     * @param resources default resources to be assigned
     * @return UserTO for the user to be created
     */
    private UserTO getUserTO(final ConnectorObject obj,
            final List<SchemaMapping> mappings,
            final Set<Long> roles, final Set<String> resources) {

        final UserTO userTO = new UserTO();
        userTO.setResources(resources);
        MembershipTO membershipTO;
        for (Long roleId : roles) {
            membershipTO = new MembershipTO();
            membershipTO.setRoleId(roleId);
            userTO.addMembership(membershipTO);
        }

        Attribute attribute;
        List<Object> values;
        AttributeTO attributeTO;
        for (SchemaMapping mapping : mappings) {
            attribute = obj.getAttributeByName(mapping.getDestAttrName());
            values = attribute == null
                    ? Collections.EMPTY_LIST : attribute.getValue();
            switch (mapping.getSourceMappingType()) {
                case SyncopeUserId:
                    break;

                case Password:
                    attribute = obj.getAttributeByName("__PASSWORD__");
                    userTO.setPassword(getPassword(attribute == null
                            ? Collections.EMPTY_LIST : attribute.getValue()));
                    break;

                case UserSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getSourceAttrName());
                    for (Object value : values) {
                        attributeTO.addValue(value.toString());
                    }
                    userTO.addAttribute(attributeTO);
                    break;

                case UserDerivedSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getSourceAttrName());
                    userTO.addDerivedAttribute(attributeTO);
                    break;

                case UserVirtualSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getSourceAttrName());
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
     * @param userId user to be updated
     * @param obj connector object
     * @param mappings schema mappings
     * @param roles default roles to be assigned
     * @param resources default resources to be assigned
     * @return UserMod for the user to be updated
     */
    private UserMod getUserMod(final Long userId, final ConnectorObject obj,
            final List<SchemaMapping> mappings,
            final Set<Long> roles, final Set<String> resources) {

        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.setResourcesToBeAdded(resources);
        MembershipMod membershipMod;
        for (Long roleId : roles) {
            membershipMod = new MembershipMod();
            membershipMod.setRole(roleId);
            userMod.addMembershipToBeAdded(membershipMod);
        }

        Attribute attribute;
        List<Object> values;
        AttributeMod attributeMod;
        for (SchemaMapping mapping : mappings) {
            attribute = obj.getAttributeByName(mapping.getDestAttrName());
            values = attribute == null
                    ? Collections.EMPTY_LIST : attribute.getValue();
            switch (mapping.getSourceMappingType()) {
                case SyncopeUserId:
                    break;

                case Password:
                    attribute = obj.getAttributeByName("__PASSWORD__");
                    userMod.setPassword(getPassword(attribute == null
                            ? Collections.EMPTY_LIST : attribute.getValue()));
                    break;

                case UserSchema:
                    userMod.addAttributeToBeRemoved(
                            mapping.getSourceAttrName());

                    attributeMod = new AttributeMod();
                    attributeMod.setSchema(mapping.getSourceAttrName());
                    for (Object value : values) {
                        attributeMod.addValueToBeAdded(value.toString());
                    }
                    userMod.addAttributeToBeUpdated(attributeMod);
                    break;

                case UserDerivedSchema:
                    userMod.addDerivedAttributeToBeAdded(
                            mapping.getSourceAttrName());
                    break;

                case UserVirtualSchema:
                    userMod.addVirtualAttributeToBeAdded(
                            mapping.getSourceAttrName());
                    break;

                default:
            }
        }

        return userMod;
    }

    /**
     * Find users based on mapped uid value (or previous uid value, if updated).
     *
     * @param schemaName schema name mapped as accountId
     * @param uidValue Uid value
     * @param previousUidValue Uid value before last update (if available)
     * @return list of matching users
     */
    private List<SyncopeUser> findExistingUsers(
            final String schemaName, final String uidValue,
            final String previousUidValue) {

        final List<SyncopeUser> result = new ArrayList<SyncopeUser>();

        USchema schema = schemaDAO.find(schemaName, USchema.class);
        if (schema != null) {
            UAttrValue value = new UAttrValue();
            value.setStringValue(previousUidValue == null
                    ? uidValue : previousUidValue);
            result.addAll(userDAO.findByAttrValue(schemaName, value));
        } else {
            UDerSchema derSchema =
                    derSchemaDAO.find(schemaName, UDerSchema.class);
            if (derSchema != null) {
                try {
                    result.addAll(userDAO.findByDerAttrValue(schemaName,
                            previousUidValue == null
                            ? uidValue : previousUidValue));
                } catch (InvalidSearchConditionException e) {
                    LOG.error("Could not search for matching users", e);
                }
            } else {
                LOG.warn("Invalid account Id source schema name: {}",
                        schemaName);
            }
        }

        return result;
    }

    @Override
    protected String doExecute()
            throws JobExecutionException {

        if (!(task instanceof SyncTask)) {
            throw new JobExecutionException("Task " + taskId
                    + " isn't a SyncTask");
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

        List<SyncDelta> deltas;
        try {
            deltas = connector.sync(
                    syncTask.getResource().getConnector().getSyncToken());
        } catch (Throwable t) {
            throw new JobExecutionException("While syncing on connector", t);
        }

        SchemaMapping accountIdMap =
                syncTask.getResource().getAccountIdMapping();
        if (accountIdMap == null) {
            throw new JobExecutionException(
                    "Invalid account id mapping for resource "
                    + syncTask.getResource());
        }

        Set<String> defaultResources = new HashSet<String>(
                syncTask.getDefaultResources().size());
        for (TargetResource resource : syncTask.getDefaultResources()) {
            defaultResources.add(resource.getName());
        }
        Set<Long> defaultRoles = new HashSet<Long>(
                syncTask.getDefaultRoles().size());
        for (SyncopeRole role : syncTask.getDefaultRoles()) {
            defaultRoles.add(role.getId());
        }

        // counters
        int created = 0;
        int updated = 0;
        int deleted = 0;
        int failCreated = 0;
        int failUpdated = 0;
        int failDeleted = 0;

        List<SyncopeUser> users;
        List<Long> userIds;
        SyncopeUser userToUpdate;
        for (SyncDelta delta : deltas) {
            users = findExistingUsers(accountIdMap.getSourceAttrName(),
                    delta.getUid().getUidValue(),
                    delta.getPreviousUid() == null
                    ? null : delta.getPreviousUid().getUidValue());

            switch (delta.getDeltaType()) {
                case CREATE_OR_UPDATE:
                    if (users.isEmpty()) {
                        try {
                            userController.create(getUserTO(delta.getObject(),
                                    syncTask.getResource().getMappings(),
                                    defaultRoles, defaultResources),
                                    null, null);
                            created++;
                        } catch (Throwable t) {
                            failCreated++;
                            LOG.error("Could not create user "
                                    + delta.getUid().getUidValue(), t);
                        }
                    } else if (users.size() == 1) {
                        if (syncTask.isUpdateIdentities()) {
                            userToUpdate = users.iterator().next();
                            try {
                                userController.update(userToUpdate,
                                        getUserMod(userToUpdate.getId(),
                                        delta.getObject(),
                                        syncTask.getResource().getMappings(),
                                        defaultRoles, defaultResources),
                                        null, null);
                                updated++;
                            } catch (Throwable t) {
                                failUpdated++;
                                LOG.error("Could not update user "
                                        + delta.getUid().getUidValue(), t);
                            }
                        }
                    } else {
                        LOG.error("More than one user matching {}", users);
                    }
                    break;

                case DELETE:
                    LOG.debug("About to delete {}", users);

                    userIds = new ArrayList<Long>(users.size());
                    for (SyncopeUser user : users) {
                        userIds.add(user.getId());
                    }
                    for (Long userId : userIds) {
                        try {
                            userController.delete(userDAO.find(userId),
                                    null, null);
                            deleted++;
                        } catch (Throwable t) {
                            failDeleted++;
                            LOG.error("Could not delete user " + userId, t);
                        }
                    }
                    break;

                default:
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("Users [created/failures]: ").append(created).append('/').
                append(failCreated).append(' ').
                append("[updated/failures]: ").append(updated).append('/').
                append(failUpdated).append(' ').
                append("[deleted/ failures]: ").append(deleted).append('/').
                append(failDeleted);
        LOG.debug("Sync result: {}", result);

        try {
            syncTask.getResource().getConnector().setSyncToken(
                    connector.getLatestSyncToken());
            connInstanceDAO.save(syncTask.getResource().getConnector());
        } catch (Throwable t) {
            throw new JobExecutionException("While updating SyncToken", t);
        }

        return result.toString();
    }
}
