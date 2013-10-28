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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.core.rest.controller.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchemaServiceImpl extends AbstractServiceImpl implements SchemaService, ContextAware {

    @Autowired
    private SchemaController controller;

    @Override
    public <T extends AbstractSchemaTO> Response create(final AttributableType attrType, final SchemaType schemaType,
            final T schemaTO) {

        T response = controller.create(attrType, schemaType, schemaTO);

        URI location = uriInfo.getAbsolutePathBuilder().path(response.getName()).build();
        return Response.created(location)
                .header(SyncopeConstants.REST_RESOURCE_ID_HEADER, response.getName())
                .build();
    }

    @Override
    public void delete(final AttributableType attrType, final SchemaType schemaType, final String schemaName) {
        controller.delete(attrType, schemaType, schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> List<T> list(final AttributableType attrType, final SchemaType schemaType) {
        return controller.list(attrType, schemaType);
    }

    @Override
    public <T extends AbstractSchemaTO> T read(final AttributableType attrType, final SchemaType schemaType,
            final String schemaName) {

        return controller.read(attrType, schemaType, schemaName);
    }

    @Override
    public <T extends AbstractSchemaTO> void update(final AttributableType attrType, final SchemaType schemaType,
            final String schemaName, final T schemaTO) {

        controller.update(attrType, schemaType, schemaName, schemaTO);
    }
}
