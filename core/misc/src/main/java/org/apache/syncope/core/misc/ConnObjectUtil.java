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
package org.apache.syncope.core.misc;

import org.apache.syncope.core.misc.policy.InvalidPasswordPolicySpecException;
import org.apache.syncope.core.misc.security.PasswordGenerator;
import org.apache.syncope.core.misc.security.SecureRandomUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AttributableOperations;
import org.apache.syncope.common.lib.mod.AbstractAttributableMod;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.jexl.JexlUtil;
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

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PasswordGenerator pwdGen;

    private final Encryptor encryptor = Encryptor.getInstance();

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

    public ObjectClass fromSubject(final Subject<?, ?, ?> subject) {
        if (subject == null) {
            throw new IllegalArgumentException("No ObjectClass could be provided for " + subject);
        }

        ObjectClass result = null;
        if (subject instanceof User) {
            result = ObjectClass.ACCOUNT;
        }
        if (subject instanceof Group) {
            result = ObjectClass.GROUP;
        }

        return result;
    }

    /**
     * Build a UserTO / GroupTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @param attrUtil AttributableUtil
     * @param <T> user/group
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AbstractSubjectTO> T getSubjectTO(final ConnectorObject obj, final SyncTask syncTask,
            final AttributableUtil attrUtil) {

        T subjectTO = getSubjectTOFromConnObject(obj, syncTask, attrUtil);

        // (for users) if password was not set above, generate
        if (subjectTO instanceof UserTO && StringUtils.isBlank(((UserTO) subjectTO).getPassword())) {
            final UserTO userTO = (UserTO) subjectTO;

            List<PasswordPolicySpec> ppSpecs = new ArrayList<>();

            PasswordPolicy globalPP = policyDAO.getGlobalPasswordPolicy();
            if (globalPP != null && globalPP.getSpecification(PasswordPolicySpec.class) != null) {
                ppSpecs.add(globalPP.getSpecification(PasswordPolicySpec.class));
            }

            for (MembershipTO memb : userTO.getMemberships()) {
                Group group = groupDAO.find(memb.getGroupId());
                if (group != null && group.getPasswordPolicy() != null
                        && group.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                    ppSpecs.add(group.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
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
     * @param key user to be updated
     * @param obj connector object
     * @param original subject to get diff from
     * @param syncTask synchronization task
     * @param attrUtil AttributableUtil
     * @param <T> user/group
     * @return modifications for the user/group to be updated
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T extends AbstractAttributableMod> T getAttributableMod(final Long key, final ConnectorObject obj,
            final AbstractAttributableTO original, final SyncTask syncTask, final AttributableUtil attrUtil) {

        final AbstractAttributableTO updated = getSubjectTOFromConnObject(obj, syncTask, attrUtil);
        updated.setKey(key);

        if (AttributableType.USER == attrUtil.getType()) {
            // update password if and only if password is really changed
            final User user = userDAO.authFetch(key);
            if (StringUtils.isBlank(((UserTO) updated).getPassword())
                    || encryptor.verify(((UserTO) updated).getPassword(),
                            user.getCipherAlgorithm(), user.getPassword())) {

                ((UserTO) updated).setPassword(null);
            }

            for (MembershipTO membTO : ((UserTO) updated).getMemberships()) {
                Membership memb = user.getMembership(membTO.getGroupId());
                if (memb != null) {
                    membTO.setKey(memb.getKey());
                }
            }

            return (T) AttributableOperations.diff(((UserTO) updated), ((UserTO) original), true);
        }
        if (AttributableType.GROUP == attrUtil.getType()) {
            // reading from connector object cannot change entitlements
            ((GroupTO) updated).getEntitlements().addAll(((GroupTO) original).getEntitlements());
            return (T) AttributableOperations.diff(((GroupTO) updated), ((GroupTO) original), true);
        }

        return null;
    }

    private <T extends AbstractSubjectTO> T getSubjectTOFromConnObject(final ConnectorObject obj,
            final SyncTask syncTask, final AttributableUtil attrUtil) {

        final T subjectTO = attrUtil.newSubjectTO();

        // 1. fill with data from connector object
        for (MappingItem item : attrUtil.getUidToMappingItems(
                syncTask.getResource(), MappingPurpose.SYNCHRONIZATION)) {

            Attribute attribute = obj.getAttributeByName(item.getExtAttrName());

            AttrTO attributeTO;
            switch (item.getIntMappingType()) {
                case UserId:
                case GroupId:
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

                case GroupName:
                    if (subjectTO instanceof GroupTO) {
                        ((GroupTO) subjectTO).setName(attribute == null || attribute.getValue().isEmpty()
                                || attribute.getValue().get(0) == null
                                        ? null
                                        : attribute.getValue().get(0).toString());
                    }
                    break;

                case GroupOwnerSchema:
                    if (subjectTO instanceof GroupTO && attribute != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // GroupOwnerSchema value
                        attributeTO = new AttrTO();
                        attributeTO.setSchema(StringUtils.EMPTY);
                        if (attribute.getValue().isEmpty() || attribute.getValue().get(0) == null) {
                            attributeTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attributeTO.getValues().add(attribute.getValue().get(0).toString());
                        }

                        ((GroupTO) subjectTO).getPlainAttrs().add(attributeTO);
                    }
                    break;

                case UserPlainSchema:
                case GroupPlainSchema:
                    attributeTO = new AttrTO();
                    attributeTO.setSchema(item.getIntAttrName());

                    PlainSchema schema = plainSchemaDAO.find(item.getIntAttrName(), attrUtil.plainSchemaClass());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {

                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            final PlainAttrValue attrValue = attrUtil.newPlainAttrValue();
                            switch (schemaType) {
                                case String:
                                    attrValue.setStringValue(value.toString());
                                    break;

                                case Binary:
                                    attrValue.setBinaryValue((byte[]) value);
                                    break;

                                default:
                                    try {
                                        attrValue.parseValue(schema, value.toString());
                                    } catch (ParsingValidationException e) {
                                        LOG.error("While parsing provided value {}", value, e);
                                        attrValue.setStringValue(value.toString());
                                        schemaType = AttrSchemaType.String;
                                    }
                                    break;
                            }
                            attributeTO.getValues().add(attrValue.getValueAsString(schemaType));
                        }
                    }

                    subjectTO.getPlainAttrs().add(attributeTO);
                    break;

                case UserDerivedSchema:
                case GroupDerivedSchema:
                    attributeTO = new AttrTO();
                    attributeTO.setSchema(item.getIntAttrName());
                    subjectTO.getDerAttrs().add(attributeTO);
                    break;

                case UserVirtualSchema:
                case GroupVirtualSchema:
                    attributeTO = new AttrTO();
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
                ? syncTask.getUserTemplate() : syncTask.getGroupTemplate();

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
                    if (currentMembs.containsKey(membTO.getGroupId())) {
                        membTBU = currentMembs.get(membTO.getGroupId());
                    } else {
                        membTBU = new MembershipTO();
                        membTBU.setGroupId(membTO.getGroupId());
                        ((UserTO) subjectTO).getMemberships().add(membTBU);
                    }
                    fillFromTemplate(membTBU, membTO);
                }
            }
            if (template instanceof GroupTO) {
                if (StringUtils.isNotBlank(((GroupTO) template).getName())) {
                    String evaluated = JexlUtil.evaluate(((GroupTO) template).getName(), subjectTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((GroupTO) subjectTO).setName(evaluated);
                    }
                }

                if (((GroupTO) template).getParent() != 0) {
                    final Group parentGroup = groupDAO.find(((GroupTO) template).getParent());
                    if (parentGroup != null) {
                        ((GroupTO) subjectTO).setParent(parentGroup.getKey());
                    }
                }

                if (((GroupTO) template).getUserOwner() != null) {
                    final User userOwner = userDAO.find(((GroupTO) template).getUserOwner());
                    if (userOwner != null) {
                        ((GroupTO) subjectTO).setUserOwner(userOwner.getKey());
                    }
                }
                if (((GroupTO) template).getGroupOwner() != null) {
                    final Group groupOwner = groupDAO.find(((GroupTO) template).getGroupOwner());
                    if (groupOwner != null) {
                        ((GroupTO) subjectTO).setGroupOwner(groupOwner.getKey());
                    }
                }

                ((GroupTO) subjectTO).getEntitlements().addAll(((GroupTO) template).getEntitlements());

                ((GroupTO) subjectTO).getGPlainAttrTemplates().addAll(((GroupTO) template).getGPlainAttrTemplates());
                ((GroupTO) subjectTO).getGDerAttrTemplates().addAll(((GroupTO) template).getGDerAttrTemplates());
                ((GroupTO) subjectTO).getGVirAttrTemplates().addAll(((GroupTO) template).getGVirAttrTemplates());
                ((GroupTO) subjectTO).getMPlainAttrTemplates().addAll(((GroupTO) template).getMPlainAttrTemplates());
                ((GroupTO) subjectTO).getMDerAttrTemplates().addAll(((GroupTO) template).getMDerAttrTemplates());
                ((GroupTO) subjectTO).getMVirAttrTemplates().addAll(((GroupTO) template).getMVirAttrTemplates());

                ((GroupTO) subjectTO).setAccountPolicy(((GroupTO) template).getAccountPolicy());
                ((GroupTO) subjectTO).setPasswordPolicy(((GroupTO) template).getPasswordPolicy());

                ((GroupTO) subjectTO).setInheritOwner(((GroupTO) template).isInheritOwner());
                ((GroupTO) subjectTO).setInheritTemplates(((GroupTO) template).isInheritTemplates());
                ((GroupTO) subjectTO).setInheritPlainAttrs(((GroupTO) template).isInheritPlainAttrs());
                ((GroupTO) subjectTO).setInheritDerAttrs(((GroupTO) template).isInheritDerAttrs());
                ((GroupTO) subjectTO).setInheritVirAttrs(((GroupTO) template).isInheritVirAttrs());
                ((GroupTO) subjectTO).setInheritPasswordPolicy(((GroupTO) template).isInheritPasswordPolicy());
                ((GroupTO) subjectTO).setInheritAccountPolicy(((GroupTO) template).isInheritAccountPolicy());
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
            AttrTO attrTO = new AttrTO();
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

            connObjectTO.getPlainAttrs().add(attrTO);
        }

        return connObjectTO;
    }

    /**
     * Query connected external resources for values to populated virtual attributes associated with the given owner.
     *
     * @param owner user or group
     * @param attrUtil attributable util
     */
    public void retrieveVirAttrValues(final Attributable<?, ?, ?> owner, final AttributableUtil attrUtil) {
        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final ConnectorFactory connFactory = context.getBean(ConnectorFactory.class);

        final IntMappingType type = attrUtil.getType() == AttributableType.USER
                ? IntMappingType.UserVirtualSchema : attrUtil.getType() == AttributableType.GROUP
                        ? IntMappingType.GroupVirtualSchema : IntMappingType.MembershipVirtualSchema;

        final Map<String, ConnectorObject> externalResources = new HashMap<>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (VirAttr virAttr : owner.getVirAttrs()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(owner, virAttr, attrUtil, type, externalResources, connFactory);
            }
        }
        // -----------------------
    }

    private void retrieveVirAttrValue(
            final Attributable<?, ?, ?> owner,
            final VirAttr virAttr,
            final AttributableUtil attrUtil,
            final IntMappingType type,
            final Map<String, ConnectorObject> externalResources,
            final ConnectorFactory connFactory) {

        final String schemaName = virAttr.getSchema().getKey();
        final VirAttrCacheValue virAttrCacheValue = virAttrCache.get(attrUtil.getType(), owner.getKey(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {} ({})", schemaName, type);

        if (virAttrCache.isValidEntry(virAttrCacheValue)) {
            // cached ...
            LOG.debug("Values found in cache {}", virAttrCacheValue);
            virAttr.getValues().clear();
            virAttr.getValues().addAll(new ArrayList<>(virAttrCacheValue.getValues()));
        } else {
            // not cached ...
            LOG.debug("Need one or more remote connections");

            final VirAttrCacheValue toBeCached = new VirAttrCacheValue();

            // SYNCOPE-458 if virattr owner is a Membership, owner must become user involved in membership because 
            // membership mapping is contained in user mapping
            final Subject<?, ?, ?> realOwner = owner instanceof Membership
                    ? ((Membership) owner).getUser()
                    : (Subject) owner;

            final Set<ExternalResource> targetResources = owner instanceof Membership
                    ? getTargetResource(virAttr, type, attrUtil, realOwner.getResources())
                    : getTargetResource(virAttr, type, attrUtil);

            for (ExternalResource resource : targetResources) {
                LOG.debug("Search values into {}", resource.getKey());
                try {
                    final List<MappingItem> mappings = attrUtil.getMappingItems(resource, MappingPurpose.BOTH);

                    final ConnectorObject connectorObject;

                    if (externalResources.containsKey(resource.getKey())) {
                        connectorObject = externalResources.get(resource.getKey());
                    } else {
                        LOG.debug("Perform connection to {}", resource.getKey());
                        final String accountId = attrUtil.getAccountIdItem(resource) == null
                                ? null
                                : MappingUtil.getAccountIdValue(
                                        realOwner, resource, attrUtil.getAccountIdItem(resource));

                        if (StringUtils.isBlank(accountId)) {
                            throw new IllegalArgumentException("No AccountId found for " + resource.getKey());
                        }

                        final Connector connector = connFactory.getConnector(resource);

                        final OperationOptions oo =
                                connector.getOperationOptions(MappingUtil.getMatchingMappingItems(mappings, type));

                        connectorObject = connector.getObject(fromSubject(realOwner), new Uid(accountId), oo);
                        externalResources.put(resource.getKey(), connectorObject);
                    }

                    if (connectorObject != null) {
                        // ask for searched virtual attribute value
                        final List<MappingItem> virAttrMappings =
                                MappingUtil.getMatchingMappingItems(mappings, schemaName, type);

                        // the same virtual attribute could be mapped with one or more external attribute 
                        for (MappingItem mapping : virAttrMappings) {
                            final Attribute attribute = connectorObject.getAttributeByName(mapping.getExtAttrName());

                            if (attribute != null && attribute.getValue() != null) {
                                for (Object obj : attribute.getValue()) {
                                    if (obj != null) {
                                        virAttr.getValues().add(obj.toString());
                                    }
                                }
                            }
                        }

                        toBeCached.setResourceValues(resource.getKey(), new HashSet<String>(virAttr.getValues()));

                        LOG.debug("Retrieved values {}", virAttr.getValues());
                    }
                } catch (Exception e) {
                    LOG.error("Error reading connector object from {}", resource.getKey(), e);

                    if (virAttrCacheValue != null) {
                        toBeCached.forceExpiring();
                        LOG.debug("Search for a cached value (even expired!) ...");
                        final Set<String> cachedValues = virAttrCacheValue.getValues(resource.getKey());
                        if (cachedValues != null) {
                            LOG.debug("Use cached value {}", cachedValues);
                            virAttr.getValues().addAll(cachedValues);
                            toBeCached.setResourceValues(resource.getKey(), new HashSet<>(cachedValues));
                        }
                    }
                }
            }

            virAttrCache.put(attrUtil.getType(), owner.getKey(), schemaName, toBeCached);
        }
    }

    private Set<ExternalResource> getTargetResource(
            final VirAttr attr, final IntMappingType type, final AttributableUtil attrUtil) {

        final Set<ExternalResource> resources = new HashSet<>();

        if (attr.getOwner() instanceof Subject) {
            for (ExternalResource res : ((Subject<?, ?, ?>) attr.getOwner()).getResources()) {
                if (!MappingUtil.getMatchingMappingItems(
                        attrUtil.getMappingItems(res, MappingPurpose.BOTH),
                        attr.getSchema().getKey(), type).isEmpty()) {

                    resources.add(res);
                }
            }
        }

        return resources;
    }

    private Set<ExternalResource> getTargetResource(final VirAttr attr, final IntMappingType type,
            final AttributableUtil attrUtil, final Set<? extends ExternalResource> ownerResources) {

        final Set<ExternalResource> resources = new HashSet<>();

        for (ExternalResource res : ownerResources) {
            if (!MappingUtil.getMatchingMappingItems(
                    attrUtil.getMappingItems(res, MappingPurpose.BOTH),
                    attr.getSchema().getKey(), type).isEmpty()) {

                resources.add(res);
            }
        }

        return resources;
    }

    private void fillFromTemplate(final AbstractAttributableTO attributableTO, final AbstractAttributableTO template) {
        Map<String, AttrTO> currentAttrMap = attributableTO.getPlainAttrMap();
        for (AttrTO templateAttr : template.getPlainAttrs()) {
            if (templateAttr.getValues() != null && !templateAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateAttr.getSchema())
                    || currentAttrMap.get(templateAttr.getSchema()).getValues().isEmpty())) {

                attributableTO.getPlainAttrs().add(evaluateAttrTemplate(attributableTO, templateAttr));
            }
        }

        currentAttrMap = attributableTO.getDerAttrMap();
        for (AttrTO templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                attributableTO.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = attributableTO.getVirAttrMap();
        for (AttrTO templateVirAttr : template.getVirAttrs()) {
            if (templateVirAttr.getValues() != null && !templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                attributableTO.getVirAttrs().add(evaluateAttrTemplate(attributableTO, templateVirAttr));
            }
        }
    }

    private AttrTO evaluateAttrTemplate(final AbstractAttributableTO attributableTO, final AttrTO template) {
        AttrTO result = new AttrTO();
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
        final Map<String, Attribute> map = new HashMap<>();
        for (Attribute attr : attributes) {
            map.put(attr.getName().toUpperCase(), attr);
        }
        return map;
    }
}
