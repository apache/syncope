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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.model.SchemaResponse;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "schemas", path = "/api/schemas")
public class SchemaResource extends BaseResource {

    private static final long serialVersionUID = 6453101466981543020L;

    @Override
    protected AbstractResource.ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        LOG.debug("Search all {} any type kind related schemas", AnyTypeKind.USER.name());

        AbstractResource.ResourceResponse response = new AbstractResource.ResourceResponse();

        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            List<String> classes = Collections.emptyList();

            final String groupParam = attributes.getParameters().get("group").toString();
            if (groupParam != null) {
                PagedResult<GroupTO> groups = SyncopeEnduserSession.get().getService(GroupService.class).search(
                        new AnyQuery.Builder().realm("/").page(1).size(1000).build());
                GroupTO group = IterableUtils.find(groups.getResult(), new Predicate<GroupTO>() {

                    @Override
                    public boolean evaluate(final GroupTO item) {
                        return groupParam.equals(item.getName());
                    }
                });

                if (group != null && group.getTypeExtension(AnyTypeKind.USER.name()) != null) {
                    classes = group.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses();
                }
            } else {
                String anyTypeClass = attributes.getParameters().get("anyTypeClass").toString();
                if (anyTypeClass != null) {
                    classes = Collections.singletonList(anyTypeClass);
                } else {
                    AnyTypeTO anyTypeUserTO = SyncopeEnduserSession.get().getService(AnyTypeService.class).
                            read(AnyTypeKind.USER.name());
                    classes = anyTypeUserTO.getClasses();
                }
            }

            SchemaService schemaService = SyncopeEnduserSession.get().getService(SchemaService.class);
            final List<AbstractSchemaTO> plainSchemas = classes.isEmpty()
                    ? Collections.<AbstractSchemaTO>emptyList()
                    : schemaService.list(
                            new SchemaQuery.Builder().type(SchemaType.PLAIN).anyTypeClasses(classes).build());
            final List<AbstractSchemaTO> derSchemas = classes.isEmpty()
                    ? Collections.<AbstractSchemaTO>emptyList()
                    : schemaService.list(
                            new SchemaQuery.Builder().type(SchemaType.DERIVED).anyTypeClasses(classes).build());
            final List<AbstractSchemaTO> virSchemas = classes.isEmpty()
                    ? Collections.<AbstractSchemaTO>emptyList()
                    : schemaService.list(
                            new SchemaQuery.Builder().type(SchemaType.VIRTUAL).anyTypeClasses(classes).build());

            if (groupParam != null) {
                for (AbstractSchemaTO schema : plainSchemas) {
                    schema.setKey(groupParam + "#" + schema.getKey());
                }
                for (AbstractSchemaTO schema : derSchemas) {
                    schema.setKey(groupParam + "#" + schema.getKey());
                }
                for (AbstractSchemaTO schema : virSchemas) {
                    schema.setKey(groupParam + "#" + schema.getKey());
                }
            }

            response.setTextEncoding(StandardCharsets.UTF_8.name());

            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    attributes.getResponse().write(MAPPER.writeValueAsString(new SchemaResponse().
                            plainSchemas(plainSchemas).
                            derSchemas(derSchemas).
                            virSchemas(virSchemas)));
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving {} any type kind related schemas", AnyTypeKind.USER.name(), e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }
        return response;
    }

}
