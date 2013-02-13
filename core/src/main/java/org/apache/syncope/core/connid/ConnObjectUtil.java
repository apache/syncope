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
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.propagation.SyncopeConnector;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.InvalidPasswordPolicySpecException;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.apache.syncope.core.util.VirAttrCache;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
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
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDAO resourceDAO;

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
     * Build an UserTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @param attrUtil AttributableUtil
     * @param <T> user/role
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AbstractAttributableTO> T getAttributableTO(final ConnectorObject obj, final SyncTask syncTask,
            final AttributableUtil attrUtil) {

        T subjectTO = getAttributableTOFromConnObject(obj, syncTask, attrUtil);

        // if password was not set above, generate
        if (AttributableType.USER == attrUtil.getType() && StringUtils.isBlank(((UserTO) subjectTO).getPassword())) {
            List<PasswordPolicySpec> ppSpecs = new ArrayList<PasswordPolicySpec>();
            ppSpecs.add((PasswordPolicySpec) policyDAO.getGlobalPasswordPolicy().getSpecification());

            for (MembershipTO memb : ((UserTO) subjectTO).getMemberships()) {
                SyncopeRole role = roleDAO.find(memb.getRoleId());
                if (role != null && role.getPasswordPolicy() != null
                        && role.getPasswordPolicy().getSpecification() != null) {

                    ppSpecs.add((PasswordPolicySpec) role.getPasswordPolicy().getSpecification());
                }
            }
            for (String resName : subjectTO.getResources()) {
                ExternalResource resource = resourceDAO.find(resName);
                if (resource != null && resource.getPasswordPolicy() != null
                        && resource.getPasswordPolicy().getSpecification() != null) {

                    ppSpecs.add((PasswordPolicySpec) resource.getPasswordPolicy().getSpecification());
                }
            }

            String password;
            try {
                password = pwdGen.generatePasswordFromPwdSpec(ppSpecs);
            } catch (InvalidPasswordPolicySpecException e) {
                LOG.error("Could not generate policy-compliant random password for {}", subjectTO, e);

                password = RandomStringUtils.randomAlphanumeric(16);
            }
            ((UserTO) subjectTO).setPassword(password);
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

        final AbstractAttributableTO updated = getAttributableTOFromConnObject(obj, syncTask, attrUtil);
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
            return (T) AttributableOperations.diff(((RoleTO) updated), ((RoleTO) original), true);
        }

        return null;
    }

    private <T extends AbstractAttributableTO> T getAttributableTOFromConnObject(final ConnectorObject obj,
            final SyncTask syncTask, final AttributableUtil attrUtil) {

        final T attributableTO = attrUtil.newAttributableTO();

        // 1. fill with data from connector object
        for (AbstractMappingItem item : attrUtil.getMappingItems(syncTask.getResource())) {
            Attribute attribute = obj.getAttributeByName(item.getExtAttrName());

            AttributeTO attributeTO;
            switch (item.getIntMappingType()) {
                case UserId:
                case RoleId:
                    break;

                case Password:
                    if (attributableTO instanceof UserTO && attribute != null && attribute.getValue() != null
                            && !attribute.getValue().isEmpty()) {

                        ((UserTO) attributableTO).setPassword(getPassword(attribute.getValue().get(0)));
                    }
                    break;

                case Username:
                    if (attributableTO instanceof UserTO) {
                        ((UserTO) attributableTO).setUsername(attribute == null || attribute.getValue().isEmpty()
                                || attribute.getValue().get(0) == null
                                ? null
                                : attribute.getValue().get(0).toString());
                    }
                    break;

                case RoleName:
                    if (attributableTO instanceof RoleTO) {
                        ((RoleTO) attributableTO).setName(attribute == null || attribute.getValue().isEmpty()
                                || attribute.getValue().get(0) == null
                                ? null
                                : attribute.getValue().get(0).toString());
                    }
                    break;

                case RoleOwnerSchema:
                    if (attributableTO instanceof RoleTO && attribute != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // RoleOwnerSchema value
                        attributeTO = new AttributeTO();
                        attributeTO.setSchema(StringUtils.EMPTY);
                        if (attribute.getValue().isEmpty() || attribute.getValue().get(0) == null) {
                            attributeTO.addValue(StringUtils.EMPTY);
                        } else {
                            attributeTO.addValue(attribute.getValue().get(0).toString());
                        }

                        ((RoleTO) attributableTO).addAttribute(attributeTO);
                    }
                    break;

                case UserSchema:
                case RoleSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {

                        if (value != null) {
                            attributeTO.addValue(value.toString());
                        }
                    }

                    attributableTO.addAttribute(attributeTO);
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());
                    attributableTO.addDerivedAttribute(attributeTO);
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(item.getIntAttrName());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {

                        if (value != null) {
                            attributeTO.addValue(value.toString());
                        }
                    }

                    attributableTO.addVirtualAttribute(attributeTO);
                    break;

                default:
            }
        }

        // 2. add data from defined template (if any)
        AbstractAttributableTO template = AttributableType.USER == attrUtil.getType()
                ? syncTask.getUserTemplate() : syncTask.getRoleTemplate();
        if (template != null) {
            if (template instanceof UserTO) {
                if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                    String evaluated = jexlUtil.evaluate(((UserTO) template).getUsername(), attributableTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) attributableTO).setUsername(evaluated);
                    }
                }

                if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                    String evaluated = jexlUtil.evaluate(((UserTO) template).getPassword(), attributableTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) attributableTO).setPassword(evaluated);
                    }
                }

                Map<Long, MembershipTO> currentMembs = ((UserTO) attributableTO).getMembershipMap();
                for (MembershipTO membTO : ((UserTO) template).getMemberships()) {
                    MembershipTO membTBU;
                    if (currentMembs.containsKey(membTO.getRoleId())) {
                        membTBU = currentMembs.get(membTO.getRoleId());
                    } else {
                        membTBU = new MembershipTO();
                        membTBU.setRoleId(membTO.getRoleId());
                        ((UserTO) attributableTO).addMembership(membTBU);
                    }
                    fillFromTemplate(membTBU, membTO);
                }
            }
            if (template instanceof RoleTO) {
                if (StringUtils.isNotBlank(((RoleTO) template).getName())) {
                    String evaluated = jexlUtil.evaluate(((RoleTO) template).getName(), attributableTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((RoleTO) attributableTO).setName(evaluated);
                    }
                }

                ((RoleTO) attributableTO).setParent(((RoleTO) template).getParent());

                ((RoleTO) attributableTO).setUserOwner(((RoleTO) template).getUserOwner());
                ((RoleTO) attributableTO).setRoleOwner(((RoleTO) template).getRoleOwner());

                ((RoleTO) attributableTO).setEntitlements(((RoleTO) template).getEntitlements());

                ((RoleTO) attributableTO).setAccountPolicy(((RoleTO) template).getAccountPolicy());
                ((RoleTO) attributableTO).setPasswordPolicy(((RoleTO) template).getPasswordPolicy());

                ((RoleTO) attributableTO).setInheritOwner(((RoleTO) template).isInheritOwner());
                ((RoleTO) attributableTO).setInheritAttributes(((RoleTO) template).isInheritAttributes());
                ((RoleTO) attributableTO).setInheritDerivedAttributes(((RoleTO) template).isInheritDerivedAttributes());
                ((RoleTO) attributableTO).setInheritVirtualAttributes(((RoleTO) template).isInheritVirtualAttributes());
                ((RoleTO) attributableTO).setInheritPasswordPolicy(((RoleTO) template).isInheritPasswordPolicy());
                ((RoleTO) attributableTO).setInheritAccountPolicy(((RoleTO) template).isInheritAccountPolicy());
            }

            fillFromTemplate(attributableTO, template);

            for (String resource : template.getResources()) {
                attributableTO.addResource(resource);
            }
        }

        return attributableTO;
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
                            attrTO.addValue(getPassword(value));
                        } else {
                            attrTO.addValue(value.toString());
                        }
                    }
                }
            }

            connObjectTO.addAttribute(attrTO);
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
        final ConnectorFactory connInstanceLoader = context.getBean(ConnectorFactory.class);

        final IntMappingType type = attrUtil.getType() == AttributableType.USER
                ? IntMappingType.UserVirtualSchema : attrUtil.getType() == AttributableType.ROLE
                ? IntMappingType.RoleVirtualSchema : IntMappingType.MembershipVirtualSchema;

        final Map<String, ConnectorObject> externalResources = new HashMap<String, ConnectorObject>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (AbstractVirAttr virAttr : owner.getVirtualAttributes()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(owner, virAttr, attrUtil, type, externalResources, connInstanceLoader);
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
            final ConnectorFactory connInstanceLoader) {

        final String schemaName = virAttr.getVirtualSchema().getName();
        final List<String> values = virAttrCache.get(attrUtil.getType(), owner.getId(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {} ({})", schemaName, type);

        if (values == null) {
            // non cached ...
            LOG.debug("Need one or more remote connections");
            for (ExternalResource resource : getTargetResource(virAttr, type, attrUtil)) {
                LOG.debug("Seach values into {}", resource.getName());
                try {
                    final ConnectorObject connectorObject;

                    if (externalResources.containsKey(resource.getName())) {
                        connectorObject = externalResources.get(resource.getName());
                    } else {
                        LOG.debug("Perform connection to {}", resource.getName());
                        final String accountId = attrUtil.getAccountIdItem(resource) == null
                                ? null
                                : MappingUtil.getAccountIdValue(
                                owner, resource, attrUtil.getAccountIdItem(resource));

                        if (StringUtils.isBlank(accountId)) {
                            throw new IllegalArgumentException("No AccountId found for " + resource.getName());
                        }

                        final Set<String> extAttrNames = new HashSet<String>();

                        // retrieve all mapped virtual attribute values
                        for (AbstractMappingItem item :
                                MappingUtil.getMatchingMappingItems(attrUtil.getMappingItems(resource), type)) {
                            extAttrNames.add(item.getExtAttrName());
                        }

                        LOG.debug("External attribute ({}) names to get '{}'", type, extAttrNames);

                        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
                        oob.setAttributesToGet(extAttrNames);

                        final SyncopeConnector connector = connInstanceLoader.getConnector(resource);
                        connectorObject = connector.getObject(fromAttributable(owner), new Uid(accountId), oob.build());
                        externalResources.put(resource.getName(), connectorObject);
                    }

                    if (connectorObject != null) {
                        // ask for searched virtual attribute value
                        final List<AbstractMappingItem> mappings = MappingUtil.getMatchingMappingItems(
                                attrUtil.getMappingItems(resource), schemaName, type);

                        // the same virtual attribute could be mapped with one or more external attribute 
                        for (AbstractMappingItem mapping : mappings) {
                            final Attribute attribute = connectorObject.getAttributeByName(mapping.getExtAttrName());

                            if (attribute != null && attribute.getValue() != null) {
                                for (Object obj : attribute.getValue()) {
                                    if (obj != null) {
                                        virAttr.addValue(obj.toString());
                                    }
                                }
                            }
                        }
                    }
                    
                    LOG.debug("Retrieved values {}", virAttr.getValues());
                } catch (Exception e) {
                    LOG.error("Error reading connector object from {}", resource.getName(), e);
                }
            }

            virAttrCache.put(attrUtil.getType(), owner.getId(), schemaName, virAttr.getValues());
        } else {
            // cached ...
            LOG.debug("Values found in cache {}", values);
            virAttr.setValues(values);
        }
    }

    private Set<ExternalResource> getTargetResource(
            final AbstractVirAttr attr, final IntMappingType type, final AttributableUtil attrUtil) {

        final Set<ExternalResource> resources = new HashSet<ExternalResource>();

        for (ExternalResource res : attr.getOwner().getResources()) {
            if (!MappingUtil.getMatchingMappingItems(
                    attrUtil.getMappingItems(res), attr.getVirtualSchema().getName(), type).isEmpty()) {

                resources.add(res);
            }
        }

        return resources;
    }

    private void fillFromTemplate(final AbstractAttributableTO attributableTO, final AbstractAttributableTO template) {
        Map<String, AttributeTO> currentAttrMap = attributableTO.getAttributeMap();
        for (AttributeTO attrTO : template.getAttributes()) {
            if (!currentAttrMap.containsKey(attrTO.getSchema())) {
                attributableTO.addAttribute(evaluateAttrTemplate(attributableTO, attrTO));
            }
        }

        currentAttrMap = attributableTO.getDerivedAttributeMap();
        for (AttributeTO attrTO : template.getDerivedAttributes()) {
            if (!currentAttrMap.containsKey(attrTO.getSchema())) {
                attributableTO.addDerivedAttribute(attrTO);
            }
        }

        currentAttrMap = attributableTO.getVirtualAttributeMap();
        for (AttributeTO attrTO : template.getDerivedAttributes()) {
            if (!currentAttrMap.containsKey(attrTO.getSchema())) {
                attributableTO.addVirtualAttribute(evaluateAttrTemplate(attributableTO, attrTO));
            }
        }
    }

    private AttributeTO evaluateAttrTemplate(final AbstractAttributableTO attributableTO, final AttributeTO template) {
        AttributeTO result = new AttributeTO();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            for (String value : template.getValues()) {
                String evaluated = jexlUtil.evaluate(value, attributableTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    result.addValue(evaluated);
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
