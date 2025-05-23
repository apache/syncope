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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.SchemaTO;
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

    public <T extends SchemaTO> List<T> getSchemas(final SchemaType schemaType, final AnyTypeKind kind) {
        List<String> classes = new ArrayList<>();

        switch (kind) {
            case USER, GROUP ->
                Optional.ofNullable(getService(AnyTypeService.class).read(kind.name())).
                        ifPresent(anyType -> classes.addAll(anyType.getClasses()));

            default ->
                getService(AnyTypeService.class).list().stream().
                        filter(anyType -> anyType.getKind() == AnyTypeKind.ANY_OBJECT).
                        forEach(anyType -> classes.addAll(anyType.getClasses()));
        }

        return getSchemas(schemaType, null, classes.toArray(String[]::new));
    }

    public <T extends SchemaTO> List<T> getSchemas(
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
            schemas.addAll(getService(SchemaService.class).search(builder.build()));
        } catch (SyncopeClientException e) {
            LOG.error("While getting all {} schemas for {}", schemaType, anyTypeClasses, e);
        }
        return schemas;
    }

    public List<String> getSchemaNames(final SchemaType schemaType) {
        List<String> schemaNames = List.of();

        try {
            schemaNames = getSchemas(schemaType, null, new String[0]).stream().
                    map(SchemaTO::getKey).collect(Collectors.toList());
        } catch (SyncopeClientException e) {
            LOG.error("While getting all user schema names", e);
        }

        return schemaNames;
    }

    public <T extends SchemaTO> T read(final SchemaType schemaType, final String key) {
        return getService(SchemaService.class).read(schemaType, key);
    }

    public void create(final SchemaType schemaType, final SchemaTO modelObject) {
        getService(SchemaService.class).create(schemaType, modelObject);
    }

    public void update(final SchemaType schemaType, final SchemaTO modelObject) {
        getService(SchemaService.class).update(schemaType, modelObject);
    }

    public void delete(final SchemaType schemaType, final String key) {
        getService(SchemaService.class).delete(schemaType, key);
    }

    public List<String> getDropdownValues(final String key, final AnyTO anyTO) {
        return getService(SchemaService.class).getDropdownValues(key, anyTO).getValues();
    }

    public List<String> getDropdownValues(final String key, final RealmTO realmTO) {
        return getService(SchemaService.class).getDropdownValues(key, realmTO).getValues();
    }
}
