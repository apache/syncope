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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.connid.PasswordGenerator;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.RVirSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.beans.user.UVirSchema;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public final class MappingUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MappingUtil.class);

    public static <T extends AbstractMappingItem> List<T> getMatchingMappingItems(
            final Collection<T> items, final IntMappingType type) {

        final List<T> result = new ArrayList<T>();

        for (T mapItem : items) {
            if (mapItem.getIntMappingType() == type) {
                result.add(mapItem);
            }
        }

        return result;
    }

    public static <T extends AbstractMappingItem> List<T> getMatchingMappingItems(final Collection<T> items,
            final String intAttrName, final IntMappingType type) {

        final List<T> result = new ArrayList<T>();

        for (T mapItem : items) {
            if (mapItem.getIntMappingType() == type && intAttrName.equals(mapItem.getIntAttrName())) {
                result.add(mapItem);
            }
        }

        return result;
    }

    public static <T extends AbstractMappingItem> Set<T> getMatchingMappingItems(final Collection<T> mapItems,
            final String intAttrName) {

        final Set<T> result = new HashSet<T>();

        for (T mapItem : mapItems) {
            if (intAttrName.equals(mapItem.getIntAttrName())) {
                result.add(mapItem);
            }
        }

        return result;
    }

    /**
     * Prepare an attribute to be sent to a connector instance.
     *
     * @param resource target resource
     * @param <T> user / role
     * @param mapItem mapping item for the given attribute
     * @param subject given user
     * @param password clear-text password
     * @param passwordGenerator password generator
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return account link + prepared attribute
     * @throws ClassNotFoundException if schema type for given mapping does not exists in current class loader
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractAttributable> Map.Entry<String, Attribute> prepareAttribute(
            final ExternalResource resource, final AbstractMappingItem mapItem,
            final T subject, final String password, final PasswordGenerator passwordGenerator,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttributeMod> vAttrsToBeUpdated) {

        final List<AbstractAttributable> attributables = new ArrayList<AbstractAttributable>();

        switch (mapItem.getIntMappingType().getAttributableType()) {
            case USER:
                if (subject instanceof SyncopeUser) {
                    attributables.add(subject);
                }
                break;

            case ROLE:
                if (subject instanceof SyncopeUser) {
                    attributables.addAll(((SyncopeUser) subject).getRoles());
                }
                if (subject instanceof SyncopeRole) {
                    attributables.add(subject);
                }
                break;

            case MEMBERSHIP:
                if (subject instanceof SyncopeUser) {
                    attributables.addAll(((SyncopeUser) subject).getMemberships());
                }
                break;

            default:
        }

        final List<AbstractAttrValue> values = MappingUtil.getIntValues(resource, mapItem, attributables,
                vAttrsToBeRemoved, vAttrsToBeUpdated);

        AbstractSchema schema = null;
        AttributeSchemaType schemaType;
        switch (mapItem.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
                final SchemaDAO schemaDAO = context.getBean(SchemaDAO.class);
                schema = schemaDAO.find(mapItem.getIntAttrName(),
                        MappingUtil.getIntMappingTypeClass(mapItem.getIntMappingType()));
                schemaType = schema == null ? AttributeSchemaType.String : schema.getType();
                break;

            default:
                schemaType = AttributeSchemaType.String;
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

        List<Object> objValues = new ArrayList<Object>();

        for (AbstractAttrValue value : values) {
            if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                objValues.add(value.getValue());
            } else {
                objValues.add(value.getValueAsString());
            }
        }

        Map.Entry<String, Attribute> result;

        if (mapItem.isAccountid()) {
            result = new AbstractMap.SimpleEntry<String, Attribute>(objValues.iterator().next().toString(), null);
        } else if (mapItem.isPassword() && subject instanceof SyncopeUser) {
            String passwordAttrValue = password;
            if (StringUtils.isBlank(passwordAttrValue)) {
                SyncopeUser user = (SyncopeUser) subject;
                if (user.canDecodePassword()) {
                    try {
                        passwordAttrValue = PasswordEncoder.decode(user.getPassword(), user.getCipherAlgorithm());
                    } catch (Exception e) {
                        LOG.error("Could not decode password for {}", user, e);
                    }
                } else if (resource.isRandomPwdIfNotProvided()) {
                    try {
                        passwordAttrValue = passwordGenerator.generate(user);
                    } catch (InvalidPasswordPolicySpecException e) {
                        LOG.error("Could not generate policy-compliant random password for {}", user, e);

                        passwordAttrValue = RandomStringUtils.randomAlphanumeric(16);
                    }
                }
            }

            result = new AbstractMap.SimpleEntry<String, Attribute>(null,
                    AttributeBuilder.buildPassword(passwordAttrValue.toCharArray()));
        } else {
            if (schema != null && schema.isMultivalue()) {
                result = new AbstractMap.SimpleEntry<String, Attribute>(null, AttributeBuilder.build(extAttrName,
                        objValues));
            } else {
                result = new AbstractMap.SimpleEntry<String, Attribute>(null, objValues.isEmpty()
                        ? AttributeBuilder.build(extAttrName)
                        : AttributeBuilder.build(extAttrName, objValues.iterator().next()));
            }
        }

        return result;
    }

    /**
     * Build __NAME__ for propagation. First look if there ia a defined accountLink for the given resource (and in this
     * case evaluate as JEXL); otherwise, take given accountId.
     *
     * @param <T> user / role
     * @param subject given user / role
     * @param resource target resource
     * @param accountId accountId
     * @return the value to be propagated as __NAME__
     */
    public static <T extends AbstractAttributable> Name evaluateNAME(final T subject,
            final ExternalResource resource, final String accountId) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(subject);

        if (StringUtils.isBlank(accountId)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing accountId for '{}': ", resource.getName());
        }

        // Evaluate AccountLink expression
        String evalAccountLink = null;
        if (StringUtils.isNotBlank(attrUtil.getAccountLink(resource))) {
            final ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
            final JexlUtil jexlUtil = context.getBean(JexlUtil.class);

            final JexlContext jexlContext = new MapContext();
            jexlUtil.addFieldsToContext(subject, jexlContext);
            jexlUtil.addAttrsToContext(subject.getAttributes(), jexlContext);
            jexlUtil.addDerAttrsToContext(subject.getDerivedAttributes(), subject.getAttributes(), jexlContext);
            evalAccountLink = jexlUtil.evaluate(attrUtil.getAccountLink(resource), jexlContext);
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

    private static <T extends AbstractAttributable> String getRoleOwnerValue(final ExternalResource resource,
            final T subject) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(subject);

        Map.Entry<String, Attribute> preparedAttr = prepareAttribute(
                resource, attrUtil.getAccountIdItem(resource), subject, null, null,
                Collections.<String>emptySet(), Collections.<String, AttributeMod>emptyMap());
        String accountId = preparedAttr.getKey();

        final Name roleOwnerName = evaluateNAME(subject, resource, accountId);
        return roleOwnerName.getNameValue();
    }

    /**
     * Get attribute values.
     *
     * @param resource target resource
     * @param mappingItem mapping item
     * @param attributables list of attributables
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return attribute values.
     */
    public static List<AbstractAttrValue> getIntValues(final ExternalResource resource,
            final AbstractMappingItem mappingItem, final List<AbstractAttributable> attributables,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttributeMod> vAttrsToBeUpdated) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", attributables, mappingItem.getIntMappingType());

        List<AbstractAttrValue> values = new ArrayList<AbstractAttrValue>();
        AbstractAttrValue attrValue;
        switch (mappingItem.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                for (AbstractAttributable attributable : attributables) {
                    final AbstractAttr attr = attributable.getAttribute(mappingItem.getIntAttrName());
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
            case RoleVirtualSchema:
            case MembershipVirtualSchema:
                for (AbstractAttributable attributable : attributables) {
                    AbstractVirAttr virAttr = attributable.getVirtualAttribute(mappingItem.getIntAttrName());
                    if (virAttr != null) {
                        if (virAttr.getValues() != null) {
                            for (String value : virAttr.getValues()) {
                                attrValue = new UAttrValue();
                                attrValue.setStringValue(value);
                                values.add(attrValue);
                            }
                        }
                        if (vAttrsToBeRemoved != null && vAttrsToBeUpdated != null) {
                            if (vAttrsToBeUpdated.containsKey(mappingItem.getIntAttrName())) {
                                virAttr.setValues(vAttrsToBeUpdated.get(mappingItem.getIntAttrName()).
                                        getValuesToBeAdded());
                            } else if (vAttrsToBeRemoved.contains(mappingItem.getIntAttrName())) {
                                virAttr.getValues().clear();
                            } else {
                                throw new RuntimeException("Virtual attribute has not to be updated");
                            }
                        }
                    }

                    LOG.debug("Retrieved virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            virAttr, mappingItem.getIntAttrName(), mappingItem.getIntMappingType(), values);
                }
                break;

            case UserDerivedSchema:
            case RoleDerivedSchema:
            case MembershipDerivedSchema:
                for (AbstractAttributable attributable : attributables) {
                    AbstractDerAttr derAttr = attributable.getDerivedAttribute(mappingItem.getIntAttrName());
                    if (derAttr != null) {
                        attrValue = (attributable instanceof SyncopeRole)
                                ? new RAttrValue() : new UAttrValue();
                        attrValue.setStringValue(derAttr.getValue(attributable.getAttributes()));
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
            case RoleId:
            case MembershipId:
                for (AbstractAttributable attributable : attributables) {
                    attrValue = new UAttrValue();
                    attrValue.setStringValue(attributable.getId().toString());
                    values.add(attrValue);
                }
                break;

            case Username:
                for (AbstractAttributable attributable : attributables) {
                    if (attributable instanceof SyncopeUser) {
                        attrValue = new UAttrValue();
                        attrValue.setStringValue(((SyncopeUser) attributable).getUsername());
                        values.add(attrValue);
                    }
                }
                break;

            case RoleName:
                for (AbstractAttributable attributable : attributables) {
                    if (attributable instanceof SyncopeRole) {
                        attrValue = new RAttrValue();
                        attrValue.setStringValue(((SyncopeRole) attributable).getName());
                        values.add(attrValue);
                    }
                }
                break;

            case RoleOwnerSchema:
                for (AbstractAttributable attributable : attributables) {
                    if (attributable instanceof SyncopeRole) {
                        SyncopeRole role = (SyncopeRole) attributable;
                        String roleOwnerValue = null;
                        if (role.getUserOwner() != null && resource.getUmapping() != null) {
                            roleOwnerValue = getRoleOwnerValue(resource, role.getUserOwner());
                        }
                        if (role.getRoleOwner() != null && resource.getRmapping() != null) {
                            roleOwnerValue = getRoleOwnerValue(resource, role.getRoleOwner());
                        }

                        if (StringUtils.isNotBlank(roleOwnerValue)) {
                            attrValue = new RAttrValue();
                            attrValue.setStringValue(roleOwnerValue);
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
     * @param accountIdItem accountid mapping item
     * @return accountId internal value
     */
    public static String getAccountIdValue(final AbstractAttributable attributable, final ExternalResource resource,
            final AbstractMappingItem accountIdItem) {

        List<AbstractAttrValue> values = getIntValues(resource, accountIdItem,
                Collections.<AbstractAttributable>singletonList(attributable), null, null);
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
    public static Class getIntMappingTypeClass(final IntMappingType intMappingType) {
        Class result;

        switch (intMappingType) {
            case UserSchema:
                result = USchema.class;
                break;

            case RoleSchema:
                result = RSchema.class;
                break;

            case MembershipSchema:
                result = MSchema.class;
                break;

            case UserDerivedSchema:
                result = UDerSchema.class;
                break;

            case RoleDerivedSchema:
                result = RDerSchema.class;
                break;

            case MembershipDerivedSchema:
                result = MDerSchema.class;
                break;

            case UserVirtualSchema:
                result = UVirSchema.class;
                break;

            case RoleVirtualSchema:
                result = RVirSchema.class;
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
    private MappingUtil() {
    }
}
