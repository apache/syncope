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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.to.AbstractSchemaTO;
import org.apache.syncope.client.to.DerivedSchemaTO;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.to.VirtualSchemaTO;
import org.apache.syncope.services.SchemaService;
import org.springframework.web.client.RestTemplate;

public class SchemaServiceProxy extends SpringServiceProxy implements SchemaService {

    public SchemaServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractSchemaTO> T create(String kind, T schemaTO) {
        String schemaType = getSchemaType(schemaTO.getClass());

        return (T) restTemplate.postForObject(baseUrl + schemaType + "/{kind}/create", schemaTO,
                schemaTO.getClass(), kind);
    }

    @Override
    public <T extends AbstractSchemaTO> T delete(String kind, String schemaName, Class<T> type) {
        String schemaType = getSchemaType(type);
        return restTemplate.getForObject(baseUrl + schemaType + "/{kind}/delete/{name}.json", type, kind,
                schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> List<T> list(String kind, Class<T[]> type) {
        String schemaType = getSchemaTypeArray(type);
        return Arrays.asList(restTemplate.getForObject(baseUrl + schemaType + "/{kind}/list.json", type,
                kind));
    }

    @Override
    public <T extends AbstractSchemaTO> T read(String kind, String schemaName, Class<T> type) {
        String schemaType = getSchemaType(type);
        return restTemplate.getForObject(baseUrl + schemaType + "/{kind}/read/{name}.json", type, kind,
                schemaName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractSchemaTO> T update(String kind, String schemaName, T schemaTO) {
        String schemaType = getSchemaType(schemaTO.getClass());
        return (T) restTemplate.postForObject(baseUrl + schemaType + "/{kind}/update", schemaTO,
                schemaTO.getClass(), kind);
    }

    private String getSchemaType(Class<? extends AbstractSchemaTO> type) {
        return (type.isAssignableFrom(SchemaTO.class))
                ? "schema"
                : (type.isAssignableFrom(DerivedSchemaTO.class))
                        ? "derivedSchema"
                        : (type.isAssignableFrom(VirtualSchemaTO.class))
                                ? "virtualSchema"
                                : "";
    }

    private <T extends AbstractSchemaTO> String getSchemaTypeArray(Class<T[]> type) {
        return (type.isAssignableFrom(SchemaTO[].class))
                ? "schema"
                : (type.isAssignableFrom(DerivedSchemaTO[].class))
                        ? "derivedSchema"
                        : (type.isAssignableFrom(VirtualSchemaTO[].class))
                                ? "virtualSchema"
                                : "";
    }
}
