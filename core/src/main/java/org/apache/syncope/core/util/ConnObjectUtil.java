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
package org.apache.syncope.core.util;

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
import org.apache.syncope.NotFoundException;
import org.apache.syncope.controller.UnauthorizedRoleException;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.ConnectorFacadeProxy;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.to.AbstractAttributableTO;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.to.ConnObjectTO;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.PasswordPolicySpec;
import org.apache.syncope.util.AttributableOperations;
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
    protected static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtil.class);

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
     * Build an UserTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public UserTO getUserTO(final ConnectorObject obj, final SyncTask syncTask) {
        UserTO userTO = getUserTOFromConnObject(obj, syncTask);

        // if password was not set above, generate
        if (StringUtils.isBlank(userTO.getPassword())) {
            List<PasswordPolicySpec> ppSpecs = new ArrayList<PasswordPolicySpec>();
            ppSpecs.add((PasswordPolicySpec) policyDAO.getGlobalPasswordPolicy().getSpecification());

            for (MembershipTO memb : userTO.getMemberships()) {
                SyncopeRole role = roleDAO.find(memb.getRoleId());
                if (role != null && role.getPasswordPolicy() != null
                        && role.getPasswordPolicy().getSpecification() != null) {

                    ppSpecs.add((PasswordPolicySpec) role.getPasswordPolicy().getSpecification());
                }
            }
            for (String resName : userTO.getResources()) {
                ExternalResource resource = resourceDAO.find(resName);
                if (resource != null && resource.getPasswordPolicy() != null
                        && resource.getPasswordPolicy().getSpecification() != null) {

                    ppSpecs.add((PasswordPolicySpec) resource.getPasswordPolicy().getSpecification());
                }
            }

            String password;
            try {
                password = pwdGen.generatePasswordFromPwdSpec(ppSpecs);
            } catch (IncompatiblePolicyException e) {
                LOG.error("Could not generate policy-compliant random password for {}", userTO, e);

                password = RandomStringUtils.randomAlphanumeric(16);
            }
            userTO.setPassword(password);
        }

        return userTO;
    }

    /**
     * Build an UserMod out of connector object attributes and schema mapping.
     *
     * @param userId user to be updated
     * @param obj connector object
     * @param syncTask synchronization task
     * @return UserMod for the user to be updated
     */
    @Transactional(readOnly = true)
    public UserMod getUserMod(final Long userId, final ConnectorObject obj, final SyncTask syncTask)
            throws NotFoundException, UnauthorizedRoleException {

        final SyncopeUser user = userDataBinder.getUserFromId(userId);
        final UserTO original = userDataBinder.getUserTO(user);

        final UserTO updated = getUserTOFromConnObject(obj, syncTask);
        updated.setId(userId);

        // update password if and only if password is really changed
        if (StringUtils.isBlank(updated.getPassword()) || userDataBinder.verifyPassword(user, updated.getPassword())) {
            updated.setPassword(null);
        }

        final UserMod userMod = AttributableOperations.diff(updated, original, true);

        return userMod;
    }

    private UserTO getUserTOFromConnObject(final ConnectorObject obj, final SyncTask syncTask) {
        final UserTO userTO = new UserTO();

        // 1. fill with data from connector object
        for (SchemaMapping mapping : syncTask.getResource().getMappings()) {
            Attribute attribute = obj.getAttributeByName(SchemaMappingUtil.getExtAttrName(mapping));

            AttributeTO attributeTO;
            switch (mapping.getIntMappingType()) {
                case SyncopeUserId:
                    break;

                case Password:
                    if (attribute != null && attribute.getValue() != null && !attribute.getValue().isEmpty()) {
                        userTO.setPassword(getPassword(attribute.getValue().get(0)));
                    }
                    break;

                case Username:
                    userTO.setUsername(attribute == null || attribute.getValue().isEmpty()
                            ? null
                            : attribute.getValue().get(0).toString());
                    break;

                case UserSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getIntAttrName());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {
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

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {
                        attributeTO.addValue(value.toString());
                    }

                    userTO.addVirtualAttribute(attributeTO);
                    break;

                default:
            }
        }

        // 2. add data from defined template (if any)
        UserTO template = syncTask.getUserTemplate();
        if (template != null) {
            if (StringUtils.isNotBlank(template.getUsername())) {
                String evaluated = jexlUtil.evaluate(template.getUsername(), userTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    userTO.setUsername(evaluated);
                }
            }

            if (StringUtils.isNotBlank(template.getPassword())) {
                String evaluated = jexlUtil.evaluate(template.getPassword(), userTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    userTO.setPassword(evaluated);
                }
            }

            fillFromTemplate(userTO, template);

            for (String resource : template.getResources()) {
                userTO.addResource(resource);
            }

            Map<Long, MembershipTO> currentMembs = userTO.getMembershipMap();
            for (MembershipTO membTO : template.getMemberships()) {
                MembershipTO membTBU;
                if (currentMembs.containsKey(membTO.getRoleId())) {
                    membTBU = currentMembs.get(membTO.getRoleId());
                } else {
                    membTBU = new MembershipTO();
                    membTBU.setRoleId(membTO.getRoleId());
                    userTO.addMembership(membTBU);
                }
                fillFromTemplate(membTBU, membTO);
            }
        }

        return userTO;
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
                        attrTO.addValue(value.toString());
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
     */
    public void retrieveVirAttrValues(final AbstractAttributable owner) {
        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final ConnInstanceLoader connInstanceLoader = context.getBean(ConnInstanceLoader.class);

        final Map<SchemaMappingWrapper, ConnectorObject> remoteObjects =
                new HashMap<SchemaMappingWrapper, ConnectorObject>();

        for (ExternalResource resource : owner.getResources()) {
            LOG.debug("Retrieve remote object from '{}'", resource.getName());
            try {
                final SchemaMappingWrapper mappings = new SchemaMappingWrapper(resource.getMappings());

                final String accountId = SchemaMappingUtil.getAccountIdValue(owner, mappings.getAccountIdMapping());

                LOG.debug("Search for object with accountId '{}'", accountId);

                if (StringUtils.isNotBlank(accountId)) {
                    // Retrieve attributes to get
                    final Set<String> extAttrNames = new HashSet<String>();

                    for (Set<SchemaMapping> virAttrMappings : mappings.getuVirMappings().values()) {
                        for (SchemaMapping virAttrMapping : virAttrMappings) {
                            extAttrNames.add(SchemaMappingUtil.getExtAttrName(virAttrMapping));
                        }
                    }

                    // Search for remote object
                    if (extAttrNames != null) {
                        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
                        oob.setAttributesToGet(extAttrNames);

                        final ConnectorFacadeProxy connector = connInstanceLoader.getConnector(resource);
                        final ConnectorObject connectorObject =
                                connector.getObject(ObjectClass.ACCOUNT, new Uid(accountId), oob.build());

                        if (connectorObject != null) {
                            remoteObjects.put(mappings, connectorObject);
                        }

                        LOG.debug("Retrieved remotye object {}", connectorObject);
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to retrieve virtual attribute values on '{}'", resource.getName(), e);
            }
        }

        for (AbstractVirAttr virAttr : owner.getVirtualAttributes()) {
            LOG.debug("Provide value for virtual attribute '{}'", virAttr.getVirtualSchema().getName());

            for (Map.Entry<SchemaMappingWrapper, ConnectorObject> entry : remoteObjects.entrySet()) {
                final Set<SchemaMapping> virAttrMappings = entry.getKey().getuVirMappings().
                        get(virAttr.getVirtualSchema().getName());

                if (virAttrMappings != null) {
                    for (SchemaMapping virAttrMapping : virAttrMappings) {
                        final String extAttrName = SchemaMappingUtil.getExtAttrName(virAttrMapping);
                        final Attribute extAttr = entry.getValue().getAttributeByName(extAttrName);
                        if (extAttr != null && extAttr.getValue() != null && !extAttr.getValue().isEmpty()) {
                            for (Object obj : extAttr.getValue()) {
                                if (obj != null) {
                                    virAttr.addValue(obj.toString());
                                }
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attribute evaluation ended");
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
