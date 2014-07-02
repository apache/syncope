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
package org.apache.syncope.core.connid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.InvalidPasswordPolicySpecException;
import org.apache.syncope.core.util.MappingUtil;
import org.apache.syncope.core.util.SecureRandomUtil;
import org.apache.syncope.core.util.VirAttrCache;
import org.apache.syncope.core.util.VirAttrCacheValue;
import org.apache.syncope.core.util.jexl.JexlUtil;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnObjectUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtil.class);

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private PasswordGenerator pwdGen;

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

    public ObjectClass fromAttributable(final AbstractAttributable attributable) {
        if (attributable == null
                || (!(attributable instanceof SyncopeUser) && !(attributable instanceof SyncopeRole))) {

            throw new IllegalArgumentException("No ObjectClass could be provided for " + attributable);
        }

        ObjectClass result = null;
        if (attributable instanceof SyncopeUser) {
            result = ObjectClass.ACCOUNT;
        }
        if (attributable instanceof SyncopeRole) {
            result = ObjectClass.GROUP;
        }

        return result;
    }

    /**
     * Build a UserTO / RoleTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @param attrUtil AttributableUtil
     * @param <T> user/role
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AbstractSubjectTO> T getSubjectTO(final ConnectorObject obj, final SyncTask syncTask,
            final AttributableUtil attrUtil) {

        T subjectTO = getSubjectTOFromConnObject(obj, syncTask, attrUtil);

        // (for users) if password was not set above, generate
        if (subjectTO instanceof UserTO && StringUtils.isBlank(((UserTO) subjectTO).getPassword())) {
            final UserTO userTO = (UserTO) subjectTO;

            List<PasswordPolicySpec> ppSpecs = new ArrayList<PasswordPolicySpec>();

            PasswordPolicy globalPP = policyDAO.getGlobalPasswordPolicy();
            if (globalPP != null && globalPP.getSpecification(PasswordPolicySpec.class) != null) {
                ppSpecs.add(globalPP.getSpecification(PasswordPolicySpec.class));
            }

            for (MembershipTO memb : userTO.getMemberships()) {
                SyncopeRole role = roleDAO.find(memb.getRoleId());
                if (role != null && role.getPasswordPolicy() != null
                        && role.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                    ppSpecs.add(role.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
                }
            }

            for (String resName : userTO.getResources()) {
                ExternalResource resource = resourceDAO.find(resName);
                if (resource != null && resource.getPasswordPolicy() != null
                        && resource.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                    ppSpecs.add(resource.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
                }
            }

            String password;
            try {
                password = pwdGen.generate(ppSpecs);
            } catch (InvalidPasswordPolicySpecException e) {
                LOG.error("Could not generate policy-compliant random password for {}", userTO, e);

                password = SecureRandomUtil.generateRandomPassword(16);
            }
            userTO.setPassword(password);
        }

        return subjectTO;
    }

    /**
     * Build an UserMod out of connector object attributes and schema mapping.
     *
     * @param id user to be updated
     * @param obj connector object
     * @param original subject to get diff from
     * @param syncTask synchronization task
     * @param attrUtil AttributableUtil
     * @param <T> user/role
     * @return modifications for the user/role to be updated
     * @throws NotFoundException if given id does not correspond to a T instance
     * @throws UnauthorizedRoleException if there are no enough entitlements to access the T instance
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T extends AbstractAttributableMod> T getAttributableMod(final Long id, final ConnectorObject obj,
            final AbstractAttributableTO original, final SyncTask syncTask, final AttributableUtil attrUtil)
            throws NotFoundException, UnauthorizedRoleException {

        final AbstractAttributableTO updated = getSubjectTOFromConnObject(obj, syncTask, attrUtil);
        updated.setId(id);

        if (AttributableType.USER == attrUtil.getType()) {
            // update password if and only if password is really changed
            final SyncopeUser user = userDataBinder.getUserFromId(id);
            if (StringUtils.isBlank(((UserTO) updated).getPassword())
                    || userDataBinder.verifyPassword(user, ((UserTO) updated).getPassword())) {

                ((UserTO) updated).setPassword(null);
            }

            for (MembershipTO membTO : ((UserTO) updated).getMemberships()) {
                Membership memb = user.getMembership(membTO.getRoleId());
                if (memb != null) {
                    membTO.setId(memb.getId());
                }
            }

            return (T) AttributableOperations.diff(((UserTO) updated), ((UserTO) original), true);
        }
        if (AttributableType.ROLE == attrUtil.getType()) {
            // reading from connector object cannot change entitlements
            ((RoleTO) updated).getEntitlements().addAll(((RoleTO) original).getEntitlements());
            return (T) AttributableOperations.diff(((RoleTO) updated), ((RoleTO) original), true);
        }

        return null;
    }

    private <T extends AbstractSubjectTO> T getSubjectTOFromConnObject(final ConnectorObject obj,
            final SyncTask syncTask, final AttributableUtil attrUtil) {

        final T subjectTO = attrUtil.newSubjectTO();

        // 1. fill with data from connector object
        for (AbstractMappingItem item : attrUtil.getMappingItems(
                syncTask.getResource(), MappingPurpose.SYNCHRONIZATION)) {

            Attribute attribute = obj.getAttributeByName(item.getExtAttrName());

            AttributeTO attributeTO;
            switch (item.getIntMappingType()) {
                case UserId:
                case RoleId:
                    break;

                case Password:
                    if (subjectTO instanceof UserTO && attribute != null && attribute.getValue() != null
                            && !attribute.getValue().isEmpty()) {

                        ((UserTO) subjectTO).setPassword(getPassword(attribute.getValue().get(0)));
                    }
                    break;

                case Username:
                    if (subjectTO instanceof UserTO) {
                        ((UserTO) subjectTO).setUsername(attribute == null || attribute.getValue().isEmpty()
                                || attribute.getValue().get(0) == null
                                ? null
                                : attribute.getValue().get(0).toString());
                    }
                    break;

                case RoleName:
                    if (subjectTO instanceof RoleTO) {
                        ((RoleTO) subjectTO).setName(attribute == null || attribute.getValue().isEmpty()
                                || attribute.getValue().get(0) == null
                                ? null
                                : attribute.getValue().get(0).toString());
                    }
                    break;

                case RoleOwnerSchema:
                    if (subjectTO instanceof RoleTO && attribute != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // RoleOwnerSchema value
                        attributeTO = new AttributeTO();
                        attributeTO.setSchema(StringUtils.EMPTY);
                        if (attribute.getValue().isEmpty() || attribute.getValue().get(0) == null) {
                            attributeTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attributeTO.getValues().add(attribute.getValue().get(0).toString());
                        }

                        ((RoleTO) subjectTO).getAttrs().add(attributeTO);
                    }
                    break;

                case UserSchema:
                case RoleSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());

                    AbstractNormalSchema schema = schemaDAO.find(item.getIntAttrName(), attrUtil.schemaClass());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {

                        if (value != null) {
                            final AbstractAttrValue attrValue = attrUtil.newAttrValue();
                            if (schema == null) {
                                attrValue.setStringValue(value.toString());
                            } else if (schema.getType() == AttributeSchemaType.Binary) {
                                attrValue.setBinaryValue((byte[]) value);
                            } else {
                                try {
                                    attrValue.parseValue(schema, value.toString());
                                } catch (ParsingValidationException e) {
                                    LOG.error("While parsing provided value {}", value, e);
                                    attrValue.setStringValue(value.toString());
                                }
                            }
                            attributeTO.getValues().add(attrValue.getValueAsString(
                                    schema == null ? AttributeSchemaType.String : schema.getType()));
                        }
                    }

                    subjectTO.getAttrs().add(attributeTO);
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());
                    subjectTO.getDerAttrs().add(attributeTO);
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {

                        if (value != null) {
                            attributeTO.getValues().add(value.toString());
                        }
                    }

                    subjectTO.getVirAttrs().add(attributeTO);
                    break;

                default:
            }
        }

        // 2. add data from defined template (if any)
        AbstractSubjectTO template = AttributableType.USER == attrUtil.getType()
                ? syncTask.getUserTemplate() : syncTask.getRoleTemplate();

        if (template != null) {
            if (template instanceof UserTO) {
                if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                    String evaluated = JexlUtil.evaluate(((UserTO) template).getUsername(), subjectTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) subjectTO).setUsername(evaluated);
                    }
                }

                if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                    String evaluated = JexlUtil.evaluate(((UserTO) template).getPassword(), subjectTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) subjectTO).setPassword(evaluated);
                    }
                }

                Map<Long, MembershipTO> currentMembs = ((UserTO) subjectTO).getMembershipMap();
                for (MembershipTO membTO : ((UserTO) template).getMemberships()) {
                    MembershipTO membTBU;
                    if (currentMembs.containsKey(membTO.getRoleId())) {
                        membTBU = currentMembs.get(membTO.getRoleId());
                    } else {
                        membTBU = new MembershipTO();
                        membTBU.setRoleId(membTO.getRoleId());
                        ((UserTO) subjectTO).getMemberships().add(membTBU);
                    }
                    fillFromTemplate(membTBU, membTO);
                }
            }
            if (template instanceof RoleTO) {
                if (StringUtils.isNotBlank(((RoleTO) template).getName())) {
                    String evaluated = JexlUtil.evaluate(((RoleTO) template).getName(), subjectTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((RoleTO) subjectTO).setName(evaluated);
                    }
                }

                if (((RoleTO) template).getParent() != 0) {
                    final SyncopeRole parentRole = roleDAO.find(((RoleTO) template).getParent());
                    if (parentRole != null) {
                        ((RoleTO) subjectTO).setParent(parentRole.getId());
                    }
                }

                if (((RoleTO) template).getUserOwner() != null) {
                    final SyncopeUser userOwner = userDAO.find(((RoleTO) template).getUserOwner());
                    if (userOwner != null) {
                        ((RoleTO) subjectTO).setUserOwner(userOwner.getId());
                    }
                }
                if (((RoleTO) template).getRoleOwner() != null) {
                    final SyncopeRole roleOwner = roleDAO.find(((RoleTO) template).getRoleOwner());
                    if (roleOwner != null) {
                        ((RoleTO) subjectTO).setRoleOwner(roleOwner.getId());
                    }
                }

                ((RoleTO) subjectTO).getEntitlements().addAll(((RoleTO) template).getEntitlements());

                ((RoleTO) subjectTO).getRAttrTemplates().addAll(((RoleTO) template).getRAttrTemplates());
                ((RoleTO) subjectTO).getRDerAttrTemplates().addAll(((RoleTO) template).getRDerAttrTemplates());
                ((RoleTO) subjectTO).getRVirAttrTemplates().addAll(((RoleTO) template).getRVirAttrTemplates());
                ((RoleTO) subjectTO).getMAttrTemplates().addAll(((RoleTO) template).getMAttrTemplates());
                ((RoleTO) subjectTO).getMDerAttrTemplates().addAll(((RoleTO) template).getMDerAttrTemplates());
                ((RoleTO) subjectTO).getMVirAttrTemplates().addAll(((RoleTO) template).getMVirAttrTemplates());

                ((RoleTO) subjectTO).setAccountPolicy(((RoleTO) template).getAccountPolicy());
                ((RoleTO) subjectTO).setPasswordPolicy(((RoleTO) template).getPasswordPolicy());

                ((RoleTO) subjectTO).setInheritOwner(((RoleTO) template).isInheritOwner());
                ((RoleTO) subjectTO).setInheritTemplates(((RoleTO) template).isInheritTemplates());
                ((RoleTO) subjectTO).setInheritAttrs(((RoleTO) template).isInheritAttrs());
                ((RoleTO) subjectTO).setInheritDerAttrs(((RoleTO) template).isInheritDerAttrs());
                ((RoleTO) subjectTO).setInheritVirAttrs(((RoleTO) template).isInheritVirAttrs());
                ((RoleTO) subjectTO).setInheritPasswordPolicy(((RoleTO) template).isInheritPasswordPolicy());
                ((RoleTO) subjectTO).setInheritAccountPolicy(((RoleTO) template).isInheritAccountPolicy());
            }

            fillFromTemplate(subjectTO, template);

            for (String resource : template.getResources()) {
                subjectTO.getResources().add(resource);
            }
        }

        return subjectTO;
    }

    /**
     * Extract password value from passed value (if instance of GuardedString or GuardedByteArray).
     *
     * @param pwd received from the underlying connector
     * @return password value
     */
    public String getPassword(final Object pwd) {
        final StringBuilder result = new StringBuilder();

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
     * Get connector object TO from a connector object.
     *
     * @param connObject connector object.
     * @return connector object TO.
     */
    public ConnObjectTO getConnObjectTO(final ConnectorObject connObject) {
        final ConnObjectTO connObjectTO = new ConnObjectTO();

        for (Attribute attr : connObject.getAttributes()) {
            AttributeTO attrTO = new AttributeTO();
            attrTO.setSchema(attr.getName());

            if (attr.getValue() != null) {
                for (Object value : attr.getValue()) {
                    if (value != null) {
                        if (value instanceof GuardedString || value instanceof GuardedByteArray) {
                            attrTO.getValues().add(getPassword(value));
                        } else if (value instanceof byte[]) {
                            attrTO.getValues().add(Base64.encode((byte[]) value));
                        } else {
                            attrTO.getValues().add(value.toString());
                        }
                    }
                }
            }

            connObjectTO.getAttrs().add(attrTO);
        }

        return connObjectTO;
    }

    /**
     * Query connected external resources for values to populated virtual attributes associated with the given owner.
     *
     * @param owner user or role
     * @param attrUtil attributable util
     */
    public void retrieveVirAttrValues(final AbstractAttributable owner, final AttributableUtil attrUtil) {
        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final ConnectorFactory connFactory = context.getBean(ConnectorFactory.class);

        final IntMappingType type = attrUtil.getType() == AttributableType.USER
                ? IntMappingType.UserVirtualSchema : attrUtil.getType() == AttributableType.ROLE
                ? IntMappingType.RoleVirtualSchema : IntMappingType.MembershipVirtualSchema;

        final Map<String, ConnectorObject> externalResources = new HashMap<String, ConnectorObject>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (AbstractVirAttr virAttr : owner.getVirAttrs()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(owner, virAttr, attrUtil, type, externalResources, connFactory);
            }
        }
        // -----------------------
    }

    private void retrieveVirAttrValue(
            final AbstractAttributable owner,
            final AbstractVirAttr virAttr,
            final AttributableUtil attrUtil,
            final IntMappingType type,
            final Map<String, ConnectorObject> externalResources,
            final ConnectorFactory connFactory) {

        final String schemaName = virAttr.getSchema().getName();
        final VirAttrCacheValue virAttrCacheValue = virAttrCache.get(attrUtil.getType(), owner.getId(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {} ({})", schemaName, type);

        if (virAttrCache.isValidEntry(virAttrCacheValue)) {
            // cached ...
            LOG.debug("Values found in cache {}", virAttrCacheValue);
            virAttr.setValues(new ArrayList<String>(virAttrCacheValue.getValues()));
        } else {
            // not cached ...
            LOG.debug("Need one or more remote connections");

            final VirAttrCacheValue toBeCached = new VirAttrCacheValue();

            // SYNCOPE-458 if virattr owner is a Membership, owner must become user involved in membership because 
            // membership mapping is contained in user mapping
            final AbstractSubject realOwner = owner instanceof Membership
                    ? ((Membership) owner).getSyncopeUser()
                    : (AbstractSubject) owner;

            final Set<ExternalResource> targetResources = owner instanceof Membership ? getTargetResource(virAttr, type,
                    attrUtil, realOwner.getResources()) : getTargetResource(virAttr, type, attrUtil);

            for (ExternalResource resource : targetResources) {
                LOG.debug("Search values into {}", resource.getName());
                try {
                    final List<AbstractMappingItem> mappings = attrUtil.getMappingItems(resource, MappingPurpose.BOTH);

                    final ConnectorObject connectorObject;

                    if (externalResources.containsKey(resource.getName())) {
                        connectorObject = externalResources.get(resource.getName());
                    } else {
                        LOG.debug("Perform connection to {}", resource.getName());
                        final String accountId = attrUtil.getAccountIdItem(resource) == null
                                ? null
                                : MappingUtil.getAccountIdValue(
                                        realOwner, resource, attrUtil.getAccountIdItem(resource));

                        if (StringUtils.isBlank(accountId)) {
                            throw new IllegalArgumentException("No AccountId found for " + resource.getName());
                        }

                        final Connector connector = connFactory.getConnector(resource);

                        final OperationOptions oo =
                                connector.getOperationOptions(MappingUtil.getMatchingMappingItems(mappings, type));

                        connectorObject = connector.getObject(fromAttributable(realOwner), new Uid(accountId), oo);
                        externalResources.put(resource.getName(), connectorObject);
                    }

                    if (connectorObject != null) {
                        // ask for searched virtual attribute value
                        final List<AbstractMappingItem> virAttrMappings =
                                MappingUtil.getMatchingMappingItems(mappings, schemaName, type);

                        // the same virtual attribute could be mapped with one or more external attribute 
                        for (AbstractMappingItem mapping : virAttrMappings) {
                            final Attribute attribute = connectorObject.getAttributeByName(mapping.getExtAttrName());

                            if (attribute != null && attribute.getValue() != null) {
                                for (Object obj : attribute.getValue()) {
                                    if (obj != null) {
                                        virAttr.getValues().add(obj.toString());
                                    }
                                }
                            }
                        }

                        toBeCached.setResourceValues(resource.getName(), new HashSet<String>(virAttr.getValues()));

                        LOG.debug("Retrieved values {}", virAttr.getValues());
                    }
                } catch (Exception e) {
                    LOG.error("Error reading connector object from {}", resource.getName(), e);

                    if (virAttrCacheValue != null) {
                        toBeCached.forceExpiring();
                        LOG.debug("Search for a cached value (even expired!) ...");
                        final Set<String> cachedValues = virAttrCacheValue.getValues(resource.getName());
                        if (cachedValues != null) {
                            LOG.debug("Use cached value {}", cachedValues);
                            virAttr.getValues().addAll(cachedValues);
                            toBeCached.setResourceValues(resource.getName(), new HashSet<String>(cachedValues));
                        }
                    }
                }
            }

            virAttrCache.put(attrUtil.getType(), owner.getId(), schemaName, toBeCached);
        }
    }

    private Set<ExternalResource> getTargetResource(
            final AbstractVirAttr attr, final IntMappingType type, final AttributableUtil attrUtil) {

        final Set<ExternalResource> resources = new HashSet<ExternalResource>();

        if (attr.getOwner() instanceof AbstractSubject) {
            for (ExternalResource res : ((AbstractSubject) attr.getOwner()).getResources()) {
                if (!MappingUtil.getMatchingMappingItems(
                        attrUtil.getMappingItems(res, MappingPurpose.BOTH),
                        attr.getSchema().getName(), type).isEmpty()) {

                    resources.add(res);
                }
            }
        }

        return resources;
    }

    private Set<ExternalResource> getTargetResource(final AbstractVirAttr attr, final IntMappingType type,
            final AttributableUtil attrUtil, final Set<ExternalResource> ownerResources) {

        final Set<ExternalResource> resources = new HashSet<ExternalResource>();

        for (ExternalResource res : ownerResources) {
            if (!MappingUtil.getMatchingMappingItems(
                    attrUtil.getMappingItems(res, MappingPurpose.BOTH),
                    attr.getSchema().getName(), type).isEmpty()) {

                resources.add(res);
            }
        }

        return resources;
    }

    private void fillFromTemplate(final AbstractAttributableTO attributableTO, final AbstractAttributableTO template) {
        Map<String, AttributeTO> currentAttrMap = attributableTO.getAttrMap();
        for (AttributeTO templateAttr : template.getAttrs()) {
            if (templateAttr.getValues() != null && !templateAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateAttr.getSchema())
                    || currentAttrMap.get(templateAttr.getSchema()).getValues().isEmpty())) {

                attributableTO.getAttrs().add(evaluateAttrTemplate(attributableTO, templateAttr));
            }
        }

        currentAttrMap = attributableTO.getDerAttrMap();
        for (AttributeTO templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                attributableTO.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = attributableTO.getVirAttrMap();
        for (AttributeTO templateVirAttr : template.getVirAttrs()) {
            if (templateVirAttr.getValues() != null && !templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                attributableTO.getVirAttrs().add(evaluateAttrTemplate(attributableTO, templateVirAttr));
            }
        }
    }

    private AttributeTO evaluateAttrTemplate(final AbstractAttributableTO attributableTO, final AttributeTO template) {
        AttributeTO result = new AttributeTO();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            for (String value : template.getValues()) {
                String evaluated = JexlUtil.evaluate(value, attributableTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            }
        }

        return result;
    }

    /**
     * Transform a
     * <code>Collection</code> of {@link Attribute} instances into a {@link Map}. The key to each element in the map is
     * the <i>name</i> of an
     * <code>Attribute</code>. The value of each element in the map is the
     * <code>Attribute</code> instance with that name. <br/> Different from the original because: <ul> <li>map keys are
     * transformed toUpperCase()</li> <li>returned map is mutable</li> </ul>
     *
     * @param attributes set of attribute to transform to a map.
     * @return a map of string and attribute.
     *
     * @see org.identityconnectors.framework.common.objects.AttributeUtil#toMap(java.util.Collection)
     */
    public Map<String, Attribute> toMap(final Collection<? extends Attribute> attributes) {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute attr : attributes) {
            map.put(attr.getName().toUpperCase(), attr);
        }
        return map;
    }
}
