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
import org.apache.syncope.core.misc.security.SecureRandomUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplate;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnObjectUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtils.class);

    @Autowired
    private RealmDAO realmDAO;

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
     * Build a UserTO / GroupTO / AnyObjectTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AnyTO> T getAnyTO(
            final ConnectorObject obj, final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = getAnyTOFromConnObject(obj, syncTask, provision, anyUtils);

        // (for users) if password was not set above, generate
        if (anyTO instanceof UserTO && StringUtils.isBlank(((UserTO) anyTO).getPassword())) {
            final UserTO userTO = (UserTO) anyTO;

            List<PasswordPolicySpec> ppSpecs = new ArrayList<>();

            Realm realm = realmDAO.find(userTO.getRealm());
            if (realm != null) {
                for (Realm ancestor : realmDAO.findAncestors(realm)) {
                    if (ancestor.getPasswordPolicy() != null
                            && ancestor.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                        ppSpecs.add(ancestor.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
                    }
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

                password = SecureRandomUtils.generateRandomPassword(16);
            }
            userTO.setPassword(password);
        }

        return anyTO;
    }

    /**
     * Build an UserMod out of connector object attributes and schema mapping.
     *
     * @param key any object to be updated
     * @param obj connector object
     * @param original any object to get diff from
     * @param syncTask synchronization task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return modifications for the any object to be updated
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T extends AnyMod> T getAnyMod(final Long key, final ConnectorObject obj,
            final AnyTO original, final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        AnyTO updated = getAnyTOFromConnObject(obj, syncTask, provision, anyUtils);
        updated.setKey(key);

        if (AnyTypeKind.USER == anyUtils.getAnyTypeKind()) {
            // update password if and only if password is really changed
            User user = userDAO.authFind(key);
            if (StringUtils.isBlank(((UserTO) updated).getPassword())
                    || encryptor.verify(((UserTO) updated).getPassword(),
                            user.getCipherAlgorithm(), user.getPassword())) {

                ((UserTO) updated).setPassword(null);
            }
            return (T) AnyOperations.diff(((UserTO) updated), ((UserTO) original), true);
        } else if (AnyTypeKind.GROUP == anyUtils.getAnyTypeKind()) {
            return (T) AnyOperations.diff(((GroupTO) updated), ((GroupTO) original), true);
        } else if (AnyTypeKind.ANY_OBJECT == anyUtils.getAnyTypeKind()) {
            return (T) AnyOperations.diff(((AnyObjectTO) updated), ((AnyObjectTO) original), true);
        }

        return null;
    }

    private <T extends AnyTO> T getAnyTOFromConnObject(final ConnectorObject obj,
            final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = anyUtils.newAnyTO();

        // 1. fill with data from connector object
        anyTO.setRealm(syncTask.getDestinatioRealm().getFullPath());
        for (MappingItem item : anyUtils.getMappingItems(provision, MappingPurpose.SYNCHRONIZATION)) {
            Attribute attr = obj.getAttributeByName(item.getExtAttrName());

            AttrTO attrTO;
            switch (item.getIntMappingType()) {
                case UserId:
                case GroupId:
                    break;

                case Password:
                    if (anyTO instanceof UserTO && attr != null && attr.getValue() != null
                            && !attr.getValue().isEmpty()) {

                        ((UserTO) anyTO).setPassword(getPassword(attr.getValue().get(0)));
                    }
                    break;

                case Username:
                    if (anyTO instanceof UserTO) {
                        ((UserTO) anyTO).setUsername(attr == null || attr.getValue().isEmpty()
                                || attr.getValue().get(0) == null
                                        ? null
                                        : attr.getValue().get(0).toString());
                    }
                    break;

                case GroupName:
                    if (anyTO instanceof GroupTO) {
                        ((GroupTO) anyTO).setName(attr == null || attr.getValue().isEmpty()
                                || attr.getValue().get(0) == null
                                        ? null
                                        : attr.getValue().get(0).toString());
                    }
                    break;

                case GroupOwnerSchema:
                    if (anyTO instanceof GroupTO && attr != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // GroupOwnerSchema value
                        attrTO = new AttrTO();
                        attrTO.setSchema(StringUtils.EMPTY);
                        if (attr.getValue().isEmpty() || attr.getValue().get(0) == null) {
                            attrTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attrTO.getValues().add(attr.getValue().get(0).toString());
                        }

                        ((GroupTO) anyTO).getPlainAttrs().add(attrTO);
                    }
                    break;

                case UserPlainSchema:
                case GroupPlainSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());

                    PlainSchema schema = plainSchemaDAO.find(item.getIntAttrName());

                    for (Object value : attr == null || attr.getValue() == null
                            ? Collections.emptyList()
                            : attr.getValue()) {

                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            final PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
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
                            attrTO.getValues().add(attrValue.getValueAsString(schemaType));
                        }
                    }

                    anyTO.getPlainAttrs().add(attrTO);
                    break;

                case UserDerivedSchema:
                case GroupDerivedSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());
                    anyTO.getDerAttrs().add(attrTO);
                    break;

                case UserVirtualSchema:
                case GroupVirtualSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());

                    for (Object value : attr == null || attr.getValue() == null
                            ? Collections.emptyList()
                            : attr.getValue()) {

                        if (value != null) {
                            attrTO.getValues().add(value.toString());
                        }
                    }

                    anyTO.getVirAttrs().add(attrTO);
                    break;

                default:
            }
        }

        // 2. add data from defined template (if any)
        AnyTemplate anyTypeTemplate = syncTask.getTemplate(provision.getAnyType());
        if (anyTypeTemplate != null) {
            AnyTO template = anyTypeTemplate.get();
            fillFromTemplate(anyTO, template);

            if (template instanceof AnyObjectTO) {
                fillRelationshipsFromTemplate(((AnyObjectTO) anyTO).getRelationshipMap(),
                        ((AnyObjectTO) anyTO).getRelationships(), ((AnyObjectTO) template).getRelationships());
                fillMembershipsFromTemplate(((AnyObjectTO) anyTO).getMembershipMap(),
                        ((AnyObjectTO) anyTO).getMemberships(), ((AnyObjectTO) template).getMemberships());
            } else if (template instanceof UserTO) {
                if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                    String evaluated = JexlUtils.evaluate(((UserTO) template).getUsername(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) anyTO).setUsername(evaluated);
                    }
                }

                if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                    String evaluated = JexlUtils.evaluate(((UserTO) template).getPassword(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) anyTO).setPassword(evaluated);
                    }
                }

                fillRelationshipsFromTemplate(((UserTO) anyTO).getRelationshipMap(),
                        ((UserTO) anyTO).getRelationships(), ((UserTO) template).getRelationships());
                fillMembershipsFromTemplate(((UserTO) anyTO).getMembershipMap(),
                        ((UserTO) anyTO).getMemberships(), ((UserTO) template).getMemberships());
            } else if (template instanceof GroupTO) {
                if (StringUtils.isNotBlank(((GroupTO) template).getName())) {
                    String evaluated = JexlUtils.evaluate(((GroupTO) template).getName(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((GroupTO) anyTO).setName(evaluated);
                    }
                }

                if (((GroupTO) template).getUserOwner() != null) {
                    final User userOwner = userDAO.find(((GroupTO) template).getUserOwner());
                    if (userOwner != null) {
                        ((GroupTO) anyTO).setUserOwner(userOwner.getKey());
                    }
                }
                if (((GroupTO) template).getGroupOwner() != null) {
                    final Group groupOwner = groupDAO.find(((GroupTO) template).getGroupOwner());
                    if (groupOwner != null) {
                        ((GroupTO) anyTO).setGroupOwner(groupOwner.getKey());
                    }
                }
            }
        }

        return anyTO;
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

    private AttrTO evaluateAttrFromTemplate(final AnyTO anyTO, final AttrTO template) {
        AttrTO result = new AttrTO();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            for (String value : template.getValues()) {
                String evaluated = JexlUtils.evaluate(value, anyTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            }
        }

        return result;
    }

    private void fillFromTemplate(final AnyTO anyTO, final AnyTO template) {
        if (template.getRealm() != null) {
            anyTO.setRealm(template.getRealm());
        }

        Map<String, AttrTO> currentAttrMap = anyTO.getPlainAttrMap();
        for (AttrTO templatePlainAttr : template.getPlainAttrs()) {
            if (!templatePlainAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templatePlainAttr.getSchema())
                    || currentAttrMap.get(templatePlainAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getPlainAttrs().add(evaluateAttrFromTemplate(anyTO, templatePlainAttr));
            }
        }

        currentAttrMap = anyTO.getDerAttrMap();
        for (AttrTO templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                anyTO.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = anyTO.getVirAttrMap();
        for (AttrTO templateVirAttr : template.getVirAttrs()) {
            if (!templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getVirAttrs().add(evaluateAttrFromTemplate(anyTO, templateVirAttr));
            }
        }

        for (String resource : template.getResources()) {
            anyTO.getResources().add(resource);
        }

        anyTO.getAuxClasses().addAll(template.getAuxClasses());
    }

    private void fillRelationshipsFromTemplate(final Map<Long, RelationshipTO> anyRelMap,
            final List<RelationshipTO> anyRels, final List<RelationshipTO> templateRels) {

        for (RelationshipTO memb : templateRels) {
            if (!anyRelMap.containsKey(memb.getRightKey())) {
                anyRels.add(memb);
            }
        }
    }

    private void fillMembershipsFromTemplate(final Map<Long, MembershipTO> anyMembMap,
            final List<MembershipTO> anyMembs, final List<MembershipTO> templateMembs) {

        for (MembershipTO memb : templateMembs) {
            if (!anyMembMap.containsKey(memb.getRightKey())) {
                anyMembs.add(memb);
            }
        }
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
