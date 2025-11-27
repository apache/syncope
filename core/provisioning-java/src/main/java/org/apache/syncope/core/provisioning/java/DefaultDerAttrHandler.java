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
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlContextBuilder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultDerAttrHandler implements DerAttrHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(DerAttrHandler.class);

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final JexlTools jexlTools;

    public DefaultDerAttrHandler(final AnyUtilsFactory anyUtilsFactory, final JexlTools jexlTools) {
        this.anyUtilsFactory = anyUtilsFactory;
        this.jexlTools = jexlTools;
    }

    protected Map<DerSchema, String> getValues(
            final Attributable attributable,
            final Collection<? extends DerSchema> schemas) {

        Map<DerSchema, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new JexlContextBuilder().
                    plainAttrs(attributable.getPlainAttrs()).
                    fields(attributable).
                    build();

            result.put(schema, jexlTools.evaluateExpression(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public String getValue(final Realm realm, final DerSchema schema) {
        if (realm.getAnyTypeClasses().stream().flatMap(atc -> atc.getDerSchemas().stream()).anyMatch(schema::equals)) {
            LOG.debug("{} not allowed for {}", schema, realm);
            return null;
        }

        return getValues(realm, Set.of(schema)).get(schema);
    }

    @Override
    public String getValue(final Any any, final DerSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, DerSchema.class).forSelfContains(schema)) {
            LOG.debug("{} not allowed for {}", schema, any);
            return null;
        }

        return getValues(any, Set.of(schema)).get(schema);
    }

    @Override
    public String getValue(final Any any, final Membership<?> membership, final DerSchema schema) {
        if (!anyUtilsFactory.getInstance(any).dao().
                findAllowedSchemas(any, DerSchema.class).getForMembership(membership.getRightEnd()).contains(schema)) {

            LOG.debug("{} not allowed for {}", schema, any);
            return null;
        }

        return getValues(any, Set.of(schema)).get(schema);
    }

    @Override
    public Map<DerSchema, String> getValues(final Realm realm) {
        return getValues(
                realm,
                realm.getAnyTypeClasses().stream().
                        flatMap(atc -> atc.getDerSchemas().stream()).collect(Collectors.toSet()));
    }

    @Override
    public Map<DerSchema, String> getValues(final Any any) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, DerSchema.class).getForSelf());
    }

    protected Map<DerSchema, String> getValues(
            final Groupable<?, ?, ?> groupable, final Membership<?> membership, final Set<DerSchema> schemas) {

        Map<DerSchema, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new JexlContextBuilder().
                    plainAttrs(groupable.getPlainAttrs(membership)).
                    fields(groupable).
                    build();

            result.put(schema, jexlTools.evaluateExpression(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public Map<DerSchema, String> getValues(final Groupable<?, ?, ?> any, final Membership<?> membership) {
        return getValues(
                any,
                membership,
                anyUtilsFactory.getInstance(any).dao().
                        findAllowedSchemas(any, DerSchema.class).getForMembership(membership.getRightEnd()));
    }
}
