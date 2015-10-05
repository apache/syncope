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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.enduser.model.SchemaResponse;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaResource.class);

    private static final long serialVersionUID = 6453101466981543020L;

    private final AnyTypeService anyTypeService;

    private final AnyTypeClassService anyTypeClassService;

    private final SchemaService schemaService;

    public SchemaResource() {
        anyTypeService = getService(AnyTypeService.class);
        anyTypeClassService = getService(AnyTypeClassService.class);
        schemaService = getService(SchemaService.class);
    }

    @Override
    protected AbstractResource.ResourceResponse newResourceResponse(final IResource.Attributes attributes) {

        AbstractResource.ResourceResponse response = new AbstractResource.ResourceResponse();

        int responseStatus = 200;

        try {

            final AnyTypeTO anyTypeUserTO = anyTypeService.read(AnyTypeKind.USER.name());

            final List<PlainSchemaTO> plainSchemas = new ArrayList<>();
            final List<DerSchemaTO> derSchemas = new ArrayList<>();
            final List<VirSchemaTO> virSchemas = new ArrayList<>();

            // read all USER type schemas
            for (String clazz : anyTypeUserTO.getClasses()) {
                plainSchemas.addAll(getSchemaTOs(anyTypeClassService.read(clazz).getPlainSchemas(), SchemaType.PLAIN,
                        PlainSchemaTO.class));
                derSchemas.addAll(getSchemaTOs(anyTypeClassService.read(clazz).getDerSchemas(), SchemaType.DERIVED,
                        DerSchemaTO.class));
                virSchemas.addAll(getSchemaTOs(anyTypeClassService.read(clazz).getVirSchemas(), SchemaType.VIRTUAL,
                        VirSchemaTO.class));
            }

            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    attributes.getResponse().write(POJOHelper.serialize(new SchemaResponse().
                            plainSchemas(plainSchemas).
                            derSchemas(derSchemas).
                            virSchemas(virSchemas)));
                }
            });

        } catch (Exception e) {
            LOG.error("Error retrieving " + AnyTypeKind.USER.name() + " class schemas", e);
            responseStatus = 400;
        }

        response.setStatusCode(responseStatus);
        return response;
    }

    private <T extends AbstractSchemaTO> List<T> getSchemaTOs(final List<String> schemaNames,
            final SchemaType schemaType, final Class<T> type) {

        List<T> schemaTOs = new ArrayList<>();

        for (String schemaName : schemaNames) {
            schemaTOs.add(type.cast(schemaService.read(schemaType, schemaName)));
        }

        return schemaTOs;
    }

}
