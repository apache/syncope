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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    public static <T extends MappingItem> Collection<T> getMatchingMappingItems(
            final Collection<T> items, final IntMappingType type) {

        return CollectionUtils.select(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return item.getIntMappingType() == type;
            }
        });
    }

    public static <T extends MappingItem> Collection<T> getMatchingMappingItems(
            final Collection<T> items, final String intAttrName, final IntMappingType type) {

        return CollectionUtils.select(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return item.getIntMappingType() == type && intAttrName.equals(item.getIntAttrName());
            }
        });
    }

    public static <T extends MappingItem> Collection<T> getMatchingMappingItems(
            final Collection<T> items, final String intAttrName) {

        return CollectionUtils.select(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return intAttrName.equals(item.getIntAttrName());
            }
        });
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param anyUtils any object
     * @param any given any object
     * @param password clear-text password
     * @param changePwd whether password should be included for propagation attributes or not
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable whether any object must be enabled or not
     * @param provision provision information
     * @return connObjectLink + prepared attributes
     */
    public static Pair<String, Set<Attribute>> prepareAttributes(
            final AnyUtils anyUtils, final Any<?, ?, ?> any,
            final String password,
            final boolean changePwd,
            final Set<String> vAttrsToBeRemoved,
            final Map<String, AttrMod> vAttrsToBeUpdated,
            final Boolean enable,
            final Provision provision) {

        LOG.debug("Preparing resource attributes for {} with provision {} for attributes {}",
                any, provision, any.getPlainAttrs());

        ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        VirAttrCache virAttrCache = context.getBean(VirAttrCache.class);
        PasswordGenerator passwordGenerator = context.getBean(PasswordGenerator.class);

        Set<Attribute> attributes = new HashSet<>();
        String connObjectKey = null;

        for (MappingItem mapping : anyUtils.getMappingItems(provision, MappingPurpose.PROPAGATION)) {
            LOG.debug("Processing schema {}", mapping.getIntAttrName());

            try {
                if (mapping.getIntMappingType() == IntMappingType.UserVirtualSchema
                        || mapping.getIntMappingType() == IntMappingType.GroupVirtualSchema
                        || mapping.getIntMappingType() == IntMappingType.AnyVirtualSchema) {

                    LOG.debug("Expire entry cache {}-{}", any.getKey(), mapping.getIntAttrName());
                    virAttrCache.expire(any.getType().getKey(), any.getKey(), mapping.getIntAttrName());
                }

                Pair<String, Attribute> preparedAttr = prepareAttr(
                        provision, mapping, any, password, passwordGenerator, vAttrsToBeRemoved, vAttrsToBeUpdated);

                if (preparedAttr != null && preparedAttr.getKey() != null) {
                    connObjectKey = preparedAttr.getKey();
                }

                if (preparedAttr != null && preparedAttr.getValue() != null) {
                    Attribute alreadyAdded = AttributeUtil.find(preparedAttr.getValue().getName(), attributes);

                    if (alreadyAdded == null) {
                        attributes.add(preparedAttr.getValue());
                    } else {
                        attributes.remove(alreadyAdded);

                        Set<Object> values = new HashSet<>(alreadyAdded.getValue());
                        values.addAll(preparedAttr.getValue().getValue());

                        attributes.add(AttributeBuilder.build(preparedAttr.getValue().getName(), values));
                    }
                }
            } catch (Exception e) {
                LOG.debug("Attribute '{}' processing failed", mapping.getIntAttrName(), e);
            }
        }

        Attribute connObjectKeyExtAttr =
                AttributeUtil.find(anyUtils.getConnObjectKeyItem(provision).getExtAttrName(), attributes);
        if (connObjectKeyExtAttr != null) {
            attributes.remove(connObjectKeyExtAttr);
            attributes.add(AttributeBuilder.build(
                    anyUtils.getConnObjectKeyItem(provision).getExtAttrName(), connObjectKey));
        }
        attributes.add(evaluateNAME(any, provision, connObjectKey));

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }
        if (!changePwd) {
            Attribute pwdAttr = AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes);
            if (pwdAttr != null) {
                attributes.remove(pwdAttr);
            }
        }

        return new ImmutablePair<>(connObjectKey, attributes);
    }

    /**
     * Prepare an attribute to be sent to a connector instance.
     *
     * @param provision external resource
     * @param mapItem mapping item for the given attribute
     * @param any any object
     * @param password clear-text password
     * @param passwordGenerator password generator
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return connObjectLink + prepared attribute
     */
    @SuppressWarnings("unchecked")
    private static Pair<String, Attribute> prepareAttr(
            final Provision provision, final MappingItem mapItem,
            final Any<?, ?, ?> any, final String password, final PasswordGenerator passwordGenerator,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated) {

        List<Any<?, ?, ?>> anys = new ArrayList<>();

        ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        AnyUtilsFactory anyUtilsFactory = context.getBean(AnyUtilsFactory.class);
        VirAttrHandler virAttrHandler = context.getBean(VirAttrHandler.class);

        switch (mapItem.getIntMappingType().getAnyTypeKind()) {
            case USER:
                if (any instanceof User) {
                    anys.add(any);
                }
                break;

            case GROUP:
                if (any instanceof User) {
                    UserDAO userDAO = context.getBean(UserDAO.class);
                    for (Group group : userDAO.findAllGroups((User) any)) {
                        virAttrHandler.retrieveVirAttrValues(group);
                        anys.add(group);
                    }
                } else if (any instanceof Group) {
                    anys.add(any);
                }
                break;

            case ANY_OBJECT:
                if (any instanceof AnyObject) {
                    anys.add(any);
                }
                break;

            default:
        }

        List<PlainAttrValue> values = getIntValues(
                provision, mapItem, anys, vAttrsToBeRemoved, vAttrsToBeUpdated);

        PlainSchema schema = null;
        boolean readOnlyVirSchema = false;
        AttrSchemaType schemaType;
        Pair<String, Attribute> result;

        switch (mapItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case AnyPlainSchema:
                final PlainSchemaDAO plainSchemaDAO = context.getBean(PlainSchemaDAO.class);
                schema = plainSchemaDAO.find(mapItem.getIntAttrName());
                schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                break;

            case UserVirtualSchema:
            case GroupVirtualSchema:
            case AnyVirtualSchema:
                VirSchemaDAO virSchemaDAO = context.getBean(VirSchemaDAO.class);
                VirSchema virSchema = virSchemaDAO.find(mapItem.getIntAttrName());
                readOnlyVirSchema = (virSchema != null && virSchema.isReadonly());
                schemaType = AttrSchemaType.String;
                break;

            default:
                schemaType = AttrSchemaType.String;
        }

        String extAttrName = mapItem.getExtAttrName();

        LOG.debug("Define mapping for: "
                + "\n* ExtAttrName " + extAttrName
                + "\n* is connObjectKey " + mapItem.isConnObjectKey()
                + "\n* is password " + (mapItem.isPassword() || mapItem.getIntMappingType() == IntMappingType.Password)
                + "\n* mandatory condition " + mapItem.getMandatoryCondition()
                + "\n* Schema " + mapItem.getIntAttrName()
                + "\n* IntMappingType " + mapItem.getIntMappingType().toString()
                + "\n* ClassType " + schemaType.getType().getName()
                + "\n* Values " + values);

        if (readOnlyVirSchema) {
            result = null;
        } else {
            List<Object> objValues = new ArrayList<>();

            for (PlainAttrValue value : values) {
                if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                    objValues.add(value.getValue());
                } else {
                    objValues.add(value.getValueAsString());
                }
            }

            if (mapItem.isConnObjectKey()) {
                result = new ImmutablePair<>(objValues.iterator().next().toString(), null);
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
                            passwordAttrValue = passwordGenerator.generate(user);
                        } catch (InvalidPasswordPolicySpecException e) {
                            LOG.error("Could not generate policy-compliant random password for {}", user, e);
                        }
                    }
                }

                if (passwordAttrValue == null) {
                    result = null;
                } else {
                    result = new ImmutablePair<>(
                            null,
                            AttributeBuilder.buildPassword(passwordAttrValue.toCharArray()));
                }
            } else {
                if ((schema != null && schema.isMultivalue())
                        || anyUtilsFactory.getInstance(any).getAnyTypeKind()
                        != mapItem.getIntMappingType().getAnyTypeKind()) {

                    result = new ImmutablePair<>(
                            null,
                            AttributeBuilder.build(extAttrName, objValues));
                } else {
                    result = new ImmutablePair<>(
                            null, objValues.isEmpty()
                                    ? AttributeBuilder.build(extAttrName)
                                    : AttributeBuilder.build(extAttrName, objValues.iterator().next()));
                }
            }
        }

        return result;
    }

    /**
     * Build __NAME__ for propagation. First look if there ia a defined connObjectLink for the given resource (and in
     * this case evaluate as JEXL); otherwise, take given connObjectKey.
     *
     * @param any given any object
     * @param provision external resource
     * @param connObjectKey connector object key
     * @return the value to be propagated as __NAME__
     */
    public static Name evaluateNAME(final Any<?, ?, ?> any,
            final Provision provision, final String connObjectKey) {

        final AnyUtilsFactory anyUtilsFactory =
                ApplicationContextProvider.getApplicationContext().getBean(AnyUtilsFactory.class);
        final AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        if (StringUtils.isBlank(connObjectKey)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing ConnObjectKey for '{}': ", provision.getResource());
        }

        // Evaluate connObjectKey expression
        String evalConnObjectLink = null;
        if (StringUtils.isNotBlank(anyUtils.getConnObjectLink(provision))) {
            final JexlContext jexlContext = new MapContext();
            JexlUtils.addFieldsToContext(any, jexlContext);
            JexlUtils.addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
            JexlUtils.addDerAttrsToContext(any.getDerAttrs(), any.getPlainAttrs(), jexlContext);
            evalConnObjectLink = JexlUtils.evaluate(anyUtils.getConnObjectLink(provision), jexlContext);
        }

        // If connObjectLink evaluates to an empty string, just use the provided connObjectKey as Name(),
        // otherwise evaluated connObjectLink expression is taken as Name().
        Name name;
        if (StringUtils.isBlank(evalConnObjectLink)) {
            // add connObjectKey as __NAME__ attribute ...
            LOG.debug("Add connObjectKey [{}] as __NAME__", connObjectKey);
            name = new Name(connObjectKey);
        } else {
            LOG.debug("Add connObjectLink [{}] as __NAME__", evalConnObjectLink);
            name = new Name(evalConnObjectLink);

            // connObjectKey not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("connObjectKey will be used just as __UID__ attribute");
        }

        return name;
    }

    private static String getGroupOwnerValue(final Provision provision, final Any<?, ?, ?> any) {
        AnyUtilsFactory anyUtilsFactory =
                ApplicationContextProvider.getApplicationContext().getBean(AnyUtilsFactory.class);

        Pair<String, Attribute> preparedAttr = prepareAttr(
                provision, anyUtilsFactory.getInstance(any).getConnObjectKeyItem(provision),
                any, null, null, Collections.<String>emptySet(), Collections.<String, AttrMod>emptyMap());
        String connObjectKey = preparedAttr.getKey();

        final Name groupOwnerName = evaluateNAME(any, provision, connObjectKey);
        return groupOwnerName.getNameValue();
    }

    /**
     * Get attribute values.
     *
     * @param provision provision information
     * @param mappingItem mapping item
     * @param anys any objects
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return attribute values.
     */
    public static List<PlainAttrValue> getIntValues(final Provision provision,
            final MappingItem mappingItem, final List<Any<?, ?, ?>> anys,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", anys, mappingItem.getIntMappingType());

        EntityFactory entityFactory =
                ApplicationContextProvider.getApplicationContext().getBean(EntityFactory.class);
        AnyUtilsFactory anyUtilsFactory =
                ApplicationContextProvider.getApplicationContext().getBean(AnyUtilsFactory.class);
        List<PlainAttrValue> values = new ArrayList<>();
        PlainAttrValue attrValue;
        switch (mappingItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case AnyPlainSchema:
                for (Any<?, ?, ?> any : anys) {
                    PlainAttr<?> attr = any.getPlainAttr(mappingItem.getIntAttrName());
                    if (attr != null) {
                        if (attr.getUniqueValue() != null) {
                            values.add(attr.getUniqueValue());
                        } else if (attr.getValues() != null) {
                            values.addAll(attr.getValues());
                        }
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            attr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }

                break;

            case UserVirtualSchema:
            case GroupVirtualSchema:
            case AnyVirtualSchema:
                for (Any<?, ?, ?> any : anys) {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
                    VirAttr<?> virAttr = any.getVirAttr(mappingItem.getIntAttrName());
                    if (virAttr != null) {
                        if (vAttrsToBeRemoved != null && vAttrsToBeUpdated != null) {
                            if (vAttrsToBeUpdated.containsKey(mappingItem.getIntAttrName())) {
                                virAttr.getValues().clear();
                                virAttr.getValues().addAll(
                                        vAttrsToBeUpdated.get(mappingItem.getIntAttrName()).getValuesToBeAdded());
                            } else if (vAttrsToBeRemoved.contains(mappingItem.getIntAttrName())) {
                                virAttr.getValues().clear();
                            } else {
                                throw new IllegalArgumentException("Don't need to update virtual attribute '"
                                        + mappingItem.getIntAttrName() + "'");
                            }
                        }
                        if (virAttr.getValues() != null) {
                            for (String value : virAttr.getValues()) {
                                attrValue = anyUtils.newPlainAttrValue();
                                attrValue.setStringValue(value);
                                values.add(attrValue);
                            }
                        }
                    }

                    LOG.debug("Retrieved {} virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            any.getClass().getSimpleName(),
                            virAttr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }
                break;

            case UserDerivedSchema:
            case GroupDerivedSchema:
            case AnyDerivedSchema:
                for (Any<?, ?, ?> any : anys) {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
                    DerAttr<?> derAttr = any.getDerAttr(mappingItem.getIntAttrName());
                    if (derAttr != null) {
                        attrValue = anyUtils.newPlainAttrValue();
                        attrValue.setStringValue(derAttr.getValue(any.getPlainAttrs()));
                        values.add(attrValue);
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            derAttr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }
                break;

            case UserId:
            case GroupId:
            case AnyId:
                for (Any<?, ?, ?> any : anys) {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
                    attrValue = anyUtils.newPlainAttrValue();
                    attrValue.setStringValue(any.getKey().toString());
                    values.add(attrValue);
                }
                break;

            case Username:
                for (Any<?, ?, ?> any : anys) {
                    if (any instanceof User) {
                        attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(((User) any).getUsername());
                        values.add(attrValue);
                    }
                }
                break;

            case GroupName:
                for (Any<?, ?, ?> any : anys) {
                    if (any instanceof Group) {
                        attrValue = entityFactory.newEntity(GPlainAttrValue.class);
                        attrValue.setStringValue(((Group) any).getName());
                        values.add(attrValue);
                    }
                }
                break;

            case GroupOwnerSchema:
                AnyTypeDAO anyTypeDAO = ApplicationContextProvider.getApplicationContext().getBean(AnyTypeDAO.class);
                Mapping uMapping = provision.getAnyType().equals(anyTypeDAO.findUser())
                        ? null
                        : provision.getMapping();
                Mapping gMapping = provision.getAnyType().equals(anyTypeDAO.findGroup())
                        ? null
                        : provision.getMapping();

                for (Any<?, ?, ?> any : anys) {
                    if (any instanceof Group) {
                        Group group = (Group) any;
                        String groupOwnerValue = null;
                        if (group.getUserOwner() != null && uMapping != null) {
                            groupOwnerValue = getGroupOwnerValue(provision, group.getUserOwner());
                        }
                        if (group.getGroupOwner() != null && gMapping != null) {
                            groupOwnerValue = getGroupOwnerValue(provision, group.getGroupOwner());
                        }

                        if (StringUtils.isNotBlank(groupOwnerValue)) {
                            attrValue = entityFactory.newEntity(GPlainAttrValue.class);
                            attrValue.setStringValue(groupOwnerValue);
                            values.add(attrValue);
                        }
                    }
                }
                break;

            default:
        }

        LOG.debug("Retrieved values '{}'", values);

        return values;
    }

    /**
     * Get connObjectKey internal value.
     *
     * @param any any object
     * @param provision provision information
     * @return connObjectKey internal value
     */
    public static String getConnObjectKeyValue(final Any<?, ?, ?> any, final Provision provision) {
        List<PlainAttrValue> values = getIntValues(provision, provision.getMapping().getConnObjectKeyItem(),
                Collections.<Any<?, ?, ?>>singletonList(any), null, null);
        return values == null || values.isEmpty()
                ? null
                : values.get(0).getValueAsString();
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private MappingUtils() {
    }
}
