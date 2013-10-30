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
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;

@Path("schemas/{kind}/{type}")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface SchemaService {

    /**
     * @param <T> actual SchemaTO
     * @param attrType Kind for schema to be created
     * @param schemaType Type for schema to be created
     * @param schemaTO Schema to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created schema
     */
    @POST
    <T extends AbstractSchemaTO> Response create(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, T schemaTO);

    /**
     * @param attrType Kind for schema to be deleted
     * @param schemaType Type for schema to be deleted
     * @param schemaName Name of schema to be deleted
     */
    @DELETE
    @Path("{name}")
    void delete(@PathParam("kind") AttributableType attrType, @PathParam("type") SchemaType schemaType,
            @PathParam("name") String schemaName);

    /**
     * @param <T> actual SchemaTO
     * @param attrType Kind for schemas to be listed
     * @param schemaType Type for schemas to be listed
     * @return List of schemas with matching kind and type
     */
    @GET
    <T extends AbstractSchemaTO> List<T> list(
            @PathParam("kind") AttributableType attrType, @PathParam("type") SchemaType schemaType);

    /**
     * @param <T> actual SchemaTO
     * @param attrType Kind for schemas to be read
     * @param schemaType Type for schemas to be read
     * @param schemaName Name of schema to be read
     * @return Returns schema with matching name, kind and type
     */
    @GET
    @Path("{name}")
    <T extends AbstractSchemaTO> T read(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, @PathParam("name") String schemaName);

    /**
     * @param <T> actual SchemaTO
     * @param attrType Kind for schemas to be updated
     * @param schemaType Type for schemas to be updated
     * @param schemaName Name of schema to be updated
     * @param schemaTO New schema to be stored
     */
    @PUT
    @Path("{name}")
    <T extends AbstractSchemaTO> void update(@PathParam("kind") AttributableType attrType,
            @PathParam("type") SchemaType schemaType, @PathParam("name") String schemaName, T schemaTO);
}
