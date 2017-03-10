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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.SchemaService;

/**
 * Console client for invoking rest schema services.
 */
public class SchemaRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2479730152700312373L;

    public <T extends AbstractSchemaTO> List<T> getSchemas(final SchemaType schemaType, final AnyTypeKind kind) {
        final AnyTypeService client = getService(AnyTypeService.class);

        final List<String> classes = new ArrayList<>();

        switch (kind) {
            case USER:
            case GROUP:
                final AnyTypeTO type = client.read(kind.name());
                if (type != null) {
                    classes.addAll(type.getClasses());
                }
                break;

            default:
                for (AnyTypeTO anyTypeTO : new AnyTypeRestClient().list()) {
                    if (anyTypeTO.getKind() != AnyTypeKind.USER && anyTypeTO.getKind() != AnyTypeKind.GROUP) {
                        classes.addAll(anyTypeTO.getClasses());
                    }
                }
        }
        return getSchemas(schemaType, classes.toArray(new String[] {}));
    }

    public <T extends AbstractSchemaTO> List<T> getSchemas(final SchemaType schemaType, final String typeName) {
        AnyTypeTO type = null;

        try {
            type = getService(AnyTypeService.class).read(typeName);
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        if (type == null) {
            return getSchemas(schemaType);
        } else {
            return getSchemas(schemaType, type.getClasses().toArray(new String[] {}));
        }
    }

    public <T extends AbstractSchemaTO> List<T> getSchemas(final SchemaType schemaType, final String... kind) {
        List<T> schemas = new ArrayList<>();

        try {
            if (kind == null || kind.length == 0) {
                schemas.addAll(getService(SchemaService.class).
                        <T>list(new SchemaQuery.Builder().type(schemaType).build()));
            } else {
                schemas.addAll(getService(SchemaService.class).
                        <T>list(new SchemaQuery.Builder().type(schemaType).anyTypeClasses(kind).build()));
            }
        } catch (SyncopeClientException e) {
            LOG.error("While getting all {} schemas for {}", schemaType, kind, e);
        }
        return schemas;
    }

    public List<String> getSchemaNames(final SchemaType schemaType) {
        List<String> schemaNames = new ArrayList<>();

        try {
            CollectionUtils.collect(getSchemas(schemaType),
                    EntityTOUtils.<AbstractSchemaTO>keyTransformer(), schemaNames);
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    public List<String> getPlainSchemaNames() {
        return getSchemaNames(SchemaType.PLAIN);
    }

    public List<String> getDerSchemaNames() {
        return getSchemaNames(SchemaType.DERIVED);
    }

    public List<String> getVirSchemaNames() {
        return getSchemaNames(SchemaType.VIRTUAL);
    }

    public PlainSchemaTO read(final SchemaType schemaType, final String key) {
        return getService(SchemaService.class).read(schemaType, key);

    }

    public void create(final SchemaType schemaType, final AbstractSchemaTO modelObject) {
        getService(SchemaService.class).create(schemaType, modelObject);
    }

    public void update(final SchemaType schemaType, final AbstractSchemaTO modelObject) {
        getService(SchemaService.class).update(schemaType, modelObject);
    }

    public PlainSchemaTO deletePlainSchema(final String name) {
        PlainSchemaTO response = getService(SchemaService.class).read(SchemaType.PLAIN, name);
        getService(SchemaService.class).delete(SchemaType.PLAIN, name);
        return response;
    }

    public DerSchemaTO deleteDerSchema(final String name) {
        DerSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(SchemaType.DERIVED, name);
        return schemaTO;
    }

    public VirSchemaTO deleteVirSchema(final String name) {
        VirSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(SchemaType.VIRTUAL, name);
        return schemaTO;
    }
}
