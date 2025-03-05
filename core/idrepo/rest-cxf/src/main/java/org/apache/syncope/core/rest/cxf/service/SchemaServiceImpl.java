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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.core.logic.SchemaLogic;
import org.identityconnectors.common.CollectionUtil;

public class SchemaServiceImpl extends AbstractService implements SchemaService {

    protected final SchemaLogic logic;

    public SchemaServiceImpl(final SchemaLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response create(final SchemaType schemaType, final SchemaTO schemaTO) {
        SchemaTO created = logic.create(schemaType, schemaTO);

        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public void delete(final SchemaType schemaType, final String key) {
        logic.delete(schemaType, key);
    }

    @Override
    public <T extends SchemaTO> List<T> search(final SchemaQuery query) {
        String keyword = query.getKeyword() == null ? null : query.getKeyword().replace('*', '%');
        return logic.search(query.getType(), CollectionUtil.nullAsEmpty(query.getAnyTypeClasses()), keyword);
    }

    @Override
    public <T extends SchemaTO> T read(final SchemaType schemaType, final String key) {
        return logic.read(schemaType, key);
    }

    @Override
    public void update(final SchemaType schemaType, final SchemaTO schemaTO) {
        logic.update(schemaType, schemaTO);
    }

    @Override
    public Attr getDropdownValues(final String key, final AnyTO anyTO) {
        return logic.getDropdownValues(key, anyTO);
    }
}
