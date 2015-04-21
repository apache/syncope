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
import org.apache.commons.collections4.Predicate;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.CollectionUtils2;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.jexl.JexlUtils;
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

        return CollectionUtils2.find(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return item.getIntMappingType() == type;
            }
        });
    }

    public static <T extends MappingItem> Collection<T> getMatchingMappingItems(
            final Collection<T> items, final String intAttrName, final IntMappingType type) {

        return CollectionUtils2.find(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return item.getIntMappingType() == type && intAttrName.equals(item.getIntAttrName());
            }
        });
    }

    public static <T extends MappingItem> Collection<T> getMatchingMappingItems(
            final Collection<T> items, final String intAttrName) {

        return CollectionUtils2.find(items, new Predicate<T>() {

            @Override
            public boolean evaluate(final T item) {
                return intAttrName.equals(item.getIntAttrName());
            }
        });
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param attrUtils user / group
     * @param subject given user / group
     * @param password clear-text password
     * @param changePwd whether password should be included for propagation attributes or not
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param membVAttrsToBeRemoved membership virtual attributes to be removed
     * @param membVAttrsToBeUpdated membership virtual attributes to be added
     * @param enable whether user must be enabled or not
     * @param resource target resource
     * @return account link + prepared attributes
     */
    public static Pair<String, Set<Attribute>> prepareAttributes(
            final AttributableUtils attrUtils, final Subject<?, ?, ?> subject,
            final String password,
            final boolean changePwd,
            final Set<String> vAttrsToBeRemoved,
            final Map<String, AttrMod> vAttrsToBeUpdated,
            final Set<String> membVAttrsToBeRemoved,
            final Map<String, AttrMod> membVAttrsToBeUpdated,
            final Boolean enable,
            final ExternalResource resource) {

        LOG.debug("Preparing resource attributes for {} on resource {} with attributes {}",
                subject, resource, subject.getPlainAttrs());

        final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        final VirAttrCache virAttrCache = context.getBean(VirAttrCache.class);
        final PasswordGenerator passwordGenerator = context.getBean(PasswordGenerator.class);

        Set<Attribute> attributes = new HashSet<>();
        String accountId = null;

        for (MappingItem mapping : attrUtils.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
            LOG.debug("Processing schema {}", mapping.getIntAttrName());

            try {
                if ((attrUtils.getType() == AttributableType.USER
                        && mapping.getIntMappingType() == IntMappingType.UserVirtualSchema)
                        || (attrUtils.getType() == AttributableType.GROUP
                        && mapping.getIntMappingType() == IntMappingType.GroupVirtualSchema)) {

                    LOG.debug("Expire entry cache {}-{}", subject.getKey(), mapping.getIntAttrName());
                    virAttrCache.expire(attrUtils.getType(), subject.getKey(), mapping.getIntAttrName());
                }

                // SYNCOPE-458 expire cache also for membership virtual schemas
                if (attrUtils.getType() == AttributableType.USER && mapping.getIntMappingType()
                        == IntMappingType.MembershipVirtualSchema && (subject instanceof User)) {

                    final User user = (User) subject;
                    for (Membership membership : user.getMemberships()) {
                        LOG.debug("Expire entry cache {}-{} for membership {}", subject.getKey(),
                                mapping.getIntAttrName(), membership);
                        virAttrCache.expire(AttributableType.MEMBERSHIP, membership.getKey(),
                                mapping.getIntAttrName());
                    }
                }

                Pair<String, Attribute> preparedAttr = prepareAttr(
                        resource, mapping, subject, password, passwordGenerator, vAttrsToBeRemoved, vAttrsToBeUpdated,
                        membVAttrsToBeRemoved, membVAttrsToBeUpdated);

                if (preparedAttr != null && preparedAttr.getKey() != null) {
                    accountId = preparedAttr.getKey();
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

        final Attribute accountIdExtAttr =
                AttributeUtil.find(attrUtils.getAccountIdItem(resource).getExtAttrName(), attributes);
        if (accountIdExtAttr != null) {
            attributes.remove(accountIdExtAttr);
            attributes.add(AttributeBuilder.build(attrUtils.getAccountIdItem(resource).getExtAttrName(), accountId));
        }
        attributes.add(evaluateNAME(subject, resource, accountId));

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }
        if (!changePwd) {
            Attribute pwdAttr = AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes);
            if (pwdAttr != null) {
                attributes.remove(pwdAttr);
            }
        }

        return new ImmutablePair<>(accountId, attributes);
    }

    /**
     * Prepare an attribute to be sent to a connector instance.
     *
     * @param resource target resource
     * @param mapItem mapping item for the given attribute
     * @param subject given user
     * @param password clear-text password
     * @param passwordGenerator password generator
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return account link + prepared attribute
     */
    @SuppressWarnings("unchecked")
    private static Pair<String, Attribute> prepareAttr(
            final ExternalResource resource, final MappingItem mapItem,
            final Subject<?, ?, ?> subject, final String password, final PasswordGenerator passwordGenerator,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated,
            final Set<String> membVAttrsToBeRemoved, final Map<String, AttrMod> membVAttrsToBeUpdated) {

        List<Attributable<?, ?, ?>> attributables = new ArrayList<>();

        ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        AttributableUtilsFactory attrUtilsFactory = context.getBean(AttributableUtilsFactory.class);
        ConnObjectUtils connObjectUtils = context.getBean(ConnObjectUtils.class);

        switch (mapItem.getIntMappingType().getAttributableType()) {
            case USER:
                if (subject instanceof User) {
                    attributables.add(subject);
                }
                break;

            case GROUP:
                if (subject instanceof User) {
                    for (Group group : ((User) subject).getGroups()) {
                        connObjectUtils.retrieveVirAttrValues(group, attrUtilsFactory.getInstance(group));
                        attributables.add(group);
                    }
                }
                if (subject instanceof Group) {
                    attributables.add(subject);
                }
                break;

            case MEMBERSHIP:
                if (subject instanceof User) {
                    attributables.addAll(((User) subject).getMemberships());
                }
                break;

            default:
        }

        List<PlainAttrValue> values = getIntValues(
                resource, mapItem, attributables, vAttrsToBeRemoved, vAttrsToBeUpdated, membVAttrsToBeRemoved,
                membVAttrsToBeUpdated);

        PlainSchema schema = null;
        boolean readOnlyVirSchema = false;
        AttrSchemaType schemaType;
        final Pair<String, Attribute> result;

        switch (mapItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case MembershipPlainSchema:
                final PlainSchemaDAO plainSchemaDAO = context.getBean(PlainSchemaDAO.class);
                schema = plainSchemaDAO.find(
                        mapItem.getIntAttrName(), getIntMappingTypeClass(mapItem.getIntMappingType()));
                schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                break;

            case UserVirtualSchema:
            case GroupVirtualSchema:
            case MembershipVirtualSchema:
                VirSchemaDAO virSchemaDAO = context.getBean(VirSchemaDAO.class);
                VirSchema virSchema = virSchemaDAO.find(
                        mapItem.getIntAttrName(), getIntMappingTypeClass(mapItem.getIntMappingType()));
                readOnlyVirSchema = (virSchema != null && virSchema.isReadonly());
                schemaType = AttrSchemaType.String;
                break;

            default:
                schemaType = AttrSchemaType.String;
        }

        final String extAttrName = mapItem.getExtAttrName();

        LOG.debug("Define mapping for: "
                + "\n* ExtAttrName " + extAttrName
                + "\n* is accountId " + mapItem.isAccountid()
                + "\n* is password " + (mapItem.isPassword() || mapItem.getIntMappingType() == IntMappingType.Password)
                + "\n* mandatory condition " + mapItem.getMandatoryCondition()
                + "\n* Schema " + mapItem.getIntAttrName()
                + "\n* IntMappingType " + mapItem.getIntMappingType().toString()
                + "\n* ClassType " + schemaType.getType().getName()
                + "\n* Values " + values);

        if (readOnlyVirSchema) {
            result = null;
        } else {
            final List<Object> objValues = new ArrayList<>();

            for (PlainAttrValue value : values) {
                if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                    objValues.add(value.getValue());
                } else {
                    objValues.add(value.getValueAsString());
                }
            }

            if (mapItem.isAccountid()) {
                result = new ImmutablePair<>(objValues.iterator().next().toString(), null);
            } else if (mapItem.isPassword() && subject instanceof User) {
                String passwordAttrValue = password;
                if (StringUtils.isBlank(passwordAttrValue)) {
                    User user = (User) subject;
                    if (user.canDecodePassword()) {
                        try {
                            passwordAttrValue = ENCRYPTOR.decode(user.getPassword(), user.getCipherAlgorithm());
                        } catch (Exception e) {
                            LOG.error("Could not decode password for {}", user, e);
                        }
                    } else if (resource.isRandomPwdIfNotProvided()) {
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
                if ((schema != null && schema.isMultivalue()) || attrUtilsFactory.getInstance(subject).getType()
                        != mapItem.getIntMappingType().getAttributableType()) {

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
     * Build __NAME__ for propagation. First look if there ia a defined accountLink for the given resource (and in this
     * case evaluate as JEXL); otherwise, take given accountId.
     *
     * @param subject given user / group
     * @param resource target resource
     * @param accountId accountId
     * @return the value to be propagated as __NAME__
     */
    public static Name evaluateNAME(final Subject<?, ?, ?> subject,
            final ExternalResource resource, final String accountId) {

        final AttributableUtilsFactory attrUtilsFactory =
                ApplicationContextProvider.getApplicationContext().getBean(AttributableUtilsFactory.class);
        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(subject);

        if (StringUtils.isBlank(accountId)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing accountId for '{}': ", resource.getKey());
        }

        // Evaluate AccountLink expression
        String evalAccountLink = null;
        if (StringUtils.isNotBlank(attrUtils.getAccountLink(resource))) {
            final JexlContext jexlContext = new MapContext();
            JexlUtils.addFieldsToContext(subject, jexlContext);
            JexlUtils.addAttrsToContext(subject.getPlainAttrs(), jexlContext);
            JexlUtils.addDerAttrsToContext(subject.getDerAttrs(), subject.getPlainAttrs(), jexlContext);
            evalAccountLink = JexlUtils.evaluate(attrUtils.getAccountLink(resource), jexlContext);
        }

        // If AccountLink evaluates to an empty string, just use the provided AccountId as Name(),
        // otherwise evaluated AccountLink expression is taken as Name().
        Name name;
        if (StringUtils.isBlank(evalAccountLink)) {
            // add AccountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            name = new Name(accountId);
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evalAccountLink);
            name = new Name(evalAccountLink);

            // AccountId not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        return name;
    }

    private static String getGroupOwnerValue(
            final ExternalResource resource, final Subject<?, ?, ?> subject) {

        AttributableUtilsFactory attrUtilsFactory =
                ApplicationContextProvider.getApplicationContext().getBean(AttributableUtilsFactory.class);

        Pair<String, Attribute> preparedAttr = prepareAttr(
                resource, attrUtilsFactory.getInstance(subject).getAccountIdItem(resource), subject, null, null,
                Collections.<String>emptySet(), Collections.<String, AttrMod>emptyMap(),
                Collections.<String>emptySet(), Collections.<String, AttrMod>emptyMap());
        String accountId = preparedAttr.getKey();

        final Name groupOwnerName = evaluateNAME(subject, resource, accountId);
        return groupOwnerName.getNameValue();
    }

    /**
     * Get attribute values.
     *
     * @param resource target resource
     * @param mappingItem mapping item
     * @param attributables list of attributables
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param membVAttrsToBeRemoved membership virtual attributes to be removed
     * @param membVAttrsToBeUpdated membership virtual attributes to be added
     * @return attribute values.
     */
    public static List<PlainAttrValue> getIntValues(final ExternalResource resource,
            final MappingItem mappingItem, final List<Attributable<?, ?, ?>> attributables,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated,
            final Set<String> membVAttrsToBeRemoved, final Map<String, AttrMod> membVAttrsToBeUpdated) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", attributables, mappingItem.getIntMappingType());

        final EntityFactory entityFactory =
                ApplicationContextProvider.getApplicationContext().getBean(EntityFactory.class);
        List<PlainAttrValue> values = new ArrayList<>();
        PlainAttrValue attrValue;
        switch (mappingItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case MembershipPlainSchema:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    final PlainAttr attr = attributable.getPlainAttr(mappingItem.getIntAttrName());
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
                for (Attributable<?, ?, ?> attributable : attributables) {
                    VirAttr virAttr = attributable.getVirAttr(mappingItem.getIntAttrName());
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
                                attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                                attrValue.setStringValue(value);
                                values.add(attrValue);
                            }
                        }
                    }

                    LOG.debug("Retrieved {} virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            attributable.getClass().getSimpleName(),
                            virAttr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }
                break;

            case MembershipVirtualSchema:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    VirAttr virAttr = attributable.getVirAttr(mappingItem.getIntAttrName());
                    if (virAttr != null) {
                        if (membVAttrsToBeRemoved != null && membVAttrsToBeUpdated != null) {
                            if (membVAttrsToBeUpdated.containsKey(mappingItem.getIntAttrName())) {
                                virAttr.getValues().clear();
                                virAttr.getValues().addAll(
                                        membVAttrsToBeUpdated.get(mappingItem.getIntAttrName()).getValuesToBeAdded());
                            } else if (membVAttrsToBeRemoved.contains(mappingItem.getIntAttrName())) {
                                virAttr.getValues().clear();
                            } else {
                                throw new IllegalArgumentException("Don't need to update membership virtual attribute '"
                                        + mappingItem.getIntAttrName() + "'");
                            }
                        }
                        if (virAttr.getValues() != null) {
                            for (String value : virAttr.getValues()) {
                                attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                                attrValue.setStringValue(value);
                                values.add(attrValue);
                            }
                        }
                    }

                    LOG.debug("Retrieved {} virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            attributable.getClass().getSimpleName(),
                            virAttr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }
                break;

            case UserDerivedSchema:
            case GroupDerivedSchema:
            case MembershipDerivedSchema:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    DerAttr derAttr = attributable.getDerAttr(mappingItem.getIntAttrName());
                    if (derAttr != null) {
                        attrValue = attributable instanceof Group
                                ? entityFactory.newEntity(GPlainAttrValue.class)
                                : entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(derAttr.getValue(attributable.getPlainAttrs()));
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
            case MembershipId:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                    attrValue.setStringValue(attributable.getKey().toString());
                    values.add(attrValue);
                }
                break;

            case Username:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    if (attributable instanceof User) {
                        attrValue = entityFactory.newEntity(UPlainAttrValue.class);
                        attrValue.setStringValue(((User) attributable).getUsername());
                        values.add(attrValue);
                    }
                }
                break;

            case GroupName:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    if (attributable instanceof Group) {
                        attrValue = entityFactory.newEntity(GPlainAttrValue.class);
                        attrValue.setStringValue(((Group) attributable).getName());
                        values.add(attrValue);
                    }
                }
                break;

            case GroupOwnerSchema:
                for (Attributable<?, ?, ?> attributable : attributables) {
                    if (attributable instanceof Group) {
                        Group group = (Group) attributable;
                        String groupOwnerValue = null;
                        if (group.getUserOwner() != null && resource.getUmapping() != null) {
                            groupOwnerValue = getGroupOwnerValue(resource, group.getUserOwner());
                        }
                        if (group.getGroupOwner() != null && resource.getGmapping() != null) {
                            groupOwnerValue = getGroupOwnerValue(resource, group.getGroupOwner());
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
     * Get accountId internal value.
     *
     * @param attributable attributable
     * @param accountIdItem accountId mapping item
     * @param resource external resource
     * @return accountId internal value
     */
    public static String getAccountIdValue(final Attributable<?, ?, ?> attributable,
            final ExternalResource resource, final MappingItem accountIdItem) {

        List<PlainAttrValue> values = getIntValues(resource, accountIdItem,
                Collections.<Attributable<?, ?, ?>>singletonList(attributable), null, null, null, null);
        return values == null || values.isEmpty()
                ? null
                : values.get(0).getValueAsString();
    }

    /**
     * For given source mapping type, return the corresponding Class object.
     *
     * @param intMappingType source mapping type
     * @return corresponding Class object, if any (can be null)
     */
    @SuppressWarnings("rawtypes")
    public static Class getIntMappingTypeClass(final IntMappingType intMappingType) {
        Class result;

        switch (intMappingType) {
            case UserPlainSchema:
                result = UPlainSchema.class;
                break;

            case GroupPlainSchema:
                result = GPlainSchema.class;
                break;

            case MembershipPlainSchema:
                result = MPlainSchema.class;
                break;

            case UserDerivedSchema:
                result = UDerSchema.class;
                break;

            case GroupDerivedSchema:
                result = GDerSchema.class;
                break;

            case MembershipDerivedSchema:
                result = MDerSchema.class;
                break;

            case UserVirtualSchema:
                result = UVirSchema.class;
                break;

            case GroupVirtualSchema:
                result = GVirSchema.class;
                break;

            case MembershipVirtualSchema:
                result = MVirSchema.class;
                break;

            default:
                result = null;
        }

        return result;
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private MappingUtils() {
    }
}
