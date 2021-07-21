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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.EntityTO;
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

    public static <T extends SchemaTO> List<T> getSchemas(final SchemaType schemaType, final AnyTypeKind kind) {
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
                AnyTypeRestClient.listAnyTypes().stream().filter(
                        anyTypeTO -> (anyTypeTO.getKind() != AnyTypeKind.USER
                        && anyTypeTO.getKind() != AnyTypeKind.GROUP)).
                        forEach((anyTypeTO) -> classes.addAll(anyTypeTO.getClasses()));
        }
        return getSchemas(schemaType, null, classes.toArray(new String[] {}));
    }

    public static <T extends SchemaTO> List<T> getSchemas(
        final SchemaType schemaType, final String keyword, final String... anyTypeClasses) {

        SchemaQuery.Builder builder = new SchemaQuery.Builder().type(schemaType);
        if (StringUtils.isNotBlank(keyword)) {
            builder.keyword(keyword);
        }
        if (anyTypeClasses != null && anyTypeClasses.length > 0) {
            builder.anyTypeClasses(anyTypeClasses);
        }

        List<T> schemas = new ArrayList<>();
        try {
            schemas.addAll(getService(SchemaService.class).<T>search(builder.build()));
        } catch (SyncopeClientException e) {
            LOG.error("While getting all {} schemas for {}", schemaType, anyTypeClasses, e);
        }
        return schemas;
    }

    public static List<String> getSchemaNames(final SchemaType schemaType) {
        List<String> schemaNames = List.of();

        try {
            schemaNames = getSchemas(schemaType, null, new String[0]).stream().
                    map(EntityTO::getKey).collect(Collectors.toList());
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    public static List<String> getPlainSchemaNames() {
        return getSchemaNames(SchemaType.PLAIN);
    }

    public static List<String> getDerSchemaNames() {
        return getSchemaNames(SchemaType.DERIVED);
    }

    public static List<String> getVirSchemaNames() {
        return getSchemaNames(SchemaType.VIRTUAL);
    }

    public static <T extends SchemaTO> T read(final SchemaType schemaType, final String key) {
        return getService(SchemaService.class).read(schemaType, key);
    }

    public static void create(final SchemaType schemaType, final SchemaTO modelObject) {
        getService(SchemaService.class).create(schemaType, modelObject);
    }

    public static void update(final SchemaType schemaType, final SchemaTO modelObject) {
        getService(SchemaService.class).update(schemaType, modelObject);
    }

    public static PlainSchemaTO deletePlainSchema(final String name) {
        PlainSchemaTO response = getService(SchemaService.class).read(SchemaType.PLAIN, name);
        getService(SchemaService.class).delete(SchemaType.PLAIN, name);
        return response;
    }

    public static DerSchemaTO deleteDerSchema(final String name) {
        DerSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.DERIVED, name);
        getService(SchemaService.class).delete(SchemaType.DERIVED, name);
        return schemaTO;
    }

    public static VirSchemaTO deleteVirSchema(final String name) {
        VirSchemaTO schemaTO = getService(SchemaService.class).read(SchemaType.VIRTUAL, name);
        getService(SchemaService.class).delete(SchemaType.VIRTUAL, name);
        return schemaTO;
    }
}
