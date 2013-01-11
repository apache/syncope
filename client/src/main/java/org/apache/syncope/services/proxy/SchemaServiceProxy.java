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

@SuppressWarnings("unchecked")
public class SchemaServiceProxy extends SpringServiceProxy implements SchemaService {

    public SchemaServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public <T extends AbstractSchemaTO> T create(String kind, SchemaKind type, T schemaTO) {
        return (T) restTemplate.postForObject(baseUrl + type + "/{kind}/create", schemaTO, getTOClass(type), kind);
    }

    @Override
    public <T extends AbstractSchemaTO> T delete(String kind, SchemaKind type, String schemaName) {
        return (T) restTemplate.getForObject(baseUrl + type + "/{kind}/delete/{name}.json", getTOClass(type), kind,
                schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> List<T> list(String kind, SchemaKind type) {
        switch (type) {
        case NORMAL:
            return (List<T>) Arrays.asList(restTemplate.getForObject(baseUrl + type + "/{kind}/list.json",
                    SchemaTO[].class, kind));
        case DERIVED:
            return (List<T>) Arrays.asList(restTemplate.getForObject(baseUrl + type + "/{kind}/list.json",
                    DerivedSchemaTO[].class, kind));
        case VIRTUAL:
            return (List<T>) Arrays.asList(restTemplate.getForObject(baseUrl + type + "/{kind}/list.json",
                    VirtualSchemaTO[].class, kind));
        default:
            throw new IllegalArgumentException("SchemaType is not supported.");
        }
    }

    private Class<? extends AbstractSchemaTO> getTOClass(SchemaKind type) {
        switch (type) {
        case NORMAL:
            return SchemaTO.class;
        case DERIVED:
            return DerivedSchemaTO.class;
        case VIRTUAL:
            return VirtualSchemaTO.class;
        default:
            throw new IllegalArgumentException("SchemaType is not supported.");
        }
    }

    @Override
    public <T extends AbstractSchemaTO> T read(String kind, SchemaKind type, String schemaName) {
        return (T) restTemplate.getForObject(baseUrl + type + "/{kind}/read/{name}.json", getTOClass(type), kind,
                schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> T update(String kind, SchemaKind type, String schemaName, T schemaTO) {
        return (T) restTemplate.postForObject(baseUrl + type + "/{kind}/update", schemaTO, getTOClass(type), kind);
    }
}
