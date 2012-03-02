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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.membership.MDerSchema;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.membership.MVirSchema;
import org.syncope.core.persistence.beans.role.RDerSchema;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.beans.role.RVirSchema;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.IntMappingType;

public class SchemaMappingUtil {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SchemaMappingUtil.class);

    public static String getExtAttrName(final SchemaMapping mapping) {
        final String name;

        if (mapping.isAccountid()) {
            name = Uid.NAME;
        } else if (mapping.isPassword()) {
            name = OperationalAttributes.PASSWORD_NAME;
        } else {
            name = mapping.getExtAttrName();
        }

        return name;
    }

    public static String getIntAttrName(final SchemaMapping mapping) {
        final String name;

        switch (mapping.getIntMappingType()) {
            case SyncopeUserId:
                name = "id";
                break;
            case Username:
                name = "username";
                break;
            case Password:
                name = "password";
                break;
            default:
                name = mapping.getIntAttrName();
        }

        return name;
    }

    public static String getIntAttrName(final SchemaMapping mapping, final IntMappingType type) {
        return type == mapping.getIntMappingType() ? getIntAttrName(mapping) : null;
    }

    /**
     * Get attribute values.
     *
     * @param mapping mapping.
     * @param attributables list of attributables.
     * @param password password.
     * @return schema and attribute values.
     */
    public static Map.Entry<AbstractSchema, List<AbstractAttrValue>> getIntValues(
            final SchemaMapping mapping,
            final List<AbstractAttributable> attributables,
            final String password,
            final SchemaDAO schemaDAO) {

        LOG.debug("Get attributes for '{}' and mapping type '{}'", attributables, mapping.getIntMappingType());

        AbstractSchema schema = null;

        List<AbstractAttrValue> values = new ArrayList<AbstractAttrValue>();

        switch (mapping.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                schema = schemaDAO.find(
                        mapping.getIntAttrName(),
                        SchemaMappingUtil.getIntMappingTypeClass(mapping.getIntMappingType()));

                for (AbstractAttributable attributable : attributables) {
                    final UAttr attr = attributable.getAttribute(mapping.getIntAttrName());

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
                    AbstractDerAttr derAttr = attributable.getDerivedAttribute(
                            mapping.getIntAttrName());

                    if (derAttr != null) {
                        AbstractAttrValue attrValue = new UAttrValue();
                        attrValue.setStringValue(
                                derAttr.getValue(attributable.getAttributes()));
                        values.add(attrValue);
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{derAttr, mapping.getIntAttrName(),
                                mapping.getIntMappingType(), values});
                }
                break;

            case Username:
                for (AbstractAttributable attributable : attributables) {
                    AbstractAttrValue attrValue = new UAttrValue();
                    attrValue.setStringValue(((SyncopeUser) attributable).getUsername());
                    values.add(attrValue);
                }
                break;

            case SyncopeUserId:
                for (AbstractAttributable attributable : attributables) {
                    AbstractAttrValue attrValue = new UAttrValue();
                    attrValue.setStringValue(attributable.getId().toString());
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

            default:
        }

        LOG.debug("Retrived values '{}'", values);

        return new DefaultMapEntry(schema, values);
    }

    public static List<String> getIntValueAsStrings(
            final AbstractAttributable attributable, final SchemaMapping mapping) {
        return getIntValueAsStrings(attributable, mapping, null);
    }

    public static List<String> getIntValueAsStrings(
            final AbstractAttributable attributable, final SchemaMapping mapping, String clearPassword) {

        final List<String> value;

        switch (mapping.getIntMappingType()) {
            case Username:
                value = new ArrayList<String>();
                value.add(((SyncopeUser) attributable).getUsername());
                break;
            case Password:
                if (clearPassword == null) {
                    value = null;
                } else {
                    value = new ArrayList<String>();
                    value.add(clearPassword);
                }
                break;
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                value = attributable.getAttribute(mapping.getIntAttrName()).getValuesAsStrings();
                break;
            case UserVirtualSchema:
            case RoleVirtualSchema:
            case MembershipVirtualSchema:
                value = attributable.getVirtualAttribute(mapping.getIntAttrName()).getValues();
                break;
            case UserDerivedSchema:
            case RoleDerivedSchema:
            case MembershipDerivedSchema:
                value = new ArrayList<String>();
                value.add(attributable.getDerivedAttribute(mapping.getIntAttrName()).getValue(
                        attributable.getAttributes()));
                break;
            default:
                value = null;
        }

        return value;
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
