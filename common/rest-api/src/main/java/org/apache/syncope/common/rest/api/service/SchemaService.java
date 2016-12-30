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
package org.apache.syncope.common.rest.api.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;

/**
 * REST operations for attribute schemas.
 */
@Path("schemas")
public interface SchemaService extends JAXRSService {

    /**
     * Returns a list of schemas matching the given query.
     *
     * @param <T> actual SchemaTO
     * @param query query conditions
     * @return list of schemas with matching type, for the given anyTypeClass if provided
     */
    @GET
    @Path("{type}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractSchemaTO> List<T> list(@BeanParam SchemaQuery query);

    /**
     * Returns schema matching the given type and key.
     *
     * @param <T> actual SchemaTO
     * @param type type for schemas to be read
     * @param key name of schema to be read
     * @return schema matching the given type and name
     */
    @GET
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractSchemaTO> T read(
            @NotNull @PathParam("type") SchemaType type, @NotNull @PathParam("key") String key);

    /**
     * Creates a new schema.
     *
     * @param type type for schema to be created
     * @param schemaTO schema to be created
     * @return Response object featuring Location header of created schema
     */
    @POST
    @Path("{type}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull @PathParam("type") SchemaType type, @NotNull AbstractSchemaTO schemaTO);

    /**
     * Updates the schema matching the given type and key.
     *
     * @param type type for schemas to be updated
     * @param schemaTO updated schema to be stored
     */
    @PUT
    @Path("{type}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull @PathParam("type") SchemaType type, @NotNull AbstractSchemaTO schemaTO);

    /**
     * Deletes the schema matching the given type and key.
     *
     * @param type type for schema to be deleted
     * @param key name of schema to be deleted
     */
    @DELETE
    @Path("{type}/{key}")
    void delete(@NotNull @PathParam("type") SchemaType type, @NotNull @PathParam("key") String key);
}
