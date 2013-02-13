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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.ConnObjectTO;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.util.AttributableOperations;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.ConnectorFacadeProxy;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.IntMappingType;
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

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

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

        // if password was not set above, generate a random string
        if (StringUtils.isBlank(userTO.getPassword())) {
            userTO.setPassword(RandomStringUtils.randomAlphanumeric(16));
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

        for (MembershipTO membTO : updated.getMemberships()) {
            Membership memb = user.getMembership(membTO.getRoleId());
            if (memb != null) {
                membTO.setId(memb.getId());
            }
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
                    userTO.setUsername(
                            attribute == null || attribute.getValue().isEmpty() || attribute.getValue().get(0) == null
                            ? null
                            : attribute.getValue().get(0).toString());
                    break;

                case UserSchema:
                    attributeTO = new AttributeTO();
                    attributeTO.setSchema(mapping.getIntAttrName());

                    for (Object value : attribute == null || attribute.getValue() == null
                            ? Collections.emptyList()
                            : attribute.getValue()) {
                        if (value != null) {
                            attributeTO.addValue(value.toString());
                        }
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
                        if (value != null) {
                            attributeTO.addValue(value.toString());
                        }
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

    public void retrieveVirAttrValues(final AbstractAttributable owner) {
        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final ConnInstanceLoader connInstanceLoader = context.getBean(ConnInstanceLoader.class);

        final IntMappingType type = owner instanceof SyncopeUser
                ? IntMappingType.UserVirtualSchema : IntMappingType.RoleVirtualSchema;

        final AttributableType ownerType = owner instanceof SyncopeUser ? AttributableType.USER : AttributableType.ROLE;

        final Map<String, ConnectorObject> externalResources = new HashMap<String, ConnectorObject>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (AbstractVirAttr virAttr : owner.getVirtualAttributes()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(owner, virAttr, ownerType, type, externalResources, connInstanceLoader);
            }
        }
        // -----------------------
    }

    private void retrieveVirAttrValue(
            final AbstractAttributable owner,
            final AbstractVirAttr virAttr,
            final AttributableType ownerType,
            final IntMappingType type,
            final Map<String, ConnectorObject> externalResources,
            final ConnInstanceLoader connInstanceLoader) {
        final String schemaName = virAttr.getVirtualSchema().getName();
        final List<String> values = virAttrCache.get(ownerType, owner.getId(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {}", schemaName);

        if (values == null) {
            // non cached ...
            LOG.debug("Need one or more remote connections");
            for (ExternalResource resource : getTargetResource(virAttr, type)) {
                LOG.debug("Seach values into {}", resource.getName());
                try {
                    final ConnectorObject connectorObject;

                    if (externalResources.containsKey(resource.getName())) {
                        connectorObject = externalResources.get(resource.getName());
                    } else {
                        LOG.debug("Perform connection to {}", resource.getName());
                        final String accountId = SchemaMappingUtil.getAccountIdValue(
                                owner, SchemaMappingUtil.getAccountIdMapping(resource.getMappings()));

                        if (StringUtils.isBlank(accountId)) {
                            throw new IllegalArgumentException("No AccountId found for " + resource.getName());
                        }

                        final Set<String> extAttrNames =
                                SchemaMappingUtil.getExtAttrNames(resource.getMappings(), type);

                        LOG.debug("External attribute ({}) names to get '{}'", type, extAttrNames);

                        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
                        oob.setAttributesToGet(extAttrNames);

                        final ConnectorFacadeProxy connector = connInstanceLoader.getConnector(resource);
                        connectorObject = connector.getObject(ObjectClass.ACCOUNT, new Uid(accountId), oob.build());
                        externalResources.put(resource.getName(), connectorObject);
                    }


                    final Set<SchemaMapping> mappings = resource.getMappings(schemaName, type);

                    // the same virtual attribute could be mapped with one or more external attribute 
                    for (SchemaMapping mapping : mappings) {
                        final Attribute attribute =
                                connectorObject.getAttributeByName(SchemaMappingUtil.getExtAttrName(mapping));

                        if (attribute != null && attribute.getValue() != null) {
                            for (Object obj : attribute.getValue()) {
                                if (obj != null) {
                                    virAttr.addValue(obj.toString());
                                }
                            }
                        }
                    }

                    LOG.debug("Retrieved values {}", virAttr.getValues());
                } catch (Exception e) {
                    LOG.error("Error reading connector object from {}", resource.getName(), e);
                }
            }

            virAttrCache.put(ownerType, owner.getId(), schemaName, virAttr.getValues());
        } else {
            // cached ...
            LOG.debug("Values found in cache {}", values);
            virAttr.setValues(values);
        }
    }

    private Set<ExternalResource> getTargetResource(final AbstractVirAttr attr, final IntMappingType type) {

        final Set<ExternalResource> resources = new HashSet<ExternalResource>();

        for (ExternalResource res : attr.getOwner().getResources()) {
            if (!SchemaMappingUtil.getMappings(res.getMappings(), attr.getVirtualSchema().getName(), type).isEmpty()) {
                resources.add(res);
            }
        }

        return resources;
    }

    private void fillFromTemplate(final AbstractAttributableTO attributableTO, final AbstractAttributableTO template) {

        Map<String, AttributeTO> currentAttrMap = attributableTO.getAttributeMap();
        for (AttributeTO attrTO : template.getAttributes()) {

            if (!currentAttrMap.containsKey(attrTO.getSchema())
                    && attrTO.getValues() != null
                    && !attrTO.getValues().isEmpty()) {
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
            if (!currentAttrMap.containsKey(attrTO.getSchema())
                    && attrTO.getValues() != null
                    && !attrTO.getValues().isEmpty()) {
                attributableTO.addVirtualAttribute(evaluateAttrTemplate(attributableTO, attrTO));
            }
        }
    }

    private AttributeTO evaluateAttrTemplate(final AbstractAttributableTO attributableTO, final AttributeTO template) {

        AttributeTO result = new AttributeTO();
        result.setSchema(template.getSchema());

        for (String value : template.getValues()) {
            String evaluated = jexlUtil.evaluate(value, attributableTO);
            if (StringUtils.isNotBlank(evaluated)) {
                result.addValue(evaluated);
            }
        }

        return result;
    }
}
