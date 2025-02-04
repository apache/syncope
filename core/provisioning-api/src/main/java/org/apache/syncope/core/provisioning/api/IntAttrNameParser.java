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

import java.text.ParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "squid:S4784", "squid:S3776" })
public class IntAttrNameParser {

    protected static final String END_PATTERN = ")\\]\\.(.+)";

    protected static final Pattern ENCLOSING_GROUP_PATTERN = Pattern.compile(
            "^groups\\[(" + Entity.ID_REGEX + END_PATTERN);

    protected static final Pattern RELATED_USER_PATTERN = Pattern.compile(
            "^users\\[(" + Entity.ID_REGEX + END_PATTERN);

    protected static final Pattern RELATED_ANY_OBJECT_PATTERN = Pattern.compile(
            "^anyObjects\\[(" + Entity.ID_REGEX + END_PATTERN);

    protected static final Pattern MEMBERSHIP_PATTERN = Pattern.compile(
            "^memberships\\[(" + Entity.ID_REGEX + END_PATTERN);

    protected static final Pattern RELATIONSHIP_PATTERN = Pattern.compile(
            "^relationships\\[(" + Entity.ID_REGEX + ")\\]"
            + "\\[(" + Entity.ID_REGEX + END_PATTERN);

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnyUtilsFactory anyUtilsFactory;

    public IntAttrNameParser(
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyUtilsFactory anyUtilsFactory) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    protected Pair<Schema, SchemaType> find(final String key) {
        Schema schema = plainSchemaDAO.findById(key).orElse(null);
        if (schema == null) {
            schema = derSchemaDAO.findById(key).orElse(null);
            if (schema == null) {
                schema = virSchemaDAO.findById(key).orElse(null);
                if (schema == null) {
                    return null;
                } else {
                    return Pair.of(schema, SchemaType.VIRTUAL);
                }
            } else {
                return Pair.of(schema, SchemaType.DERIVED);
            }
        } else {
            return Pair.of(schema, SchemaType.PLAIN);
        }
    }

    protected void setFieldOrSchemaName(
            final String fieldOrSchemaName,
            final AnyTypeKind anyTypeKind,
            final IntAttrName result) {

        anyUtilsFactory.getInstance(anyTypeKind).getField(fieldOrSchemaName).ifPresentOrElse(
                field -> result.setField(fieldOrSchemaName),
                () -> Optional.ofNullable(find(fieldOrSchemaName)).ifPresent(schemaInfo -> {
                    result.setSchemaType(schemaInfo.getRight());
                    result.setSchema(schemaInfo.getLeft());
                }));
    }

    @Transactional(readOnly = true)
    public IntAttrName parse(final String intAttrName, final AnyTypeKind provisionAnyTypeKind) throws ParseException {
        IntAttrName result = new IntAttrName();

        Matcher matcher;
        if (intAttrName.indexOf('.') == -1) {
            result.setAnyTypeKind(provisionAnyTypeKind);
            setFieldOrSchemaName(intAttrName, result.getAnyTypeKind(), result);
        } else {
            matcher = ENCLOSING_GROUP_PATTERN.matcher(intAttrName);
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
                        matcher = RELATED_USER_PATTERN.matcher(intAttrName);
                        if (matcher.matches()) {
                            result.setAnyTypeKind(AnyTypeKind.USER);
                            result.setRelatedUser(matcher.group(1));
                            setFieldOrSchemaName(matcher.group(2), result.getAnyTypeKind(), result);
                        } else {
                            matcher = RELATIONSHIP_PATTERN.matcher(intAttrName);
                            if (matcher.matches()) {
                                result.setAnyTypeKind(AnyTypeKind.ANY_OBJECT);
                                result.setRelationshipType(matcher.group(1));
                                result.setRelationshipAnyType(matcher.group(2));
                                setFieldOrSchemaName(matcher.group(3), result.getAnyTypeKind(), result);
                            } else {
                                throw new ParseException("Unparsable expression: " + intAttrName, 0);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
