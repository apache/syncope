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
package org.syncope.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
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
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.controller.UnauthorizedRoleException;
import org.syncope.core.rest.data.UserDataBinder;

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
     * Build an UserTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public UserTO getUserTO(final ConnectorObject obj, final SyncTask syncTask) {

        UserTO userTO = getUserTOFromConnObject(obj, syncTask);

        // 3. if password was not set above, generate a random string
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
     * @return UserMod for the user to be updated
     */
    @Transactional(readOnly = true)
    public UserMod getUserMod(final Long userId, final ConnectorObject obj, final SyncTask syncTask)
            throws NotFoundException, UnauthorizedRoleException {

        final SyncopeUser user = userDataBinder.getUserFromId(userId);
        final UserTO original = userDataBinder.getUserTO(user);

        final UserTO updated = getUserTOFromConnObject(obj, syncTask);
        updated.setId(userId);

        if (StringUtils.isNotBlank(updated.getPassword())) {
            // update password if and only if password has really changed
            if (userDataBinder.verifyPassword(user, updated.getPassword())) {
                updated.setPassword(null);
            }
        }

        return AttributableOperations.diff(updated, original);
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
                            ? Collections.EMPTY_LIST
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
                            ? Collections.EMPTY_LIST
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
            if (StringUtils.isBlank(userTO.getUsername()) && StringUtils.isNotBlank(template.getUsername())) {
                String evaluated = jexlUtil.evaluate(template.getUsername(), userTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    userTO.setUsername(template.getUsername());
                }
            }

            if (StringUtils.isBlank(userTO.getPassword()) && StringUtils.isNotBlank(template.getPassword())) {
                String evaluated = jexlUtil.evaluate(template.getPassword(), userTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    userTO.setPassword(template.getPassword());
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
        final ConfigurableApplicationContext context = ApplicationContextManager.getApplicationContext();
        final ConnInstanceLoader connInstanceLoader = context.getBean(ConnInstanceLoader.class);

        final Map<SchemaMappingUtil.SchemaMappingsWrapper, ConnectorObject> remoteObjects =
                new HashMap<SchemaMappingUtil.SchemaMappingsWrapper, ConnectorObject>();

        for (ExternalResource resource : owner.getResources()) {
            LOG.debug("Retrieve remote object from '{}'", resource.getName());
            try {
                final ConnectorFacadeProxy connector = connInstanceLoader.getConnector(resource);

                final SchemaMappingUtil.SchemaMappingsWrapper mappings = new SchemaMappingUtil.SchemaMappingsWrapper(
                        resource.getMappings());

                final String accountId = SchemaMappingUtil.getAccountIdValue(owner, mappings.getAccountIdMapping());

                LOG.debug("Search for object with accountId '{}'", accountId);

                if (StringUtils.isNotBlank(accountId)) {
                    // Retrieve attributes to get
                    final Set<String> extAttrNames = new HashSet<String>();

                    for (Collection<SchemaMapping> virAttrMappings : mappings.getuVirMappings().values()) {
                        for (SchemaMapping virAttrMapping : virAttrMappings) {
                            extAttrNames.add(SchemaMappingUtil.getExtAttrName(virAttrMapping));
                        }
                    }

                    // Search for remote object
                    if (extAttrNames != null) {
                        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
                        oob.setAttributesToGet(extAttrNames);

                        final ConnectorObject connectorObject = connector.getObject(ObjectClass.ACCOUNT, new Uid(
                                accountId), oob.build());

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

            for (SchemaMappingUtil.SchemaMappingsWrapper mappings : remoteObjects.keySet()) {
                Collection<SchemaMapping> virAttrMappings = mappings.getuVirMappings().get(
                        virAttr.getVirtualSchema().getName());

                if (virAttrMappings != null) {
                    for (SchemaMapping virAttrMapping : virAttrMappings) {
                        String extAttrName = SchemaMappingUtil.getExtAttrName(virAttrMapping);
                        Attribute extAttr = remoteObjects.get(mappings).getAttributeByName(extAttrName);

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
}
