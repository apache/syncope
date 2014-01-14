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
package org.apache.syncope.common.services;

import java.util.List;
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
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;

/**
 * REST operations for attribute schemas.
 */
@Path("schemas/{kind}/{type}")
public interface SchemaService extends JAXRSService {

    /**
     * Returns schema matching the given kind, type and name.
     *
     * @param <T> actual SchemaTO
     * @param attrType kind for schemas to be read
     * @param schemaType type for schemas to be read
     * @param schemaName name of schema to be read
     * @return schema matching the given kind, type and name
     */
    @GET
    @Path("{name}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractSchemaTO> T read(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, @PathParam("name") String schemaName);

    /**
     * Returns a list of schemas with matching kind and type.
     *
     * @param <T> actual SchemaTO
     * @param attrType kind for schemas to be listed
     * @param schemaType type for schemas to be listed
     * @return list of schemas with matching kind and type
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractSchemaTO> List<T> list(
            @PathParam("kind") AttributableType attrType, @PathParam("type") SchemaType schemaType);

    /**
     * Creates a new schema.
     *
     * @param <T> actual SchemaTO
     * @param attrType kind for schema to be created
     * @param schemaType type for schema to be created
     * @param schemaTO schema to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created schema
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE, value = "Featuring <tt>Location</tt> header of created schema")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractSchemaTO> Response create(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, T schemaTO);

    /**
     * Updates the schema matching the given kind, type and name.
     *
     * @param <T> actual SchemaTO
     * @param attrType kind for schemas to be updated
     * @param schemaType type for schemas to be updated
     * @param schemaName name of schema to be updated
     * @param schemaTO updated schema to be stored
     */
    @PUT
    @Path("{name}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractSchemaTO> void update(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, @PathParam("name") String schemaName, T schemaTO);

    /**
     * Deletes the schema matching the given kind, type and name.
     *
     * @param attrType kind for schema to be deleted
     * @param schemaType type for schema to be deleted
     * @param schemaName name of schema to be deleted
     */
    @DELETE
    @Path("{name}")
    void delete(@PathParam("kind") AttributableType attrType, @PathParam("type") SchemaType schemaType,
            @PathParam("name") String schemaName);
}
