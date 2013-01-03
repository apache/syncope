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
import org.apache.syncope.client.mod.AttributeMod;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
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
     * @param mappingItem mapping item
     * @param attributables list of attributables
     * @param password password
     * @param schemaDAO schema DAO
     * @return attribute values.
     */
    public static List<AbstractAttrValue> getIntValues(final AbstractMappingItem mappingItem,
            final List<AbstractAttributable> attributables, final String password,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttributeMod> vAttrsToBeUpdated) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", attributables, mappingItem.getIntMappingType());

        List<AbstractAttrValue> values = new ArrayList<AbstractAttrValue>();

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
                                AbstractAttrValue attrValue = new UAttrValue();
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
                        AbstractAttrValue attrValue = (attributable instanceof SyncopeRole)
                                ? new RAttrValue() : new UAttrValue();
                        attrValue.setStringValue(derAttr.getValue(attributable.getAttributes()));
                        values.add(attrValue);
                    }

                    LOG.
                            debug("Retrieved attribute {}"
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
                    AbstractAttrValue attrValue = new UAttrValue();
                    attrValue.setStringValue(attributable.getId().toString());
                    values.add(attrValue);
                }
                break;

            case Username:
                for (AbstractAttributable attributable : attributables) {
                    if (attributable instanceof SyncopeUser) {
                        AbstractAttrValue attrValue = new UAttrValue();
                        attrValue.setStringValue(((SyncopeUser) attributable).getUsername());
                        values.add(attrValue);
                    }
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
                    if (attributable instanceof SyncopeRole) {
                        attrValue = new RAttrValue();
                        attrValue.setStringValue(((SyncopeRole) attributable).getName());
                        values.add(attrValue);
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
    public static String getAccountIdValue(final AbstractAttributable attributable,
            final AbstractMappingItem accountIdItem) {

        List<AbstractAttrValue> values = getIntValues(
                accountIdItem, Collections.<AbstractAttributable>singletonList(attributable), null, null, null);
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
}
