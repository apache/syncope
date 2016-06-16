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

import org.apache.syncope.core.provisioning.api.IntAttrName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class IntAttrNameParser {

    private static final Pattern ENCLOSING_GROUP_PATTERN = Pattern.compile(
            "^groups\\[(" + SyncopeConstants.NAME_PATTERN + ")\\]\\.(.+)");

    private static final Pattern RELATED_ANY_OBJECT_PATTERN = Pattern.compile(
            "^anyObjects\\[(" + SyncopeConstants.NAME_PATTERN + ")\\]\\.(.+)");

    private static final Pattern MEMBERSHIP_PATTERN = Pattern.compile(
            "^memberships\\[(" + SyncopeConstants.NAME_PATTERN + ")\\]\\.(.+)");

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private SchemaType find(final String key) {
        Schema schema = plainSchemaDAO.find(key);
        if (schema == null) {
            schema = derSchemaDAO.find(key);
            if (schema == null) {
                schema = virSchemaDAO.find(key);
                if (schema == null) {
                    return null;
                } else {
                    return SchemaType.VIRTUAL;
                }
            } else {
                return SchemaType.DERIVED;
            }
        } else {
            return SchemaType.PLAIN;
        }
    }

    private void setFieldOrSchemaName(
            final String fieldOrSchemaName,
            final AnyTypeKind anyTypeKind,
            final IntAttrName result) {

        if (anyUtilsFactory.getInstance(anyTypeKind).isFieldName(fieldOrSchemaName)) {
            result.setField(fieldOrSchemaName);
        } else {
            result.setSchemaType(find(fieldOrSchemaName));
            result.setSchemaName(fieldOrSchemaName);
        }
    }

    @Transactional(readOnly = true)
    public IntAttrName parse(final String intAttrName, final AnyTypeKind provisionAnyTypeKind) {
        IntAttrName result = new IntAttrName();

        if (intAttrName.indexOf('.') == -1) {
            result.setAnyTypeKind(provisionAnyTypeKind);
            setFieldOrSchemaName(intAttrName, result.getAnyTypeKind(), result);
        } else {
            Matcher matcher = ENCLOSING_GROUP_PATTERN.matcher(intAttrName);
            if (matcher.matches()) {
                result.setAnyTypeKind(AnyTypeKind.GROUP);
                result.setEnclosingGroup(matcher.group(1));
                setFieldOrSchemaName(matcher.group(2), result.getAnyTypeKind(), result);
            } else {
                matcher = RELATED_ANY_OBJECT_PATTERN.matcher(intAttrName);
                if (matcher.matches()) {
                    result.setAnyTypeKind(AnyTypeKind.ANY_OBJECT);
                    result.setRelatedAnyObject(matcher.group(1));
                    setFieldOrSchemaName(matcher.group(2), result.getAnyTypeKind(), result);
                } else {
                    matcher = MEMBERSHIP_PATTERN.matcher(intAttrName);
                    if (matcher.matches()) {
                        result.setAnyTypeKind(AnyTypeKind.USER);
                        result.setMembershipOfGroup(matcher.group(1));
                        setFieldOrSchemaName(matcher.group(2), result.getAnyTypeKind(), result);
                    } else {
                        throw new IllegalArgumentException("Unparsable expression: " + intAttrName);
                    }
                }
            }
        }

        return result;
    }
}
