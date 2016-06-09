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
package org.apache.syncope.core.provisioning.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;

public final class IntAttrNameParser {

    private static final Pattern ENCLOSING_GROUP_PATTERN = Pattern.compile("^groups\\[[\\w]+\\]\\.[\\w]+");

    private static final Pattern RELATED_ANY_OBJECT_PATTERN = Pattern.compile("^anyObjects\\[[\\w]+\\]\\.[\\w]+");

    private static final Pattern MEMBERSHIP_PATTERN = Pattern.compile("^\\[[\\w]+\\]\\.[\\w]+");

    public static class IntAttrName {

        private AnyTypeKind anyTypeKind;

        private String field;

        private SchemaType schemaType;

        private String schemaName;

        private String enclosingGroup;

        private String relatedAnyObject;

        private String membershipOfGroup;

        public AnyTypeKind getAnyTypeKind() {
            return anyTypeKind;
        }

        public String getField() {
            return field;
        }

        public SchemaType getSchemaType() {
            return schemaType;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getEnclosingGroup() {
            return enclosingGroup;
        }

        public String getRelatedAnyObject() {
            return relatedAnyObject;
        }

        public String getMembershipOfGroup() {
            return membershipOfGroup;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(field, ToStringStyle.MULTI_LINE_STYLE);
        }

    }

    private static void setFieldOrSchemaName(
            final String fieldOrSchemaName, final AnyUtils anyUtils, final IntAttrName result) {

        if (anyUtils.isFieldName(fieldOrSchemaName)) {
            result.field = fieldOrSchemaName;
        } else {
            result.schemaName = fieldOrSchemaName;
        }
    }

    public static IntAttrName parse(
            final String intAttrName,
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeKind provisionAnyTypeKind) {

        IntAttrName result = new IntAttrName();

        if (intAttrName.indexOf('.') == -1) {
            result.anyTypeKind = provisionAnyTypeKind;
            setFieldOrSchemaName(intAttrName, anyUtilsFactory.getInstance(provisionAnyTypeKind), result);
        } else {
            Matcher matcher = ENCLOSING_GROUP_PATTERN.matcher(intAttrName);
            if (matcher.matches()) {
                result.anyTypeKind = AnyTypeKind.GROUP;
                result.enclosingGroup = matcher.group(1);
                setFieldOrSchemaName(matcher.group(2), anyUtilsFactory.getInstance(AnyTypeKind.GROUP), result);
            } else {
                matcher = RELATED_ANY_OBJECT_PATTERN.matcher(intAttrName);
                if (matcher.matches()) {
                    result.anyTypeKind = AnyTypeKind.ANY_OBJECT;
                    result.relatedAnyObject = matcher.group(1);
                    setFieldOrSchemaName(matcher.group(2), anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), result);
                } else {
                    matcher = MEMBERSHIP_PATTERN.matcher(intAttrName);
                    if (matcher.matches()) {
                        result.anyTypeKind = AnyTypeKind.USER;
                        result.membershipOfGroup = matcher.group(1);
                        setFieldOrSchemaName(matcher.group(2), anyUtilsFactory.getInstance(AnyTypeKind.USER), result);
                    } else {
                        throw new IllegalArgumentException("Unparsable expression: " + intAttrName);
                    }
                }
            }
        }

        return result;
    }

    private IntAttrNameParser() {
        // private constructor for static utility class
    }
}
