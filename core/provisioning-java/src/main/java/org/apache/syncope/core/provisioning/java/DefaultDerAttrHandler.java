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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultDerAttrHandler implements DerAttrHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(DerAttrHandler.class);

    protected static Map<DerSchema, String> getValues(final Any any, final Set<DerSchema> schemas) {
        Map<DerSchema, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new MapContext();
            JexlUtils.addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
            JexlUtils.addFieldsToContext(any, jexlContext);

            result.put(schema, JexlUtils.evaluateExpr(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    protected final AnyUtilsFactory anyUtilsFactory;

    public DefaultDerAttrHandler(final AnyUtilsFactory anyUtilsFactory) {
        this.anyUtilsFactory = anyUtilsFactory;
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
    public Map<DerSchema, String> getValues(final Any any) {
        return getValues(
                any,
                anyUtilsFactory.getInstance(any).dao().findAllowedSchemas(any, DerSchema.class).getForSelf());
    }

    protected static Map<DerSchema, String> getValues(
            final Groupable<?, ?, ?, ?> any, final Membership<?> membership, final Set<DerSchema> schemas) {

        Map<DerSchema, String> result = new HashMap<>(schemas.size());

        schemas.forEach(schema -> {
            JexlContext jexlContext = new MapContext();
            JexlUtils.addPlainAttrsToContext(any.getPlainAttrs(membership), jexlContext);
            JexlUtils.addFieldsToContext(any, jexlContext);

            result.put(schema, JexlUtils.evaluateExpr(schema.getExpression(), jexlContext).toString());
        });

        return result;
    }

    @Override
    public Map<DerSchema, String> getValues(final Groupable<?, ?, ?, ?> any, final Membership<?> membership) {
        return getValues(
                any,
                membership,
                anyUtilsFactory.getInstance(any).dao().
                        findAllowedSchemas(any, DerSchema.class).getForMembership(membership.getRightEnd()));
    }

}
