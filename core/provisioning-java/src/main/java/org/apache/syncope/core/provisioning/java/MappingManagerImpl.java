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
package org.apache.syncope.core.provisioning.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.GroupableRelatable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.utils.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.identityconnectors.framework.common.objects.Name;

@Component
public class MappingManagerImpl implements MappingManager {

    private static final Logger LOG = LoggerFactory.getLogger(MappingManager.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private DerAttrHandler derAttrHandler;

    @Autowired
    private VirAttrHandler virAttrHandler;

    @Autowired
    private VirAttrCache virAttrCache;

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Transactional(readOnly = true)
    @Override
    public Pair<String, Set<Attribute>> prepareAttrs(
            final Any<?> any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final Provision provision) {

        LOG.debug("Preparing resource attributes for {} with provision {} for attributes {}",
                any, provision, any.getPlainAttrs());

        Set<Attribute> attributes = new HashSet<>();
        String connObjectKey = null;

        for (Item mapItem : MappingUtils.getPropagationItems(provision.getMapping().getItems())) {
            LOG.debug("Processing expression '{}'", mapItem.getIntAttrName());

            try {
                Pair<String, Attribute> preparedAttr = prepareAttr(provision, mapItem, any, password);
                if (preparedAttr != null) {
                    if (preparedAttr.getLeft() != null) {
                        connObjectKey = preparedAttr.getLeft();
                    }

                    if (preparedAttr.getRight() != null) {
                        Attribute alreadyAdded = AttributeUtil.find(preparedAttr.getRight().getName(), attributes);

                        if (alreadyAdded == null) {
                            attributes.add(preparedAttr.getRight());
                        } else {
                            attributes.remove(alreadyAdded);

                            Set<Object> values = new HashSet<>();
                            if (alreadyAdded.getValue() != null && !alreadyAdded.getValue().isEmpty()) {
                                values.addAll(alreadyAdded.getValue());
                            }

                            values.addAll(preparedAttr.getRight().getValue());

                            attributes.add(AttributeBuilder.build(preparedAttr.getRight().getName(), values));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Expression '{}' processing failed", mapItem.getIntAttrName(), e);
            }
        }

        Attribute connObjectKeyExtAttr =
                AttributeUtil.find(MappingUtils.getConnObjectKeyItem(provision).get().getExtAttrName(), attributes);
        if (connObjectKeyExtAttr != null) {
            attributes.remove(connObjectKeyExtAttr);
            attributes.add(AttributeBuilder.build(
                    MappingUtils.getConnObjectKeyItem(provision).get().getExtAttrName(), connObjectKey));
        }
        Name name = MappingUtils.evaluateNAME(any, provision, connObjectKey);
        attributes.add(name);
        if (connObjectKey != null && !connObjectKey.equals(name.getNameValue()) && connObjectKeyExtAttr == null) {
            attributes.add(AttributeBuilder.build(
                    MappingUtils.getConnObjectKeyItem(provision).get().getExtAttrName(), connObjectKey));
        }

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }
        if (!changePwd) {
            Attribute pwdAttr = AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes);
            if (pwdAttr != null) {
                attributes.remove(pwdAttr);
            }
        }

        return Pair.of(connObjectKey, attributes);
    }

    private String getIntValue(final Realm realm, final Item orgUnitItem) {
        String value = null;
        switch (orgUnitItem.getIntAttrName()) {
            case "key":
                value = realm.getKey();
                break;

            case "name":
                value = realm.getName();
                break;

            case "fullpath":
                value = realm.getFullPath();
                break;

            default:
        }

        return value;
    }

    @Override
    public Pair<String, Set<Attribute>> prepareAttrs(final Realm realm, final OrgUnit orgUnit) {
        LOG.debug("Preparing resource attributes for {} with orgUnit {}", realm, orgUnit);

        Set<Attribute> attributes = new HashSet<>();
        String connObjectKey = null;

        for (Item orgUnitItem : MappingUtils.getPropagationItems(orgUnit.getItems())) {
            LOG.debug("Processing expression '{}'", orgUnitItem.getIntAttrName());

            String value = getIntValue(realm, orgUnitItem);

            if (orgUnitItem.isConnObjectKey()) {
                connObjectKey = value;
            }

            Attribute alreadyAdded = AttributeUtil.find(orgUnitItem.getExtAttrName(), attributes);
            if (alreadyAdded == null) {
                if (value == null) {
                    attributes.add(AttributeBuilder.build(orgUnitItem.getExtAttrName()));
                } else {
                    attributes.add(AttributeBuilder.build(orgUnitItem.getExtAttrName(), value));
                }
            } else if (value != null) {
                attributes.remove(alreadyAdded);

                Set<Object> values = new HashSet<>();
                if (alreadyAdded.getValue() != null && !alreadyAdded.getValue().isEmpty()) {
                    values.addAll(alreadyAdded.getValue());
                }
                values.add(value);

                attributes.add(AttributeBuilder.build(orgUnitItem.getExtAttrName(), values));
            }
        }

        Attribute connObjectKeyExtAttr =
                AttributeUtil.find(orgUnit.getConnObjectKeyItem().get().getExtAttrName(), attributes);
        if (connObjectKeyExtAttr != null) {
            attributes.remove(connObjectKeyExtAttr);
            attributes.add(
                    AttributeBuilder.build(orgUnit.getConnObjectKeyItem().get().getExtAttrName(), connObjectKey));
        }
        attributes.add(MappingUtils.evaluateNAME(realm, orgUnit, connObjectKey));

        return Pair.of(connObjectKey, attributes);
    }

    /**
     * Prepare an attribute to be sent to a connector instance.
     *
     * @param provision external resource
     * @param mapItem mapping item for the given attribute
     * @param any given any object
     * @param password clear-text password
     * @return connObjectKey + prepared attribute
     */
    private Pair<String, Attribute> prepareAttr(
            final Provision provision, final Item mapItem, final Any<?> any, final String password) {

        IntAttrName intAttrName =
                intAttrNameParser.parse(mapItem.getIntAttrName(), provision.getAnyType().getKind());

        boolean readOnlyVirSchema = false;
        Schema schema = null;
        AttrSchemaType schemaType = AttrSchemaType.String;
        if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    schema = plainSchemaDAO.find(intAttrName.getSchemaName());
                    if (schema != null) {
                        schemaType = schema.getType();
                    }
                    break;

                case VIRTUAL:
                    schema = virSchemaDAO.find(intAttrName.getSchemaName());
                    readOnlyVirSchema = (schema != null && schema.isReadonly());
                    break;

                default:
            }
        }

        List<PlainAttrValue> values = getIntValues(provision, mapItem, intAttrName, any);

        LOG.debug("Define mapping for: "
                + "\n* ExtAttrName " + mapItem.getExtAttrName()
                + "\n* is connObjectKey " + mapItem.isConnObjectKey()
                + "\n* is password " + mapItem.isPassword()
                + "\n* mandatory condition " + mapItem.getMandatoryCondition()
                + "\n* Schema " + intAttrName.getSchemaName()
                + "\n* ClassType " + schemaType.getType().getName()
                + "\n* Values " + values);

        Pair<String, Attribute> result;
        if (readOnlyVirSchema) {
            result = null;
        } else {
            List<Object> objValues = new ArrayList<>();

            for (PlainAttrValue value : values) {
                if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                    objValues.add(value.getValue());
                } else {
                    objValues.add(value.getValueAsString(schemaType));
                }
            }

            if (mapItem.isConnObjectKey()) {
                result = Pair.of(objValues.isEmpty() ? null : objValues.iterator().next().toString(), null);
            } else if (mapItem.isPassword() && any instanceof User) {
                String passwordAttrValue = password;
                if (StringUtils.isBlank(passwordAttrValue)) {
                    User user = (User) any;
                    if (user.canDecodePassword()) {
                        try {
                            passwordAttrValue = ENCRYPTOR.decode(user.getPassword(), user.getCipherAlgorithm());
                        } catch (Exception e) {
                            LOG.error("Could not decode password for {}", user, e);
                        }
                    } else if (provision.getResource().isRandomPwdIfNotProvided()) {
                        try {
                            passwordAttrValue = passwordGenerator.generate(provision.getResource());
                        } catch (InvalidPasswordRuleConf e) {
                            LOG.error("Could not generate policy-compliant random password for {}", user, e);
                        }
                    }
                }

                if (passwordAttrValue == null) {
                    result = null;
                } else {
                    result = Pair.of(null, AttributeBuilder.buildPassword(passwordAttrValue.toCharArray()));
                }
            } else if (schema != null && schema.isMultivalue()) {
                result = Pair.of(null, AttributeBuilder.build(mapItem.getExtAttrName(), objValues));
            } else {
                result = Pair.of(null, objValues.isEmpty()
                        ? AttributeBuilder.build(mapItem.getExtAttrName())
                        : AttributeBuilder.build(mapItem.getExtAttrName(), objValues.iterator().next()));
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<PlainAttrValue> getIntValues(
            final Provision provision,
            final Item mapItem,
            final IntAttrName intAttrName,
            final Any<?> any) {

        LOG.debug("Get internal values for {} as '{}' on {}", any, mapItem.getIntAttrName(), provision.getResource());

        Any<?> reference = null;
        Membership<?> membership = null;
        if (intAttrName.getEnclosingGroup() == null && intAttrName.getRelatedAnyObject() == null) {
            reference = any;
        }
        if (any instanceof GroupableRelatable) {
            GroupableRelatable<?, ?, ?, ?, ?> groupableRelatable = (GroupableRelatable<?, ?, ?, ?, ?>) any;

            if (intAttrName.getEnclosingGroup() != null) {
                Group group = groupDAO.findByName(intAttrName.getEnclosingGroup());
                if (group == null || groupableRelatable.getMembership(group.getKey()) == null) {
                    LOG.warn("No membership for {} in {}, ignoring",
                            intAttrName.getEnclosingGroup(), groupableRelatable);
                } else {
                    reference = group;
                }
            } else if (intAttrName.getRelatedAnyObject() != null) {
                AnyObject anyObject = anyObjectDAO.findByName(intAttrName.getRelatedAnyObject());
                if (anyObject == null || groupableRelatable.getRelationships(anyObject.getKey()).isEmpty()) {
                    LOG.warn("No relationship for {} in {}, ignoring",
                            intAttrName.getRelatedAnyObject(), groupableRelatable);
                } else {
                    reference = anyObject;
                }
            } else if (intAttrName.getMembershipOfGroup() != null) {
                Group group = groupDAO.findByName(intAttrName.getMembershipOfGroup());
                membership = groupableRelatable.getMembership(group.getKey()).orElse(null);
            }
        }
        if (reference == null) {
            LOG.warn("Could not determine the reference instance for {}", mapItem.getIntAttrName());
            return Collections.emptyList();
        }

        List<PlainAttrValue> values = new ArrayList<>();
        boolean transform = true;

        AnyUtils anyUtils = anyUtilsFactory.getInstance(reference);
        if (intAttrName.getField() != null) {
            PlainAttrValue attrValue = anyUtils.newPlainAttrValue();

            switch (intAttrName.getField()) {
                case "key":
                    attrValue.setStringValue(reference.getKey());
                    values.add(attrValue);
                    break;

                case "password":
                    // ignore
                    break;

                case "username":
                    if (reference instanceof User) {
                        attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(((User) reference).getUsername());
                        values.add(attrValue);
                    }
                    break;

                case "name":
                    if (reference instanceof Group) {
                        attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(((Group) reference).getName());
                        values.add(attrValue);
                    } else if (reference instanceof AnyObject) {
                        attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(((AnyObject) reference).getName());
                        values.add(attrValue);
                    }
                    break;

                case "userOwner":
                case "groupOwner":
                    Mapping uMapping = provision.getAnyType().equals(anyTypeDAO.findUser())
                            ? provision.getMapping()
                            : null;
                    Mapping gMapping = provision.getAnyType().equals(anyTypeDAO.findGroup())
                            ? provision.getMapping()
                            : null;

                    if (reference instanceof Group) {
                        Group group = (Group) reference;
                        String groupOwnerValue = null;
                        if (group.getUserOwner() != null && uMapping != null) {
                            groupOwnerValue = getGroupOwnerValue(provision, group.getUserOwner());
                        }
                        if (group.getGroupOwner() != null && gMapping != null) {
                            groupOwnerValue = getGroupOwnerValue(provision, group.getGroupOwner());
                        }

                        if (StringUtils.isNotBlank(groupOwnerValue)) {
                            attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                            attrValue.setStringValue(groupOwnerValue);
                            values.add(attrValue);
                        }
                    }
                    break;

                default:
                    try {
                        attrValue.setStringValue(FieldUtils.readField(
                                reference, intAttrName.getField(), true).toString());
                        values.add(attrValue);
                    } catch (IllegalAccessException e) {
                        LOG.error("Could not read value of '{}' from {}", intAttrName.getField(), reference, e);
                    }
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    PlainAttr<?> attr;
                    if (membership == null) {
                        attr = reference.getPlainAttr(intAttrName.getSchemaName()).orElse(null);
                    } else {
                        attr = ((GroupableRelatable<?, ?, ?, ?, ?>) reference).getPlainAttr(
                                intAttrName.getSchemaName(), membership).orElse(null);
                    }
                    if (attr != null) {
                        if (attr.getUniqueValue() != null) {
                            values.add(anyUtils.clonePlainAttrValue(attr.getUniqueValue()));
                        } else if (attr.getValues() != null) {
                            attr.getValues().forEach(value -> values.add(anyUtils.clonePlainAttrValue(value)));
                        }
                    }
                    break;

                case DERIVED:
                    DerSchema derSchema = derSchemaDAO.find(intAttrName.getSchemaName());
                    if (derSchema != null) {
                        String value = membership == null
                                ? derAttrHandler.getValue(reference, derSchema)
                                : derAttrHandler.getValue(reference, membership, derSchema);
                        if (value != null) {
                            PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
                            attrValue.setStringValue(value);
                            values.add(attrValue);
                        }
                    }
                    break;

                case VIRTUAL:
                    // virtual attributes don't get transformed
                    transform = false;

                    VirSchema virSchema = virSchemaDAO.find(intAttrName.getSchemaName());
                    if (virSchema != null) {
                        LOG.debug("Expire entry cache {}-{}", reference, intAttrName.getSchemaName());
                        virAttrCache.expire(
                                reference.getType().getKey(), reference.getKey(), intAttrName.getSchemaName());

                        List<String> virValues = membership == null
                                ? virAttrHandler.getValues(reference, virSchema)
                                : virAttrHandler.getValues(reference, membership, virSchema);
                        virValues.stream().
                                map(value -> {
                                    PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
                                    attrValue.setStringValue(value);
                                    return attrValue;
                                }).
                                forEachOrdered(attrValue -> values.add(attrValue));
                    }
                    break;

                default:
            }
        }

        LOG.debug("Internal values: {}", values);

        List<PlainAttrValue> transformed = values;
        if (transform) {
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(mapItem)) {
                transformed = transformer.beforePropagation(mapItem, any, transformed);
            }
            LOG.debug("Transformed values: {}", values);
        } else {
            LOG.debug("No transformation occurred");
        }

        return transformed;
    }

    private String getGroupOwnerValue(final Provision provision, final Any<?> any) {
        Pair<String, Attribute> preparedAttr =
                prepareAttr(provision, MappingUtils.getConnObjectKeyItem(provision).get(), any, null);
        String connObjectKey = preparedAttr.getKey();

        return MappingUtils.evaluateNAME(any, provision, connObjectKey).getNameValue();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> getConnObjectKeyValue(final Any<?> any, final Provision provision) {
        MappingItem mapItem = provision.getMapping().getConnObjectKeyItem().get();
        List<PlainAttrValue> values = getIntValues(
                provision,
                mapItem,
                intAttrNameParser.parse(mapItem.getIntAttrName(), provision.getAnyType().getKind()),
                any);
        return Optional.ofNullable(values.isEmpty()
                ? null
                : values.get(0).getValueAsString());
    }

    @Transactional(readOnly = true)
    @Override
    public String getConnObjectKeyValue(final Realm realm, final OrgUnit orgUnit) {
        OrgUnitItem orgUnitItem = orgUnit.getConnObjectKeyItem().get();

        return getIntValue(realm, orgUnitItem);
    }

    @Transactional(readOnly = true)
    @Override
    public void setIntValues(final Item mapItem, final Attribute attr, final AnyTO anyTO, final AnyUtils anyUtils) {
        List<Object> values = null;
        if (attr != null) {
            values = attr.getValue();
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(mapItem)) {
                values = transformer.beforePull(mapItem, anyTO, values);
            }
        }
        values = values == null ? Collections.emptyList() : values;

        IntAttrName intAttrName =
                intAttrNameParser.parse(mapItem.getIntAttrName(), anyUtils.getAnyTypeKind());

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "password":
                    if (anyTO instanceof UserTO && !values.isEmpty()) {
                        ((UserTO) anyTO).setPassword(ConnObjectUtils.getPassword(values.get(0)));
                    }
                    break;

                case "username":
                    if (anyTO instanceof UserTO) {
                        ((UserTO) anyTO).setUsername(values.isEmpty() || values.get(0) == null
                                ? null
                                : values.get(0).toString());
                    }
                    break;

                case "name":
                    if (anyTO instanceof GroupTO) {
                        ((GroupTO) anyTO).setName(values.isEmpty() || values.get(0) == null
                                ? null
                                : values.get(0).toString());
                    } else if (anyTO instanceof AnyObjectTO) {
                        ((AnyObjectTO) anyTO).setName(values.isEmpty() || values.get(0) == null
                                ? null
                                : values.get(0).toString());
                    }
                    break;

                case "userOwner":
                case "groupOwner":
                    if (anyTO instanceof GroupTO && attr != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // GroupOwnerSchema value
                        AttrTO attrTO = new AttrTO();
                        attrTO.setSchema(StringUtils.EMPTY);
                        if (values.isEmpty() || values.get(0) == null) {
                            attrTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attrTO.getValues().add(values.get(0).toString());
                        }

                        ((GroupTO) anyTO).getPlainAttrs().add(attrTO);
                    }
                    break;

                default:
            }
        } else if (intAttrName.getSchemaType() != null) {
            GroupableRelatableTO groupableTO = null;
            Group group = null;
            if (anyTO instanceof GroupableRelatableTO && intAttrName.getMembershipOfGroup() != null) {
                groupableTO = (GroupableRelatableTO) anyTO;
                group = groupDAO.findByName(intAttrName.getMembershipOfGroup());
            }

            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    AttrTO attrTO = new AttrTO();
                    attrTO.setSchema(intAttrName.getSchemaName());

                    PlainSchema schema = plainSchemaDAO.find(intAttrName.getSchemaName());

                    for (Object value : values) {
                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
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

                    if (groupableTO == null || group == null) {
                        anyTO.getPlainAttrs().add(attrTO);
                    } else {
                        Optional<MembershipTO> membership = groupableTO.getMembership(group.getKey());
                        if (!membership.isPresent()) {
                            membership = Optional.of(
                                    new MembershipTO.Builder().group(group.getKey(), group.getName()).build());
                            groupableTO.getMemberships().add(membership.get());
                        }
                        membership.get().getPlainAttrs().add(attrTO);
                    }
                    break;

                case DERIVED:
                    attrTO = new AttrTO();
                    attrTO.setSchema(intAttrName.getSchemaName());
                    if (groupableTO == null || group == null) {
                        anyTO.getDerAttrs().add(attrTO);
                    } else {
                        Optional<MembershipTO> membership = groupableTO.getMembership(group.getKey());
                        if (!membership.isPresent()) {
                            membership = Optional.of(
                                    new MembershipTO.Builder().group(group.getKey(), group.getName()).build());
                            groupableTO.getMemberships().add(membership.get());
                        }
                        membership.get().getDerAttrs().add(attrTO);
                    }
                    break;

                case VIRTUAL:
                    attrTO = new AttrTO();
                    attrTO.setSchema(intAttrName.getSchemaName());

                    // virtual attributes don't get transformed, iterate over original attr.getValue()
                    if (attr != null && attr.getValue() != null && !attr.getValue().isEmpty()) {
                        attr.getValue().stream().
                                filter(value -> value != null).
                                forEachOrdered(value -> attrTO.getValues().add(value.toString()));
                    }

                    if (groupableTO == null || group == null) {
                        anyTO.getVirAttrs().add(attrTO);
                    } else {
                        Optional<MembershipTO> membership = groupableTO.getMembership(group.getKey());
                        if (!membership.isPresent()) {
                            membership = Optional.of(
                                    new MembershipTO.Builder().group(group.getKey(), group.getName()).build());
                            groupableTO.getMemberships().add(membership.get());
                        }
                        membership.get().getVirAttrs().add(attrTO);
                    }
                    break;

                default:
            }
        }
    }

    @Override
    public void setIntValues(final Item orgUnitItem, final Attribute attr, final RealmTO realmTO) {
        List<Object> values = null;
        if (attr != null) {
            values = attr.getValue();
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(orgUnitItem)) {
                values = transformer.beforePull(orgUnitItem, realmTO, values);
            }
        }

        if (values != null && !values.isEmpty() && values.get(0) != null) {
            switch (orgUnitItem.getIntAttrName()) {
                case "name":
                    realmTO.setName(values.get(0).toString());
                    break;

                case "fullpath":
                    String parentFullPath = StringUtils.substringBeforeLast(values.get(0).toString(), "/");
                    Realm parent = realmDAO.findByFullPath(parentFullPath);
                    if (parent == null) {
                        LOG.warn("Could not find Realm with path {}, ignoring", parentFullPath);
                    } else {
                        realmTO.setParent(parent.getFullPath());
                    }
                    break;

                default:
            }
        }
    }

}
