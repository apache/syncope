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
package org.apache.syncope.client.services.proxy;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.to.DerivedSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.VirtualSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("unchecked")
public class SchemaServiceProxy extends SpringServiceProxy implements SchemaService {

    public SchemaServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public <T extends AbstractSchemaTO> Response create(final AttributableType kind, final SchemaType type,
            final T schemaTO) {
        AbstractSchemaTO schema = getRestTemplate().postForObject(baseUrl + type.toSpringURL() + "/{kind}/create",
                schemaTO, getTOClass(type), kind);

        try {
            URI location = URI.create(baseUrl
                    + type.toSpringURL() + "/" + kind + "/read/"
                    + URLEncoder.encode(schema.getName(), SyncopeConstants.DEFAULT_ENCODING)
                    + ".json");
            return Response.created(location)
                    .header(SyncopeConstants.REST_HEADER_ID, schema.getName())
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public void delete(final AttributableType kind, final SchemaType type, final String schemaName) {

        getRestTemplate().getForObject(baseUrl + type.toSpringURL() + "/{kind}/delete/{name}.json", getTOClass(type),
                kind, schemaName);
    }

    @Override
    public List<? extends AbstractSchemaTO> list(final AttributableType kind, final SchemaType type) {
        switch (type) {
            case NORMAL:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + type.toSpringURL() + "/{kind}/list.json",
                        SchemaTO[].class, kind));

            case DERIVED:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + type.toSpringURL() + "/{kind}/list.json",
                        DerivedSchemaTO[].class, kind));

            case VIRTUAL:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + type.toSpringURL() + "/{kind}/list.json",
                        VirtualSchemaTO[].class, kind));

            default:
                throw new IllegalArgumentException("SchemaType is not supported.");
        }
    }

    @Override
    public <T extends AbstractSchemaTO> T read(final AttributableType kind, final SchemaType type,
            final String schemaName) {

        return (T) getRestTemplate().getForObject(baseUrl + type.toSpringURL() + "/{kind}/read/{name}.json",
                getTOClass(type), kind, schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> void update(final AttributableType kind, final SchemaType type,
            final String schemaName, final T schemaTO) {

        getRestTemplate().postForObject(baseUrl + type.toSpringURL() + "/{kind}/update", schemaTO, getTOClass(type),
                kind);
    }

    private Class<? extends AbstractSchemaTO> getTOClass(final SchemaType type) {
        switch (type) {
            case NORMAL:
                return SchemaTO.class;

            case DERIVED:
                return DerivedSchemaTO.class;

            case VIRTUAL:
                return VirtualSchemaTO.class;

            default:
                throw new IllegalArgumentException("SchemaType is not supported: " + type);
        }
    }
}
