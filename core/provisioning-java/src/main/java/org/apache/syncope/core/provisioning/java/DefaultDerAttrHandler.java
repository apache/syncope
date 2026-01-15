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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlContext;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlContextBuilder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultDerAttrHandler implements DerAttrHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(DerAttrHandler.class);

    protected final AnyChecker anyChecker;

    protected final JexlTools jexlTools;

    public DefaultDerAttrHandler(final AnyChecker anyChecker, final JexlTools jexlTools) {
        this.anyChecker = anyChecker;
        this.jexlTools = jexlTools;
    }

    protected Map<String, String> getValues(
            final Attributable attributable,
            final Collection<? extends DerSchema> schemas) {

        Map<String, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new JexlContextBuilder().
                    plainAttrs(attributable.getPlainAttrs()).
                    fields(attributable).
                    build();

            result.put(schema.getKey(), jexlTools.evaluateExpression(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public Map<String, String> getValues(final Realm realm) {
        return getValues(
                realm,
                realm.getAnyTypeClasses().stream().
                        flatMap(atc -> atc.getDerSchemas().stream()).collect(Collectors.toSet()));
    }

    @Override
    public String getValue(final Realm realm, final DerSchema schema) {
        if (realm.getAnyTypeClasses().stream().flatMap(atc -> atc.getDerSchemas().stream()).anyMatch(schema::equals)) {
            LOG.debug("{} not allowed for {}", schema, realm);
            return null;
        }

        return getValues(realm, Set.of(schema)).get(schema.getKey());
    }

    @Override
    public Map<String, String> getValues(final Any any) {
        return getValues(
                any,
                anyChecker.findAllowedSchemas(any, DerSchema.class).self());
    }

    @Override
    public String getValue(final Any any, final DerSchema schema) {
        if (!anyChecker.findAllowedSchemas(any, DerSchema.class).selfContains(schema)) {
            LOG.debug("{} not allowed for {}", schema, any);
            return null;
        }

        return getValues(any, Set.of(schema)).get(schema.getKey());
    }

    protected Map<String, String> getValues(
            final Groupable<?, ?, ?> groupable,
            final Membership<?> membership,
            final Set<DerSchema> schemas) {

        Map<String, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new JexlContextBuilder().
                    plainAttrs(groupable.getPlainAttrs(membership)).
                    fields(groupable).
                    build();

            result.put(schema.getKey(), jexlTools.evaluateExpression(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public Map<String, String> getValues(final Groupable<?, ?, ?> groupable, final Membership<?> membership) {
        Set<DerSchema> schemas = anyChecker.
                findAllowedSchemas(groupable, DerSchema.class).membership(membership.getRightEnd());
        return getValues(groupable, membership, schemas);
    }

    @Override
    public String getValue(final Groupable<?, ?, ?> groupable, final Membership<?> membership, final DerSchema schema) {
        if (!anyChecker.
                findAllowedSchemas(groupable, DerSchema.class).
                membership(membership.getRightEnd()).contains(schema)) {

            LOG.debug("{} not allowed for {}", schema, groupable);
            return null;
        }

        return getValues(groupable, membership, Set.of(schema)).get(schema.getKey());
    }

    protected Map<String, String> getValues(
            final Relatable<?, ?> relatable,
            final Relationship<?, ?> relationship,
            final Set<DerSchema> schemas) {

        Map<String, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new JexlContextBuilder().
                    plainAttrs(relatable.getPlainAttrs(relationship)).
                    fields(relatable).
                    build();

            result.put(schema.getKey(), jexlTools.evaluateExpression(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public Map<String, String> getValues(final Relatable<?, ?> relatable, final Relationship<?, ?> relationship) {
        Set<DerSchema> schemas = anyChecker.
                findAllowedSchemas(relatable, DerSchema.class).relationshipType(relationship.getType());
        return getValues(relatable, relationship, schemas);
    }

    @Override
    public String getValue(
            final Relatable<?, ?> relatable,
            final Relationship<?, ?> relationship,
            final DerSchema schema) {

        if (!anyChecker.
                findAllowedSchemas(relatable, DerSchema.class).
                relationshipTypesContains(relationship.getType(), schema)) {

            LOG.debug("{} not allowed for {}", schema, relatable);
            return null;
        }

        return getValues(relatable, relationship, Set.of(schema)).get(schema.getKey());
    }
}
