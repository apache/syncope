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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractMapping;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
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
import org.apache.syncope.types.IntMappingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MappingUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MappingUtil.class);

    private MappingUtil() {
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
     * Get attribute values.
     *
     * @param mapping mapping
     * @param attributables list of attributables
     * @param password password
     * @param schemaDAO schema DAO
     * @return schema and attribute values.
     */
    public static Map.Entry<AbstractSchema, List<AbstractAttrValue>> getIntValues(final AbstractMappingItem mapping,
            final List<AbstractAttributable> attributables, final String password, final SchemaDAO schemaDAO) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", attributables, mapping.getIntMappingType());

        AbstractSchema schema = null;

        List<AbstractAttrValue> values = new ArrayList<AbstractAttrValue>();

        switch (mapping.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                schema = schemaDAO.find(mapping.getIntAttrName(),
                        MappingUtil.getIntMappingTypeClass(mapping.getIntMappingType()));

                for (AbstractAttributable attributable : attributables) {
                    final AbstractAttr attr = attributable.getAttribute(mapping.getIntAttrName());

                    if (attr != null && attr.getValues() != null) {
                        values.addAll(schema.isUniqueConstraint()
                                ? Collections.singletonList(attr.getUniqueValue()) : attr.getValues());
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{attr, mapping.getIntAttrName(), mapping.getIntMappingType(), values});
                }

                break;

            case UserVirtualSchema:
            case RoleVirtualSchema:
            case MembershipVirtualSchema:
                for (AbstractAttributable attributable : attributables) {
                    AbstractVirAttr virAttr = attributable.getVirtualAttribute(mapping.getIntAttrName());

                    if (virAttr != null && virAttr.getValues() != null) {
                        for (String value : virAttr.getValues()) {
                            AbstractAttrValue attrValue = new UAttrValue();
                            attrValue.setStringValue(value);
                            values.add(attrValue);
                        }
                    }

                    LOG.debug("Retrieved virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{virAttr, mapping.getIntAttrName(), mapping.getIntMappingType(), values});
                }
                break;

            case UserDerivedSchema:
            case RoleDerivedSchema:
            case MembershipDerivedSchema:
                for (AbstractAttributable attributable : attributables) {
                    AbstractDerAttr derAttr = attributable.getDerivedAttribute(mapping.getIntAttrName());

                    if (derAttr != null) {
                        AbstractAttrValue attrValue = new UAttrValue();
                        attrValue.setStringValue(derAttr.getValue(attributable.getAttributes()));
                        values.add(attrValue);
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{derAttr, mapping.getIntAttrName(), mapping.getIntMappingType(), values});
                }
                break;

            case UserId:
            case RoleId:
            case MembershipId:
                for (AbstractAttributable attributable : attributables) {
                    AbstractAttrValue attrValue = new UAttrValue();
                    attrValue.setStringValue(attributable.getId().toString());
                    values.add(attrValue);
                }
                break;

            case Username:
                for (AbstractAttributable attributable : attributables) {
                    AbstractAttrValue attrValue = new UAttrValue();
                    attrValue.setStringValue(((SyncopeUser) attributable).getUsername());
                    values.add(attrValue);
                }
                break;

            case Password:
                AbstractAttrValue attrValue = new UAttrValue();
                if (password != null) {
                    attrValue.setStringValue(password);
                }
                values.add(attrValue);
                break;

            case RoleName:
                for (AbstractAttributable attributable : attributables) {
                    attrValue = new UAttrValue();
                    attrValue.setStringValue(((SyncopeRole) attributable).getName());
                    values.add(attrValue);
                }
                break;

            default:
        }

        LOG.debug("Retrived values '{}'", values);

        return new DefaultMapEntry(schema, values);
    }

    public static List<String> getIntValueAsStrings(final AbstractAttributable attributable,
            final AbstractMappingItem mapItem, final String clearPassword) {

        List<String> value = new ArrayList<String>();

        if (mapItem != null) {
            switch (mapItem.getIntMappingType()) {
                case Username:
                    if (!(attributable instanceof SyncopeUser)) {
                        throw new ClassCastException("mappingtype is Username, but attributable is not SyncopeUser: "
                                + attributable.getClass().getName());
                    }
                    value.add(((SyncopeUser) attributable).getUsername());
                    break;

                case Password:
                    if (clearPassword != null) {
                        value.add(clearPassword);
                    }
                    break;

                case UserSchema:
                case RoleSchema:
                case MembershipSchema:
                    AbstractAttr abstractAttr = attributable.getAttribute(mapItem.getIntAttrName());
                    if (abstractAttr != null && abstractAttr.getValues() != null) {
                        value.addAll(abstractAttr.getValuesAsStrings());
                    }
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                case MembershipVirtualSchema:
                    AbstractVirAttr abstractVirAttr = attributable.getVirtualAttribute(mapItem.getIntAttrName());
                    if (abstractVirAttr != null && abstractVirAttr.getValues() != null) {
                        value.addAll(abstractVirAttr.getValues());
                    }
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                case MembershipDerivedSchema:
                    AbstractDerAttr abstractDerAttr = attributable.getDerivedAttribute(mapItem.getIntAttrName());
                    if (abstractDerAttr != null) {
                        String abstractDerAttrValue = abstractDerAttr.getValue(attributable.getAttributes());
                        if (abstractDerAttrValue != null) {
                            value.add(abstractDerAttrValue);
                        }
                    }
                    break;

                default:
            }
        }

        return value;
    }

    public static List<String> getIntValueAsStrings(final AbstractAttributable attributable,
            final AbstractMappingItem mapItem) {

        return getIntValueAsStrings(attributable, mapItem, null);
    }

    /**
     * Get accountId internal value.
     *
     * @param attributable attributable
     * @param mapping mapping
     * @return accountId internal value
     */
    public static String getAccountIdValue(final AbstractAttributable attributable, final AbstractMapping mapping) {
        final List<String> values = getIntValueAsStrings(attributable, mapping.getAccountIdItem());
        return values == null || values.isEmpty()
                ? null
                : values.get(0);
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
}
